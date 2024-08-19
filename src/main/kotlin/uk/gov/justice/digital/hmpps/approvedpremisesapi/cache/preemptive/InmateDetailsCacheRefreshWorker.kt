package uk.gov.justice.digital.hmpps.approvedpremisesapi.cache.preemptive

import org.springframework.http.HttpStatus
import redis.lock.redlock.RedLock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PreemptiveCacheEntryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CacheRefreshExclusionsInmateDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CacheRefreshExclusionsInmateDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@SuppressWarnings("LongParameterList", "MagicNumber", "CyclomaticComplexMethod")
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

    logConspicuously("${distinctNomsNumbers.count()} cache records to check")

    if (loggingEnabled) { log.info("Got $distinctNomsNumbers to consider updating for Inmate Details") }

    val started = LocalDateTime.now()
    val refreshStarted = LocalDateTime.now()
    val candidatesCount = distinctNomsNumbers.size
    var entryUpdates = 0
    var entryUpdateFails = 0

    fun stats() = "Of $candidatesCount candidates we have successfully updated $entryUpdates with $entryUpdateFails update failures. " +
      "Total time taken ${ChronoUnit.SECONDS.between(started,LocalDateTime.now())} seconds"

    distinctNomsNumbers.shuffled().forEach { nomsNumber ->
      logConspicuously("Current NOMS number: $nomsNumber")

      val prematureStopReason = checkShouldStop()
      if (prematureStopReason != null) {
        if (prematureStopReason == PrematureStopReason.LockExpired) {
          val message =
            """Inmate details refresh has stopped prematurely because the lock has expired
Refresh started at $refreshStarted
${stats()}"""
          logConspicuously(message)
          sentryService.captureErrorMessage(message)
        }
        return
      }

      interruptableSleep(25)

      val cacheEntryStatus = prisonsApiClient.getInmateDetailsCacheEntryStatus(nomsNumber)

      logConspicuously("Cache status for $nomsNumber: $cacheEntryStatus")

      if (cacheEntryStatus == PreemptiveCacheEntryStatus.EXISTS) {
        if (loggingEnabled) {
          log.info("No upstream call made when refreshing Inmate Details for $nomsNumber, stored result still within soft TTL")
        }

        return@forEach
      }

      interruptableSleep(25)

      val prisonsApiResult = prisonsApiClient.getInmateDetailsWithCall(nomsNumber)

      logConspicuously("Upstream API response: $prisonsApiResult")

      if (prisonsApiResult is ClientResult.Failure.StatusCode) {
        if (!prisonsApiResult.isPreemptivelyCachedResponse) {
          if (prisonsApiResult.status == HttpStatus.NOT_FOUND && entryUpdateFails > 1) {
            log.error("Multiple ${prisonsApiResult.status} responses when updating offender details. Adding $nomsNumber, adding cache refresh exclusion.")
            cacheRefreshExclusionsInmateDetailsRepository.save(
              CacheRefreshExclusionsInmateDetailsEntity(
                nomsNumber,
                "$entryUpdateFails attempts to update cached offender resulted in ${prisonsApiResult.status}",
              ),
            )
          } else {
            log.error("Unable to refresh Inmate Details for $nomsNumber, response status: ${prisonsApiResult.status}")
            entryUpdateFails += 1
          }
        }
      }

      if (prisonsApiResult is ClientResult.Failure.Other) {
        log.error("Unable to refresh Inmate Details for $nomsNumber: ${prisonsApiResult.exception.message}")
        entryUpdateFails += 1
      }

      if (prisonsApiResult is ClientResult.Success) {
        if (loggingEnabled) {
          log.info("Successfully refreshed Inmate Details for $nomsNumber")
        }
        entryUpdates += 1
      }
    }

    logConspicuously("Have completed refreshing inmate details cache. ${stats()}")

    interruptableSleep(delayMs)
  }

  private fun logConspicuously(message: String) {
    log.error("[CONSPICUOUS] $message")
  }
}
