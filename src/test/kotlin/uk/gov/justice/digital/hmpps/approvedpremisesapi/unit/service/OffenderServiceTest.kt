package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.AdjudicationsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CaseNotesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Success
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ExcludedCategoryBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationsPageFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AgencyFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseNoteFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRatingsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.asOffenderDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.MappaDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Registration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshRatings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID

class OffenderServiceTest {
  private val mockCommunityApiClient = mockk<CommunityApiClient>()
  private val mockHMPPSTierApiClient = mockk<HMPPSTierApiClient>()
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockCaseNotesClient = mockk<CaseNotesClient>()
  private val mockApOASysContextApiClient = mockk<ApOASysContextApiClient>()
  private val mockAdjudicationsApiClient = mockk<AdjudicationsApiClient>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()

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
      },
    )
  }
  private val adjudicationsConfigBindingModel = PrisonAdjudicationsConfigBindingModel().apply {
    prisonApiPageSize = 2
  }

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val offenderService = OffenderService(
    mockCommunityApiClient,
    mockHMPPSTierApiClient,
    mockPrisonsApiClient,
    mockCaseNotesClient,
    mockApOASysContextApiClient,
    mockAdjudicationsApiClient,
    mockApDeliusContextApiClient,
    prisonCaseNotesConfigBindingModel,
    adjudicationsConfigBindingModel,
  )

  @Test
  fun `getOffenderByCrn returns NotFound result when crn not found`() {
    val crn = "N123456"
    every { mockApDeliusContextApiClient.getSummariesForCrns(listOf(crn)) } returns Success(
      status = HttpStatus.OK,
      body = CaseSummaries(listOf()),
    )
    every { mockApDeliusContextApiClient.getUserAccessForCrns("deliusUsername", listOf(crn)) } returns Success(
      status = HttpStatus.OK,
      body = UserAccess(listOf()),
    )

    assertThat(offenderService.getOffenderByCrn(crn, "distinguished.name") is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getOffenderByCrn throws when Client returns other non-2xx status code except 403`() {
    val crn = "B412356"
    every { mockApDeliusContextApiClient.getSummariesForCrns(listOf(crn)) } returns StatusCode(
      method = HttpMethod.GET,
      status = HttpStatus.BAD_REQUEST,
      path = "/probation-cases/summaries",
      body = null,
    )

    val exception = assertThrows<RuntimeException> { offenderService.getOffenderByCrn(crn, "distinguished.name") }
    assertThat(exception.message).isEqualTo("Unable to complete GET request to /probation-cases/summaries: 400 BAD_REQUEST")
  }

  @Test
  fun `getOffenderByCrn returns OffenderDetails without further checks when Offender has no LAO constraints`() {
    val crn = "N123456"
    val resultBody = CaseSummaryFactory()
      .withCrn(crn)
      .withName(NameFactory().withForename("Bob").withSurname("Doe").produce())
      .withCurrentExclusion(false)
      .withCurrentRestriction(false)
      .produce()

    every { mockApDeliusContextApiClient.getSummariesForCrns(listOf(crn)) } returns Success(
      status = HttpStatus.OK,
      body = CaseSummaries(listOf(resultBody)),
    )

    val result = offenderService.getOffenderByCrn(crn, "distinguished.name")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.otherIds.crn).isEqualTo(crn)
    assertThat(result.entity.firstName).isEqualTo("Bob")
    assertThat(result.entity.surname).isEqualTo("Doe")
  }

  @Test
  fun `getOffenderByCrn does not enforce LAO when ignoreLao is enabled (because user has LAO qualification)`() {
    val resultBody = CaseSummaryFactory()
      .withCrn("A123456")
      .withName(NameFactory().withForename("Bob").withSurname("Doe").produce())
      .withCurrentRestriction(true)
      .produce()

    every { mockApDeliusContextApiClient.getSummariesForCrns(listOf(resultBody.crn)) } returns Success(
      status = HttpStatus.OK,
      body = CaseSummaries(listOf(resultBody)),
    )

    every {
      mockApDeliusContextApiClient.getUserAccessForCrns(
        "distinguished.name",
        listOf(resultBody.crn),
      )
    } returns Success(
      status = HttpStatus.OK,
      body = UserAccess(
        listOf(
          CaseAccess(
            resultBody.crn,
            userRestricted = true,
            userExcluded = false,
            restrictionMessage = null,
            exclusionMessage = null,
          ),
        ),
      ),
    )
    assertThat(offenderService.getOffenderByCrn(resultBody.crn, "distinguished.name", true) is AuthorisableActionResult.Success).isTrue
  }

  @Test
  fun `getOffenderByCrn returns Unauthorised result when distinguished name is excluded from viewing`() {
    val resultBody = CaseSummaryFactory()
      .withCrn("A123456")
      .withName(NameFactory().withForename("Bob").withSurname("Doe").produce())
      .withCurrentRestriction(true)
      .produce()

    every { mockApDeliusContextApiClient.getSummariesForCrns(listOf(resultBody.crn)) } returns Success(
      status = HttpStatus.OK,
      body = CaseSummaries(listOf(resultBody)),
    )

    every {
      mockApDeliusContextApiClient.getUserAccessForCrns(
        "distinguished.name",
        listOf(resultBody.crn),
      )
    } returns Success(
      status = HttpStatus.OK,
      body = UserAccess(
        listOf(
          CaseAccess(
            resultBody.crn,
            userRestricted = true,
            userExcluded = false,
            restrictionMessage = null,
            exclusionMessage = null,
          ),
        ),
      ),
    )

    assertThat(offenderService.getOffenderByCrn(resultBody.crn, "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getOffenderByCrn returns Unauthorised result when Client returns 403 with valid user access response body`() {
    val resultBody = CaseSummaryFactory()
      .withCrn("A123456")
      .withName(NameFactory().withForename("Bob").withSurname("Doe").produce())
      .withCurrentExclusion(true)
      .produce()

    every { mockApDeliusContextApiClient.getSummariesForCrns(listOf(resultBody.crn)) } returns Success(
      status = HttpStatus.OK,
      body = CaseSummaries(listOf(resultBody)),
    )

    every {
      mockApDeliusContextApiClient.getUserAccessForCrns(
        "distinguished.name",
        listOf(resultBody.crn),
      )
    } returns Success(
      status = HttpStatus.OK,
      body = UserAccess(
        listOf(
          CaseAccess(
            resultBody.crn,
            userRestricted = false,
            userExcluded = true,
            restrictionMessage = null,
            exclusionMessage = null,
          ),
        ),
      ),
    )

    assertThat(offenderService.getOffenderByCrn(resultBody.crn, "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getOffenderByCrn returns OffenderDetails when LAO restrictions are passed`() {
    val resultBody = CaseSummaryFactory()
      .withCrn("A123456")
      .withName(NameFactory().withForename("Bob").withSurname("Doe").produce())
      .withCurrentExclusion(true)
      .produce()

    every { mockApDeliusContextApiClient.getSummariesForCrns(listOf(resultBody.crn)) } returns Success(
      status = HttpStatus.OK,
      body = CaseSummaries(listOf(resultBody)),
    )

    every {
      mockApDeliusContextApiClient.getUserAccessForCrns(
        "distinguished.name",
        listOf(resultBody.crn),
      )
    } returns Success(
      status = HttpStatus.OK,
      body = UserAccess(
        listOf(
          CaseAccess(
            resultBody.crn,
            userRestricted = false,
            userExcluded = false,
            restrictionMessage = null,
            exclusionMessage = null,
          ),
        ),
      ),
    )

    val result = offenderService.getOffenderByCrn(resultBody.crn, "distinguished.name")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.otherIds.crn).isEqualTo(resultBody.crn)
    assertThat(result.entity.firstName).isEqualTo("Bob")
    assertThat(result.entity.surname).isEqualTo("Doe")
  }

  @Test
  fun `getRisksByCrn returns NotFound result when Community API Client returns 404`() {
    every { mockApDeliusContextApiClient.getCaseDetail("a-crn") } returns StatusCode(HttpMethod.GET, "/probation-cases/a-crn/details", HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getRiskByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getRisksByCrn returns Unauthorised result when distinguished name is excluded from viewing`() {
    val detail = CaseDetailFactory().withCase(
      CaseSummaryFactory()
        .withCrn("R123456")
        .withName(NameFactory().withForename("Bob").withSurname("Doe").produce())
        .withCurrentExclusion(true)
        .produce(),
    ).produce()

    val accessBody = UserAccess(
      listOf(
        CaseAccess(
          crn = detail.case.crn,
          userRestricted = false,
          userExcluded = true,
          restrictionMessage = null,
          exclusionMessage = "Excluded",
        ),
      ),
    )

    every { mockApDeliusContextApiClient.getCaseDetail(detail.case.crn) } returns Success(HttpStatus.OK, detail)
    every { mockApDeliusContextApiClient.getUserAccessForCrns("distinguished.name", listOf(detail.case.crn)) } returns Success(HttpStatus.OK, accessBody)

    assertThat(offenderService.getRiskByCrn(detail.case.crn, "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getRisksByCrn returns NotFound envelopes for RoSH, Tier, Mappa & flags when respective Clients return 404`() {
    val crn = "a-crn"
    val distinguishedName = "distinguished.name"

    mockExistingNonLaoOffender()
    mock404RoSH(crn)
    mock404Tier(crn)
    mock404Registrations(crn)

    val result = offenderService.getRiskByCrn(crn, distinguishedName)
    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.roshRisks.status).isEqualTo(RiskStatus.NotFound)
    assertThat(result.entity.tier.status).isEqualTo(RiskStatus.NotFound)
    assertThat(result.entity.mappa.status).isEqualTo(RiskStatus.NotFound)
    assertThat(result.entity.flags.status).isEqualTo(RiskStatus.NotFound)
  }

  @Test
  fun `getRisksByCrn returns Error envelopes for RoSH, Tier, Mappa & flags when respective Clients return 500`() {
    val crn = "T123456"
    val distinguishedName = "distinguished.name"

    mockExistingNonLaoOffender()
    mock500RoSH(crn)
    mock500Tier(crn)
    mock500Registrations(crn)

    val result = offenderService.getRiskByCrn(crn, distinguishedName)
    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.roshRisks.status).isEqualTo(RiskStatus.Error)
    assertThat(result.entity.tier.status).isEqualTo(RiskStatus.Error)
    assertThat(result.entity.mappa.status).isEqualTo(RiskStatus.Error)
    assertThat(result.entity.flags.status).isEqualTo(RiskStatus.Error)
  }

  @Test
  fun `getRisksByCrn returns Retrieved envelopes with expected contents for RoSH, Tier, Mappa & flags when respective Clients return 200`() {
    val crn = "a-crn"
    val distinguishedName = "distinguished.name"

    mockExistingNonLaoOffender()

    mock200RoSH(
      crn,
      RoshRatingsFactory().apply {
        withDateCompleted(OffsetDateTime.parse("2022-09-06T13:45:00Z"))
        withAssessmentId(34853487)
        withRiskChildrenCommunity(RiskLevel.LOW)
        withRiskPublicCommunity(RiskLevel.MEDIUM)
        withRiskKnownAdultCommunity(RiskLevel.HIGH)
        withRiskStaffCommunity(RiskLevel.VERY_HIGH)
      }.produce(),
    )

    mock200Tier(
      crn,
      Tier(
        tierScore = "M2",
        calculationId = UUID.randomUUID(),
        calculationDate = LocalDateTime.parse("2022-09-06T14:59:00"),
      ),
    )

    mock200Registrations(
      crn,
      CaseDetailFactory()
        .withRegistrations(
          listOf(
            Registration("FLAG", "RISK FLAG", LocalDate.now()),
          ),
        )
        .withMappaDetail(
          MappaDetail(1, "L1", 1, "C1", LocalDate.parse("2022-09-06"), ZonedDateTime.parse("2022-09-06T09:02:13.451Z")),
        )
        .produce(),
    )

    val result = offenderService.getRiskByCrn(crn, distinguishedName)
    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success

    assertThat(result.entity.roshRisks.status).isEqualTo(RiskStatus.Retrieved)
    result.entity.roshRisks.value!!.let {
      assertThat(it.lastUpdated).isEqualTo(LocalDate.parse("2022-09-06"))
      assertThat(it.overallRisk).isEqualTo("Very High")
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
  fun `getInmateDetailByNomsNumber returns not found result when for Offender without Application or Booking and Client responds with 404`() {
    val crn = "CRN123"
    val nomsNumber = "NOMS321"

    every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

    val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getInmateDetailByNomsNumber returns not found result when for Offender with Application or Booking and Client responds with 404`() {
    val crn = "CRN123"
    val nomsNumber = "NOMS321"

    every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

    val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getInmateDetailByNomsNumber returns unauthorised result when Client responds with 403`() {
    val crn = "CRN123"
    val nomsNumber = "NOMS321"

    every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.FORBIDDEN, null)

    val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getInmateDetailByNomsNumber returns successfully when Client responds with 200`() {
    val crn = "CRN123"
    val nomsNumber = "NOMS321"

    every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns Success(
      HttpStatus.OK,
      InmateDetail(
        offenderNo = nomsNumber,
        inOutStatus = InOutStatus.IN,
        assignedLivingUnit = AssignedLivingUnit(
          agencyId = "AGY",
          locationId = 89,
          description = "AGENCY DESCRIPTION",
          agencyName = "AGENCY NAME",
        ),
      ),
    )

    val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

    assertThat(result is AuthorisableActionResult.Success)
    result as AuthorisableActionResult.Success
    assertThat(result.entity).isNotNull
    assertThat(result.entity!!.offenderNo).isEqualTo(nomsNumber)
    assertThat(result.entity!!.inOutStatus).isEqualTo(InOutStatus.IN)
    assertThat(result.entity!!.assignedLivingUnit).isEqualTo(
      AssignedLivingUnit(
        agencyId = "AGY",
        locationId = 89,
        description = "AGENCY DESCRIPTION",
        agencyName = "AGENCY NAME",
      ),
    )
  }

  @Test
  fun `getPrisonCaseNotesByNomsNumber returns NotFound when Case Notes request returns a 404`() {
    val nomsNumber = "NOMS456"

    every {
      mockCaseNotesClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 0,
        pageSize = 2,
      )
    } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/v2", HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getPrisonCaseNotesByNomsNumber(nomsNumber) is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getPrisonCaseNotesByNomsNumber returns Unauthorised when Case Notes request returns a 403`() {
    val nomsNumber = "NOMS456"

    every {
      mockCaseNotesClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 0,
        pageSize = 2,
      )
    } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/v2", HttpStatus.FORBIDDEN, null)

    assertThat(offenderService.getPrisonCaseNotesByNomsNumber(nomsNumber) is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getPrisonCaseNotesByNomsNumber returns Success, traverses pages from Client & excludes categories + subcategories`() {
    val nomsNumber = "NOMS456"

    val caseNotesPageOne = listOf(
      CaseNoteFactory().produce(),
      CaseNoteFactory().produce(),
      CaseNoteFactory().withType("EXCLUDED_TYPE").produce(),
    )

    val caseNotesPageTwo = listOf(
      CaseNoteFactory().produce(),
      CaseNoteFactory().produce(),
      CaseNoteFactory().withType("TYPE").withSubType("EXCLUDED_SUBTYPE").produce(),
    )

    every {
      mockCaseNotesClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 0,
        pageSize = 2,
      )
    } returns Success(
      HttpStatus.OK,
      CaseNotesPage(
        totalElements = 6,
        totalPages = 2,
        number = 1,
        content = caseNotesPageOne,
      ),
    )

    every {
      mockCaseNotesClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 1,
        pageSize = 2,
      )
    } returns Success(
      HttpStatus.OK,
      CaseNotesPage(
        totalElements = 4,
        totalPages = 2,
        number = 2,
        content = caseNotesPageTwo,
      ),
    )

    val result = offenderService.getPrisonCaseNotesByNomsNumber(nomsNumber)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity).containsAll(caseNotesPageOne.subList(0, 1) + caseNotesPageTwo.subList(0, 1))
  }

  @Test
  fun `getAdjudicationsByNomsNumber returns NotFound when Adjudications request returns a 404`() {
    val nomsNumber = "NOMS456"

    every {
      mockAdjudicationsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        page = 0,
        pageSize = 2,
      )
    } returns StatusCode(
      HttpMethod.GET,
      "/adjudications/$nomsNumber/adjudications",
      HttpStatus.NOT_FOUND,
      null,
    )

    assertThat(offenderService.getAdjudicationsByNomsNumber(nomsNumber) is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getAdjudicationsByNomsNumber returns Unauthorised when Case Notes request returns a 403`() {
    val nomsNumber = "NOMS456"

    every {
      mockAdjudicationsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        page = 0,
        pageSize = 2,
      )
    } returns StatusCode(
      HttpMethod.GET,
      "/adjudications/$nomsNumber/adjudications",
      HttpStatus.FORBIDDEN,
      null,
    )

    assertThat(offenderService.getAdjudicationsByNomsNumber(nomsNumber) is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getAdjudicationsByNomsNumber returns Success, traverses pages from Client`() {
    val nomsNumber = "NOMS456"

    val adjudicationsPageOne = AdjudicationsPageFactory()
      .withResults(
        listOf(
          AdjudicationFactory().withAgencyId("AGNCY1").produce(),
          AdjudicationFactory().withAgencyId("AGNCY2").produce(),
        ),
      )
      .withAgencies(
        listOf(
          AgencyFactory().withAgencyId("AGNCY1").produce(),
          AgencyFactory().withAgencyId("AGNCY2").produce(),
        ),
      )
      .produce()

    val adjudicationsPageTwo = AdjudicationsPageFactory()
      .withResults(
        listOf(
          AdjudicationFactory().withAgencyId("AGNCY3").produce(),
        ),
      )
      .withAgencies(
        listOf(
          AgencyFactory().withAgencyId("AGNCY3").produce(),
        ),
      )
      .produce()

    every {
      mockAdjudicationsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        page = 0,
        pageSize = 2,
      )
    } returns Success(
      HttpStatus.OK,
      adjudicationsPageOne,
    )

    every {
      mockAdjudicationsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        page = 1,
        pageSize = 2,
      )
    } returns Success(
      HttpStatus.OK,
      adjudicationsPageTwo,
    )

    val result = offenderService.getAdjudicationsByNomsNumber(nomsNumber)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.results.content).containsAll(
      adjudicationsPageOne.results.content.plus(adjudicationsPageTwo.results.content),
    )
    assertThat(result.entity.agencies).containsExactlyInAnyOrder(
      *adjudicationsPageOne.agencies.union(adjudicationsPageTwo.agencies).toTypedArray(),
    )
  }

//  @Test
//  fun `isLao returns true for Offender with current restriction`() {
//    val offenderDetails = OffenderDetailsSummaryFactory()
//      .withCurrentRestriction(true)
//      .withCurrentExclusion(false)
//      .produce()
//
//    every { mockCommunityApiClient.getOffenderDetailSummaryWithWait(offenderDetails.otherIds.crn) } returns ClientResult.Success(
//      HttpStatus.OK,
//      offenderDetails,
//    )
//
//    assertThat(offenderService.isLao(offenderDetails.otherIds.crn)).isTrue
//  }

//  @Test
//  fun `isLao returns true for Offender with current exclusion`() {
//    val offenderDetails = OffenderDetailsSummaryFactory()
//      .withCurrentRestriction(false)
//      .withCurrentExclusion(true)
//      .produce()
//
//    every { mockCommunityApiClient.getOffenderDetailSummaryWithWait(offenderDetails.otherIds.crn) } returns ClientResult.Success(
//      HttpStatus.OK,
//      offenderDetails,
//    )
//
//    assertThat(offenderService.isLao(offenderDetails.otherIds.crn)).isTrue
//  }
//
//  @Test
//  fun `isLao returns false for Offender without current exclusion or restriction`() {
//    val offenderDetails = OffenderDetailsSummaryFactory()
//      .withCurrentRestriction(false)
//      .withCurrentExclusion(false)
//      .produce()
//
//    every { mockCommunityApiClient.getOffenderDetailSummaryWithWait(offenderDetails.otherIds.crn) } returns ClientResult.Success(
//      HttpStatus.OK,
//      offenderDetails,
//    )
//
//    assertThat(offenderService.isLao(offenderDetails.otherIds.crn)).isFalse
//  }

  @Nested
  inner class GetInfoForPerson {

    @Test
    fun `returns Unknown if Community API responds with a 500`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      every { mockApDeliusContextApiClient.getSummariesForCrns(listOf(crn)) } returns StatusCode(
        HttpMethod.GET,
        "/probation-cases/summaries",
        HttpStatus.INTERNAL_SERVER_ERROR,
        null,
        false,
      )

      val result = offenderService.getInfoForPerson(crn, deliusUsername, false)

      assertThat(result is PersonInfoResult.Unknown).isTrue
      result as PersonInfoResult.Unknown
      assertThat(result.throwable).isNotNull()
    }

    @Test
    fun `returns Restricted for LAO Offender where user does not pass check and ignoreLao is false`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = CaseSummaryFactory()
        .withCrn(crn)
        .produce()

      every { mockApDeliusContextApiClient.getSummariesForCrns(listOf(crn)) } returns Success(
        status = HttpStatus.OK,
        body = CaseSummaries(listOf(offenderDetails)),
      )

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          deliusUsername,
          listOf(crn),
        )
      } returns Success(
        status = HttpStatus.OK,
        body = UserAccess(
          listOf(
            CaseAccess(
              crn,
              userRestricted = true,
              userExcluded = false,
              restrictionMessage = null,
              exclusionMessage = null,
            ),
          ),
        ),
      )

      val result = offenderService.getInfoForPerson(crn, deliusUsername, false)

      assertThat(result is PersonInfoResult.Success.Restricted).isTrue
      result as PersonInfoResult.Success.Restricted
      assertThat(result.crn).isEqualTo(crn)
    }

    @Test
    fun `returns Full for LAO Offender where user does pass check and ignoreLao is false`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = CaseSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .produce().copy(nomsId = null)

      every { mockApDeliusContextApiClient.getSummariesForCrns(listOf(crn)) } returns Success(
        status = HttpStatus.OK,
        body = CaseSummaries(listOf(offenderDetails)),
      )

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          deliusUsername,
          listOf(crn),
        )
      } returns Success(
        status = HttpStatus.OK,
        body = UserAccess(
          listOf(
            CaseAccess(
              crn,
              userRestricted = false,
              userExcluded = false,
              restrictionMessage = null,
              exclusionMessage = null,
            ),
          ),
        ),
      )

      val result = offenderService.getInfoForPerson(crn, deliusUsername, false)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails.asOffenderDetail())
      assertThat(result.inmateDetail).isEqualTo(null)
    }

    @Test
    fun `returns Full for LAO Offender where user does not pass check but ignoreLao is true`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = CaseSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .produce().copy(nomsId = null)

      every { mockApDeliusContextApiClient.getSummariesForCrns(listOf(crn)) } returns Success(
        status = HttpStatus.OK,
        body = CaseSummaries(listOf(offenderDetails)),
      )

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          deliusUsername,
          listOf(crn),
        )
      } returns Success(
        status = HttpStatus.OK,
        body = UserAccess(
          listOf(
            CaseAccess(
              crn,
              userRestricted = true,
              userExcluded = false,
              restrictionMessage = null,
              exclusionMessage = null,
            ),
          ),
        ),
      )

      val result = offenderService.getInfoForPerson(crn, deliusUsername, true)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails.asOffenderDetail())
      assertThat(result.inmateDetail).isEqualTo(null)
    }

    @Test
    fun `returns Full for CRN with both Community API and Prison API data where Community API links to Prison API`() {
      val crn = "ABC123"
      val nomsNumber = "NOMSABC"
      val deliusUsername = "USER"

      val offenderDetails = CaseSummaryFactory()
        .withCrn(crn)
        .withNomsId(nomsNumber)
        .produce()

      every { mockApDeliusContextApiClient.getSummariesForCrns(listOf(crn)) } returns Success(
        status = HttpStatus.OK,
        body = CaseSummaries(listOf(offenderDetails)),
      )

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          deliusUsername,
          listOf(crn),
        )
      } returns Success(
        status = HttpStatus.OK,
        body = UserAccess(
          listOf(
            CaseAccess(
              crn,
              userRestricted = false,
              userExcluded = false,
              restrictionMessage = null,
              exclusionMessage = null,
            ),
          ),
        ),
      )

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      val result = offenderService.getInfoForPerson(crn, deliusUsername, false)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails.asOffenderDetail())
      assertThat(result.inmateDetail).isEqualTo(inmateDetail)
    }
  }

  @Nested
  inner class GetOffenderSummariesByCrns {
    private val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    private val crns = listOf(
      "ABC1",
      "ABC2",
      "ABC3",
      "NOTFOUND",
    )

    private val caseSummaries = listOf(
      CaseSummaryFactory().withCrn(crns[0]).produce(),
      CaseSummaryFactory().withCrn(crns[1]).produce(),
      CaseSummaryFactory().withCrn(crns[2]).produce(),
    )

    @Test
    fun `it returns full summaries when the user has the correct access`() {
      val caseAccess = crns.map {
        CaseAccessFactory()
          .withCrn(it)
          .withUserExcluded(false)
          .withUserRestricted(false)
          .produce()
      }

      every { mockApDeliusContextApiClient.getSummariesForCrns(crns) } returns Success(
        status = HttpStatus.OK,
        body = CaseSummaries(
          caseSummaries,
        ),
      )

      every { mockApDeliusContextApiClient.getUserAccessForCrns(user.deliusUsername, crns) } returns Success(
        status = HttpStatus.OK,
        body = UserAccess(
          access = caseAccess,
        ),
      )

      val result = offenderService.getOffenderSummariesByCrns(crns, user.deliusUsername, false)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[0], caseSummaries[0]))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[1], caseSummaries[1]))
      assertThat(result[2]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[2], caseSummaries[2]))
      assertThat(result[3]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[3]))
    }

    @Test
    fun `it returns full and restricted summaries when the user has the access for some CRNs, but not others`() {
      val caseAccess = listOf(
        CaseAccessFactory()
          .withCrn(crns[0])
          .withUserExcluded(true)
          .withUserRestricted(false)
          .produce(),
        CaseAccessFactory()
          .withCrn(crns[1])
          .withUserExcluded(false)
          .withUserRestricted(true)
          .produce(),
        CaseAccessFactory()
          .withCrn(crns[2])
          .withUserExcluded(false)
          .withUserRestricted(false)
          .produce(),
      )

      every { mockApDeliusContextApiClient.getSummariesForCrns(crns) } returns Success(
        status = HttpStatus.OK,
        body = CaseSummaries(
          caseSummaries,
        ),
      )

      every { mockApDeliusContextApiClient.getUserAccessForCrns(user.deliusUsername, crns) } returns Success(
        status = HttpStatus.OK,
        body = UserAccess(
          access = caseAccess,
        ),
      )

      val result = offenderService.getOffenderSummariesByCrns(crns, user.deliusUsername, false)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Restricted(crns[0], caseSummaries[0].nomsId))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.Success.Restricted(crns[1], caseSummaries[1].nomsId))
      assertThat(result[2]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[2], caseSummaries[2]))
      assertThat(result[3]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[3]))
    }

    @Test
    fun `it ignores LAO when ignoreLao is set to true`() {
      val caseAccess = listOf(
        CaseAccessFactory()
          .withCrn(crns[0])
          .withUserExcluded(true)
          .withUserRestricted(false)
          .produce(),
        CaseAccessFactory()
          .withCrn(crns[1])
          .withUserExcluded(false)
          .withUserRestricted(true)
          .produce(),
        CaseAccessFactory()
          .withCrn(crns[2])
          .withUserExcluded(false)
          .withUserRestricted(false)
          .produce(),
      )

      every { mockApDeliusContextApiClient.getSummariesForCrns(crns) } returns Success(
        status = HttpStatus.OK,
        body = CaseSummaries(
          caseSummaries,
        ),
      )

      every { mockApDeliusContextApiClient.getUserAccessForCrns(user.deliusUsername, crns) } returns Success(
        status = HttpStatus.OK,
        body = UserAccess(
          access = caseAccess,
        ),
      )

      val result = offenderService.getOffenderSummariesByCrns(crns, user.deliusUsername, true)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[0], caseSummaries[0]))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[1], caseSummaries[1]))
      assertThat(result[2]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[2], caseSummaries[2]))
      assertThat(result[3]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[3]))
    }
  }

  private fun mockExistingNonLaoOffender() {
    val detail = CaseSummaryFactory()
      .withCrn("T123456")
      .withName(NameFactory().withForename("Bob").withSurname("Doe").produce())
      .withCurrentRestriction(false)
      .withCurrentExclusion(false)
      .produce()

    every { mockApDeliusContextApiClient.getSummariesForCrns(listOf(detail.crn)) } returns Success(
      HttpStatus.OK,
      CaseSummaries(listOf(detail)),
    )
  }

  private fun mock404RoSH(crn: String) = every { mockApOASysContextApiClient.getRoshRatings(crn) } returns StatusCode(HttpMethod.GET, "/rosh/a-crn", HttpStatus.NOT_FOUND, body = null)
  private fun mock404Tier(crn: String) = every { mockHMPPSTierApiClient.getTier(crn) } returns StatusCode(HttpMethod.GET, "/crn/a-crn/tier", HttpStatus.NOT_FOUND, body = null)
  private fun mock404Registrations(crn: String) = every { mockApDeliusContextApiClient.getCaseDetail(crn) } returns Success(
    status = HttpStatus.OK,
    body = CaseDetailFactory().withCase(CaseSummaryFactory().withCrn(crn).produce()).withRegistrations(listOf()).withMappaDetail(null).produce(),
  )

  private fun mock500RoSH(crn: String) = every { mockApOASysContextApiClient.getRoshRatings(crn) } returns StatusCode(HttpMethod.GET, "/rosh/a-crn", HttpStatus.INTERNAL_SERVER_ERROR, body = null)
  private fun mock500Tier(crn: String) = every { mockHMPPSTierApiClient.getTier(crn) } returns StatusCode(HttpMethod.GET, "/crn/a-crn/tier", HttpStatus.INTERNAL_SERVER_ERROR, body = null)
  private fun mock500Registrations(crn: String) = every { mockApDeliusContextApiClient.getCaseDetail(crn) } returns StatusCode(HttpMethod.GET, "/probation-cases/$crn/details", HttpStatus.INTERNAL_SERVER_ERROR, body = null)

  private fun mock200RoSH(crn: String, body: RoshRatings) = every { mockApOASysContextApiClient.getRoshRatings(crn) } returns Success(HttpStatus.OK, body = body)
  private fun mock200Tier(crn: String, body: Tier) = every { mockHMPPSTierApiClient.getTier(crn) } returns Success(HttpStatus.OK, body = body)
  private fun mock200Registrations(crn: String, body: CaseDetail) = every { mockApDeliusContextApiClient.getCaseDetail(crn) } returns Success(
    HttpStatus.OK,
    body = body,
  )
}
