package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer

@Service
class OffenderDetailService(
  private val prisonsApiClient: PrisonsApiClient,
  private val personTransformer: PersonTransformer,
  private val offenderService: OffenderService,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getPersonInfoResult(
    crn: String,
    laoStrategy: LaoStrategy,
  ) = getPersonInfoResults(setOf(crn), laoStrategy).first()

  @Deprecated(
    """Use version that takes limitedAccessStrategy, derive from [UserEntity.cas1LimitedAccessStrategy()] 
    |or [UserEntity.cas3LimitedAccessStrategy()]""",
  )
  fun getPersonInfoResult(
    crn: String,
    deliusUsername: String?,
    ignoreLaoRestrictions: Boolean,
  ): PersonInfoResult {
    check(ignoreLaoRestrictions || deliusUsername != null) { "If ignoreLao is false, delius username must be provided " }

    return getPersonInfoResults(
      crns = setOf(crn),
      laoStrategy = if (ignoreLaoRestrictions) {
        LaoStrategy.NeverRestricted
      } else {
        LaoStrategy.CheckUserAccess(deliusUsername!!)
      },
    ).first()
  }

  /**
   * Returns a list of [PersonInfoResult] for the given set of CRNs.
   * If the CRN is not found, it will return a [PersonInfoResult.NotFound] for that CRN so there will always be a result for each CRN.
   */
  fun getPersonInfoResults(
    crns: Set<String>,
    laoStrategy: LaoStrategy,
  ): List<PersonInfoResult> {
    if (crns.isEmpty()) return emptyList()

    val offendersDetails = offenderService.getPersonSummaryInfoResults(crns, laoStrategy)

    return offendersDetails.map {
      when (it) {
        is PersonSummaryInfoResult.Success.Full -> {
          val inmateDetails = it.summary.nomsId?.let { nomsNumber ->
            when (val inmateDetailsResult = getInmateDetailByNomsNumber(it.crn, nomsNumber)) {
              is AuthorisableActionResult.Success -> inmateDetailsResult.entity
              else -> null
            }
          }
          personTransformer.transformPersonSummaryInfoToPersonInfo(it, inmateDetails)
        }

        is PersonSummaryInfoResult.Success.Restricted,
        is PersonSummaryInfoResult.NotFound,
        is PersonSummaryInfoResult.Unknown,
        ->
          personTransformer.transformPersonSummaryInfoToPersonInfo(it, null)
      }
    }
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
}
