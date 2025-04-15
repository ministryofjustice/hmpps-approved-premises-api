package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2v2

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ProbationOffenderSearchApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOffenderDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.IDs
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2OffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2PersonTransformer

class Cas2v2OffenderServiceTest {
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockProbationOffenderSearchClient = mockk<ProbationOffenderSearchApiClient>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockOffenderDetailsDataSource = mockk<OffenderDetailsDataSource>()

  private val cas2v2PersonTransformer = Cas2v2PersonTransformer()

  private val cas2v2OffenderService = Cas2v2OffenderService(
    mockPrisonsApiClient,
    mockProbationOffenderSearchClient,
    mockApDeliusContextApiClient,
    cas2v2PersonTransformer,
  )

  val deliusUser = Cas2v2UserEntityFactory()
    .withUserType(Cas2v2UserType.DELIUS)
    .produce()

  val nomisUser = Cas2v2UserEntityFactory()
    .withUserType(Cas2v2UserType.NOMIS)
    .produce()

  @Nested
  inner class GetPersonByNomsNumber {
    val nomsNumber = "DEF123"

    @Test
    fun `returns NotFound result when Probation Offender Search returns 404`() {
      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns StatusCode(HttpMethod.POST, "/search", HttpStatus.NOT_FOUND, null)

      assertThat(cas2v2OffenderService.getPersonByNomsNumber(nomsNumber) is Cas2v2OffenderSearchResult.NotFound).isTrue
    }

    @Test
    fun `returns NotFound result when Probation Offender Search does not return a matching offender`() {
      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns ClientResult.Success(
        HttpStatus.OK,
        emptyList(),
      )

      assertThat(cas2v2OffenderService.getPersonByNomsNumber(nomsNumber) is Cas2v2OffenderSearchResult.NotFound).isTrue
    }

    @Test
    fun `returns result when the matching offender is not in the same prison as the user`() {
      val crn = "ABC123"

      val offenderDetails = ProbationOffenderDetailFactory()
        .withOtherIds(otherIds = IDs(crn = crn, nomsNumber = nomsNumber))
        .produce()

      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = listOf(offenderDetails),
      )

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .withAssignedLivingUnit(
          AssignedLivingUnit(
            agencyId = "not-my-prison",
            agencyName = "Not My Prison",
            locationId = 5,
            description = "",
          ),
        )
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      assertThat(cas2v2OffenderService.getPersonByNomsNumber(nomsNumber) is Cas2v2OffenderSearchResult.Success).isTrue
    }

    @Test
    fun `returns Unknown if Probation Offender Search responds with a 500`() {
      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns StatusCode(HttpMethod.GET, "/search", HttpStatus.INTERNAL_SERVER_ERROR, null, true)

      val result = cas2v2OffenderService.getPersonByNomsNumber(nomsNumber)

      assertThat(result is Cas2v2OffenderSearchResult.Unknown).isTrue
      result as Cas2v2OffenderSearchResult.Unknown
      assertThat(result.throwable).isNotNull()
    }

    @Test
    fun `returns not found if Prison API does not find a matching offender`() {
      val crn = "ABC123"

      val offenderDetails = ProbationOffenderDetailFactory()
        .withOtherIds(otherIds = IDs(crn = crn, nomsNumber = nomsNumber))
        .produce()

      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns StatusCode(
        HttpMethod.GET,
        "/search",
        HttpStatus.NOT_FOUND,
        null,
      )

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(
        HttpMethod.GET,
        "/api/offenders/$nomsNumber",
        HttpStatus.NOT_FOUND,
        null,
      )

      val result = cas2v2OffenderService.getPersonByNomsNumber(nomsNumber)

      assertThat(result is Cas2v2OffenderSearchResult.NotFound).isTrue
    }

    @Test
    fun `returns Full Person when Probation Offender Search and Prison API returns a matching offender`() {
      val crn = "ABC123"

      val offenderDetails = ProbationOffenderDetailFactory()
        .withOtherIds(otherIds = IDs(crn = crn, nomsNumber = nomsNumber))
        .produce()

      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = listOf(offenderDetails),
      )

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .withAssignedLivingUnit(
          AssignedLivingUnit(
            agencyId = "my-prison",
            agencyName = "My Prison",
            locationId = 6,
            description = "",
          ),
        )
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      val result = cas2v2OffenderService.getPersonByNomsNumber(nomsNumber)

