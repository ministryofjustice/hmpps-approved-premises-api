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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ReferralDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserAccess
import java.io.OutputStream

@Component
class ApDeliusContextApiClient(
  @Qualifier("apDeliusContextApiWebClient") private val webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {
  @Cacheable(value = ["qCodeStaffMembersCache"], unless = IS_NOT_SUCCESSFUL)
  fun getStaffMembers(qCode: String) = getRequest<StaffMembersPage> {
    path = "/approved-premises/$qCode/staff"
  }

  @Cacheable(value = ["teamsManagingCaseCache"], unless = IS_NOT_SUCCESSFUL)
  fun getTeamsManagingCase(crn: String) = getRequest<ManagingTeamsResponse> {
    path = "/teams/managingCase/$crn"
  }

  @Cacheable(value = ["crnGetCaseDetailCache"], unless = IS_NOT_SUCCESSFUL)
  fun getCaseDetail(crn: String) = getRequest<CaseDetail> {
    path = "/probation-cases/$crn/details"
  }

  fun getSummariesForCrns(crns: List<String>) = getRequest<CaseSummaries> {
    path = "/probation-cases/summaries"
    body = crns
  }

  fun getUserAccessForCrns(deliusUsername: String, crns: List<String>) = getRequest<UserAccess> {
    path = "/users/access?username=$deliusUsername"
    body = crns
  }

  fun getReferralDetails(crn: String, bookingId: String) = getRequest<ReferralDetail> {
    path = "/probation-case/$crn/referrals/$bookingId"
  }

  @Cacheable(value = ["staffDetailsCache"], unless = IS_NOT_SUCCESSFUL)
  fun getStaffDetail(username: String) = getRequest<StaffDetail> {
    path = "/staff/$username"
  }

  fun getDocuments(crn: String) = getRequest<List<APDeliusDocument>> {
    path = "/documents/$crn/all"
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  fun getDocument(crn: String, documentId: String, outputStream: OutputStream): ClientResult<Unit> {
    val path = "/documents/$crn/$documentId"

    return try {
      val bodyDataBuffer = webClientConfig.webClient.get().uri(path)
        .retrieve()
        .bodyToFlux(DataBuffer::class.java)

      DataBufferUtils.write(bodyDataBuffer, outputStream)
        .share().blockLast()

      ClientResult.Success(HttpStatus.OK, Unit)
    } catch (exception: WebClientResponseException) {
      ClientResult.Failure.StatusCode(HttpMethod.GET, path, exception.statusCode.toHttpStatus(), exception.responseBodyAsString)
    } catch (exception: Exception) {
      ClientResult.Failure.Other(HttpMethod.GET, path, exception)
    }
  }
}
