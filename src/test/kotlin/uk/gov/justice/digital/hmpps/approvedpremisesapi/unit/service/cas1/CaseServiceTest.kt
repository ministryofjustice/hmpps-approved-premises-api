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
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CasePersistenceService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.TierService
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

  @MockK
  private lateinit var mockTierService: TierService

  @InjectMockKs
  private lateinit var service: CaseService

  @Nested
  inner class EnsureCaseExists {
    @Test
    fun `existing case, update`() {
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
      val tierV2 = TierFactory()
        .withTierScore("tierv2score")
        .withCalculationId(tierV2CalculationId)
        .withCalculationDate(now.toLocalDateTime())
        .withChangeReason("v2Reason")
        .withVersion(TierVersion.V2)
        .produce()
      every { mockTierService.fetchTierOrNull(crn, TierVersion.V2) } returns tierV2

      val tierV3CalculationId = UUID.randomUUID()
      val tierV3 = TierFactory()
        .withTierScore("tierv3score")
        .withCalculationId(tierV3CalculationId)
        .withCalculationDate(LocalDateTime.now())
        .withChangeReason("v3Reason")
        .withVersion(TierVersion.V3)
        .produce()
      every { mockTierService.fetchTierOrNull(crn, TierVersion.V3) } returns tierV3

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

      every { mockTierService.fetchTierOrNull(crn, any()) } returns null

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
      val tierV2 = TierFactory()
        .withTierScore("tierv2score")
        .withCalculationId(tierV2CalculationId)
        .withCalculationDate(now.toLocalDateTime())
        .withChangeReason("v2Reason")
        .withVersion(TierVersion.V2)
        .produce()
      every { mockTierService.fetchTierOrNull(crn, TierVersion.V2) } returns tierV2

      val tierV3CalculationId = UUID.randomUUID()
      val tierV3 = TierFactory()
        .withTierScore("tierv3score")
        .withCalculationId(tierV3CalculationId)
        .withCalculationDate(LocalDateTime.now())
        .withChangeReason("v3Reason")
        .withVersion(TierVersion.V3)
        .produce()
      every { mockTierService.fetchTierOrNull(crn, TierVersion.V3) } returns tierV3

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
      every { mockTierService.fetchTierOrNull(crn, any()) } returns null

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
      every { mockTierService.fetchTierOrNull(crn, any()) } returns null

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

      every { mockTierService.fetchTierOrNull(uppercasedCrn, any()) } returns null

      service.ensureCaseExists(crn)

      verify { mockCasePersistenceService.updateIfExist(uppercasedCrn, any(), any()) }
      verify { mockCasePersistenceService.createCase(any(), any()) }
      verify { mockApDeliusContextApiClient.getCaseSummaries(listOf(uppercasedCrn)) }
      verify { mockTierService.fetchTierOrNull(uppercasedCrn, TierVersion.V2) }
    }
  }

  @Nested
  inner class ReviseTier {
    @Test
    fun `should update tierV2 and tierV3`() {
      val crn = "CRN123"

      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .produce()

      every { mockCaseRepository.findByCrn(crn) } returns caseEntity
      every { mockCaseRepository.save(any()) } returns caseEntity
      every { mockTierService.fetchTierOrError(crn, TierVersion.V2) } returns TierFactory()
        .withTierScore("V2_NEW")
        .withVersion(TierVersion.V2)
        .produce()
      every { mockTierService.fetchTierOrError(crn, TierVersion.V3) } returns TierFactory()
        .withTierScore("V3_NEW")
        .withVersion(TierVersion.V3)
        .produce()

      val result = service.reviseTier(crn)

      assertThat(result).isTrue()
      verify { mockTierService.fetchTierOrError(crn, TierVersion.V2) }
      verify { mockTierService.fetchTierOrError(crn, TierVersion.V3) }
      verify {
        mockCaseRepository.save(
          match {
            it.tierV2?.tierScore == "V2_NEW" && it.tierV3?.tierScore == "V3_NEW"
          },
        )
      }
    }

    @Test
    fun `should return false if case does not exist`() {
      val crn = "CRN123"
      every { mockCaseRepository.findByCrn(crn) } returns null

      val result = service.reviseTier(crn)

      assertThat(result).isFalse()
    }

    @Test
    fun `should throw exception if fetch fails`() {
      val crn = "CRN123"
      val caseEntity = CaseEntityFactory().withCrn(crn).produce()

      every { mockCaseRepository.findByCrn(crn) } returns caseEntity
      every { mockTierService.fetchTierOrError(crn, any()) } throws RuntimeException("error")

      assertThatThrownBy {
        service.reviseTier(crn)
      }.isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `should uppercase CRN when revising tier`() {
      val crn = "crn123"
      val uppercasedCrn = "CRN123"
      val caseEntity = CaseEntityFactory().withCrn(uppercasedCrn).produce()

      every { mockCaseRepository.findByCrn(uppercasedCrn) } returns caseEntity
      every { mockTierService.fetchTierOrError(uppercasedCrn, any()) } throws RuntimeException("error")

      assertThatThrownBy {
        service.reviseTier(crn)
      }.isInstanceOf(RuntimeException::class.java)

      verify { mockCaseRepository.findByCrn(uppercasedCrn) }
      verify { mockTierService.fetchTierOrError(uppercasedCrn, TierVersion.V2) }
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
}
