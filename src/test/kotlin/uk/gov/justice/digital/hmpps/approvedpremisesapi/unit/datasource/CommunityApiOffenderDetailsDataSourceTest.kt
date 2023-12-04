package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.datasource

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.CommunityApiOffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess
import java.util.stream.Stream

class CommunityApiOffenderDetailsDataSourceTest {
  private val mockCommunityApiClient = mockk<CommunityApiClient>()

  private val communityApiOffenderDetailsDataSource = CommunityApiOffenderDetailsDataSource(mockCommunityApiClient)

  @ParameterizedTest
  @MethodSource("cacheableOffenderDetailSummaryClientResults")
  fun `getOffenderDetailSummary returns cached response from Community API when it exists`(
    expectedResult: ClientResult<OffenderDetailSummary>,
  ) {
    every { mockCommunityApiClient.getOffenderDetailSummaryWithWait("SOME-CRN") } returns expectedResult

    val result = communityApiOffenderDetailsDataSource.getOffenderDetailSummary("SOME-CRN")

    assertThat(result).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @MethodSource("cacheableOffenderDetailSummaryClientResults")
  fun `getOffenderDetailSummary returns response from Community API call when cached response does not exist`(
    expectedResult: ClientResult<OffenderDetailSummary>,
  ) {
    every { mockCommunityApiClient.getOffenderDetailSummaryWithWait("SOME-CRN") } returns cacheTimeoutClientResult()

    every { mockCommunityApiClient.getOffenderDetailSummaryWithCall("SOME-CRN") } returns expectedResult

    val result = communityApiOffenderDetailsDataSource.getOffenderDetailSummary("SOME-CRN")

    assertThat(result).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @MethodSource("userOffenderAccessClientResults")
  fun `getUserAccessForOffenderCrn returns response from Community API call`(
    expectedResult: ClientResult<UserOffenderAccess>,
  ) {
    every { mockCommunityApiClient.getUserAccessForOffenderCrn("DELIUS-USER", "SOME-CRN") } returns expectedResult

    val result = communityApiOffenderDetailsDataSource.getUserAccessForOffenderCrn("DELIUS-USER", "SOME-CRN")

    assertThat(result).isEqualTo(expectedResult)
  }

  private companion object {
    @JvmStatic
    fun cacheableOffenderDetailSummaryClientResults(): Stream<Arguments> {
      val successBody = OffenderDetailsSummaryFactory()
        .withCrn("SOME-CRN")
        .produce()

      return allClientResults(successBody)
        .filter { it !is ClientResult.Failure.PreemptiveCacheTimeout }
        .intoArgumentStream()
    }

    @JvmStatic
    fun <T> cacheTimeoutClientResult() =
      ClientResult.Failure.PreemptiveCacheTimeout<T>("some-cache", "some-cache-key", 1000)

    @JvmStatic
    fun userOffenderAccessClientResults(): Stream<Arguments> {
      val successBody = UserOffenderAccess(
        userRestricted = false,
        userExcluded = false,
        restrictionMessage = null,
      )

      return allClientResults(successBody).intoArgumentStream()
    }

    private fun <T> allClientResults(successBody: T): List<ClientResult<T>> = listOf(
      ClientResult.Failure.CachedValueUnavailable("some-cache-key"),
      ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/",
        HttpStatus.NOT_FOUND,
        null,
        false,
      ),
      ClientResult.Failure.Other(
        HttpMethod.POST,
        "/",
        RuntimeException("Some error"),
      ),
      cacheTimeoutClientResult(),
      ClientResult.Success(HttpStatus.OK, successBody, true),
    )

    private fun <T> List<ClientResult<T>>.intoArgumentStream(): Stream<Arguments> = this.stream().map { Arguments.of(it) }
  }
}
