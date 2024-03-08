package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CaseNotesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ExcludedCategoryBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderRisksDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationsPageFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AgencyFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseNoteFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import java.time.LocalDate

class OffenderServiceTest {
  private val mockCommunityApiClient = mockk<CommunityApiClient>()
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockCaseNotesClient = mockk<CaseNotesClient>()
  private val mockApOASysContextApiClient = mockk<ApOASysContextApiClient>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockOffenderDetailsDataSource = mockk<OffenderDetailsDataSource>()
  private val mockOffenderRisksDataSource = mockk<OffenderRisksDataSource>()

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
    mockPrisonsApiClient,
    mockCaseNotesClient,
    mockApOASysContextApiClient,
    mockApDeliusContextApiClient,
    mockOffenderDetailsDataSource,
    mockOffenderRisksDataSource,
    prisonCaseNotesConfigBindingModel,
    adjudicationsConfigBindingModel,
  )

  @Test
  fun `getOffenderByCrn returns NotFound result when Client returns 404`() {
    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getOffenderByCrn throws when Client returns other non-2xx status code except 403`() {
    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.BAD_REQUEST, null)

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

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)

    val result = offenderService.getOffenderByCrn("a-crn", "distinguished.name")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.otherIds.crn).isEqualTo("a-crn")
    assertThat(result.entity.firstName).isEqualTo("Bob")
    assertThat(result.entity.surname).isEqualTo("Doe")
  }

  @Test
  fun `getOffenderByCrn does not enforce LAO when ignoreLao is enabled (because user has LAO qualification)`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name", true) is AuthorisableActionResult.Success).isTrue
  }

  @Test
  fun `getOffenderByCrn returns Unauthorised result when distinguished name is excluded from viewing`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

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

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns StatusCode(
      HttpMethod.GET,
      "/secure/crn/a-crn/user/distinguished.name/userAccess",
      HttpStatus.FORBIDDEN,
      jacksonObjectMapper().writeValueAsString(accessBody),
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

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns StatusCode(
      HttpMethod.GET,
      "/secure/crn/a-crn/user/distinguished.name/userAccess",
      HttpStatus.FORBIDDEN,
      null,
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

    val accessBody = UserOffenderAccess(userRestricted = true, userExcluded = false, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

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

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = false, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)

    val result = offenderService.getOffenderByCrn("a-crn", "distinguished.name")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.otherIds.crn).isEqualTo("a-crn")
    assertThat(result.entity.firstName).isEqualTo("Bob")
    assertThat(result.entity.surname).isEqualTo("Doe")
  }

  @Test
  fun `getRisksByCrn returns NotFound result when Community API Client returns 404`() {
    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getRiskByCrn("a-crn", "jwt", "distinguished.name") is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getRisksByCrn throws when Community API Client returns other non-2xx status code`() {
    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.BAD_REQUEST, null)

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

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThat(offenderService.getRiskByCrn("a-crn", "jwt", "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getRisksByCrn returns Success result with information from offender risks data source`() {
    val crn = "a-crn"
    val deliusUsername = "SOME-USER"

    val expectedRisks = PersonRisksFactory().produce()
    every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns
      ClientResult.Success(
        HttpStatus.OK,
        OffenderDetailsSummaryFactory().produce(),
      )

    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn(crn, deliusUsername) } returns
      ClientResult.Success(
        HttpStatus.OK,
        UserOffenderAccess(userRestricted = false, userExcluded = false, restrictionMessage = null),
      )

    every { mockOffenderRisksDataSource.getPersonRisks(crn) } returns expectedRisks

    val result = offenderService.getRiskByCrn(crn, deliusUsername)
    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity).isEqualTo(expectedRisks)
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

    every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
      HttpStatus.OK,
      InmateDetail(
        offenderNo = nomsNumber,
        custodyStatus = InmateStatus.IN,
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
    assertThat(result.entity!!.custodyStatus).isEqualTo(InmateStatus.IN)
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
    } returns ClientResult.Success(
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
    } returns ClientResult.Success(
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
      mockPrisonsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        pageSize = 2,
        offset = 0,
      )
    } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/adjudications", HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getAdjudicationsByNomsNumber(nomsNumber) is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getAdjudicationsByNomsNumber returns Unauthorised when Case Notes request returns a 403`() {
    val nomsNumber = "NOMS456"

    every {
      mockPrisonsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        pageSize = 2,
        offset = 0,
      )
    } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/adjudications", HttpStatus.FORBIDDEN, null)

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
      mockPrisonsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        pageSize = 2,
        offset = 0,
      )
    } returns ClientResult.Success(
      HttpStatus.OK,
      adjudicationsPageOne,
    )

    every {
      mockPrisonsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        pageSize = 2,
        offset = 2,
      )
    } returns ClientResult.Success(
      HttpStatus.OK,
      adjudicationsPageTwo,
    )

    val result = offenderService.getAdjudicationsByNomsNumber(nomsNumber)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.results).containsAll(
      adjudicationsPageOne.results.plus(adjudicationsPageTwo.results),
    )
    assertThat(result.entity.agencies).containsExactlyInAnyOrder(
      *adjudicationsPageOne.agencies.union(adjudicationsPageTwo.agencies).toTypedArray(),
    )
  }

  @Test
  fun `isLao returns true for Offender with current restriction`() {
    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCurrentRestriction(true)
      .withCurrentExclusion(false)
      .produce()

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary(offenderDetails.otherIds.crn) } returns ClientResult.Success(
      HttpStatus.OK,
      offenderDetails,
    )

    assertThat(offenderService.isLao(offenderDetails.otherIds.crn)).isTrue
  }

  @Test
  fun `isLao returns true for Offender with current exclusion`() {
    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCurrentRestriction(false)
      .withCurrentExclusion(true)
      .produce()

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary(offenderDetails.otherIds.crn) } returns ClientResult.Success(
      HttpStatus.OK,
      offenderDetails,
    )

    assertThat(offenderService.isLao(offenderDetails.otherIds.crn)).isTrue
  }

  @Test
  fun `isLao returns false for Offender without current exclusion or restriction`() {
    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCurrentRestriction(false)
      .withCurrentExclusion(false)
      .produce()

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary(offenderDetails.otherIds.crn) } returns ClientResult.Success(
      HttpStatus.OK,
      offenderDetails,
    )

    assertThat(offenderService.isLao(offenderDetails.otherIds.crn)).isFalse
  }

  @Test
  fun `returns false and parse getUserAccessForOffenderCrn response successfully by ignoring unexpected element in the 403 response`() {
    val crn = "ABC123"
    val deliusUsername = "USER"
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn(deliusUsername, crn) } returns StatusCode(
      status = HttpStatus.FORBIDDEN,
      method = HttpMethod.GET,
      path = "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
      body = "{\"userRestricted\":false,\"userExcluded\":true,\"exclusionMessage\":\"You are excluded\"}",
    )

    val result = offenderService.canAccessOffender(deliusUsername, crn)

    assertThat(result).isFalse()
  }

  @Nested
  inner class GetInfoForPerson {
    @Test
    fun `returns NotFound if Community API responds with a 404`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/ABC123", HttpStatus.NOT_FOUND, null, true)

      val result = offenderService.getInfoForPerson(crn, deliusUsername, false)

      assertThat(result is PersonInfoResult.NotFound).isTrue
    }

    @Test
    fun `returns Unknown if Community API responds with a 500`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/ABC123", HttpStatus.INTERNAL_SERVER_ERROR, null, true)

      val result = offenderService.getInfoForPerson(crn, deliusUsername, false)

      assertThat(result is PersonInfoResult.Unknown).isTrue
      result as PersonInfoResult.Unknown
      assertThat(result.throwable).isNotNull()
    }

    @Test
    fun `getInfoForPerson returns restricted result when LAO calls fail with Forbidden error and without body response`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = offenderDetails,
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn(deliusUsername, crn) } returns StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
        HttpStatus.FORBIDDEN,
        null,
      )

      val result = offenderService.getInfoForPerson(crn, deliusUsername, false)

      assertThat(result is PersonInfoResult.Success.Restricted).isTrue
      result as PersonInfoResult.Success.Restricted
      assertThat(result.crn).isEqualTo(crn)
    }

    @Test
    fun `throws runtime exception when LAO calls fail with BadRequest exception`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = offenderDetails,
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn(deliusUsername, crn) } returns StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
        HttpStatus.BAD_REQUEST,
        null,
      )

      assertThrows<RuntimeException> { offenderService.getInfoForPerson(crn, deliusUsername, false) }
    }

    @Test
    fun `returns Restricted for LAO Offender where user does not pass check and ignoreLao is false`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = OffenderDetailsSummaryFactory()
          .withCrn(crn)
          .withCurrentRestriction(true)
          .produce(),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn(deliusUsername, crn) } returns StatusCode(
        status = HttpStatus.FORBIDDEN,
        method = HttpMethod.GET,
        path = "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
        body = objectMapper.writeValueAsString(
          UserOffenderAccess(
            userRestricted = true,
            userExcluded = false,
            restrictionMessage = null,
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

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = offenderDetails,
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn(deliusUsername, crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = UserOffenderAccess(
          userRestricted = false,
          userExcluded = false,
          restrictionMessage = null,
        ),
      )

      val result = offenderService.getInfoForPerson(crn, deliusUsername, false)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(null)
    }

    @Test
    fun `returns Full for LAO Offender where user does not pass check but ignoreLao is true`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = offenderDetails,
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn(deliusUsername, crn) } returns StatusCode(
        status = HttpStatus.FORBIDDEN,
        method = HttpMethod.GET,
        path = "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
        body = objectMapper.writeValueAsString(
          UserOffenderAccess(
            userRestricted = true,
            userExcluded = false,
            restrictionMessage = null,
          ),
        ),
      )

      val result = offenderService.getInfoForPerson(crn, deliusUsername, true)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(null)
    }

    @Test
    fun `returns Full for CRN with both Community API and Prison API data where Community API links to Prison API`() {
      val crn = "ABC123"
      val nomsNumber = "NOMSABC"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = offenderDetails,
      )

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      val result = offenderService.getInfoForPerson(crn, deliusUsername, false)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
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

    private val offenderSummaryResultsByCrn = mapOf(
      crns[1] to
        ClientResult.Success(
          HttpStatus.OK,
          OffenderDetailsSummaryFactory()
            .withCrn(crns[1])
            .withCurrentExclusion(false)
            .withCurrentRestriction(true)
            .produce(),
          false,
        ),
      crns[0] to
        ClientResult.Success(
          HttpStatus.OK,
          OffenderDetailsSummaryFactory()
            .withCrn(crns[0])
            .withCurrentExclusion(true)
            .withCurrentRestriction(false)
            .produce(),
          false,
        ),
      crns[2] to
        ClientResult.Success(
          HttpStatus.OK,
          OffenderDetailsSummaryFactory()
            .withCrn(crns[2])
            .withCurrentExclusion(false)
            .withCurrentRestriction(false)
            .produce(),
          false,
        ),
      crns[3] to
        ClientResult.Failure.StatusCode(
          HttpMethod.GET,
          "/",
          HttpStatus.NOT_FOUND,
          null,
          false,
        ),
    )

    private val caseSummariesByCrn = offenderSummaryResultsByCrn.mapNotNull {
      val key = it.key
      val value = it.value
      when (value) {
        is ClientResult.Success -> key to value.body.asCaseSummary()
        else -> null
      }
    }.toMap()

    @Test
    fun `it returns an empty list when no CRNs are provided (forceApDeliusContextApi = true)`() {
      val result = offenderService.getOffenderSummariesByCrns(emptySet(), user.deliusUsername, false, true)

      assertThat(result).isEmpty()
    }

    @Test
    fun `it returns full summaries when the user has the correct access (forceApDeliusContextApi = true)`() {
      val caseAccess = crns.map {
        CaseAccessFactory()
          .withCrn(it)
          .withUserExcluded(false)
          .withUserRestricted(false)
          .produce()
      }

      every { mockApDeliusContextApiClient.getSummariesForCrns(crns) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = CaseSummaries(
          caseSummariesByCrn.values.toList(),
        ),
      )

      every { mockApDeliusContextApiClient.getUserAccessForCrns(user.deliusUsername, crns) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = UserAccess(
          access = caseAccess,
        ),
      )

      val result = offenderService.getOffenderSummariesByCrns(crns.toSet(), user.deliusUsername, false, true)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[0], caseSummariesByCrn[crns[0]]!!))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[1], caseSummariesByCrn[crns[1]]!!))
      assertThat(result[2]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[2], caseSummariesByCrn[crns[2]]!!))
      assertThat(result[3]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[3]))
    }

    @Test
    fun `it returns full and restricted summaries when the user has the access for some CRNs, but not others (forceApDeliusContextApi = true)`() {
      val caseAccess = listOf(
        CaseAccessFactory()
          .withCrn(crns[0])
          .withUserExcluded(true)
          .withUserRestricted(false)
          .produce(),
        CaseAccessFactory()
          .withCrn(crns[2])
          .withUserExcluded(false)
          .withUserRestricted(false)
          .produce(),
        CaseAccessFactory()
          .withCrn(crns[1])
          .withUserExcluded(false)
          .withUserRestricted(true)
          .produce(),
      )

      every { mockApDeliusContextApiClient.getSummariesForCrns(crns) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = CaseSummaries(
          caseSummariesByCrn.values.toList(),
        ),
      )

      every { mockApDeliusContextApiClient.getUserAccessForCrns(user.deliusUsername, crns) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = UserAccess(
          access = caseAccess,
        ),
      )

      val result = offenderService.getOffenderSummariesByCrns(crns.toSet(), user.deliusUsername, false, true)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Restricted(crns[0], caseSummariesByCrn[crns[0]]!!.nomsId))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.Success.Restricted(crns[1], caseSummariesByCrn[crns[1]]!!.nomsId))
      assertThat(result[2]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[2], caseSummariesByCrn[crns[2]]!!))
      assertThat(result[3]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[3]))
    }

    @Test
    fun `it ignores LAO when ignoreLao is set to true (forceApDeliusContextApi = true)`() {
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

      every { mockApDeliusContextApiClient.getSummariesForCrns(crns) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = CaseSummaries(
          caseSummariesByCrn.values.toList(),
        ),
      )

      every { mockApDeliusContextApiClient.getUserAccessForCrns(user.deliusUsername, crns) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = UserAccess(
          access = caseAccess,
        ),
      )

      val result = offenderService.getOffenderSummariesByCrns(crns.toSet(), user.deliusUsername, true, true)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[0], caseSummariesByCrn[crns[0]]!!))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[1], caseSummariesByCrn[crns[1]]!!))
      assertThat(result[2]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[2], caseSummariesByCrn[crns[2]]!!))
      assertThat(result[3]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[3]))
    }

    @Test
    fun `it returns an empty list when no CRNs are provided (forceApDeliusContextApi = false)`() {
      val result = offenderService.getOffenderSummariesByCrns(emptySet(), user.deliusUsername, false, false)

      assertThat(result).isEmpty()
    }

    @Test
    fun `it returns not found for CRN if the CRN isn't included in offender details list`() {
      val requestCrns = setOf(crns[0], crns[1])

      every {
        mockOffenderDetailsDataSource.getOffenderDetailSummaries(requestCrns.toList())
      } returns mapOf(
        crns[0] to offenderSummaryResultsByCrn[crns[0]]!!,
      )

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(user.deliusUsername, requestCrns.toList())
      } returns mapOf(
        crns[0] to clientResultSuccess(false, false),
        crns[1] to clientResultSuccess(false, false),
      )

      val result = offenderService.getOffenderSummariesByCrns(requestCrns, user.deliusUsername, false, false)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[0], caseSummariesByCrn[crns[0]]!!))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[1]))
    }

    @Test
    fun `it returns not found for CRN if the CRN isn't included in user access list`() {
      val requestCrns = setOf(crns[0], crns[1])

      every {
        mockOffenderDetailsDataSource.getOffenderDetailSummaries(requestCrns.toList())
      } returns mapOf(
        crns[0] to offenderSummaryResultsByCrn[crns[0]]!!,
        crns[1] to offenderSummaryResultsByCrn[crns[1]]!!,
      )

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(user.deliusUsername, requestCrns.toList())
      } returns mapOf(
        crns[0] to clientResultSuccess(false, false),
      )

      val result = offenderService.getOffenderSummariesByCrns(requestCrns, user.deliusUsername, false, false)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[0], caseSummariesByCrn[crns[0]]!!))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[1]))
    }

    @Test
    fun `it returns full summaries when the user has the correct access (forceApDeliusContextApi = false)`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(crns) } returns offenderSummaryResultsByCrn

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(user.deliusUsername, crns)
      } returns crns.associateWith {
        clientResultSuccess(false, false)
      }

      val result = offenderService.getOffenderSummariesByCrns(crns.toSet(), user.deliusUsername, false, false)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[0], caseSummariesByCrn[crns[0]]!!))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[1], caseSummariesByCrn[crns[1]]!!))
      assertThat(result[2]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[2], caseSummariesByCrn[crns[2]]!!))
      assertThat(result[3]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[3]))
    }

    @Test
    fun `it returns full and restricted summaries when the user has the access for some CRNs, but not others (forceApDeliusContextApi = false)`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(crns) } returns offenderSummaryResultsByCrn

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(user.deliusUsername, crns)
      } returns mapOf(
        crns[1] to clientResultSuccess(true, false),
        crns[0] to clientResultSuccess(false, true),
        crns[3] to
          ClientResult.Failure.StatusCode(
            HttpMethod.GET,
            "/",
            HttpStatus.NOT_FOUND,
            null,
            false,
          ),
        crns[2] to clientResultSuccess(false, false),
      )

      val result = offenderService.getOffenderSummariesByCrns(crns.toSet(), user.deliusUsername, false, false)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Restricted(crns[0], caseSummariesByCrn[crns[0]]!!.nomsId))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.Success.Restricted(crns[1], caseSummariesByCrn[crns[1]]!!.nomsId))
      assertThat(result[2]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[2], caseSummariesByCrn[crns[2]]!!))
      assertThat(result[3]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[3]))
    }

    @Test
    fun `it ignores LAO when ignoreLao is set to true (forceApDeliusContextApi = false)`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(crns) } returns offenderSummaryResultsByCrn

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(user.deliusUsername, crns)
      } returns mapOf(
        crns[0] to clientResultSuccess(false, true),
        crns[1] to clientResultSuccess(true, false),
        crns[2] to clientResultSuccess(false, false),
        crns[3] to
          ClientResult.Failure.StatusCode(
            HttpMethod.GET,
            "/",
            HttpStatus.NOT_FOUND,
            null,
            false,
          ),
      )

      val result = offenderService.getOffenderSummariesByCrns(crns.toSet(), user.deliusUsername, true, false)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[0], caseSummariesByCrn[crns[0]]!!))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[1], caseSummariesByCrn[crns[1]]!!))
      assertThat(result[2]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[2], caseSummariesByCrn[crns[2]]!!))
      assertThat(result[3]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[3]))
    }

    private fun clientResultSuccess(
      userRestricted: Boolean,
      userExcluded: Boolean,
    ) = ClientResult.Success(
      HttpStatus.OK,
      UserOffenderAccess(userRestricted, userExcluded, restrictionMessage = null),
      false,
    )
  }
}
