package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.IS_NOT_SUCCESSFUL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Conviction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffWithoutUsernameUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess
import java.io.OutputStream
import java.time.Duration

@Component
class CommunityApiClient(
  @Qualifier("communityApiWebClient") private val webClient: WebClient,
  objectMapper: ObjectMapper,
  redisTemplate: RedisTemplate<String, String>,
  @Value("\${preemptive-cache-key-prefix}") preemptiveCacheKeyPrefix: String,
) : BaseHMPPSClient(webClient, objectMapper, redisTemplate, preemptiveCacheKeyPrefix) {
  private val offenderDetailCacheConfig = PreemptiveCacheConfig(
    cacheName = "offenderDetails",
    successSoftTtlSeconds = Duration.ofHours(6).toSeconds().toInt(),
    failureSoftTtlSeconds = Duration.ofMinutes(30).toSeconds().toInt(),
    hardTtlSeconds = Duration.ofHours(12).toSeconds().toInt()
  )

  fun getOffenderDetailSummary(crn: String) = getRequest<OffenderDetailSummary> {
    path = "/secure/offenders/crn/$crn"
  }

  fun getOffenderDetailSummaryWithWait(crn: String) = getRequest<OffenderDetailSummary> {
    preemptiveCacheConfig = offenderDetailCacheConfig
    preemptiveCacheKey = crn
    preemptiveCacheTimeoutMs = 0
  }

  fun getOffenderDetailSummaryWithCall(crn: String) = getRequest<OffenderDetailSummary> {
    path = "/secure/offenders/crn/$crn"
    isPreemptiveCall = true
    preemptiveCacheConfig = offenderDetailCacheConfig
    preemptiveCacheKey = crn
  }

  @Cacheable(value = ["userAccessCache"], unless = IS_NOT_SUCCESSFUL)
  fun getUserAccessForOffenderCrn(userDistinguishedName: String, crn: String) = getRequest<UserOffenderAccess> {
    path = "/secure/offenders/crn/$crn/user/$userDistinguishedName/userAccess"
  }

  fun getRegistrationsForOffenderCrn(crn: String) = getRequest<Registrations> {
    path = "/secure/offenders/crn/$crn/registrations?activeOnly=true"
  }

  @Cacheable(value = ["staffDetailsCache"], unless = IS_NOT_SUCCESSFUL)
  fun getStaffUserDetails(deliusUsername: String) = getRequest<StaffUserDetails> {
    path = "/secure/staff/username/$deliusUsername"
  }

  fun getStaffUserDetailsForStaffCode(staffCode: String) = getRequest<StaffWithoutUsernameUserDetails> {
    path = "/secure/staff/staffCode/$staffCode"
  }

  fun getConvictions(crn: String) = getRequest<List<Conviction>> {
    path = "/secure/offenders/crn/$crn/convictions"
  }

  fun getDocuments(crn: String) = getRequest<GroupedDocuments> {
    path = "/secure/offenders/crn/$crn/documents/grouped"
  }

  fun getDocument(crn: String, documentId: String, outputStream: OutputStream): ClientResult<Unit> {
    val path = "/secure/offenders/crn/$crn/documents/$documentId"

    return try {
      val bodyDataBuffer = webClient.get().uri(path)
        .retrieve()
        .bodyToFlux(DataBuffer::class.java)

      DataBufferUtils.write(bodyDataBuffer, outputStream)
        .share().blockLast()

      ClientResult.Success(HttpStatus.OK, Unit)
    } catch (exception: WebClientResponseException) {
      ClientResult.Failure.StatusCode(HttpMethod.GET, path, exception.statusCode, exception.responseBodyAsString)
    } catch (exception: Exception) {
      ClientResult.Failure.Other(HttpMethod.GET, path, exception)
    }
  }
}
