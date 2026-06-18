package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ManagerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class CaseServiceTest {
  private val mockCaseRepository = mockk<CaseRepository>()
  private val mockHMPPSTierApiClient = mockk<HMPPSTierApiClient>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()

  private val service = CaseService(mockCaseRepository, mockApDeliusContextApiClient, mockHMPPSTierApiClient)

  @Test
  fun `should return existing offender when exist`() {
    val crn = "CRN123"
    val caseEntity = CaseEntityFactory()
      .withCrn(crn)
      .withName("NAME")
      .withNomsNumber("NOMS123")
      .withTier("tier")
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

    every { mockHMPPSTierApiClient.getTier(crn) } returns ClientResult.Success(
      body = Tier(
        tierScore = "low",
        calculationId = UUID.randomUUID(),
        calculationDate = LocalDateTime.now(),
      ),
      status = HttpStatus.OK,
    )

    val result = service.ensureCaseExists(crn)

    assertThat(result).isEqualTo(caseEntity)
  }

  @Test
  fun `should create new offender when not exist`() {
    val crn = "CRN123"
    val caseEntity = CaseEntityFactory()
      .withCrn(crn)
      .withName("NAME")
      .withNomsNumber("NOMS123")
      .withTier("tier")
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
    every { mockHMPPSTierApiClient.getTier(crn) } returns ClientResult.Success(
      body = Tier(
        tierScore = "tier",
        calculationId = UUID.randomUUID(),
        calculationDate = LocalDateTime.now(),
      ),
      status = HttpStatus.OK,
    )

    val result = service.ensureCaseExists(crn)
    assertThat(result).isEqualTo(caseEntity)
  }

  @Test
  fun `should set tier null and create new offender when not exist and get tier return error`() {
    val crn = "CRN123"
    val caseEntity = CaseEntityFactory()
      .withCrn(crn)
      .withName("NAME")
      .withNomsNumber("NOMS123")
      .withTier(null)
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
    every { mockHMPPSTierApiClient.getTier(crn) } returns ClientResult.Failure.StatusCode(
      status = HttpStatus.INTERNAL_SERVER_ERROR,
      method = HttpMethod.GET,
      path = "/crn/crn123/tier",
      body = null,
    )

    val result = service.ensureCaseExists(crn)
    assertThat(result).isEqualTo(caseEntity)
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
