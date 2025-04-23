package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.apache.commons.collections4.ListUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CaseNotesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerAlertsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ExcludedCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Agency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import java.time.LocalDate
import java.util.stream.Collectors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisoneralertsapi.Alert as PrisionerAlert

@Service
class OffenderService(
  private val prisonsApiClient: PrisonsApiClient,
  private val prisionerAlertsApiClient: PrisonerAlertsApiClient,
  private val caseNotesClient: CaseNotesClient,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val offenderDetailsDataSource: OffenderDetailsDataSource,
  private val personTransformer: PersonTransformer,
  prisonCaseNotesConfigBindingModel: PrisonCaseNotesConfigBindingModel,
  adjudicationsConfigBindingModel: PrisonAdjudicationsConfigBindingModel,
) {
  companion object {
    const val MAX_OFFENDER_REQUEST_COUNT = 500
  }

  private val log = LoggerFactory.getLogger(this::class.java)

  private val prisonCaseNotesConfig: PrisonCaseNotesConfig
  private val adjudicationsConfig: PrisonAdjudicationsConfig

  init {
    val excludedCategories = prisonCaseNotesConfigBindingModel.excludedCategories
      ?: throw RuntimeException("No prison-case-notes.excluded-categories provided")

    prisonCaseNotesConfig = PrisonCaseNotesConfig(
      lookbackDays = prisonCaseNotesConfigBindingModel.lookbackDays ?: throw RuntimeException("No prison-case-notes.lookback-days configuration provided"),
      prisonApiPageSize = prisonCaseNotesConfigBindingModel.prisonApiPageSize ?: throw RuntimeException("No prison-api-page-size configuration provided"),
      excludedCategories = excludedCategories.mapIndexed { index, categoryConfig ->
        ExcludedCategory(
          category = categoryConfig.category ?: throw RuntimeException("No category provided for prison-case-notes.excluded-categories at index $index"),
          subcategory = categoryConfig.subcategory,
        )
      },
    )

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
  ) = getPersonSummaryInfoResults(setOf(crn), laoStrategy).first()

  fun getPersonSummaryInfoResults(
    crns: Set<String>,
    laoStrategy: LaoStrategy,
  ): List<PersonSummaryInfoResult> {
    if (crns.isEmpty()) {
      return emptyList()
    }

    if (crns.size > MAX_OFFENDER_REQUEST_COUNT) {
      throw InternalServerErrorProblem("Cannot request more than $MAX_OFFENDER_REQUEST_COUNT CRNs. ${crns.size} have been provided.")
    }

    val crnsList = crns.toList()

    val caseSummariesByCrn = when (val result = apDeliusContextApiClient.getCaseSummaries(crnsList)) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }.cases.associateBy(
      keySelector = { it.crn },
      valueTransform = { it },
    )

   /*
    * this could be more efficient by only retrieving access information for CRNs where
    * the corresponding [CaseSummary.hasLimitedAccess()] is true. A similar short-circuit
    * was implemented in the new deprecated [getOffender()] function.
    *
    * It could also be more efficient if ignoring offenders that can't be found on calls to
    * [apDeliusContextApiClient.getSummariesForCrns(crnsList)] (although that shouldn't
    * happen often)
    */
    val caseAccessByCrn = when (laoStrategy) {
      is LaoStrategy.NeverRestricted -> emptyMap()
      is LaoStrategy.CheckUserAccess ->
        when (val result = apDeliusContextApiClient.getUserAccessForCrns(laoStrategy.deliusUsername, crnsList)) {
          is ClientResult.Success -> result.body
          is ClientResult.Failure -> result.throwException()
        }.access.associateBy(
          keySelector = { it.crn },
          valueTransform = { it },
        )
    }

    return crns.map { crn ->
      toPersonSummaryInfo(
        crn,
        caseSummariesByCrn[crn],
        caseAccessByCrn[crn],
        laoStrategy,
      )
    }
  }

  private fun toPersonSummaryInfo(
    crn: String,
    caseSummary: CaseSummary?,
    caseAccess: CaseAccess?,
    laoStrategy: LaoStrategy,
  ): PersonSummaryInfoResult {
    if (caseSummary == null) {
      log.debug("Could not find case summary for '$crn'. Returning not found")
      return PersonSummaryInfoResult.NotFound(crn)
    }

    if (!caseSummary.hasLimitedAccess() || laoStrategy is LaoStrategy.NeverRestricted) {
      log.debug("No restrictions apply, or the caller has indicated to ignore restrictions for '$crn'. Returning full details")
      return PersonSummaryInfoResult.Success.Full(crn, caseSummary)
    }

    return if (caseAccess == null) {
      // This shouldn't happen
      log.warn("Could not find case access details for LAO '$crn'. Returning 'Not Found'")
      PersonSummaryInfoResult.NotFound(crn)
    } else {
      if (caseAccess.hasLimitedAccess()) {
        log.debug("Caller cannot access LAO '$crn'. Returning restricted")
        PersonSummaryInfoResult.Success.Restricted(crn, caseSummary.nomsId)
      } else {
        log.debug("Caller can access LAO '$crn'. Returning full details")
        PersonSummaryInfoResult.Success.Full(crn, caseSummary)
      }
    }
  }

  private fun CaseSummary.hasLimitedAccess() = this.currentExclusion == true || this.currentRestriction == true
  private fun CaseAccess.hasLimitedAccess() = this.userExcluded || this.userRestricted
  private fun CaseAccess.hasNotLimitedAccess() = !this.userExcluded && !this.userRestricted

  @Deprecated(
    "This function uses the now deprecated [OffenderDetailsDataSource]",
    ReplaceWith("getPersonSummaryInfoResults(crns, limitedAccessStrategy)"),
  )
  fun getOffenderSummariesByCrns(
    crns: Set<String>,
    deliusUsername: String?,
    ignoreLaoRestrictions: Boolean = false,
  ): List<PersonSummaryInfoResult> {
    check(ignoreLaoRestrictions || deliusUsername != null) { "If ignoreLao is false, delius username must be provided " }

    if (crns.isEmpty()) return emptyList()

    val offenderDetailsList = offenderDetailsDataSource.getOffenderDetailSummaries(crns.toList())
    val userAccessList = deliusUsername?.let { offenderDetailsDataSource.getUserAccessForOffenderCrns(it, crns.toList()) }

    return crns.map { crn ->
      val offenderResponse = offenderDetailsList[crn]
      val accessResponse = userAccessList?.get(crn)

      val offender = getOffender(
        ignoreLaoRestrictions,
        { offenderResponse },
        { accessResponse },
      )

      when (offender) {
        is AuthorisableActionResult.Success -> {
          PersonSummaryInfoResult.Success.Full(crn, offender.entity.asCaseSummary())
        }
        is AuthorisableActionResult.NotFound -> PersonSummaryInfoResult.NotFound(crn)
        is AuthorisableActionResult.Unauthorised -> {
          val nomsNumber = (offenderResponse as ClientResult.Success).body.otherIds.nomsNumber
          PersonSummaryInfoResult.Success.Restricted(crn, nomsNumber)
        }
      }
    }
  }

  @Deprecated(
    """ This function returns the now deprecated [OffenderDetailSummary], which is the community-api data model
     
      Note that whilst this function returns CasResult.Unauthorised if offender is LAO and user can't access
      them, the non-deprecated functions will instead return PersonSummaryInfoResult.Full.Restricted""",
    ReplaceWith("getPersonSummaryInfoResults(crns, limitedAccessStrategy)"),
  )
  fun getOffenderByCrn(crn: String, userDistinguishedName: String, ignoreLaoRestrictions: Boolean = false): AuthorisableActionResult<OffenderDetailSummary> = getOffender(
    ignoreLaoRestrictions,
    { offenderDetailsDataSource.getOffenderDetailSummary(crn) },
    { offenderDetailsDataSource.getUserAccessForOffenderCrn(userDistinguishedName, crn) },
  )

  /**
   * Returns CasResult.Unauthorised if offender is LAO and user can't access them
   */
  @Deprecated(
    """
      This function returns the now deprecated [OffenderDetailSummary], which is the community-api data model
      
      Note that whilst this function returns CasResult.Unauthorised if offender is LAO and user can't access
      them, the non-deprecated functions will instead return PersonSummaryInfoResult.Full.Restricted
    """,
    ReplaceWith("getPersonSummaryInfoResults(crns, limitedAccessStrategy)"),
  )
  @Suppress("detekt:CyclomaticComplexMethod", "detekt:NestedBlockDepth", "detekt:ReturnCount")
  private fun getOffender(
    ignoreLaoRestrictions: Boolean,
    offenderProducer: () -> ClientResult<OffenderDetailSummary>?,
    userAccessProducer: () -> ClientResult<UserOffenderAccess>?,
  ): AuthorisableActionResult<OffenderDetailSummary> {
    val offender = when (val offenderResponse = offenderProducer()) {
      is ClientResult.Success -> offenderResponse.body
      is ClientResult.Failure.StatusCode -> when (offenderResponse.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        else -> offenderResponse.throwException()
      }
      is ClientResult.Failure -> offenderResponse.throwException()
      null -> return AuthorisableActionResult.NotFound()
    }

    if (!ignoreLaoRestrictions) {
      if (offender.currentExclusion || offender.currentRestriction) {
        val access = when (val accessResponse = userAccessProducer()) {
          is ClientResult.Success -> accessResponse.body
          is ClientResult.Failure.StatusCode -> {
            if (accessResponse.status == HttpStatus.FORBIDDEN) {
              // This is legacy behaviour to differentiate between the community api get access for
              // a single CRN endpoint returning 403 (meaning the client can't access the endpoint),
              // and the endpoint returning a response indicating the user can't access the offender
              // This isn't required if directly calling apDeliusContextApiClient.getUserAccessForCrns
              try {
                accessResponse.deserializeTo<UserOffenderAccess>()
                return AuthorisableActionResult.Unauthorised()
              } catch (exception: Exception) {
                accessResponse.throwException()
              }
            }

            accessResponse.throwException()
          }
          is ClientResult.Failure -> accessResponse.throwException()
          null -> return AuthorisableActionResult.NotFound()
        }

        if (access.userExcluded || access.userRestricted) {
          return AuthorisableActionResult.Unauthorised()
        }
      }
    }

    return AuthorisableActionResult.Success(offender)
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
  ) = when (laoStrategy) {
    is LaoStrategy.NeverRestricted -> true
    is LaoStrategy.CheckUserAccess -> canAccessOffender(
      username = laoStrategy.deliusUsername,
      crn = crn,
    )
  }

  fun canAccessOffender(username: String, crn: String) = canAccessOffenders(username, listOf(crn))[crn] == true

  fun canAccessOffenders(username: String, crns: List<String>): Map<String, Boolean> {
    if (crns.isEmpty()) return emptyMap()

    if (crns.size > MAX_OFFENDER_REQUEST_COUNT) {
      throw InternalServerErrorProblem("Cannot request access details for more than $MAX_OFFENDER_REQUEST_COUNT CRNs. ${crns.size} have been provided.")
    }

    return when (val clientResult = apDeliusContextApiClient.getUserAccessForCrns(username, crns)) {
      is ClientResult.Success -> {
        val crnToAccessResult = clientResult.body.access.associateBy(
          keySelector = { it.crn },
          valueTransform = { it },
        )

        crns.associateBy(
          keySelector = { it },
          valueTransform = { crn ->
            val access = crnToAccessResult[crn]

            access?.hasNotLimitedAccess() ?: false
          },
        )
      }
      is ClientResult.Failure -> clientResult.throwException()
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

  fun getFilteredPrisonCaseNotesByNomsNumber(nomsNumber: String, getCas1SpecificNoteTypes: Boolean): CasResult<List<CaseNote>> {
    val cas1PrisonNoteTypesToInclude = listOf(
      "Alert", "Conduct & Behaviour", "Custodial Violence Management", "Negative Behaviours", "Enforcement", "Interventions / Keywork",
      "Mental Health", "Drug Rehabilitation", "Social Care", "Positive Behaviour / Achievements", "Alcohol Treatment",
    )
    val allCaseNotes = mutableListOf<CaseNote>()

    val fromDate = LocalDate.now().minusDays(prisonCaseNotesConfig.lookbackDays.toLong())

    var currentPage: CaseNotesPage?
    var currentPageIndex: Int? = null
    do {
      if (currentPageIndex == null) {
        currentPageIndex = 0
      } else {
        currentPageIndex += 1
      }

      val caseNotesPageResponse = caseNotesClient.getCaseNotesPage(nomsNumber, fromDate, currentPageIndex, prisonCaseNotesConfig.prisonApiPageSize)
      currentPage = when (caseNotesPageResponse) {
        is ClientResult.Success -> caseNotesPageResponse.body
        is ClientResult.Failure.StatusCode -> when (caseNotesPageResponse.status) {
          HttpStatus.NOT_FOUND -> return CasResult.NotFound(entityType = "CaseNotes", id = "nomsNumber")
          HttpStatus.FORBIDDEN -> return CasResult.Unauthorised()
          else -> caseNotesPageResponse.throwException()
        }
        is ClientResult.Failure -> caseNotesPageResponse.throwException()
      }

      allCaseNotes.addAll(
        if (getCas1SpecificNoteTypes) {
          currentPage.content.filter { caseNote ->
            cas1PrisonNoteTypesToInclude.any { it == (caseNote.typeDescription ?: caseNote.type) }
          }
        } else {
          currentPage.content.filter { caseNote ->
            prisonCaseNotesConfig.excludedCategories.none { it.excluded(caseNote.type, caseNote.subType) }
          }
        },
      )
    } while (currentPage != null && currentPage.totalPages > currentPageIndex!! + 1)

    return CasResult.Success(allCaseNotes)
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
    val alertsResult = prisionerAlertsApiClient.getAlerts(nomsNumber, "HA")

    val alerts = when (alertsResult) {
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
    return CasResult.Success(caseDetail)
  }

  fun getPersonInfoResult(
    crn: String,
    laoStrategy: LaoStrategy,
  ) = getPersonInfoResults(setOf(crn), laoStrategy).first()

  fun getPersonInfoResults(
    crns: Set<String>,
    laoStrategy: LaoStrategy,
  ) = getPersonInfoResults(
    crns = crns,
    deliusUsername = when (laoStrategy) {
      is LaoStrategy.NeverRestricted -> null
      is LaoStrategy.CheckUserAccess -> laoStrategy.deliusUsername
    },
    ignoreLaoRestrictions = when (laoStrategy) {
      is LaoStrategy.NeverRestricted -> true
      is LaoStrategy.CheckUserAccess -> false
    },
  )

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
    return getPersonInfoResults(setOf(crn), deliusUsername, ignoreLaoRestrictions).first()
  }

  private fun getPersonInfoResults(
    crns: Set<String>,
    deliusUsername: String?,
    ignoreLaoRestrictions: Boolean,
  ): List<PersonInfoResult> {
    check(ignoreLaoRestrictions || deliusUsername != null) { "If ignoreLao is false, delius username must be provided" }

    if (crns.isEmpty()) return emptyList()

    val offendersDetails = getOffenderSummariesByCrns(crns, deliusUsername, ignoreLaoRestrictions)

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
}
