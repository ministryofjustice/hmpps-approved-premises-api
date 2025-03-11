package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2v2

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ProbationOffenderSearchApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOffenderDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.IDs
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2OffenderService

class Cas2v2OffenderServiceTest {
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockProbationOffenderSearchClient = mockk<ProbationOffenderSearchApiClient>()

  private val offenderService = Cas2v2OffenderService(
    mockPrisonsApiClient,
    mockProbationOffenderSearchClient,
  )

  @Nested
  inner class GetPersonByNomsNumber {
    val nomsNumber = "DEF123"

    @Test
    fun `returns NotFound result when Probation Offender Search returns 404`() {
      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns StatusCode(HttpMethod.POST, "/search", HttpStatus.NOT_FOUND, null)

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber) is ProbationOffenderSearchResult.NotFound).isTrue
    }

    @Test
    fun `returns NotFound result when Probation Offender Search does not return a matching offender`() {
      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns ClientResult.Success(
        HttpStatus.OK,
        emptyList(),
      )

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber) is ProbationOffenderSearchResult.NotFound).isTrue
    }

    @Test
    fun `returns Forbidden result when the matching offender has exclusion`() {
      val offenderDetails = ProbationOffenderDetailFactory()
        .withCurrentExclusion(true)
        .withOtherIds(otherIds = IDs(nomsNumber))
        .produce()

      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns ClientResult.Success(
        HttpStatus.OK,
        listOf(offenderDetails),
      )

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber) is ProbationOffenderSearchResult.Forbidden).isTrue
    }

    @Test
    fun `returns Forbidden result when the matching offender has restriction`() {
      val offenderDetails = ProbationOffenderDetailFactory()
        .withCurrentRestriction(true)
        .withOtherIds(otherIds = IDs(nomsNumber))
        .produce()

      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns ClientResult.Success(
        HttpStatus.OK,
        listOf(offenderDetails),
      )

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber) is ProbationOffenderSearchResult.Forbidden).isTrue
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

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber) is ProbationOffenderSearchResult.Success).isTrue
    }

    @Test
    fun `returns Unknown if Probation Offender Search responds with a 500`() {
      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns StatusCode(HttpMethod.GET, "/search", HttpStatus.INTERNAL_SERVER_ERROR, null, true)

      val result = offenderService.getPersonByNomsNumber(nomsNumber)

      assertThat(result is ProbationOffenderSearchResult.Unknown).isTrue
      result as ProbationOffenderSearchResult.Unknown
      assertThat(result.throwable).isNotNull()
    }

    @Test
    fun `returns not found if Prison API does not find a matching offender`() {
      val crn = "ABC123"

      val offenderDetails = ProbationOffenderDetailFactory()
        .withOtherIds(otherIds = IDs(crn = crn, nomsNumber = nomsNumber))
        .produce()

      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = listOf(offenderDetails),
      )

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.BAD_GATEWAY, null)

      val result = offenderService.getPersonByNomsNumber(nomsNumber)

      assertThat(result is ProbationOffenderSearchResult.NotFound).isTrue
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

      val result = offenderService.getPersonByNomsNumber(nomsNumber)

      assertThat(result is ProbationOffenderSearchResult.Success.Full).isTrue
      result as ProbationOffenderSearchResult.Success.Full
      assertThat(result.probationOffenderDetail.otherIds.crn).isEqualTo(crn)
      assertThat(result.probationOffenderDetail.otherIds.nomsNumber).isEqualTo(offenderDetails.otherIds.nomsNumber)
      assertThat(result.inmateDetail).isEqualTo(inmateDetail)
      assertThat(result.inmateDetail?.offenderNo).isEqualTo(nomsNumber)
    }
  }

  @Nested
  inner class GetInmateDetailByNomsNumber {
    @Test
    fun `returns not found result when for Offender without Application or Booking and Client responds with 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

      val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns not found result when for Offender with Application or Booking and Client responds with 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

      val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns unauthorised result when Client responds with 403`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.FORBIDDEN, null)

      val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

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
  }
}
