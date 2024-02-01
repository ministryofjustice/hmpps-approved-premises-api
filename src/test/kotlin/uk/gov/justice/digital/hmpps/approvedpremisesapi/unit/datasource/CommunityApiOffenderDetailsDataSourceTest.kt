package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.datasource

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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

  @Test
  fun `getOffenderDetailSummaries returns cached response or calls Community API for each CRN`() {
    val successBody = OffenderDetailsSummaryFactory()
      .withCrn("CRN-SUCCESS")
      .produce()
    val expectedResults = allClientResultTypes(successBody)

    expectedResults.forEach { (crn, expected) ->
      every { mockCommunityApiClient.getOffenderDetailSummaryWithWait(crn) } returns expected
    }

    every { mockCommunityApiClient.getOffenderDetailSummaryWithCall("CRN-CACHE-TIMEOUT") } returns
      expectedResults.values.first { it is ClientResult.Failure.PreemptiveCacheTimeout }

    val results = communityApiOffenderDetailsDataSource.getOffenderDetailSummaries(expectedResults.keys.toList())

    assertThat(results).isEqualTo(expectedResults)
  }

  @ParameterizedTest
  @MethodSource("userOffenderAccessFailureResultTypes")
  fun `getUserAccessForOffenderCrn returns response from Community API call for failure types`(
    expectedResult: ClientResult<UserOffenderAccess>,
  ) {
    every { mockCommunityApiClient.getUserAccessForOffenderCrn("DELIUS-USER", "SOME-CRN") } returns expectedResult

    val result = communityApiOffenderDetailsDataSource.getUserAccessForOffenderCrn("DELIUS-USER", "SOME-CRN")

    assertThat(result).isEqualTo(expectedResult)
  }

  @Test
  fun `getUserAccessForOffenderCrn returns response from Community API call for success type`() {
    val successBody = UserOffenderAccess(
      userRestricted = false,
      userExcluded = false,
      restrictionMessage = null,
    )

    every {
      mockCommunityApiClient.getUserAccessForOffenderCrn("DELIUS-USER", "SOME-CRN")
    } returns ClientResult.Success(HttpStatus.OK, successBody, true)

    val result = communityApiOffenderDetailsDataSource.getUserAccessForOffenderCrn("DELIUS-USER", "SOME-CRN")

    assertThat(result).isEqualTo(
      ClientResult.Success(
        HttpStatus.OK,
        successBody,
        true,
      ),
    )
  }

  @Test
  fun `getUserAccessForOffenderCrns returns response from Community API call for each CRN`() {
    val apiSuccess = UserOffenderAccess(
      userRestricted = false,
      userExcluded = false,
      restrictionMessage = null,
    )

    val apiClientResponse = allClientResultTypes(apiSuccess)

    apiClientResponse.forEach { (crn, expected) ->
      every { mockCommunityApiClient.getUserAccessForOffenderCrn("DELIUS-USER", crn) } returns expected
    }

    val results = communityApiOffenderDetailsDataSource.getUserAccessForOffenderCrns("DELIUS-USER", apiClientResponse.keys.toList())

    val expectedResults = allClientResultTypes(
      successBody = apiSuccess,
    )

    assertThat(results.toString()).isEqualTo(expectedResults.toString())
  }

  private companion object {
    @JvmStatic
    fun cacheableOffenderDetailSummaryClientResults(): Stream<Arguments> {
      val successBody = OffenderDetailsSummaryFactory()
        .withCrn("SOME-CRN")
        .produce()

      return allClientResultTypes(successBody)
        .filter { it.value !is ClientResult.Failure.PreemptiveCacheTimeout }
        .intoArgumentStream()
    }

    @JvmStatic
    fun <T> cacheTimeoutClientResult() =
      ClientResult.Failure.PreemptiveCacheTimeout<T>("some-cache", "some-cache-key", 1000)

    @JvmStatic
    fun userOffenderAccessFailureResultTypes(): Stream<Arguments> {
      val values = allClientFailureResultTypes<UserOffenderAccess>()

      return values.intoArgumentStream()
    }

    private fun <T> allClientFailureResultTypes(): Map<String, ClientResult<T>> = mapOf(
      "CRN-CACHE-UNAVAILABLE" to ClientResult.Failure.CachedValueUnavailable("some-cache-key"),
      "CRN-HTTP-404" to ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/",
        HttpStatus.NOT_FOUND,
        null,
        false,
      ),
      "CRN-MISC-ERROR" to ClientResult.Failure.Other(
        HttpMethod.POST,
        "/",
        RuntimeException("Some error"),
      ),
      "CRN-CACHE-TIMEOUT" to cacheTimeoutClientResult(),
    )

    private fun <T> allClientResultTypes(successBody: T): Map<String, ClientResult<T>> =
      allClientFailureResultTypes<T>()
        .plus(
          "CRN-SUCCESS" to ClientResult.Success(HttpStatus.OK, successBody, true),
        )

    private fun <T> Map<String, ClientResult<T>>.intoArgumentStream(): Stream<Arguments> = this.values.stream().map { Arguments.of(it) }
  }
}
