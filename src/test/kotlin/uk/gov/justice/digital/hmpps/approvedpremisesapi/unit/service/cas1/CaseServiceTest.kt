package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.CaseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ManagerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TierFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class CaseServiceTest {
  private val mockCaseRepository = mockk<CaseRepository>()
  private val mockHMPPSTierApiClient = mockk<HMPPSTierApiClient>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockFeatureFlagService = mockk<FeatureFlagService>()

  private lateinit var service: CaseService

  private fun setupService(includeTierV3: Boolean = false) {
    every { mockFeatureFlagService.getBooleanFlag("include-tier-v3") } returns includeTierV3
    service = CaseService(mockCaseRepository, mockApDeliusContextApiClient, mockHMPPSTierApiClient, mockFeatureFlagService)
  }

  @Nested
  inner class EnsureCaseExists {
    @BeforeEach
    fun setup() {
      setupService(includeTierV3 = false)
    }

    @Test
    fun `should return existing offender when exist`() {
      val crn = "CRN123"
      val now = OffsetDateTime.now()
      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withName("NAME")
        .withNomsNumber("NOMS123")
        .withCreatedAt(now)
        .produce()

      val caseSummary = CaseSummary(
        crn = crn,
        nomsId = "NOMS123",
        name = Name(
          forename = "John",
          surname = "Smith",
          middleNames = emptyList(),
        ),
        pnc = "PNC123",
        dateOfBirth = LocalDate.parse("2023-06-26"),
        gender = "male",
        profile = null,
        manager = ManagerFactory().produce(),
        currentExclusion = false,
        currentRestriction = false,
      )

      every { mockCaseRepository.findByCrn(crn) } returns caseEntity
      every { mockCaseRepository.saveAndFlush(any()) } returns caseEntity
      every { mockApDeliusContextApiClient.getCaseSummaries(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(
          listOf(
            caseSummary,
          ),
        ),
      )

      mockkStatic(OffsetDateTime::class)
      every { OffsetDateTime.now() } returns now

      every { mockHMPPSTierApiClient.getTier(crn, any()) } returns ClientResult.Success(
        body = Tier(
          tierScore = "low",
          calculationId = UUID.randomUUID(),
          calculationDate = now.toLocalDateTime(),
          changeReason = "reason",
        ),
        status = HttpStatus.OK,
      )

      val result = service.ensureCaseExists(crn)

      assertThat(result).isEqualTo(CaseDto(
        crn = "CRN123",
        nomsNumber = "NOMS123",
        name = "JOHN SMITH",
        createdAt = now,
        lastUpdatedAt = now,
        tier = TierDto(
          tierScore = "low",
          calculationDate = now.toLocalDateTime(),
          provisional = null,
          version = TierVersionDto.V2,
        ),
      ))
    }

    @Test
    fun `should create new offender when not exist`() {
      val crn = "CRN123"
      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withName("NAME")
        .withNomsNumber("NOMS123")
        .withTierV2(TierFactory().withTierScore("tier").produce())
        .produce()

      val caseSummary = CaseSummary(
        crn = crn,
        nomsId = "NOMS123",
        name = Name(
          forename = "John",
          surname = "Smith",
          middleNames = emptyList(),
        ),
        pnc = "PNC123",
        dateOfBirth = LocalDate.parse("2023-06-26"),
        gender = "male",
        profile = null,
        manager = ManagerFactory().produce(),
        currentExclusion = false,
        currentRestriction = false,
      )

      every { mockCaseRepository.findByCrn(crn) } returns null
      every { mockCaseRepository.saveAndFlush(any()) } returns caseEntity
      every { mockApDeliusContextApiClient.getCaseSummaries(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(
          listOf(
            caseSummary,
          ),
        ),
      )
      val now = OffsetDateTime.now()
      every { mockHMPPSTierApiClient.getTier(crn, any()) } returns ClientResult.Success(
        body = Tier(
          tierScore = "tier",
          calculationId = UUID.randomUUID(),
          calculationDate = now.toLocalDateTime(),
          changeReason = "reason",
        ),
        status = HttpStatus.OK,
      )

      mockkStatic(OffsetDateTime::class)
      every { OffsetDateTime.now() } returns now

      val result = service.ensureCaseExists(crn)
      assertThat(result).isEqualTo(CaseDto(
        crn = "CRN123",
        nomsNumber = "NOMS123",
        name = "JOHN SMITH",
        createdAt = now,
        lastUpdatedAt = now,
        tier = TierDto(
          tierScore = "tier",
          calculationDate = now.toLocalDateTime(),
          provisional = null,
          version = TierVersionDto.V2,
        ),
      ))
    }

    @Test
    fun `should set tier null and create new offender when not exist and get tier return error`() {
      val crn = "CRN123"
      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withName("NAME")
        .withNomsNumber("NOMS123")
        .withTierV2(null)
        .produce()

      val caseSummary = CaseSummary(
        crn = crn,
        nomsId = "NOMS123",
        name = Name(
          forename = "John",
          surname = "Smith",
          middleNames = emptyList(),
        ),
        pnc = "PNC123",
        dateOfBirth = LocalDate.parse("2023-06-26"),
        gender = "male",
        profile = null,
        manager = ManagerFactory().produce(),
        currentExclusion = false,
        currentRestriction = false,
      )

      every { mockCaseRepository.findByCrn(crn) } returns null
      every { mockCaseRepository.saveAndFlush(any()) } returns caseEntity
      every { mockApDeliusContextApiClient.getCaseSummaries(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(
          listOf(
            caseSummary,
          ),
        ),
      )
      every { mockHMPPSTierApiClient.getTier(crn, any()) } returns ClientResult.Failure.StatusCode(
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        method = HttpMethod.GET,
        path = "/crn/crn123/tier",
        body = null,
      )

      mockkStatic(OffsetDateTime::class)
      val now = OffsetDateTime.now()
      every { OffsetDateTime.now() } returns now

      val result = service.ensureCaseExists(crn)

      assertThat(result).isEqualTo(CaseDto(
        crn = "CRN123",
        nomsNumber = "NOMS123",
        name = "JOHN SMITH",
        createdAt = now,
        lastUpdatedAt = now,
        tier = null,
      ))
    }

    @Test
    fun `should only include tier v2 when flag is disabled`() {
      val crn = "CRN123"
      setupService(includeTierV3 = false)

      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .produce()

      val caseSummary = CaseSummary(
        crn = crn,
        nomsId = "NOMS123",
        name = Name(forename = "John", surname = "Smith", middleNames = emptyList()),
        pnc = "PNC123",
        dateOfBirth = LocalDate.parse("2023-06-26"),
        gender = "male",
        profile = null,
        manager = ManagerFactory().produce(),
        currentExclusion = false,
        currentRestriction = false,
      )

      every { mockCaseRepository.findByCrn(crn) } returns null
      every { mockCaseRepository.saveAndFlush(any()) } returns caseEntity
      every { mockApDeliusContextApiClient.getCaseSummaries(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(listOf(caseSummary)),
      )
      every { mockHMPPSTierApiClient.getTier(crn, any()) } returns ClientResult.Success(
        body = Tier(tierScore = "V2", calculationId = UUID.randomUUID(), calculationDate = LocalDateTime.now(), changeReason = "reason"),
        status = HttpStatus.OK,
      )

      service.ensureCaseExists(crn)

      verify { mockHMPPSTierApiClient.getTier(crn, TierVersion.V2) }
      verify(exactly = 0) { mockHMPPSTierApiClient.getTier(any(), TierVersion.V3) }
      verify {
        mockCaseRepository.saveAndFlush(
          withArg {
            assertThat(it.tierV2?.tierScore).isEqualTo("V2")
            assertThat(it.tierV3).isNull()
          },
        )
      }
    }

    @Test
    fun `should include tier v3 when flag is enabled`() {
      val crn = "CRN123"
      setupService(includeTierV3 = true)

      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .produce()

      val caseSummary = CaseSummary(
        crn = crn,
        nomsId = "NOMS123",
        name = Name(forename = "John", surname = "Smith", middleNames = emptyList()),
        pnc = "PNC123",
        dateOfBirth = LocalDate.parse("2023-06-26"),
        gender = "male",
        profile = null,
        manager = ManagerFactory().produce(),
        currentExclusion = false,
        currentRestriction = false,
      )

      every { mockCaseRepository.findByCrn(crn) } returns null
      every { mockCaseRepository.saveAndFlush(any()) } returns caseEntity
      every { mockApDeliusContextApiClient.getCaseSummaries(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(listOf(caseSummary)),
      )
      every { mockHMPPSTierApiClient.getTier(crn, TierVersion.V2) } returns ClientResult.Success(
        body = Tier(tierScore = "V2", calculationId = UUID.randomUUID(), calculationDate = LocalDateTime.now(), changeReason = "reason"),
        status = HttpStatus.OK,
      )
      every { mockHMPPSTierApiClient.getTier(crn, TierVersion.V3) } returns ClientResult.Success(
        body = Tier(tierScore = "V3", calculationId = UUID.randomUUID(), calculationDate = LocalDateTime.now(), changeReason = "reason"),
        status = HttpStatus.OK,
      )

      service.ensureCaseExists(crn)

      verify { mockHMPPSTierApiClient.getTier(crn, TierVersion.V2) }
      verify { mockHMPPSTierApiClient.getTier(crn, TierVersion.V3) }
      verify {
        mockCaseRepository.saveAndFlush(
          withArg {
            assertThat(it.tierV2?.tierScore).isEqualTo("V2")
            assertThat(it.tierV3?.tierScore).isEqualTo("V3")
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
  }

  @Nested
  inner class ReviseTier {
    @Test
    fun `should update tierV2 and tierV3 when flag is enabled`() {
      val crn = "CRN123"
      setupService(includeTierV3 = true)

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
      setupService(includeTierV3 = false)

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
      setupService()
      every { mockCaseRepository.findByCrn(crn) } returns null

      val result = service.reviseTier(crn)

      assertThat(result).isFalse()
    }

    @Test
    fun `should throw exception if fetch fails`() {
      val crn = "CRN123"
      setupService()
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
    fun `should return tierV2`() {
      val crn = "CRN123"
      setupService(includeTierV3 = false)

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
  }
}
