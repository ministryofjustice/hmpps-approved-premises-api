package uk.gov.justice.digital.hmpps.approvedpremisesapi.cache.preemptive

import redis.lock.redlock.RedLock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository

class OffenderDetailsCacheRefreshWorker(
  private val applicationRepository: ApplicationRepository,
  private val bookingRepository: BookingRepository,
  private val communityApiClient: CommunityApiClient,
  redLock: RedLock
) : CacheRefreshWorker(redLock, "offenderDetails") {
  override fun work(checkShouldStop: () -> Boolean) {
    val distinctCrns = (applicationRepository.getDistinctCrns() + bookingRepository.getDistinctCrns()).distinct()

    distinctCrns.forEach {
      if (checkShouldStop()) return

      interruptableSleep(50)

      // A potential further improvement is to do these calls in concurrent batches
      val result = communityApiClient.getOffenderDetailSummaryWithCall(it)
      if (result is ClientResult.Failure.StatusCode) {
        log.error("Unable to refresh Offender Details for $it, response status: ${result.status}")
      }

      if (result is ClientResult.Failure.Other) {
        log.error("Unable to refresh Offender Details for $it: ${result.exception.message}")
      }
    }
  }
}
