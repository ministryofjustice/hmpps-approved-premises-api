package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PreemptiveCacheEntryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CacheRefreshExclusionsInmateDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class InmateDetailsCacheRefreshResults(
  var started: LocalDateTime = LocalDateTime.now(),
  var candidatesCount: Int = 0,
  var entriesProcessed: Int = 0,
  var entryUpdates: Int = 0,
  var entryUpdateFails: Int = 0,
) {
  fun stats() = """
      Of $candidatesCount candidates we have processed $entriesProcessed and successfully updated $entryUpdates with $entryUpdateFails update failures. 
      Total time taken ${ChronoUnit.SECONDS.between(started, LocalDateTime.now())} seconds
  """
}

@SuppressWarnings("MagicNumber")
@Service
class InmateDetailsCacheRefreshService(
  private val bookingRepository: BookingRepository,
  private val applicationRepository: ApplicationRepository,
  private val prisonsApiClient: PrisonsApiClient,
  private val cacheRefreshExclusionsInmateDetailsRepository: CacheRefreshExclusionsInmateDetailsRepository,
  private val featureFlagService: FeatureFlagService,
  @Value("\${refresh-inmate-details-cache.logging-enabled}") private val loggingEnabled: Boolean,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun refreshInmateDetailsCache(): InmateDetailsCacheRefreshResults? {
    if (!featureFlagService.getBooleanFlag("cas1-enable-scheduled-job-refresh-inmate-details")) {
      log.info("Scheduled job to refresh inmate details cache is switched off so PreemptiveCacheRefresher will run instead")
      return null
    } else {
      log.info("Scheduled job to refresh inmate details cache starting...")
      val distinctNomsNumbers = getDistinctNomsNumbers()
      val inmateDetailsCacheRefreshResults = InmateDetailsCacheRefreshResults(
        candidatesCount = distinctNomsNumbers.size,
      )
      distinctNomsNumbers.shuffled().forEach { nomsNumber ->
        inmateDetailsCacheRefreshResults.entriesProcessed += 1
        if (shouldLogProgress(inmateDetailsCacheRefreshResults.entriesProcessed)) {
          log.info("Processing inmate-details cache-entry ${inmateDetailsCacheRefreshResults.entriesProcessed}")
        }
        if (getInmateDetailsCacheEntryStatus(nomsNumber) != PreemptiveCacheEntryStatus.EXISTS) {
          Thread.sleep(25)
          val prisonsApiResult = prisonsApiClient.getInmateDetailsWithCall(nomsNumber)
          incrementCacheEntryStats(inmateDetailsCacheRefreshResults, nomsNumber, prisonsApiResult)
        }
      }
      log.info("Have completed refreshing inmate details cache. ${inmateDetailsCacheRefreshResults.stats()}")
      return inmateDetailsCacheRefreshResults
    }
  }

  private fun shouldLogProgress(entriesProcessed: Int) = entriesProcessed == 1 || entriesProcessed % 5000 == 0

  private fun getDistinctNomsNumbers(): List<String> {
    val distinctNomsNumbers = (applicationRepository.getDistinctNomsNumbers() + bookingRepository.getDistinctNomsNumbers())
      .distinct() - cacheRefreshExclusionsInmateDetailsRepository.getDistinctNomsNumbers().toSet()
    log.info("${distinctNomsNumbers.count()} cache records to check")
    if (loggingEnabled) {
      log.info("Got $distinctNomsNumbers to consider updating for Inmate Details")
    }
    return distinctNomsNumbers
  }

  private fun getInmateDetailsCacheEntryStatus(nomsNumber: String): PreemptiveCacheEntryStatus {
    Thread.sleep(25)
    val cacheEntryStatus = prisonsApiClient.getInmateDetailsCacheEntryStatus(nomsNumber)
    if (loggingEnabled) {
      log.info("Cache status for $nomsNumber: $cacheEntryStatus")
      if (cacheEntryStatus == PreemptiveCacheEntryStatus.EXISTS) {
        log.info("No upstream call made when refreshing Inmate Details for $nomsNumber, stored result still within soft TTL")
      }
    }
    return cacheEntryStatus
  }

  private fun incrementCacheEntryStats(
    inmateDetailsCacheRefreshResults: InmateDetailsCacheRefreshResults,
    nomsNumber: String,
    prisonsApiResult: ClientResult<InmateDetail>,
  ) {
    val successLog = "Successfully refreshed Inmate Details for $nomsNumber"
    val failLogPrefix = "Unable to refresh Inmate Details for $nomsNumber"
    when (prisonsApiResult) {
      is ClientResult.Success -> {
        if (loggingEnabled) {
          log.info(successLog)
        }
        inmateDetailsCacheRefreshResults.entryUpdates += 1
      }
      is ClientResult.Failure.StatusCode -> if (!prisonsApiResult.isPreemptivelyCachedResponse) {
        log.error("$failLogPrefix, response status: ${prisonsApiResult.status}")
        inmateDetailsCacheRefreshResults.entryUpdateFails += 1
      }
      is ClientResult.Failure.Other -> {
        log.error("$failLogPrefix: ${prisonsApiResult.exception.message}")
        inmateDetailsCacheRefreshResults.entryUpdateFails += 1
      }
      is ClientResult.Failure.CachedValueUnavailable -> {
        log.error("$failLogPrefix: CachedValueUnavailable failure occurred")
        inmateDetailsCacheRefreshResults.entryUpdateFails += 1
      }
      is ClientResult.Failure.PreemptiveCacheTimeout -> {
        log.error("$failLogPrefix: PreemptiveCacheTimeout failure occurred")
        inmateDetailsCacheRefreshResults.entryUpdateFails += 1
      }
    }
  }
}
