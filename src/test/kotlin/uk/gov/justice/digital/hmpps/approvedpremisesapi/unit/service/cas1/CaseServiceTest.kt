package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TierFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class CaseServiceTest {

  @MockK
  private lateinit var mockCaseRepository: CaseRepository

  @MockK
  private lateinit var mockHMPPSTierApiClient: HMPPSTierApiClient

  @MockK
  private lateinit var mockApDeliusContextApiClient: ApDeliusContextApiClient

  @MockK
  private lateinit var mockFeatureFlagService: FeatureFlagService

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

      every { mockCaseRepository.findByCrn(crn) } returns caseEntity
      val createdCase = slot<CaseEntity>()
      every { mockCaseRepository.saveAndFlush(capture(createdCase)) } returnsArgument 0
      every { mockApDeliusContextApiClient.getCaseSummaries(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(listOf(caseSummary)),
      )

      val tierV2CalculationId = UUID.randomUUID()
      every { mockHMPPSTierApiClient.getTier(crn, TierVersion.V2) } returns ClientResult.Success(
        body = Tier(
          tierScore = "tierv2score",
          calculationId = tierV2CalculationId,
          calculationDate = now.toLocalDateTime(),
          changeReason = "v2Reason",
        ),
        status = HttpStatus.OK,
      )

      val tierV3CalculationId = UUID.randomUUID()
      every { mockHMPPSTierApiClient.getTier(crn, TierVersion.V3) } returns ClientResult.Success(
        body = Tier(
          tierScore = "tierv3score",
          calculationId = tierV3CalculationId,
          calculationDate = LocalDateTime.now(),
          changeReason = "v3Reason",
        ),
        status = HttpStatus.OK,
      )

      val result = service.ensureCaseExists(crn)

      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.name).isEqualTo("JOHN SMITH")
      assertThat(result.nomsNumber).isEqualTo("NOMS123")
      assertThat(result.tier?.tierScore).isEqualTo("tierv2score")
      assertThat(result.tier?.calculationDate).isEqualTo(now.toLocalDateTime())
      assertThat(result.tier?.version).isEqualTo(TierVersionDto.V2)
      assertThat(result.tier?.provisional).isNull()

      assertThat(createdCase.captured.crn).isEqualTo(crn)
      assertThat(createdCase.captured.name).isEqualTo("JOHN SMITH")
      assertThat(createdCase.captured.nomsNumber).isEqualTo("NOMS123")
      assertThat(createdCase.captured.tierV2?.tierScore).isEqualTo("tierv2score")
      assertThat(createdCase.captured.tierV2?.calculationId).isEqualTo(tierV2CalculationId)
      assertThat(createdCase.captured.tierV2?.changeReason).isEqualTo("v2Reason")
      assertThat(createdCase.captured.tierV3?.tierScore).isEqualTo("tierv3score")
      assertThat(createdCase.captured.tierV3?.calculationId).isEqualTo(tierV3CalculationId)
      assertThat(createdCase.captured.tierV3?.changeReason).isEqualTo("v3Reason")
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

      every { mockCaseRepository.findByCrn(crn) } returns caseEntity
      val createdCase = slot<CaseEntity>()
      every { mockCaseRepository.saveAndFlush(capture(createdCase)) } returnsArgument 0
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

      val result = service.ensureCaseExists(crn)
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.name).isEqualTo("JOHN SMITH")
      assertThat(result.nomsNumber).isEqualTo("NOMS123")
      assertThat(result.tier?.tierScore).isEqualTo("oldv2")
      assertThat(result.tier?.calculationDate).isEqualTo(now.toLocalDateTime())
      assertThat(result.tier?.version).isEqualTo(TierVersionDto.V2)
      assertThat(result.tier?.provisional).isNull()

      assertThat(createdCase.captured.tierV2?.tierScore).isEqualTo("oldv2")
      assertThat(createdCase.captured.tierV3?.tierScore).isEqualTo("oldv3")
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

      every { mockCaseRepository.findByCrn(crn) } returns null

      val createdCase = slot<CaseEntity>()
      every { mockCaseRepository.saveAndFlush(capture(createdCase)) } returnsArgument 0
      every { mockApDeliusContextApiClient.getCaseSummaries(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(listOf(caseSummary)),
      )

      val tierV2CalculationId = UUID.randomUUID()
      every { mockHMPPSTierApiClient.getTier(crn, TierVersion.V2) } returns ClientResult.Success(
        body = Tier(
          tierScore = "tierv2score",
          calculationId = tierV2CalculationId,
          calculationDate = now.toLocalDateTime(),
          changeReason = "v2Reason",
        ),
        status = HttpStatus.OK,
      )

      val tierV3CalculationId = UUID.randomUUID()
      every { mockHMPPSTierApiClient.getTier(crn, TierVersion.V3) } returns ClientResult.Success(
        body = Tier(
          tierScore = "tierv3score",
          calculationId = tierV3CalculationId,
          calculationDate = LocalDateTime.now(),
          changeReason = "v3Reason",
        ),
        status = HttpStatus.OK,
      )

      val result = service.ensureCaseExists(crn)
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.name).isEqualTo("JOHN SMITH")
      assertThat(result.nomsNumber).isEqualTo("NOMS123")
      assertThat(result.tier?.tierScore).isEqualTo("tierv2score")
      assertThat(result.tier?.calculationDate).isEqualTo(now.toLocalDateTime())
      assertThat(result.tier?.version).isEqualTo(TierVersionDto.V2)
      assertThat(result.tier?.provisional).isNull()

      assertThat(createdCase.captured.tierV2?.tierScore).isEqualTo("tierv2score")
      assertThat(createdCase.captured.tierV2?.calculationId).isEqualTo(tierV2CalculationId)
      assertThat(createdCase.captured.tierV2?.changeReason).isEqualTo("v2Reason")
      assertThat(createdCase.captured.tierV3?.tierScore).isEqualTo("tierv3score")
      assertThat(createdCase.captured.tierV3?.calculationId).isEqualTo(tierV3CalculationId)
      assertThat(createdCase.captured.tierV3?.changeReason).isEqualTo("v3Reason")
    }

    @Test
    fun `no existing case, create new, setting tiers to null if get tiers errors`() {
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns true
      every { mockFeatureFlagService.getBooleanFlag("use-tier-v3") } returns false

      val crn = "CRN123"

      every { mockCaseRepository.findByCrn(crn) } returns null

      val createdCase = slot<CaseEntity>()
      every { mockCaseRepository.saveAndFlush(capture(createdCase)) } returnsArgument 0
      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(crn)) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(
          listOf(
            CaseSummaryFactory().withCrn(crn).withName(Name("JOHN", "SMITH")).withNomsId("NOMS123").produce(),
          ),
        ),
      )
      every { mockHMPPSTierApiClient.getTier(crn, any()) } returns ClientResult.Failure.StatusCode(
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        method = HttpMethod.GET,
        path = "/crn/crn123/tier",
        body = null,
      )

      val result = service.ensureCaseExists(crn)

      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.name).isEqualTo("JOHN SMITH")
      assertThat(result.nomsNumber).isEqualTo("NOMS123")
      assertThat(result.tier).isNull()

      assertThat(createdCase.captured.tierV2).isNull()
      assertThat(createdCase.captured.tierV3).isNull()
    }

    @Test
    fun `no existing case, create new, dont include v3 tier if flag is disabled`() {
      val crn = "CRN123"
      val now = OffsetDateTime.now()
      every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns false
      every { mockFeatureFlagService.getBooleanFlag("use-tier-v3") } returns false

      every { mockCaseRepository.findByCrn(crn) } returns null
      val createdCase = slot<CaseEntity>()
      every { mockCaseRepository.saveAndFlush(capture(createdCase)) } returnsArgument 0
      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(crn)) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(listOf(CaseSummaryFactory().withCrn(crn).withName(Name("JOHN", "SMITH")).withNomsId("NOMS123").produce())),
      )
      every { mockHMPPSTierApiClient.getTier(crn, any()) } returns ClientResult.Success(
        body = Tier(tierScore = "tier value", calculationId = UUID.randomUUID(), calculationDate = now.toLocalDateTime(), changeReason = "reason"),
        status = HttpStatus.OK,
      )

      val result = service.ensureCaseExists(crn)

      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.name).isEqualTo("JOHN SMITH")
      assertThat(result.nomsNumber).isEqualTo("NOMS123")
      assertThat(result.tier?.tierScore).isEqualTo("tier value")
      assertThat(result.tier?.calculationDate).isEqualTo(now.toLocalDateTime())
      assertThat(result.tier?.version).isEqualTo(TierVersionDto.V2)
      assertThat(result.tier?.provisional).isNull()

      assertThat(createdCase.captured.tierV2?.tierScore).isEqualTo("tier value")
      assertThat(createdCase.captured.tierV3).isNull()
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
  }

  @Nested
  inner class GetCase {

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
  }
}
