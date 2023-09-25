package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult

@Service("CAS2OffenderService")
class OffenderService(
  private val communityApiClient: CommunityApiClient,
  private val prisonsApiClient: PrisonsApiClient,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getInfoForPerson(crn: String, nomisUsername: String): PersonInfoResult {
    var offenderResponse = communityApiClient.getOffenderDetailSummaryWithWait(crn)

    if (offenderResponse is ClientResult.Failure.PreemptiveCacheTimeout) {
      offenderResponse = communityApiClient.getOffenderDetailSummaryWithCall(crn)
    }

    val offender = when (offenderResponse) {
      is ClientResult.Success -> offenderResponse.body

      is ClientResult.Failure.StatusCode -> if (offenderResponse.status == HttpStatus.NOT_FOUND) {
        return PersonInfoResult.NotFound(crn)
      } else {
        return PersonInfoResult.Unknown(crn, offenderResponse.toException())
      }

      is ClientResult.Failure -> return PersonInfoResult.Unknown(crn, offenderResponse.toException())
    }

    val inmateDetails = offender.otherIds.nomsNumber?.let { nomsNumber ->
      when (val inmateDetailsResult = getInmateDetailByNomsNumber(offender.otherIds.crn, nomsNumber)) {
        is AuthorisableActionResult.Success -> inmateDetailsResult.entity
        else -> null
      }
    }

    return PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offender,
      inmateDetail = inmateDetails,
    )
  }

  fun getInmateDetailByNomsNumber(crn: String, nomsNumber: String): AuthorisableActionResult<InmateDetail?> {
    var inmateDetailResponse = prisonsApiClient.getInmateDetailsWithWait(nomsNumber)

    val hasCacheTimedOut = inmateDetailResponse is ClientResult.Failure.PreemptiveCacheTimeout
    if (hasCacheTimedOut) {
      inmateDetailResponse = prisonsApiClient.getInmateDetailsWithCall(nomsNumber)
    }

    fun logFailedResponse(inmateDetailResponse: ClientResult.Failure<InmateDetail>) = when (hasCacheTimedOut) {
      true -> log.warn("Could not get inmate details for $crn after cache timed out", inmateDetailResponse.toException())
      false -> log.warn("Could not get inmate details for $crn as an unsuccessful response was cached", inmateDetailResponse.toException())
    }

    val inmateDetail = when (inmateDetailResponse) {
      is ClientResult.Success -> inmateDetailResponse.body
      is ClientResult.Failure.StatusCode -> when (inmateDetailResponse.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> {
          logFailedResponse(inmateDetailResponse)
          null
        }
      }
      is ClientResult.Failure -> {
        logFailedResponse(inmateDetailResponse)
        null
      }
    }

    return AuthorisableActionResult.Success(inmateDetail)
  }

  fun getOffenderByCrn(crn: String): AuthorisableActionResult<OffenderDetailSummary> {
    var offenderResponse = communityApiClient.getOffenderDetailSummaryWithWait(crn)

    if (offenderResponse is ClientResult.Failure.PreemptiveCacheTimeout) {
      offenderResponse = communityApiClient.getOffenderDetailSummaryWithCall(crn)
    }

    val offender = when (offenderResponse) {
      is ClientResult.Success -> offenderResponse.body
      is ClientResult.Failure.StatusCode -> if (offenderResponse.status == HttpStatus.NOT_FOUND) return AuthorisableActionResult.NotFound() else offenderResponse.throwException()
      is ClientResult.Failure -> offenderResponse.throwException()
    }

    return AuthorisableActionResult.Success(offender)
  }
}
