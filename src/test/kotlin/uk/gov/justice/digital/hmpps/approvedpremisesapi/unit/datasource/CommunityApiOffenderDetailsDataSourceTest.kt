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

  private companion object {
    @JvmStatic
    fun cacheableOffenderDetailSummaryClientResults(): Stream<Arguments> {
      val successBody = OffenderDetailsSummaryFactory()
        .withCrn("SOME-CRN")
        .produce()

      return Stream.of(
        Arguments.of(
          ClientResult.Failure.CachedValueUnavailable<OffenderDetailSummary>("some-cache-key"),
        ),
        Arguments.of(
          ClientResult.Failure.StatusCode<OffenderDetailSummary>(
            HttpMethod.GET,
            "/",
            HttpStatus.NOT_FOUND,
            null,
            false,
          ),
        ),
        Arguments.of(
          ClientResult.Failure.Other<OffenderDetailSummary>(
            HttpMethod.POST,
            "/",
            RuntimeException("Some error"),
          ),
        ),
        Arguments.of(
          ClientResult.Success(HttpStatus.OK, successBody, true),
        ),
      )
    }

    @JvmStatic
    fun <T> cacheTimeoutClientResult() =
      ClientResult.Failure.PreemptiveCacheTimeout<T>("some-cache", "some-cache-key", 1000)
  }
}
