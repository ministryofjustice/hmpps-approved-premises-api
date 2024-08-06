package uk.gov.justice.digital.hmpps.approvedpremisesapi.cache.preemptive

import redis.lock.redlock.RedLock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PreemptiveCacheEntryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CacheRefreshExclusionsInmateDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import java.time.LocalDateTime

@SuppressWarnings("LongParameterList")
class InmateDetailsCacheRefreshWorker(
  private val applicationRepository: ApplicationRepository,
  private val bookingRepository: BookingRepository,
  private val cacheRefreshExclusionsInmateDetailsRepository: CacheRefreshExclusionsInmateDetailsRepository,
  private val prisonsApiClient: PrisonsApiClient,
  private val sentryService: SentryService,
  private val loggingEnabled: Boolean,
  private val delayMs: Long,
  redLock: RedLock,
  lockDurationMs: Int,
) : CacheRefreshWorker(redLock, "inmateDetails", lockDurationMs) {
  override fun work(checkShouldStop: () -> PrematureStopReason?) {
    val distinctNomsNumbers =
      (
        applicationRepository.getDistinctNomsNumbers() +
          bookingRepository.getDistinctNomsNumbers()
        ).distinct() - cacheRefreshExclusionsInmateDetailsRepository.getDistinctNomsNumbers().toSet()

    logConspicuously("${distinctNomsNumbers.count()} cache fields to update")

    if (loggingEnabled) { log.info("Got $distinctNomsNumbers to refresh for Inmate Details") }

    val refreshStarted = LocalDateTime.now()

    distinctNomsNumbers.shuffled().forEachIndexed { index, nomsNumber ->
      logConspicuously("Current NOMS number: $nomsNumber")

      val prematureStopReason = checkShouldStop()
      if (prematureStopReason != null) {
        if (prematureStopReason == PrematureStopReason.LockExpired) {
          val message =
            """Inmate details refresh has stopped prematurely because the lock has expired
Have processed $index of ${distinctNomsNumbers.size} candidates
Refresh started at $refreshStarted"""
          logConspicuously(message)
          sentryService.captureErrorMessage(message)
        }
        return
      }

      interruptableSleep(50)

      val cacheEntryStatus = prisonsApiClient.getInmateDetailsCacheEntryStatus(nomsNumber)

      logConspicuously("Cache status for $nomsNumber: $cacheEntryStatus")

      if (cacheEntryStatus == PreemptiveCacheEntryStatus.EXISTS) {
        if (loggingEnabled) {
          log.info("No upstream call made when refreshing Inmate Details for $nomsNumber, stored result still within soft TTL")
        }

        return@forEachIndexed
      }

      val prisonsApiResult = prisonsApiClient.getInmateDetailsWithCall(nomsNumber)

      logConspicuously("Upstream API response: $prisonsApiResult")

      if (prisonsApiResult is ClientResult.Failure.StatusCode) {
        if (!prisonsApiResult.isPreemptivelyCachedResponse) {
          log.error("Unable to refresh Inmate Details for $nomsNumber, response status: ${prisonsApiResult.status}")
        }
      }

      if (prisonsApiResult is ClientResult.Failure.Other) {
        log.error("Unable to refresh Inmate Details for $nomsNumber: ${prisonsApiResult.exception.message}")
      }

      if (prisonsApiResult is ClientResult.Success) {
        if (loggingEnabled) {
          log.info("Successfully refreshed Inmate Details for $nomsNumber")
        }
      }
    }

    logConspicuously("Have completed refreshing inmate details cache")

    interruptableSleep(delayMs)
  }

  private fun logConspicuously(message: String) {
    log.error("[CONSPICUOUS] $message")
  }
}
