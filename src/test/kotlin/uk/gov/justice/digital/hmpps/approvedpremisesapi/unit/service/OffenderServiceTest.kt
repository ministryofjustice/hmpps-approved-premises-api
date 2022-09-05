package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.AssessRisksAndNeedsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.lang.RuntimeException

class OffenderServiceTest {
  private val mockCommunityApiClient = mockk<CommunityApiClient>()
  private val mockAssessRisksAndNeedsApiClient = mockk<AssessRisksAndNeedsApiClient>()

  private val offenderService = OffenderService(mockCommunityApiClient, mockAssessRisksAndNeedsApiClient)

  @Test
  fun `getOffenderByCrn returns null when Client returns 404`() {
    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.StatusCodeFailure(HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name")).isNull()
  }

  @Test
  fun `getOffenderByCrn throws when Client returns other non-2xx status code`() {
    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.StatusCodeFailure(HttpStatus.BAD_REQUEST, null)

    val exception = assertThrows<RuntimeException> { offenderService.getOffenderByCrn("a-crn", "distinguished.name") }
    assertThat(exception.message).isEqualTo("Unable to complete request: 400 BAD_REQUEST")
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

    assertThat(result!!.otherIds!!.crn).isEqualTo("a-crn")
    assertThat(result.firstName).isEqualTo("Bob")
    assertThat(result.surname).isEqualTo("Doe")
  }

  @Test
  fun `getOffenderByCrn throws a ForbiddenProblem when distinguished name is excluded from viewing`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true)

    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockCommunityApiClient.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThrows<ForbiddenProblem> { offenderService.getOffenderByCrn("a-crn", "distinguished.name") }
  }

  @Test
  fun `getOffenderByCrn throws a ForbiddenProblem when distinguished name is not explicitly allowed to view`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentRestriction(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = true, userExcluded = false)

    every { mockCommunityApiClient.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockCommunityApiClient.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThrows<ForbiddenProblem> { offenderService.getOffenderByCrn("a-crn", "distinguished.name") }
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

    assertThat(result!!.otherIds!!.crn).isEqualTo("a-crn")
    assertThat(result.firstName).isEqualTo("Bob")
    assertThat(result.surname).isEqualTo("Doe")
  }
}
