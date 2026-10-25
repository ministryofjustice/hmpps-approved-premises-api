package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.apache.commons.collections4.ListUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerAlertsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.Agency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.BookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CsraSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.toCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.OffenderRiskNoteParser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.TierService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import java.util.stream.Collectors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisoneralertsapi.Alert as PrisionerAlert

@Service
class OffenderService(
  private val prisonsApiClient: PrisonsApiClient,
  private val prisionerAlertsApiClient: PrisonerAlertsApiClient,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  adjudicationsConfigBindingModel: PrisonAdjudicationsConfigBindingModel,
  private val offenderRiskNoteParser: OffenderRiskNoteParser,
  private val caseService: CaseService,
  private val tierService: TierService,
  private val laoService: LaoService,
) {
  companion object {
    const val MAX_OFFENDER_REQUEST_COUNT = 500
  }

  private val log = LoggerFactory.getLogger(this::class.java)

  private val adjudicationsConfig: PrisonAdjudicationsConfig

  init {
    adjudicationsConfig = PrisonAdjudicationsConfig(
      prisonApiPageSize = adjudicationsConfigBindingModel.prisonApiPageSize ?: throw RuntimeException("No prison-adjudications.prison-api-page-size configuration provided"),
    )
  }

  /**
   * The [getPersonSummaryInfoResults] function is limited to providing information for up to 500 CRNs
   *
   * If information is required about more than 500 CRNs, this function should be used instead
   *
   * Note - this should be used with care and only used for infrequently ran operations (monthly
   * reports, seed jobs etc.) as fetching this number of offenders should not be a typical operation
   */
  fun getPersonSummaryInfoResultsInBatches(
    crns: Set<String>,
    laoStrategy: LaoStrategy,
    batchSize: Int = 500,
  ): List<PersonSummaryInfoResult> {
    if (batchSize > MAX_OFFENDER_REQUEST_COUNT) {
      throw InternalServerErrorProblem("Cannot request more than $MAX_OFFENDER_REQUEST_COUNT CRNs. A batch size of $batchSize has been requested.")
    }

    return ListUtils.partition(crns.toList(), batchSize)
      .stream()
      .map { crnSubset ->
        getPersonSummaryInfoResults(crnSubset.toSet(), laoStrategy)
      }
      .flatMap { it.stream() }
      .collect(Collectors.toList())
  }

  fun getPersonSummaryInfoResult(
    crn: String,
    laoStrategy: LaoStrategy,
    includeTier: Boolean = true,
  ) = getPersonSummaryInfoResults(setOf(crn), laoStrategy, includeTier).first()

  /**
   * Returns a list of [PersonSummaryInfoResult] for the given set of CRNs.
   * If the CRN is not found, it will return a [PersonSummaryInfoResult.NotFound] for that CRN so there will always be a result for each CRN.
   */
  fun getPersonSummaryInfoResults(
    crns: Set<String>,
    laoStrategy: LaoStrategy,
    includeTier: Boolean = true,
  ): List<PersonSummaryInfoResult> {
    if (crns.isEmpty()) {
      return emptyList()
    }

    if (crns.size > MAX_OFFENDER_REQUEST_COUNT) {
      throw InternalServerErrorProblem("Cannot request more than $MAX_OFFENDER_REQUEST_COUNT CRNs. ${crns.size} have been provided.")
    }

    val caseSummaries = when (val result = apDeliusContextApiClient.getCaseSummaries(crns.toList())) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }.cases

    val tiersByCrn = when (includeTier) {
      true -> tierService.getTiers(crns)
      false -> emptyMap()
    }

    val visibilities = laoService.isVisible(caseSummaries, laoStrategy)

    val visibilityByCrn = visibilities.associateBy(
      keySelector = { it.caseSummary.crn },
      valueTransform = { it },
    )

    return crns.map { crn ->
      toPersonSummaryInfo(
        crn = crn,
        visibility = visibilityByCrn[crn],
        tier = tiersByCrn[crn.uppercase()],
      )
    }
  }

  private fun toPersonSummaryInfo(
    crn: String,
    visibility: LaoService.IsVisible?,
    tier: TierDto?,
  ) = when {
    visibility == null -> {
      log.debug("Could not find case summary for '$crn'. Returning not found")
      PersonSummaryInfoResult.NotFound(crn)
    }
    visibility.isVisible -> {
      log.debug("Caller can access LAO '$crn'. Returning full details")
      PersonSummaryInfoResult.Success.Full(
        crn = crn,
        summary = visibility.caseSummary,
        tier = tier,
      )
    }
    else -> {
      log.debug("Caller cannot access LAO '$crn'. Returning restricted")
      PersonSummaryInfoResult.Success.Restricted(
        crn = crn,
        nomsNumber = visibility.caseSummary.nomsId,
        tier = tier,
      )
    }
  }

  fun isLao(crn: String): Boolean {
    val offender = when (val offenderResponse = apDeliusContextApiClient.getCaseSummaries(listOf(crn))) {
      is ClientResult.Success -> offenderResponse.body.cases.first()
      is ClientResult.Failure -> offenderResponse.throwException()
    }

    return offender.currentExclusion || offender.currentRestriction
  }

  fun canAccessOffender(
    crn: String,
    laoStrategy: LaoStrategy,
  ) = canAccessOffenders(
    crns = listOf(crn),
    laoStrategy = laoStrategy,
  ).entries.first().value

  fun canAccessOffenders(
    crns: List<String>,
    laoStrategy: LaoStrategy,
  ): Map<String, Boolean> {
    if (crns.isEmpty()) return emptyMap()

    val visibilities = laoService.isVisibleFromCaseAccess(
      crns = crns,
      laoStrategy = laoStrategy,
    )

    return visibilities.associateBy(
      keySelector = { it.crn },
      valueTransform = { it.isVisible },
    )
  }

  fun getAdjudicationsByNomsNumber(nomsNumber: String): AuthorisableActionResult<AdjudicationsPage> {
    val allAdjudications = mutableListOf<Adjudication>()
    val allAgencies = mutableListOf<Agency>()

    var currentPage: AdjudicationsPage? = null
    var currentPageIndex = 0
    do {
      if (currentPage != null) {
        currentPageIndex += 1
      }

      val offset = currentPageIndex * adjudicationsConfig.prisonApiPageSize

      val adjudicationsPageResponse = prisonsApiClient.getAdjudicationsPage(nomsNumber, offset, adjudicationsConfig.prisonApiPageSize)
      currentPage = when (adjudicationsPageResponse) {
        is ClientResult.Success -> adjudicationsPageResponse.body
        is ClientResult.Failure.StatusCode -> when (adjudicationsPageResponse.status) {
          HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
          HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
          else -> adjudicationsPageResponse.throwException()
        }
        is ClientResult.Failure -> adjudicationsPageResponse.throwException()
      }

      allAdjudications.addAll(currentPage.results)
      allAgencies.addAll(currentPage.agencies)
    } while (currentPage != null && currentPage.results.size == adjudicationsConfig.prisonApiPageSize)

    return AuthorisableActionResult.Success(
      AdjudicationsPage(
        results = allAdjudications,
        agencies = allAgencies,
      ),
    )
  }

  fun getAcctPrisonerAlertsByNomsNumber(nomsNumber: String): CasResult<List<PrisionerAlert>> {
    val alerts = when (val alertsResult = prisionerAlertsApiClient.getAlerts(nomsNumber, "HA")) {
      is ClientResult.Success -> alertsResult.body
      is ClientResult.Failure.StatusCode -> when (alertsResult.status) {
        HttpStatus.NOT_FOUND -> return CasResult.NotFound(entityType = "Alert", id = nomsNumber)
        HttpStatus.FORBIDDEN -> return CasResult.Unauthorised()
        else -> alertsResult.throwException()
      }
      is ClientResult.Failure -> alertsResult.throwException()
    }

    return CasResult.Success(alerts.content)
  }

  fun getCaseDetail(crn: String): CasResult<CaseDetail> {
    val caseDetail = when (val caseDetailResult = apDeliusContextApiClient.getCaseDetail(crn)) {
      is ClientResult.Success -> caseDetailResult.body
      is ClientResult.Failure.StatusCode -> when (caseDetailResult.status) {
        HttpStatus.NOT_FOUND -> return CasResult.NotFound("CaseDetail", crn)
        HttpStatus.FORBIDDEN -> return CasResult.Unauthorised()
        else -> caseDetailResult.throwException()
      }

      is ClientResult.Failure -> caseDetailResult.throwException()
    }
    return CasResult.Success(offenderRiskNoteParser.withParsedRiskNotes(caseDetail))
  }

  fun getCsraSummariesForOffender(
    crn: String,
    laoStrategy: LaoStrategy,
  ) = getPersonSummaryInfoResult(crn, laoStrategy).toCsraSummaries(crn)

  private fun PersonSummaryInfoResult.toCsraSummaries(crn: String): CasResult<List<CsraSummary>> = when (this) {
    is PersonSummaryInfoResult.Success.Full -> {
      this.summary.nomsId?.let { nomsNumber ->
        prisonsApiClient.getCsraSummariesForOffender(nomsNumber)
          .toCasResult(entityType = "CsraSummary", id = crn)
      } ?: CasResult.NotFound("Noms Number", crn)
    }

    is PersonSummaryInfoResult.Success.Restricted -> CasResult.Unauthorised()
    is PersonSummaryInfoResult.NotFound -> CasResult.NotFound("Person", crn)
  }

  fun getOffenderBookingDetails(crn: String): CasResult<BookingDetails> {
    val nomsNumber = caseService.getCase(crn)?.nomsNumber
      ?: return CasResult.NotFound("Offender", crn)

    return when (val inmateDetailResult = prisonsApiClient.getInmateDetailsWithCall(nomsNumber)) {
      is ClientResult.Success -> {
        val bookingId = inmateDetailResult.body.bookingId
          ?: return CasResult.NotFound("BookingDetails", crn)

        prisonsApiClient.getOffenderBookingDetails(bookingId)
          .toCasResult(entityType = "BookingDetails", id = crn)
      }

      is ClientResult.Failure.StatusCode -> when (inmateDetailResult.status) {
        HttpStatus.NOT_FOUND -> CasResult.NotFound("BookingDetails", crn)
        HttpStatus.FORBIDDEN -> CasResult.Unauthorised()
        else -> inmateDetailResult.throwException()
      }

      is ClientResult.Failure -> inmateDetailResult.throwException()
    }
  }
}
