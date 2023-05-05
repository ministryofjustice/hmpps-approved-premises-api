package uk.gov.justice.digital.hmpps.approvedpremisesapi.cache.preemptive

import redis.lock.redlock.RedLock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository

class OffenderDetailsCacheRefreshWorker(
  private val applicationRepository: ApplicationRepository,
  private val bookingRepository: BookingRepository,
  private val communityApiClient: CommunityApiClient,
  private val prisonsApiClient: PrisonsApiClient,
  private val loggingEnabled: Boolean,
  redLock: RedLock
) : CacheRefreshWorker(redLock, "offenderDetailsAndInmateDetails") {
  override fun work(checkShouldStop: () -> Boolean) {
    val distinctCrns = (applicationRepository.getDistinctCrns() + bookingRepository.getDistinctCrns()).distinct()

    if (loggingEnabled) { log.info("Got $distinctCrns to refresh for Offender Details/Inmate Details") }

    distinctCrns.forEach {
      if (checkShouldStop()) return

      interruptableSleep(50)

      val communityApiResult = communityApiClient.getOffenderDetailSummaryWithCall(it)
      if (communityApiResult is ClientResult.Failure.StatusCode) {
        if (! communityApiResult.isPreemptivelyCachedResponse) {
          log.error("Unable to refresh Offender Details for $it, response status: ${communityApiResult.status}")
        } else if (loggingEnabled) {
          log.info("No upstream call made when refreshing Offender Details for $it, status code failure is still within soft TTL")
        }
      }

      if (communityApiResult is ClientResult.Failure.Other) {
        log.error("Unable to refresh Offender Details for $it: ${communityApiResult.exception.message}")
      }

      if (communityApiResult is ClientResult.Success) {
        if (! communityApiResult.isPreemptivelyCachedResponse && loggingEnabled) {
          log.info("Successfully refreshed Offender Details for $it")
        } else if (loggingEnabled) {
          log.info("No upstream call made when refreshing Offender Details for $it, successful response is still within soft TTL")
        }

        val nomsNumber = communityApiResult.body.otherIds.nomsNumber

        if (nomsNumber != null) {
          val prisonsApiResult = prisonsApiClient.getInmateDetailsWithCall(nomsNumber)

          if (prisonsApiResult is ClientResult.Failure.Other) {
            log.error("Unable to refresh Inmate Details for $nomsNumber: ${prisonsApiResult.exception.message}")
          }

          if (prisonsApiResult is ClientResult.Failure.StatusCode) {
            if (! prisonsApiResult.isPreemptivelyCachedResponse) {
              log.error("Unable to refresh Inmate Details for $nomsNumber, response status: ${prisonsApiResult.status}")
            } else if (loggingEnabled) {
              log.info("No upstream call made when refreshing Inmate Details for $nomsNumber, status code failure is still within soft TTL")
            }
          }
        }
      }
    }
  }
}
