package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ManagerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1OffenderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OffenderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1OffenderServiceTest {
  private val cas1OffenderRepository = mockk<Cas1OffenderRepository>()
  private val offenderService = mockk<OffenderService>()
  private val hmppsTierApiClient = mockk<HMPPSTierApiClient>()
  private val userService = mockk<UserService>()

  private val service = Cas1OffenderService(
    cas1OffenderRepository,
    offenderService,
    hmppsTierApiClient,
  )

  @Test
  fun `should return existing offender when exist`() {
    val cas1OffenderEntity = Cas1OffenderEntityFactory()
      .withCrn("CRN123")
      .withName("NAME")
      .withNomsNumber("NOMS123")
      .withTier("tier")
      .produce()

    val caseSummary = CaseSummary(
      crn = "CRN123",
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

    val risk = PersonRisksFactory()
      .withTier(
        RiskWithStatus(
          RiskTier(
            level = "M1",
            lastUpdated = LocalDate.parse("2023-06-26"),
          ),
        ),
      )
      .produce()

    every { cas1OffenderRepository.findByCrn("CRN123") } returns cas1OffenderEntity
    every { cas1OffenderRepository.saveAndFlush(any()) } returns cas1OffenderEntity

    val result = service.getOrCreateOffender(caseSummary, risk)

    assertThat(result).isEqualTo(cas1OffenderEntity)
  }

  @Test
  fun `should create new offender when not exist`() {
    val cas1OffenderEntity = Cas1OffenderEntityFactory()
      .withCrn("CRN123")
      .withName("NAME")
      .withNomsNumber("NOMS123")
      .withTier("tier")
      .produce()

    val caseSummary = CaseSummary(
      crn = "CRN123",
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

    val risk = PersonRisksFactory()
      .withTier(
        RiskWithStatus(
          RiskTier(
            level = "M1",
            lastUpdated = LocalDate.parse("2023-06-26"),
          ),
        ),
      )
      .produce()

    every { cas1OffenderRepository.findByCrn("CRN123") } returns null
    every { cas1OffenderRepository.saveAndFlush(any()) } returns cas1OffenderEntity

    val result = service.getOrCreateOffender(caseSummary, risk)

    assertThat(result).isEqualTo(cas1OffenderEntity)
  }

  @Test
  fun `should return CasResult Success of type RiskTier when HMPPSTierApiClient returns success`() {
    val crn = "CRN123"
    val tier = Tier(tierScore = "A1", calculationId = UUID.randomUUID(), calculationDate = LocalDateTime.parse("2023-12-01T00:00:00"))

    every { hmppsTierApiClient.getTier(crn) } returns ClientResult.Success<Tier>(status = HttpStatus.OK, body = tier)

    val result = service.getRiskTier(crn)
    assertThat(result).isInstanceOf(CasResult.Success::class.java)
    assertThat((result as CasResult.Success).value.level).isEqualTo("A1")
    assertThat(result.value.lastUpdated).isEqualTo(LocalDate.parse("2023-12-01"))
  }

  @Test
  fun `should return CasResult Failure Not Found when HMPPSTierApiClient returns 404 Not Found`() {
    val crn = "CRN123"
    every { hmppsTierApiClient.getTier(crn) } returns ClientResult.Failure.StatusCode(status = HttpStatus.NOT_FOUND, method = HttpMethod.GET, path = "/", body = null)

    val result = service.getRiskTier(crn)
    assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
    assertThat((result as CasResult.NotFound).id).isEqualTo(crn)
    assertThat(result.entityType).isEqualTo("Risk Tier")
  }

  @Test
  fun `should return CasResult Failure Unauthorised when HMPPSTierApiClient returns 401 Unauthorised`() {
    val crn = "CRN123"
    every { hmppsTierApiClient.getTier(crn) } returns ClientResult.Failure.StatusCode(status = HttpStatus.UNAUTHORIZED, method = HttpMethod.GET, path = "/", body = null)

    val result = service.getRiskTier(crn)
    assertThat(result).isInstanceOf(CasResult.Unauthorised::class.java)
    assertThat((result as CasResult.Unauthorised).message).isEqualTo("Not authorised to access Risk Tier for CRN: CRN123")
  }

  @Test
  fun `should return CasResult Failure GeneralValidationError when HMPPSTierApiClient returns 400 Bad Request`() {
    val crn = "CRN123"
    every { hmppsTierApiClient.getTier(crn) } returns ClientResult.Failure.StatusCode(status = HttpStatus.BAD_REQUEST, method = HttpMethod.GET, path = "/", body = null)

    val result = service.getRiskTier(crn)
    assertThat(result).isInstanceOf(CasResult.GeneralValidationError::class.java)
    assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Failed to retrieve Risk Tier for CRN: CRN123: 400 BAD_REQUEST")
  }

  @Test
  fun `should return CasResult Failure GeneralValidationError when HMPPSTierApiClient returns PreemptiveCacheTimeout`() {
    val crn = "CRN123"
    every { hmppsTierApiClient.getTier(crn) } returns ClientResult.Failure.PreemptiveCacheTimeout(cacheName = "", cacheKey = "", timeoutMs = 0)

    val result = service.getRiskTier(crn)
    assertThat(result).isInstanceOf(CasResult.GeneralValidationError::class.java)
    assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Error retrieving Risk Tier for CRN: CRN123: Timed out after 0ms waiting for  on pre-emptive cache ")
  }

  @Test
  fun `should return CasResult Failure GeneralValidationError when HMPPSTierApiClient returns CachedValueUnavailable`() {
    val crn = "CRN123"
    every { hmppsTierApiClient.getTier(crn) } returns ClientResult.Failure.CachedValueUnavailable(cacheKey = "keyId")

    val result = service.getRiskTier(crn)
    assertThat(result).isInstanceOf(CasResult.GeneralValidationError::class.java)
    assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Error retrieving Risk Tier for CRN: CRN123: No Redis entry exists for keyId")
  }

  @Test
  fun `should return CasResult Failure GeneralValidationError when HMPPSTierApiClient returns Other`() {
    val crn = "CRN123"
    every { hmppsTierApiClient.getTier(crn) } returns ClientResult.Failure.Other(method = HttpMethod.GET, path = "/", exception = RuntimeException("error"))

    val result = service.getRiskTier(crn)
    assertThat(result).isInstanceOf(CasResult.GeneralValidationError::class.java)
    assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Error retrieving Risk Tier for CRN: CRN123: Unable to complete GET request to /")
  }

  @Test
  fun `should return CasResult Failure Not Found when offender service returns 404 Not Found`() {
    val crn = "CRN123"
    val user = UserEntityFactory().withDefaults().produce()
    every { userService.getUserForRequest() } returns user
    every { offenderService.getPersonSummaryInfoResults(setOf(crn), user.cas1LaoStrategy()) } returns listOf(PersonSummaryInfoResult.NotFound(crn))

    val result = service.getCas1PersonSummaryInfoResult(crn, user.cas1LaoStrategy())
    assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
    assertThat((result as CasResult.NotFound).id).isEqualTo(crn)
    assertThat(result.entityType).isEqualTo("Person")
  }

  @Test
  fun `should return CasResult Failure Not Found when offender service returns Unknown`() {
    val crn = "CRN123"
    val user = UserEntityFactory().withDefaults().produce()
    every { userService.getUserForRequest() } returns user
    every { offenderService.getPersonSummaryInfoResults(setOf(crn), user.cas1LaoStrategy()) } returns listOf(PersonSummaryInfoResult.Unknown(crn))

    val result = service.getCas1PersonSummaryInfoResult(crn, user.cas1LaoStrategy())
    assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
    assertThat((result as CasResult.NotFound).id).isEqualTo(crn)
    assertThat(result.entityType).isEqualTo("Person")
  }

  @Test
  fun `should return CasResult Success when offender service returns Success Full`() {
    val crn = "CRN123"
    val user = UserEntityFactory().withDefaults().produce()
    val caseSummary = CaseSummaryFactory().withCrn(crn).produce()
    every { userService.getUserForRequest() } returns user
    every { offenderService.getPersonSummaryInfoResults(setOf(crn), user.cas1LaoStrategy()) } returns listOf(PersonSummaryInfoResult.Success.Full(crn, caseSummary))

    val result = service.getCas1PersonSummaryInfoResult(crn, user.cas1LaoStrategy())
    assertThat(result).isInstanceOf(CasResult.Success::class.java)
    assertThat((result as CasResult.Success<PersonSummaryInfoResult.Success.Full>).value.crn).isEqualTo(crn)
    assertThat(result.value.summary).isEqualTo(caseSummary)
  }

  @Test
  fun `should return CasResult Success when offender service returns Success Restricted`() {
    val crn = "CRN123"
    val nomsNumber = "NOMS123"
    val user = UserEntityFactory().withDefaults().produce()
    every { userService.getUserForRequest() } returns user
    every { offenderService.getPersonSummaryInfoResults(setOf(crn), user.cas1LaoStrategy()) } returns listOf(PersonSummaryInfoResult.Success.Restricted(crn, nomsNumber))

    val result = service.getCas1PersonSummaryInfoResult(crn, user.cas1LaoStrategy())
    assertThat(result).isInstanceOf(CasResult.Success::class.java)
    assertThat((result as CasResult.Success<PersonSummaryInfoResult.Success.Restricted>).value.crn).isEqualTo(crn)
    assertThat(result.value.nomsNumber).isEqualTo(nomsNumber)
  }
}
