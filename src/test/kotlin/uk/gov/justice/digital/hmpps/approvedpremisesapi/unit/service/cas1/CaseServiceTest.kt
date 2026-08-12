package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CasePersistenceService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TierFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class CaseServiceTest {

  @MockK
  private lateinit var mockCaseRepository: CaseRepository

  @MockK
  private lateinit var mockCasePersistenceService: CasePersistenceService

  @MockK
  private lateinit var mockHMPPSTierApiClient: HMPPSTierApiClient

  @MockK
  private lateinit var mockApDeliusContextApiClient: ApDeliusContextApiClient

  @RelaxedMockK
  private lateinit var mockFeatureFlagService: FeatureFlagService

  @RelaxedMockK
  private lateinit var mockSentryService: SentryService

  @InjectMockKs
  private lateinit var service: CaseService

  @Nested
  inner class EnsureCaseExists {
    @Test
    fun `existing case, update`() {
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns true
      every { mockFeatureFlagService.getBooleanFlag("use-tier-v3") } returns false

      val crn = "CRN123"
      val now = OffsetDateTime.now()
      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withName("old name")
        .withNomsNumber("old noms")
        .withTierV2(TierFactory().withTierScore("oldv2").produce())
        .withTierV3(TierFactory().withTierScore("oldv3").produce())
        .withCreatedAt(now)
        .produce()

      val caseSummary = CaseSummaryFactory()
        .withCrn(crn)
        .withName(NameFactory().withForename("John").withSurname("Smith").produce())
        .withNomsId("NOMS123")
        .produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(listOf(caseSummary)),
      )

      val tierV2CalculationId = UUID.randomUUID()
      val tierV2 = Tier(
        tierScore = "tierv2score",
        calculationId = tierV2CalculationId,
        calculationDate = now.toLocalDateTime(),
        changeReason = "v2Reason",
      )
      every { mockHMPPSTierApiClient.getTier(crn, TierVersion.V2) } returns ClientResult.Success(
        body = tierV2,
        status = HttpStatus.OK,
      )

      val tierV3CalculationId = UUID.randomUUID()
      val tierV3 = Tier(
        tierScore = "tierv3score",
        calculationId = tierV3CalculationId,
        calculationDate = LocalDateTime.now(),
        changeReason = "v3Reason",
      )
      every { mockHMPPSTierApiClient.getTier(crn, TierVersion.V3) } returns ClientResult.Success(
        body = tierV3,
        status = HttpStatus.OK,
      )

      val updatedCaseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withName("JOHN SMITH")
        .withNomsNumber("NOMS123")
        .withTierV2(TierFactory().withTierScore("tierv2score").withCalculationId(tierV2CalculationId).withCalculationDate(tierV2.calculationDate).produce())
        .withTierV3(TierFactory().withTierScore("tierv3score").withCalculationId(tierV3CalculationId).withCalculationDate(tierV3.calculationDate).produce())
        .withCreatedAt(now)
        .produce()

      every { mockCasePersistenceService.getCase(crn) } returns caseEntity
      every { mockCasePersistenceService.updateIfExist(crn, any(), any()) } returns updatedCaseEntity

      val result = service.ensureCaseExists(crn)

      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.name).isEqualTo("JOHN SMITH")
      assertThat(result.nomsNumber).isEqualTo("NOMS123")
      assertThat(result.tier?.tierScore).isEqualTo("tierv2score")
      assertThat(result.tier?.calculationDate).isEqualTo(now.toLocalDateTime())
      assertThat(result.tier?.version).isEqualTo(TierVersionDto.V2)
      assertThat(result.tier?.provisional).isNull()

      verify {
        mockCasePersistenceService.updateIfExist(
          crn,
          caseSummary,
          match {
            it.v2?.tierScore == "tierv2score" &&
              it.v2.calculationId == tierV2CalculationId &&
              it.v3?.tierScore == "tierv3score" &&
              it.v3.calculationId == tierV3CalculationId
          },
        )
      }
    }

    @Test
    fun `existing case, update, leaving tiers as current value if getting tiers fails`() {
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns true
      every { mockFeatureFlagService.getBooleanFlag("use-tier-v3") } returns false

      val crn = "CRN123"
      val now = OffsetDateTime.now()
      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withTierV2(TierFactory().withTierScore("oldv2").withCalculationDate(now.toLocalDateTime()).produce())
        .withTierV3(TierFactory().withTierScore("oldv3").produce())
        .produce()

      val caseSummary = CaseSummaryFactory().withCrn(crn).withName(Name("JOHN", "SMITH")).withNomsId("NOMS123").produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(listOf(caseSummary)),
      )

      every { mockHMPPSTierApiClient.getTier(crn, any()) } returns ClientResult.Failure.StatusCode(
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        method = HttpMethod.GET,
        path = "/crn/crn123/tier",
        body = null,
      )

      val updatedCaseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withName("JOHN SMITH")
        .withNomsNumber("NOMS123")
        .withTierV2(TierFactory().withTierScore("oldv2").withCalculationDate(now.toLocalDateTime()).produce())
        .withTierV3(TierFactory().withTierScore("oldv3").produce())
        .produce()

      every { mockCasePersistenceService.getCase(crn) } returns caseEntity
      every { mockCasePersistenceService.updateIfExist(crn, any(), any()) } returns updatedCaseEntity

      val result = service.ensureCaseExists(crn)
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.name).isEqualTo("JOHN SMITH")
      assertThat(result.nomsNumber).isEqualTo("NOMS123")
      assertThat(result.tier?.tierScore).isEqualTo("oldv2")
      assertThat(result.tier?.calculationDate).isEqualTo(now.toLocalDateTime())
      assertThat(result.tier?.version).isEqualTo(TierVersionDto.V2)
      assertThat(result.tier?.provisional).isNull()

      verify {
        mockCasePersistenceService.updateIfExist(
          crn,
          caseSummary,
          match {
            it.v2 == null && it.v3 == null
          },
        )
      }
    }

    @Test
    fun `no existing case, create new`() {
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns true
      every { mockFeatureFlagService.getBooleanFlag("use-tier-v3") } returns false

      val crn = "CRN123"
      val now = OffsetDateTime.now()

      val caseSummary = CaseSummaryFactory()
        .withCrn(crn)
        .withName(NameFactory().withForename("John").withSurname("Smith").produce())
        .withNomsId("NOMS123")
        .produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(listOf(caseSummary)),
      )

      val tierV2CalculationId = UUID.randomUUID()
      val tierV2 = Tier(
        tierScore = "tierv2score",
        calculationId = tierV2CalculationId,
        calculationDate = now.toLocalDateTime(),
        changeReason = "v2Reason",
      )
      every { mockHMPPSTierApiClient.getTier(crn, TierVersion.V2) } returns ClientResult.Success(
        body = tierV2,
        status = HttpStatus.OK,
      )

      val tierV3CalculationId = UUID.randomUUID()
      val tierV3 = Tier(
        tierScore = "tierv3score",
        calculationId = tierV3CalculationId,
        calculationDate = LocalDateTime.now(),
        changeReason = "v3Reason",
      )
      every { mockHMPPSTierApiClient.getTier(crn, TierVersion.V3) } returns ClientResult.Success(
        body = tierV3,
        status = HttpStatus.OK,
      )

      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withName("JOHN SMITH")
        .withNomsNumber("NOMS123")
        .withTierV2(TierFactory().withTierScore("tierv2score").withCalculationId(tierV2CalculationId).withCalculationDate(tierV2.calculationDate).produce())
        .withTierV3(TierFactory().withTierScore("tierv3score").withCalculationId(tierV3CalculationId).withCalculationDate(tierV3.calculationDate).produce())
        .produce()

      every { mockCasePersistenceService.getCase(crn) } returns null
      every { mockCasePersistenceService.updateIfExist(crn, any(), any()) } returns null
      every { mockCasePersistenceService.createCase(any(), any()) } returns caseEntity

      val result = service.ensureCaseExists(crn)
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.tier?.calculationDate).isEqualTo(now.toLocalDateTime())
      assertThat(result.tier?.version).isEqualTo(TierVersionDto.V2)
      assertThat(result.tier?.provisional).isNull()

      verify {
        mockCasePersistenceService.createCase(
          caseSummary,
          match {
            it.v2?.tierScore == "tierv2score" &&
              it.v3?.tierScore == "tierv3score"
          },
        )
      }
    }

    @Test
    fun `no existing case, returns case created by another transaction when insert hits crn unique constraint`() {
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns false
      every { mockFeatureFlagService.getBooleanFlag("use-tier-v3") } returns false

      val crn = "CRN123"
      val existingCase = CaseEntityFactory()
        .withCrn(crn)
        .withName("JOHN SMITH")
        .withNomsNumber("NOMS123")
        .produce()

      val caseSummary = CaseSummaryFactory()
        .withCrn(crn)
        .withName(NameFactory().withForename("John").withSurname("Smith").produce())
        .withNomsId("NOMS123")
        .produce()

      every { mockCaseRepository.findByCrn(crn) } returns existingCase
      every { mockCasePersistenceService.getCase(crn) } returnsMany listOf(null, existingCase)
      every { mockCasePersistenceService.updateIfExist(crn, any(), any()) } returns null
      every { mockCasePersistenceService.createCase(any(), any()) } throws DataIntegrityViolationException(
        "duplicate case crn",
        ConstraintViolationException(
          "duplicate case crn",
          SQLException("duplicate key value violates unique constraint", "23505"),
          "cas1_offenders_crn",
        ),
      )

      every { mockApDeliusContextApiClient.getCaseSummaries(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(listOf(caseSummary)),
      )
      every { mockHMPPSTierApiClient.getTier(crn, TierVersion.V2) } returns ClientResult.Failure.StatusCode(
        status = HttpStatus.NOT_FOUND,
        method = HttpMethod.GET,
        path = "/crn/crn123/tier",
        body = null,
      )

      val result = try {
        service.ensureCaseExists(crn)
      } catch (e: DataIntegrityViolationException) {
        if (e.message == "duplicate case crn") {
          service.getCase(crn)!!
        } else {
          throw e
        }
      }

      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.name).isEqualTo("JOHN SMITH")
      assertThat(result.nomsNumber).isEqualTo("NOMS123")
      verify { mockCasePersistenceService.getCase(crn) }
    }

    @Test
    fun `no existing case, create new, setting tiers to null if get tiers errors`() {
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns true
      every { mockFeatureFlagService.getBooleanFlag("use-tier-v3") } returns false

      val crn = "CRN123"

      val caseSummary = CaseSummaryFactory().withCrn(crn).withName(Name("JOHN", "SMITH")).withNomsId("NOMS123").produce()
      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withName("JOHN SMITH")
        .withNomsNumber("NOMS123")
        .withTierV2(null)
        .withTierV3(null)
        .produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(crn)) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(listOf(caseSummary)),
      )
      every { mockHMPPSTierApiClient.getTier(crn, any()) } returns ClientResult.Failure.StatusCode(
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        method = HttpMethod.GET,
        path = "/crn/crn123/tier",
        body = null,
      )

      every { mockCasePersistenceService.getCase(crn) } returns null
      every { mockCasePersistenceService.updateIfExist(crn, any(), any()) } returns null
      every { mockCasePersistenceService.createCase(any(), any()) } returns caseEntity

      val result = service.ensureCaseExists(crn)

      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.name).isEqualTo("JOHN SMITH")
      assertThat(result.nomsNumber).isEqualTo("NOMS123")
      assertThat(result.tier).isNull()

      verify {
        mockCasePersistenceService.createCase(
          caseSummary,
          match {
            it.v2 == null && it.v3 == null
          },
        )
      }
    }

    @Test
    fun `no existing case, create new, dont include v3 tier if flag is disabled`() {
      val crn = "CRN123"
      val now = OffsetDateTime.now()
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns false
      every { mockFeatureFlagService.getBooleanFlag("use-tier-v3") } returns false

      val caseSummary = CaseSummaryFactory().withCrn(crn).withName(Name("JOHN", "SMITH")).withNomsId("NOMS123").produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(crn)) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(listOf(caseSummary)),
      )
      val tierV2 = Tier(tierScore = "tier value", calculationId = UUID.randomUUID(), calculationDate = now.toLocalDateTime(), changeReason = "reason")
      every { mockHMPPSTierApiClient.getTier(crn, any()) } returns ClientResult.Success(
        body = tierV2,
        status = HttpStatus.OK,
      )

      every { mockCasePersistenceService.getCase(crn) } returns null
      every { mockCasePersistenceService.updateIfExist(crn, any(), any()) } returns null
      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withName("JOHN SMITH")
        .withNomsNumber("NOMS123")
        .withTierV2(TierFactory().withTierScore("tier value").withCalculationDate(tierV2.calculationDate).produce())
        .produce()
      every { mockCasePersistenceService.createCase(any(), any()) } returns caseEntity

      val result = service.ensureCaseExists(crn)

      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.name).isEqualTo("JOHN SMITH")
      assertThat(result.nomsNumber).isEqualTo("NOMS123")
      assertThat(result.tier?.tierScore).isEqualTo("tier value")
      assertThat(result.tier?.calculationDate).isEqualTo(now.toLocalDateTime())
      assertThat(result.tier?.version).isEqualTo(TierVersionDto.V2)
      assertThat(result.tier?.provisional).isNull()

      verify(exactly = 0) { mockHMPPSTierApiClient.getTier(crn, TierVersion.V3) }
      verify {
        mockCasePersistenceService.createCase(
          caseSummary,
          match {
            it.v2?.tierScore == "tier value" && it.v3 == null
          },
        )
      }
    }

    @Test
    fun `should throw NotFoundProblem when no case summary found`() {
      val crn = "CRN123"

      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(crn)) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(
          emptyList(),
        ),
      )

      assertThatThrownBy {
        service.ensureCaseExists(crn)
      }.isInstanceOf(NotFoundProblem::class.java)
        .hasMessageContaining(crn)
        .hasMessageContaining("Offender")
    }

    @Test
    fun `should uppercase CRN when ensuring case exists`() {
      val crn = "crn123"
      val uppercasedCrn = "CRN123"

      val caseSummary = CaseSummaryFactory()
        .withCrn(uppercasedCrn)
        .withName(NameFactory().withForename("John").withSurname("Smith").produce())
        .withNomsId("NOMS123")
        .produce()

      every { mockCasePersistenceService.getCase(uppercasedCrn) } returns null
      every { mockCasePersistenceService.updateIfExist(uppercasedCrn, any(), any()) } returns null
      val caseEntity = CaseEntityFactory().withCrn(uppercasedCrn).withName("JOHN SMITH").produce()
      every { mockCasePersistenceService.createCase(any(), any()) } returns caseEntity

      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(uppercasedCrn)) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(listOf(caseSummary)),
      )

      every { mockHMPPSTierApiClient.getTier(uppercasedCrn, any()) } returns ClientResult.Failure.Other(
        HttpMethod.GET,
        "/tier",
        RuntimeException("Fail"),
      )

      service.ensureCaseExists(crn)

      verify { mockCasePersistenceService.updateIfExist(uppercasedCrn, any(), any()) }
      verify { mockCasePersistenceService.createCase(any(), any()) }
      verify { mockApDeliusContextApiClient.getCaseSummaries(listOf(uppercasedCrn)) }
      verify { mockHMPPSTierApiClient.getTier(uppercasedCrn, TierVersion.V2) }
    }
  }

  @Nested
  inner class ReviseTier {
    @Test
    fun `should update tierV2 and tierV3 when flag is enabled`() {
      val crn = "CRN123"
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns true

      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .produce()

      every { mockCaseRepository.findByCrn(crn) } returns caseEntity
      every { mockCaseRepository.save(any()) } returns caseEntity
      every { mockHMPPSTierApiClient.getTier(crn, TierVersion.V2) } returns ClientResult.Success(
        body = Tier(tierScore = "V2_NEW", calculationId = UUID.randomUUID(), calculationDate = LocalDateTime.now(), changeReason = "reason"),
        status = HttpStatus.OK,
      )
      every { mockHMPPSTierApiClient.getTier(crn, TierVersion.V3) } returns ClientResult.Success(
        body = Tier(tierScore = "V3_NEW", calculationId = UUID.randomUUID(), calculationDate = LocalDateTime.now(), changeReason = "reason"),
        status = HttpStatus.OK,
      )

      val result = service.reviseTier(crn)

      assertThat(result).isTrue()
      verify { mockHMPPSTierApiClient.getTier(crn, TierVersion.V2) }
      verify { mockHMPPSTierApiClient.getTier(crn, TierVersion.V3) }
      verify {
        mockCaseRepository.save(
          match {
            it.tierV2?.tierScore == "V2_NEW" && it.tierV3?.tierScore == "V3_NEW"
          },
        )
      }
    }

    @Test
    fun `should only update tierV2 when flag is disabled`() {
      val crn = "CRN123"
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns false

      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .produce()

      every { mockCaseRepository.findByCrn(crn) } returns caseEntity
      every { mockCaseRepository.save(any()) } returns caseEntity
      every { mockHMPPSTierApiClient.getTier(crn, any()) } returns ClientResult.Success(
        body = Tier(tierScore = "V2_NEW", calculationId = UUID.randomUUID(), calculationDate = LocalDateTime.now(), changeReason = "reason"),
        status = HttpStatus.OK,
      )

      val result = service.reviseTier(crn)

      assertThat(result).isTrue()
      verify { mockHMPPSTierApiClient.getTier(crn, TierVersion.V2) }
      verify(exactly = 0) { mockHMPPSTierApiClient.getTier(any(), TierVersion.V3) }
      verify {
        mockCaseRepository.save(
          match {
            it.tierV2?.tierScore == "V2_NEW"
          },
        )
      }
    }

    @Test
    fun `should return false if case does not exist`() {
      val crn = "CRN123"
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns false
      every { mockCaseRepository.findByCrn(crn) } returns null

      val result = service.reviseTier(crn)

      assertThat(result).isFalse()
    }

    @Test
    fun `should throw exception if fetch fails`() {
      val crn = "CRN123"
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns false
      val caseEntity = CaseEntityFactory().withCrn(crn).produce()

      every { mockCaseRepository.findByCrn(crn) } returns caseEntity
      every { mockHMPPSTierApiClient.getTier(crn, any()) } returns ClientResult.Failure.StatusCode(
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        method = HttpMethod.GET,
        path = "/crn/crn123/tier",
        body = null,
      )

      assertThatThrownBy {
        service.reviseTier(crn)
      }.isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `should uppercase CRN when revising tier`() {
      val crn = "crn123"
      val uppercasedCrn = "CRN123"
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns false
      val caseEntity = CaseEntityFactory().withCrn(uppercasedCrn).produce()

      every { mockCaseRepository.findByCrn(uppercasedCrn) } returns caseEntity
      every { mockHMPPSTierApiClient.getTier(uppercasedCrn, any()) } returns ClientResult.Failure.StatusCode(
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        method = HttpMethod.GET,
        path = "/crn/$uppercasedCrn/tier",
        body = null,
      )

      assertThatThrownBy {
        service.reviseTier(crn)
      }.isInstanceOf(RuntimeException::class.java)

      verify { mockCaseRepository.findByCrn(uppercasedCrn) }
      verify { mockHMPPSTierApiClient.getTier(uppercasedCrn, TierVersion.V2) }
    }
  }

  @Nested
  inner class GetCase {

    @Test
    fun `if case is missing, raise an alert`() {
      val crn = "CRN123"
      every { mockCaseRepository.findByCrn(crn) } returns null

      val result = service.getCase(crn)

      assertThat(result).isNull()

      val thrownExceptionSlot = slot<CaseService.CaseNotFound>()
      verify { mockSentryService.captureException(capture(thrownExceptionSlot)) }

      assertThat(thrownExceptionSlot.captured.message).isEqualTo("Case with CRN CRN123 not found")
    }

    @Test
    fun `if feature flag 'use-tier-v3' is false, should return tierV2`() {
      val crn = "CRN123"
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns true
      every { mockFeatureFlagService.getBooleanFlag("use-tier-v3") } returns false

      val tierV2 = TierFactory().withTierScore("V2").withVersion(TierVersion.V2).produce()
      val tierV3 = TierFactory().withTierScore("V3").withVersion(TierVersion.V3).produce()
      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withTierV2(tierV2)
        .withTierV3(tierV3)
        .produce()

      every { mockCaseRepository.findByCrn(crn) } returns caseEntity

      val result = service.getCase(crn)

      assertThat(result).isNotNull
      assertThat(result!!.tier?.tierScore).isEqualTo("V2")
    }

    @Test
    fun `if feature flag 'use-tier-v3' is true, should return tierV3`() {
      val crn = "CRN123"
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns true
      every { mockFeatureFlagService.getBooleanFlag("use-tier-v3") } returns true

      val tierV2 = TierFactory().withTierScore("V2").withVersion(TierVersion.V2).produce()
      val tierV3 = TierFactory().withTierScore("V3").withVersion(TierVersion.V3).produce()
      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withTierV2(tierV2)
        .withTierV3(tierV3)
        .produce()

      every { mockCaseRepository.findByCrn(crn) } returns caseEntity

      val result = service.getCase(crn)

      assertThat(result).isNotNull
      assertThat(result!!.tier?.tierScore).isEqualTo("V3")
    }

    @Test
    fun `should uppercase CRN when getting case`() {
      val crn = "crn123"
      val uppercasedCrn = "CRN123"

      every { mockCaseRepository.findByCrn(uppercasedCrn) } returns null

      service.getCase(crn)

      verify { mockCaseRepository.findByCrn(uppercasedCrn) }
      verify { mockSentryService.captureException(any<CaseService.CaseNotFound>()) }
    }
  }

  @Nested
  inner class GetCases {
    @Test
    fun `should return all cases for given CRNs, raising alerts for missing cases`() {
      val crn1 = "CRN123"
      val crn2 = "CRN456"
      val crn3 = "CRN789"

      val caseEntity1 = CaseEntityFactory().withCrn(crn1).produce()
      val caseEntity3 = CaseEntityFactory().withCrn(crn3).produce()

      every { mockCaseRepository.findByCrnIn(listOf(crn1, crn2, crn3)) } returns listOf(caseEntity1, caseEntity3)

      val result = service.getCases(listOf(crn1, crn2, crn3))

      assertThat(result).hasSize(2)
      assertThat(result.map { it.crn }).containsExactlyInAnyOrder(crn1, crn3)

      val thrownExceptionSlot = slot<CaseService.CaseNotFound>()
      verify { mockSentryService.captureException(capture(thrownExceptionSlot)) }

      assertThat(thrownExceptionSlot.captured.message).isEqualTo("Case with CRN CRN456 not found")
    }

    @Test
    fun `should uppercase CRNs when getting multiple cases`() {
      val crn1 = "crn123"
      val crn2 = "crn456"
      val uppercasedCrn1 = "CRN123"
      val uppercasedCrn2 = "CRN456"

      val caseEntity1 = CaseEntityFactory().withCrn(uppercasedCrn1).produce()

      every { mockCaseRepository.findByCrnIn(listOf(uppercasedCrn1, uppercasedCrn2)) } returns listOf(caseEntity1)

      val result = service.getCases(listOf(crn1, crn2))

      assertThat(result).hasSize(1)
      assertThat(result[0].crn).isEqualTo(uppercasedCrn1)

      verify { mockCaseRepository.findByCrnIn(listOf(uppercasedCrn1, uppercasedCrn2)) }
      verify { mockSentryService.captureException(any<CaseService.CaseNotFound>()) }
    }
  }
}
