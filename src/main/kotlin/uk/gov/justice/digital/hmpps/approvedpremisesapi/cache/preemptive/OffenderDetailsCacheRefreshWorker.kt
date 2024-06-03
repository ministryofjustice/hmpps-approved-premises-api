package uk.gov.justice.digital.hmpps.approvedpremisesapi.cache.preemptive

import redis.lock.redlock.RedLock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PreemptiveCacheEntryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository

class OffenderDetailsCacheRefreshWorker(
  private val applicationRepository: ApplicationRepository,
  private val bookingRepository: BookingRepository,
  private val communityApiClient: CommunityApiClient,
  private val loggingEnabled: Boolean,
  private val delayMs: Long,
  redLock: RedLock,
  lockDurationMs: Int,
) : CacheRefreshWorker(redLock, "offenderDetails", lockDurationMs) {
  override fun work(checkShouldStop: () -> Boolean) {
    val distinctCrns = (applicationRepository.getDistinctCrns() + bookingRepository.getDistinctCrns()).distinct()

    if (loggingEnabled) {
      log.info("Got $distinctCrns to refresh for Offender Details")
    }

    distinctCrns.forEach {
      if (checkShouldStop()) return

      interruptableSleep(50)

      val cacheEntryStatus = communityApiClient.getOffenderDetailsCacheEntryStatus(it)

      if (cacheEntryStatus == PreemptiveCacheEntryStatus.EXISTS) {
        if (loggingEnabled) {
          log.info("No upstream call made when refreshing Offender Details for $it, stored result still within soft TTL")
        }

        return@forEach
      }

      val communityApiResult = communityApiClient.getOffenderDetailSummaryWithCall(it)
      if (communityApiResult is ClientResult.Failure.StatusCode) {
        if (!communityApiResult.isPreemptivelyCachedResponse) {
          log.error("Unable to refresh Offender Details for $it, response status: ${communityApiResult.status}")
        }
      }

      if (communityApiResult is ClientResult.Failure.Other) {
        log.error("Unable to refresh Offender Details for $it: ${communityApiResult.exception.message}")
      }

      if (communityApiResult is ClientResult.Success) {
        if (loggingEnabled) {
          log.info("Successfully refreshed Offender Details for $it")
        }
      }
    }

    interruptableSleep(delayMs)
  }
}
