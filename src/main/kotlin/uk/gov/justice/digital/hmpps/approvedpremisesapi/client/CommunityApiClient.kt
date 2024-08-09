package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.IS_NOT_SUCCESSFUL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Conviction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffWithoutUsernameUserDetails
import java.io.OutputStream

@Component
class CommunityApiClient(
  @Qualifier("communityApiWebClient") private val webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {

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
      val bodyDataBuffer = webClientConfig.webClient.get().uri(path)
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
