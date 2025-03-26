package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ProbationOffenderSearchApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.ProbationOffenderDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2PersonTransformer

@Service
@Suppress(
  "ReturnCount",
)
class Cas2v2OffenderService(
  private val prisonsApiClient: PrisonsApiClient,
  private val probationOffenderSearchApiClient: ProbationOffenderSearchApiClient,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val cas2v2PersonTransformer: Cas2v2PersonTransformer,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getPersonByNomsNumber(nomsNumber: String): Cas2v2OffenderSearchResult {
    fun logFailedResponse(probationResponse: ClientResult.Failure<List<ProbationOffenderDetail>>) = log.warn("Could not get inmate details for $nomsNumber", probationResponse.toException())

    val probationResponse = probationOffenderSearchApiClient.searchOffenderByNomsNumber(nomsNumber)

    val probationOffenderDetailList = when (probationResponse) {
      is ClientResult.Success -> probationResponse.body
      is ClientResult.Failure.StatusCode -> when (probationResponse.status) {
        HttpStatus.NOT_FOUND -> return Cas2v2OffenderSearchResult.NotFound(nomisIdOrCrn = nomsNumber)
        HttpStatus.FORBIDDEN -> return Cas2v2OffenderSearchResult.Forbidden(nomisIdOrCrn = nomsNumber, probationResponse.toException())
        else -> {
          logFailedResponse(probationResponse)
          return Cas2v2OffenderSearchResult.Unknown(nomisIdOrCrn = nomsNumber, probationResponse.toException())
        }
      }
      is ClientResult.Failure -> {
        logFailedResponse(probationResponse)
        return Cas2v2OffenderSearchResult.Unknown(nomisIdOrCrn = nomsNumber, probationResponse.toException())
      }
    }

    if (probationOffenderDetailList.isEmpty()) {
      return Cas2v2OffenderSearchResult.NotFound(nomisIdOrCrn = nomsNumber)
    }

    val probationOffenderDetail = probationOffenderDetailList[0]

    return when (probationOffenderDetail.currentRestriction) {
      false -> Cas2v2OffenderSearchResult.Success.Full(
        nomisIdOrCrn = nomsNumber,
        person = cas2v2PersonTransformer.transformProbationOffenderDetailAndInmateDetailToFullPerson(
          probationOffenderDetail,
        ),
      )

      else -> Cas2v2OffenderSearchResult.Forbidden(nomsNumber)
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

  fun getPersonByCrn(crn: String): Cas2v2OffenderSearchResult {
    val caseSummariesByCrn = when (val result = apDeliusContextApiClient.getSummariesForCrns(listOf(crn))) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> return Cas2v2OffenderSearchResult.NotFound(crn)
    }

    if (caseSummariesByCrn.cases.isEmpty()) {
      return Cas2v2OffenderSearchResult.NotFound(nomisIdOrCrn = crn)
    }

    val caseSummary = caseSummariesByCrn.cases[0]
    val nomsNumber = caseSummary.nomsId

    if (nomsNumber.isNullOrEmpty()) {
      return when (caseSummary.currentRestriction) {
        false -> Cas2v2OffenderSearchResult.Success.Full(
          nomisIdOrCrn = crn,
          person = cas2v2PersonTransformer.transformCaseSummaryToFullPerson(caseSummary),
        )
        else -> Cas2v2OffenderSearchResult.Forbidden(crn)
      }
    }

    return getPersonByNomsNumber(nomsNumber)
  }
}

sealed interface Cas2v2OffenderSearchResult {
  val nomisIdOrCrn: String

  sealed interface Success : Cas2v2OffenderSearchResult {
    data class Full(
      override val nomisIdOrCrn: String,
      val person: FullPerson,
    ) : Success
  }

  data class NotFound(override val nomisIdOrCrn: String) : Cas2v2OffenderSearchResult
  data class Unknown(override val nomisIdOrCrn: String, val throwable: Throwable? = null) : Cas2v2OffenderSearchResult
  data class Forbidden(override val nomisIdOrCrn: String, val throwable: Throwable? = null) : Cas2v2OffenderSearchResult
}
