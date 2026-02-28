package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult

@Service
@Suppress(
  "ReturnCount",
)
@Deprecated("Replaced with Cas2OffenderService")
class Cas2v2OffenderService(
  private val prisonsApiClient: PrisonsApiClient,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val cas2v2PersonTransformer: Cas2v2PersonTransformer,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getPersonByNomisIdOrCrn(nomisIdOrCrn: String): Cas2v2OffenderSearchResult {
    fun logFailedResponse(probationResponse: ClientResult.Failure<CaseSummaries>) = log.warn("Could not get inmate details for $nomisIdOrCrn", probationResponse.toException())

    val caseSummaries = apDeliusContextApiClient.getCaseSummaries(listOf(nomisIdOrCrn))
    val caseSummaryList = when (caseSummaries) {
      is ClientResult.Success -> caseSummaries.body.cases
      is ClientResult.Failure.StatusCode -> when (caseSummaries.status) {
        HttpStatus.NOT_FOUND -> return emitMessageAndCreateNotFound("Person not found ($nomisIdOrCrn) via the Delius Integration Api", nomisIdOrCrn)
        HttpStatus.FORBIDDEN -> return Cas2v2OffenderSearchResult.Forbidden(nomisIdOrCrn = nomisIdOrCrn, caseSummaries.toException())
        else -> {
          logFailedResponse(caseSummaries)
          return Cas2v2OffenderSearchResult.Unknown(nomisIdOrCrn = nomisIdOrCrn, caseSummaries.toException())
        }
      }

      is ClientResult.Failure -> {
        logFailedResponse(caseSummaries)
        return Cas2v2OffenderSearchResult.Unknown(nomisIdOrCrn = nomisIdOrCrn, caseSummaries.toException())
      }
    }

    if (caseSummaryList.isEmpty()) {
      return Cas2v2OffenderSearchResult.NotFound(nomisIdOrCrn = nomisIdOrCrn)
    }

    val caseSummary = caseSummaryList[0]

    return when (caseSummary.currentRestriction) {
      false -> Cas2v2OffenderSearchResult.Success.Full(
        nomisIdOrCrn = nomisIdOrCrn,
        person = cas2v2PersonTransformer.transformCaseSummaryToFullPerson(caseSummary),
      )

      else -> Cas2v2OffenderSearchResult.Forbidden(nomisIdOrCrn)
    }
  }

  fun getInmateDetailByNomsNumber(crn: String, nomsNumber: String): AuthorisableActionResult<InmateDetail?> {
    var inmateDetailResponse = prisonsApiClient.getInmateDetailsWithWait(nomsNumber)

    val hasCacheTimedOut = inmateDetailResponse is ClientResult.Failure.PreemptiveCacheTimeout
    if (hasCacheTimedOut) {
      inmateDetailResponse = prisonsApiClient.getInmateDetailsWithCall(nomsNumber)
    }

    fun logFailedResponse(inmateDetailResponse: ClientResult.Failure<InmateDetail>) = when (hasCacheTimedOut) {
      true -> {
        log.warn("Could not get inmate details for $crn after cache timed out", inmateDetailResponse.toException())
      }

      false -> {
        log.warn("Could not get inmate details for $crn as an unsuccessful response was cached", inmateDetailResponse.toException())
      }
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

  private fun emitMessageAndCreateNotFound(message: String, nomisIdOrCrn: String): Cas2v2OffenderSearchResult.NotFound {
    log.warn(message)
    return Cas2v2OffenderSearchResult.NotFound(nomisIdOrCrn)
  }
}

@Deprecated("Replaced with Cas2OffenderSearchResult")
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
