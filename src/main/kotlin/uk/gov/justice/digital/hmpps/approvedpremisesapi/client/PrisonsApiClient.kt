package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Alert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import java.time.Duration

@Component
class PrisonsApiClient(
  @Qualifier("prisonsApiWebClient") webClient: WebClient,
  objectMapper: ObjectMapper,
  redisTemplate: RedisTemplate<String, String>,
  @Value("\${preemptive-cache-key-prefix}") preemptiveCacheKeyPrefix: String,
) : BaseHMPPSClient(webClient, objectMapper, redisTemplate, preemptiveCacheKeyPrefix) {
  private val inmateDetailsCacheConfig = PreemptiveCacheConfig(
    cacheName = "inmateDetails",
    successSoftTtlSeconds = Duration.ofHours(6).toSeconds().toInt(),
    failureSoftTtlSeconds = Duration.ofMinutes(30).toSeconds().toInt(),
    hardTtlSeconds = Duration.ofHours(12).toSeconds().toInt()
  )

  fun getInmateDetails(nomsNumber: String) = getRequest<InmateDetail> {
    path = "/api/offenders/$nomsNumber"
  }

  fun getInmateDetailsWithWait(nomsNumber: String) = getRequest<InmateDetail> {
    preemptiveCacheConfig = inmateDetailsCacheConfig
    preemptiveCacheKey = nomsNumber
    preemptiveCacheTimeoutMs = 0
  }

  fun getInmateDetailsWithCall(nomsNumber: String) = getRequest<InmateDetail> {
    path = "/api/offenders/$nomsNumber"
    isPreemptiveCall = true
    preemptiveCacheConfig = inmateDetailsCacheConfig
    preemptiveCacheKey = nomsNumber
  }

  fun getAdjudicationsPage(nomsNumber: String, offset: Int?, pageSize: Int) = getRequest<AdjudicationsPage> {
    withHeader("Page-Limit", pageSize.toString())

    if (offset != null) {
      withHeader("Page-Offset", offset.toString())
    }

    path = "/api/offenders/$nomsNumber/adjudications"
  }

  fun getAlerts(nomsNumber: String, alertCode: String) = getRequest<List<Alert>> {
    path = "/api/offenders/$nomsNumber/alerts/v2?alertCodes=HA&sort=dateCreated&direction=DESC"
  }
}
