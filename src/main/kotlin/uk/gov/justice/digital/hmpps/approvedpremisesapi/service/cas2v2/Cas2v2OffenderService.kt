package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ProbationOffenderSearchApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.ProbationOffenderDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.ProbationOffenderSearchResult

@Service
@Suppress(
  "ReturnCount",
)
class Cas2v2OffenderService(
  private val prisonsApiClient: PrisonsApiClient,
  private val probationOffenderSearchApiClient: ProbationOffenderSearchApiClient,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getPersonByNomsNumber(
    nomsNumber: String,
  ): ProbationOffenderSearchResult {
    fun logFailedResponse(probationResponse: ClientResult.Failure<List<ProbationOffenderDetail>>) = log.warn("Could not get inmate details for $nomsNumber", probationResponse.toException())

    val probationResponse = probationOffenderSearchApiClient.searchOffenderByNomsNumber(nomsNumber)

    val probationOffenderDetailList = when (probationResponse) {
      is ClientResult.Success -> probationResponse.body
      is ClientResult.Failure.StatusCode -> when (probationResponse.status) {
        HttpStatus.NOT_FOUND -> return ProbationOffenderSearchResult.NotFound(nomsNumber)
        HttpStatus.FORBIDDEN -> return ProbationOffenderSearchResult.Forbidden(
          nomsNumber,
          probationResponse.toException(),
        )

        else -> {
          logFailedResponse(probationResponse)
          return ProbationOffenderSearchResult.Unknown(nomsNumber, probationResponse.toException())
        }
      }

      is ClientResult.Failure -> {
        logFailedResponse(probationResponse)
        return ProbationOffenderSearchResult.Unknown(nomsNumber, probationResponse.toException())
      }
    }

    if (probationOffenderDetailList.isEmpty()) {
      return ProbationOffenderSearchResult.NotFound(nomsNumber)
    } else {
      val probationOffenderDetail = probationOffenderDetailList[0]

      // check for restrictions or exclusions
      if (hasRestrictionOrExclusion(probationOffenderDetail)) return ProbationOffenderSearchResult.Forbidden(nomsNumber)

      // check inmate details from Prison API
      val inmateDetails = getInmateDetailsForProbationOffender(probationOffenderDetail)
        ?: return ProbationOffenderSearchResult.NotFound(nomsNumber)

      return ProbationOffenderSearchResult.Success.Full(nomsNumber, probationOffenderDetail, inmateDetails)
    }
  }

  private fun hasRestrictionOrExclusion(probationOffenderDetail: ProbationOffenderDetail): Boolean = probationOffenderDetail.currentExclusion == true || probationOffenderDetail.currentRestriction == true

  private fun getInmateDetailsForProbationOffender(probationOffenderDetail: ProbationOffenderDetail): InmateDetail? = probationOffenderDetail.otherIds.nomsNumber?.let { nomsNumber ->
    when (val inmateDetailsResult = getInmateDetailByNomsNumber(probationOffenderDetail.otherIds.crn, nomsNumber)) {
      is AuthorisableActionResult.Success -> inmateDetailsResult.entity
      else -> null
    }
  }

  fun getInmateDetailByNomsNumber(crn: String, nomsNumber: String): AuthorisableActionResult<InmateDetail?> {
    var inmateDetailResponse = prisonsApiClient.getInmateDetailsWithWait(nomsNumber)

    val hasCacheTimedOut = inmateDetailResponse is ClientResult.Failure.PreemptiveCacheTimeout
    if (hasCacheTimedOut) {
      inmateDetailResponse = prisonsApiClient.getInmateDetailsWithCall(nomsNumber)
    }

    fun logFailedResponse(inmateDetailResponse: ClientResult.Failure<InmateDetail>) = when (hasCacheTimedOut) {
      true -> log.warn(
        "Could not get inmate details for $crn after cache timed out",
        inmateDetailResponse.toException(),
      )

      false -> log.warn(
        "Could not get inmate details for $crn as an unsuccessful response was cached",
        inmateDetailResponse.toException(),
      )
    }

    val inmateDetail = when (inmateDetailResponse) {
      is ClientResult.Success -> inmateDetailResponse.body
      is ClientResult.Failure.StatusCode -> when (inmateDetailResponse.status) {
        HttpStatus.NOT_FOUND -> {
          logFailedResponse(inmateDetailResponse)
          return AuthorisableActionResult.NotFound()
        }

        HttpStatus.FORBIDDEN -> {
          logFailedResponse(inmateDetailResponse)
          return AuthorisableActionResult.Unauthorised()
        }

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
}
