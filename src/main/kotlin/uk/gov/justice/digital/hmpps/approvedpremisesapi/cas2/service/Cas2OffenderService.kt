package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.TierService

@Service
@Suppress(
  "ReturnCount",
)
class Cas2OffenderService(
  private val prisonsApiClient: PrisonsApiClient,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val cas2PersonTransformer: Cas2PersonTransformer,
  private val tierService: TierService,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getPersonByNomisIdOrCrn(nomisIdOrCrn: String): Cas2v2OffenderSearchResult {
    fun logFailedResponse(caseSummariesResponse: ClientResult.Failure<CaseSummaries>) = log.warn("Could not get inmate details for $nomisIdOrCrn", caseSummariesResponse.toException())

    val caseSummaryList = when (val caseSummariesResponse = apDeliusContextApiClient.getCaseSummaries(listOf(nomisIdOrCrn))) {
      is ClientResult.Success -> caseSummariesResponse.body.cases
      is ClientResult.Failure.StatusCode -> when (caseSummariesResponse.status) {
        // this isn't technically correct because we should get an empty response if offender not found. instead, this should be treated as an unexpected error
        HttpStatus.NOT_FOUND -> {
          logFailedResponse(caseSummariesResponse)
          error("Received a 404 from the Delius Integration Api when searching for $nomisIdOrCrn. This should not happen, as the API should return an empty response instead")
        }
        HttpStatus.FORBIDDEN -> {
          logFailedResponse(caseSummariesResponse)
          error("Received a 403 from the Delius Integration Api when searching for $nomisIdOrCrn. This indicates a service permission issue")
        }
        else -> {
          logFailedResponse(caseSummariesResponse)
          return Cas2v2OffenderSearchResult.Unknown(nomisIdOrCrn = nomisIdOrCrn, caseSummariesResponse.toException())
        }
      }

      is ClientResult.Failure -> {
        logFailedResponse(caseSummariesResponse)
        return Cas2v2OffenderSearchResult.Unknown(nomisIdOrCrn = nomisIdOrCrn, caseSummariesResponse.toException())
      }
    }

    if (caseSummaryList.isEmpty()) {
      log.warn("Person information for $nomisIdOrCrn not found via the Delius Integration Api")
      return Cas2v2OffenderSearchResult.NotFound(nomisIdOrCrn = nomisIdOrCrn)
    }

    val caseSummary = caseSummaryList[0]

    return when (caseSummary.currentRestriction) {
      false -> Cas2v2OffenderSearchResult.Success.Full(
        nomisIdOrCrn = nomisIdOrCrn,
        person = cas2PersonTransformer.transformCaseSummaryToFullPerson(caseSummary, tierService.getTier(caseSummary.crn)),
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
