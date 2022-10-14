package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.AssessRisksAndNeedsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ExcludedCategoryBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseNoteFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RegistrationClientResponseFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRisksClientResponseFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.RoshRisksSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.RegistrationKeyValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.lang.RuntimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OffenderServiceTest {
  private val mockCommunityApiClient = mockk<CommunityApiClient>()
  private val mockAssessRisksAndNeedsApiClient = mockk<AssessRisksAndNeedsApiClient>()
  private val mockHMPPSTierApiClient = mockk<HMPPSTierApiClient>()
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val prisonCaseNotesConfigBindingModel = PrisonCaseNotesConfigBindingModel().apply {
    lookbackDays = 30
    prisonApiPageSize = 2
    excludedCategories = listOf(
      ExcludedCategoryBindingModel().apply {
        this.category = "CATEGORY"
        this.subcategory = "EXCLUDED_SUBTYPE"
      },
      ExcludedCategoryBindingModel().apply {
        this.category = "EXCLUDED_CATEGORY"
        this.subcategory = null
      }
    )
  }

  private val offenderService = OffenderService(
    mockCommunityApiClient,
    mockAssessRisksAndNeedsApiClient,
    mockHMPPSTierApiClient,
    mockPrisonsApiClient,
    prisonCaseNotesConfigBindingModel
  )

  @Test
  fun `getOffenderByCrn returns NotFound result when Client returns 404`() {
    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getOffenderByCrn throws when Client returns other non-2xx status code except 403`() {
    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.BAD_REQUEST, null)

    val exception = assertThrows<RuntimeException> { offenderService.getOffenderByCrn("a-crn", "distinguished.name") }
    assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/a-crn: 400 BAD_REQUEST")
  }

  @Test
  fun `getOffenderByCrn returns OffenderDetails without further checks when Offender has no LAO constraints`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .produce()

    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)

    val result = offenderService.getOffenderByCrn("a-crn", "distinguished.name")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.otherIds.crn).isEqualTo("a-crn")
    assertThat(result.entity.firstName).isEqualTo("Bob")
    assertThat(result.entity.surname).isEqualTo("Doe")
  }

  @Test
  fun `getOffenderByCrn returns Unauthorised result when distinguished name is excluded from viewing`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true)

    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockCommunityApiClient.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getOffenderByCrn returns Unauthorised result when Client returns 403 with valid user access response body`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true)

    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockCommunityApiClient.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Failure.StatusCode(
      HttpMethod.GET,
      "/secure/crn/a-crn/user/distinguished.name/userAccess",
      HttpStatus.FORBIDDEN,
      jacksonObjectMapper().writeValueAsString(accessBody)
    )

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getOffenderByCrn throws when Client returns 403 without valid user access response body (problem with our service to service JWT)`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockCommunityApiClient.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Failure.StatusCode(
      HttpMethod.GET,
      "/secure/crn/a-crn/user/distinguished.name/userAccess",
      HttpStatus.FORBIDDEN,
      null
    )

    val exception = assertThrows<RuntimeException> { offenderService.getOffenderByCrn("a-crn", "distinguished.name") }
    assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/crn/a-crn/user/distinguished.name/userAccess: 403 FORBIDDEN")
  }

  @Test
  fun `getOffenderByCrn returns Unauthorised result when distinguished name is not explicitly allowed to view`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentRestriction(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = true, userExcluded = false)

    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockCommunityApiClient.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getOffenderByCrn returns OffenderDetails when LAO restrictions are passed`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentRestriction(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = false)

    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockCommunityApiClient.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)

    val result = offenderService.getOffenderByCrn("a-crn", "distinguished.name")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.otherIds.crn).isEqualTo("a-crn")
    assertThat(result.entity.firstName).isEqualTo("Bob")
    assertThat(result.entity.surname).isEqualTo("Doe")
  }

  @Test
  fun `getRisksByCrn returns NotFound result when Community API Client returns 404`() {
    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getRiskByCrn("a-crn", "jwt", "distinguished.name") is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getRisksByCrn throws when Community API Client returns other non-2xx status code`() {
    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.BAD_REQUEST, null)

    val exception = assertThrows<RuntimeException> { offenderService.getRiskByCrn("a-crn", "jwt", "distinguished.name") }
    assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/a-crn: 400 BAD_REQUEST")
  }

  @Test
  fun `getRisksByCrn returns Unauthorised result when distinguished name is excluded from viewing`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true)

    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockCommunityApiClient.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThat(offenderService.getRiskByCrn("a-crn", "jwt", "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getRisksByCrn returns NotFound envelopes for RoSH, Tier, Mappa & flags when respective Clients return 404`() {
    val crn = "a-crn"
    val jwt = "jwt"
    val distinguishedName = "distinguished.name"

    mockExistingNonLaoOffender()
    mock404RoSH(crn, jwt)
    mock404Tier(crn)
    mock404Registrations(crn)

    val result = offenderService.getRiskByCrn(crn, jwt, distinguishedName)
    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.crn).isEqualTo(crn)
    assertThat(result.entity.roshRisks.status).isEqualTo(RiskStatus.NotFound)
    assertThat(result.entity.tier.status).isEqualTo(RiskStatus.NotFound)
    assertThat(result.entity.mappa.status).isEqualTo(RiskStatus.NotFound)
    assertThat(result.entity.flags.status).isEqualTo(RiskStatus.NotFound)
  }

  @Test
  fun `getRisksByCrn returns Error envelopes for RoSH, Tier, Mappa & flags when respective Clients return 500`() {
    val crn = "a-crn"
    val jwt = "jwt"
    val distinguishedName = "distinguished.name"

    mockExistingNonLaoOffender()
    mock500RoSH(crn, jwt)
    mock500Tier(crn)
    mock500Registrations(crn)

    val result = offenderService.getRiskByCrn(crn, jwt, distinguishedName)
    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.crn).isEqualTo(crn)
    assertThat(result.entity.roshRisks.status).isEqualTo(RiskStatus.Error)
    assertThat(result.entity.tier.status).isEqualTo(RiskStatus.Error)
    assertThat(result.entity.mappa.status).isEqualTo(RiskStatus.Error)
    assertThat(result.entity.flags.status).isEqualTo(RiskStatus.Error)
  }

  @Test
  fun `getRisksByCrn returns Retrieved envelopes with expected contents for RoSH, Tier, Mappa & flags when respective Clients return 200`() {
    val crn = "a-crn"
    val jwt = "jwt"
    val distinguishedName = "distinguished.name"

    mockExistingNonLaoOffender()

    mock200RoSH(
      crn,
      jwt,
      RoshRisksClientResponseFactory().withSummary(
        RoshRisksSummary(
          whoIsAtRisk = null,
          natureOfRisk = null,
          riskImminence = null,
          riskIncreaseFactors = null,
          riskMitigationFactors = null,
          riskInCommunity = mapOf(
            RiskLevel.LOW to listOf("Children"),
            RiskLevel.MEDIUM to listOf("Public"),
            RiskLevel.HIGH to listOf("Known Adult"),
            RiskLevel.VERY_HIGH to listOf("Staff")
          ),
          riskInCustody = mapOf(),
          assessedOn = LocalDateTime.parse("2022-09-06T13:45:00"),
          overallRiskLevel = RiskLevel.MEDIUM
        )
      ).produce()
    )

    mock200Tier(
      crn,
      Tier(
        tierScore = "M2",
        calculationId = UUID.randomUUID(),
        calculationDate = LocalDateTime.parse("2022-09-06T14:59:00")
      )
    )

    mock200Registrations(
      crn,
      Registrations(
        registrations = listOf(
          RegistrationClientResponseFactory()
            .withType(RegistrationKeyValue(code = "MAPP", description = "MAPPA"))
            .withRegisterCategory(RegistrationKeyValue(code = "C1", description = "C1"))
            .withRegisterLevel(RegistrationKeyValue(code = "L1", description = "L1"))
            .withStartDate(LocalDate.parse("2022-09-06"))
            .produce(),
          RegistrationClientResponseFactory()
            .withType(RegistrationKeyValue(code = "FLAG", description = "RISK FLAG"))
            .produce()
        )
      )
    )

    val result = offenderService.getRiskByCrn(crn, jwt, distinguishedName)
    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.crn).isEqualTo(crn)

    assertThat(result.entity.roshRisks.status).isEqualTo(RiskStatus.Retrieved)
    result.entity.roshRisks.value!!.let {
      assertThat(it.lastUpdated).isEqualTo(LocalDate.parse("2022-09-06"))
      assertThat(it.overallRisk).isEqualTo("Medium")
      assertThat(it.riskToChildren).isEqualTo("Low")
      assertThat(it.riskToPublic).isEqualTo("Medium")
      assertThat(it.riskToKnownAdult).isEqualTo("High")
      assertThat(it.riskToStaff).isEqualTo("Very High")
    }

    assertThat(result.entity.tier.status).isEqualTo(RiskStatus.Retrieved)
    result.entity.tier.value!!.let {
      assertThat(it.lastUpdated).isEqualTo(LocalDate.parse("2022-09-06"))
      assertThat(it.level).isEqualTo("M2")
    }

    assertThat(result.entity.mappa.status).isEqualTo(RiskStatus.Retrieved)
    result.entity.mappa.value!!.let {
      assertThat(it.lastUpdated).isEqualTo(LocalDate.parse("2022-09-06"))
      assertThat(it.level).isEqualTo("CAT C1/LEVEL L1")
    }

    assertThat(result.entity.flags.status).isEqualTo(RiskStatus.Retrieved)
    assertThat(result.entity.flags.value).contains("RISK FLAG")
  }

  @Test
  fun `getInmateDetailByNomsNumber returns not found result when Client responds with 404`() {
    val nomsNumber = "NOMS321"

    every { mockPrisonsApiClient.getInmateDetails(nomsNumber) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

    val result = offenderService.getInmateDetailByNomsNumber(nomsNumber)

    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getInmateDetailByNomsNumber returns unauthorised result when Client responds with 403`() {
    val nomsNumber = "NOMS321"

    every { mockPrisonsApiClient.getInmateDetails(nomsNumber) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.FORBIDDEN, null)

    val result = offenderService.getInmateDetailByNomsNumber(nomsNumber)

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getInmateDetailByNomsNumber returns succesfully when Client responds with 200`() {
    val nomsNumber = "NOMS321"

    every { mockPrisonsApiClient.getInmateDetails(nomsNumber) } returns ClientResult.Success(
      HttpStatus.OK,
      InmateDetail(
        offenderNo = nomsNumber,
        inOutStatus = InOutStatus.IN,
        assignedLivingUnit = AssignedLivingUnit(
          agencyId = "AGY",
          locationId = 89,
          description = "AGENCY DESCRIPTION",
          agencyName = "AGENCY NAME"
        )
      )
    )

    val result = offenderService.getInmateDetailByNomsNumber(nomsNumber)

    assertThat(result is AuthorisableActionResult.Success)
    result as AuthorisableActionResult.Success
    assertThat(result.entity.offenderNo).isEqualTo(nomsNumber)
    assertThat(result.entity.inOutStatus).isEqualTo(InOutStatus.IN)
    assertThat(result.entity.assignedLivingUnit).isEqualTo(
      AssignedLivingUnit(
        agencyId = "AGY",
        locationId = 89,
        description = "AGENCY DESCRIPTION",
        agencyName = "AGENCY NAME"
      )
    )
  }

  @Test
  fun `getPrisonCaseNotesByNomsNumber returns NotFound when Case Notes request returns a 404`() {
    val nomsNumber = "NOMS456"

    every {
      mockPrisonsApiClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 0,
        pageSize = 2
      )
    } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/v2", HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getPrisonCaseNotesByNomsNumber(nomsNumber) is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getPrisonCaseNotesByNomsNumber returns Unauthorised when Case Notes request returns a 403`() {
    val nomsNumber = "NOMS456"

    every {
      mockPrisonsApiClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 0,
        pageSize = 2
      )
    } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/v2", HttpStatus.FORBIDDEN, null)

    assertThat(offenderService.getPrisonCaseNotesByNomsNumber(nomsNumber) is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getPrisonCaseNotesByNomsNumber returns Success, traverses pages from Client & excludes categories + subcategories`() {
    val nomsNumber = "NOMS456"

    val caseNotesPageOne = listOf(
      CaseNoteFactory().produce(),
      CaseNoteFactory().produce(),
      CaseNoteFactory().withType("EXCLUDED_TYPE").produce()
    )

    val caseNotesPageTwo = listOf(
      CaseNoteFactory().produce(),
      CaseNoteFactory().produce(),
      CaseNoteFactory().withType("TYPE").withSubType("EXCLUDED_SUBTYPE").produce()
    )

    every {
      mockPrisonsApiClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 0,
        pageSize = 2
      )
    } returns ClientResult.Success(
      HttpStatus.OK,
      CaseNotesPage(
        totalElements = 6,
        totalPages = 2,
        number = 1,
        content = caseNotesPageOne
      )
    )

    every {
      mockPrisonsApiClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 1,
        pageSize = 2
      )
    } returns ClientResult.Success(
      HttpStatus.OK,
      CaseNotesPage(
        totalElements = 4,
        totalPages = 2,
        number = 2,
        content = caseNotesPageTwo
      )
    )

    val result = offenderService.getPrisonCaseNotesByNomsNumber(nomsNumber)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity).containsAll(caseNotesPageOne.subList(0, 1) + caseNotesPageTwo.subList(0, 1))
  }

  private fun mockExistingNonLaoOffender() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentRestriction(false)
      .withCurrentExclusion(false)
      .produce()

    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
  }

  private fun mock404RoSH(crn: String, jwt: String) = every { mockAssessRisksAndNeedsApiClient.getRoshRisks(crn, jwt) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/risks/crn/a-crn", HttpStatus.NOT_FOUND, body = null)
  private fun mock404Tier(crn: String) = every { mockHMPPSTierApiClient.getTier(crn) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/crn/a-crn/tier", HttpStatus.NOT_FOUND, body = null)
  private fun mock404Registrations(crn: String) = every { mockCommunityApiClient.getRegistrationsForOffenderCrn(crn) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn/registrations?activeOnly=true", HttpStatus.NOT_FOUND, body = null)

  private fun mock500RoSH(crn: String, jwt: String) = every { mockAssessRisksAndNeedsApiClient.getRoshRisks(crn, jwt) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/risks/crn/a-crn", HttpStatus.INTERNAL_SERVER_ERROR, body = null)
  private fun mock500Tier(crn: String) = every { mockHMPPSTierApiClient.getTier(crn) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/crn/a-crn/tier", HttpStatus.INTERNAL_SERVER_ERROR, body = null)
  private fun mock500Registrations(crn: String) = every { mockCommunityApiClient.getRegistrationsForOffenderCrn(crn) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn/registrations?activeOnly=true", HttpStatus.INTERNAL_SERVER_ERROR, body = null)

  private fun mock200RoSH(crn: String, jwt: String, body: RoshRisks) = every { mockAssessRisksAndNeedsApiClient.getRoshRisks(crn, jwt) } returns ClientResult.Success(HttpStatus.OK, body = body)
  private fun mock200Tier(crn: String, body: Tier) = every { mockHMPPSTierApiClient.getTier(crn) } returns ClientResult.Success(HttpStatus.OK, body = body)
  private fun mock200Registrations(crn: String, body: Registrations) = every { mockCommunityApiClient.getRegistrationsForOffenderCrn(crn) } returns ClientResult.Success(HttpStatus.OK, body = body)
}
