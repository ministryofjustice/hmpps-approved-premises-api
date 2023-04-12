package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
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

@Component
class CommunityApiClient(
  @Qualifier("communityApiWebClient") private val webClient: WebClient,
  objectMapper: ObjectMapper
) : BaseHMPPSClient(webClient, objectMapper) {
  fun getOffenderDetailSummary(crn: String) = getRequest<OffenderDetailSummary> {
    path = "/secure/offenders/crn/$crn"
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