      assertThat(result is Cas2v2OffenderSearchResult.Success.Full).isTrue
      result as Cas2v2OffenderSearchResult.Success.Full
      assertThat(result.person.crn).isEqualTo(crn)
      assertThat(result.person.nomsNumber).isEqualTo(offenderDetails.otherIds.nomsNumber)
    }
  }

  @Nested
  inner class GetInmateDetailByNomsNumber {
    @Test
    fun `returns not found result when for Offender without Application or Booking and Client responds with 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(
        HttpMethod.GET,
        "/api/offenders/$nomsNumber",
        HttpStatus.NOT_FOUND,
        null,
      )

      val result = cas2v2OffenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns not found result when for Offender with Application or Booking and Client responds with 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(
        HttpMethod.GET,
        "/api/offenders/$nomsNumber",
        HttpStatus.NOT_FOUND,
        null,
      )

      val result = cas2v2OffenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns unauthorised result when Client responds with 403`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(
        HttpMethod.GET,
        "/api/offenders/$nomsNumber",
        HttpStatus.FORBIDDEN,
        null,
      )

      val result = cas2v2OffenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
    }

    @Test
    fun `returns successfully when Client responds with 200`() {
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

      val result = cas2v2OffenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

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
  }

  @Nested
  inner class CheckRestrictedOffender {
    val crn = "CRN123"
    val nomsNumber = "DEF123"

    @BeforeEach
    fun setup() {
      val offenderDetailSummary = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withFirstName("Bob")
        .withLastName("Doe")
        .withCurrentRestriction(true)
        .withCurrentExclusion(false)
        .produce()

      every {
        mockOffenderDetailsDataSource.getOffenderDetailSummary(crn)
      } returns ClientResult.Success(HttpStatus.OK, offenderDetailSummary)

      val caseSummaries = CaseSummaries(
        listOf(
          CaseSummaryFactory()
            .withCrn(crn)
            .withNomsId(nomsNumber)
            .withCurrentRestriction(true)
            .withCurrentExclusion(false)
            .produce(),
        ),
      )

      every {
        mockApDeliusContextApiClient.getSummariesForCrns(listOf(crn))
      } returns ClientResult.Success(HttpStatus.OK, caseSummaries)

      val probationOffenderDetail = ProbationOffenderDetailFactory()
        .withOtherIds(IDs(crn = crn, nomsNumber = nomsNumber))
        .withCurrentRestriction(true)
        .produce()
      every {
        mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber)
      } returns ClientResult.Success(
        HttpStatus.OK,
        listOf(probationOffenderDetail),
      )

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        HttpStatus.OK,
        InmateDetailFactory().produce(),
      )
    }

    @Test
    fun `Check searching by crn cannot view an offender with a currentRestriction`() {
      assertThat(cas2v2OffenderService.getPersonByCrn(crn) is Cas2v2OffenderSearchResult.Forbidden).isTrue
    }

    @Test
    fun `Check searching by nomis cannot view an offender with a currentRestriction`() {
      assertThat(cas2v2OffenderService.getPersonByNomsNumber(nomsNumber) is Cas2v2OffenderSearchResult.Forbidden).isTrue
    }
  }

  @Nested
  inner class CheckExcludedOffender {
    val crn = "CRN123"
    val nomsNumber = "DEF123"

    @BeforeEach
    fun setup() {
      val offenderDetailSummary = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withFirstName("Bob")
        .withLastName("Doe")
        .withCurrentRestriction(false)
        .withCurrentExclusion(true)
        .produce()

      every {
        mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn")
      } returns ClientResult.Success(HttpStatus.OK, offenderDetailSummary)

      val caseSummaries = CaseSummaries(
        listOf(
          CaseSummaryFactory()
            .withCrn(crn)
            .withNomsId(nomsNumber)
            .withCurrentRestriction(true)
            .withCurrentExclusion(false)
            .produce(),
        ),
      )

      every {
        mockApDeliusContextApiClient.getSummariesForCrns(listOf(crn))
      } returns ClientResult.Success(HttpStatus.OK, caseSummaries)

      val probationOffenderDetail = ProbationOffenderDetailFactory()
        .withOtherIds(IDs(crn = crn, nomsNumber = nomsNumber))
        .withCurrentRestriction(false)
        .withCurrentExclusion(true)
        .produce()
      every {
        mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber)
      } returns ClientResult.Success(
        HttpStatus.OK,
        listOf(probationOffenderDetail),
      )

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        HttpStatus.OK,
        InmateDetailFactory().produce(),
      )
    }

    @Test
    fun `Check searching by crn can view an offender with a currentExclusion`() {
      assertThat(cas2v2OffenderService.getPersonByCrn(crn) is Cas2v2OffenderSearchResult.Success.Full).isTrue
    }

    @Test
    fun `Check searching by nomis can view an offender with a currentExclusion`() {
      assertThat(cas2v2OffenderService.getPersonByNomsNumber(nomsNumber) is Cas2v2OffenderSearchResult.Success.Full).isTrue
    }
  }
}
