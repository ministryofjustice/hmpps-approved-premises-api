package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CaseNotesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ExcludedCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderRisksDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Conviction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Agency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Alert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import java.io.OutputStream
import java.time.LocalDate

@Service
class OffenderService(
  private val communityApiClient: CommunityApiClient,
  private val prisonsApiClient: PrisonsApiClient,
  private val caseNotesClient: CaseNotesClient,
  private val apOASysContextApiClient: ApOASysContextApiClient,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val offenderDetailsDataSource: OffenderDetailsDataSource,
  private val offenderRisksDataSource: OffenderRisksDataSource,
  prisonCaseNotesConfigBindingModel: PrisonCaseNotesConfigBindingModel,
  adjudicationsConfigBindingModel: PrisonAdjudicationsConfigBindingModel,
) {
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

  fun getOffenderSummariesByCrns(
    crns: Set<String>,
    deliusUsername: String,
    ignoreLaoRestrictions: Boolean = false,
    forceApDeliusContextApi: Boolean = false,
  ): List<PersonSummaryInfoResult> {
    if (forceApDeliusContextApi) return getOffenderSummariesByCrns(crns.toList(), deliusUsername, ignoreLaoRestrictions)

    if (crns.isEmpty()) return emptyList()

    val offenderDetailsList = offenderDetailsDataSource.getOffenderDetailSummaries(crns.toList())
    val userAccessList = offenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, crns.toList())

    return crns.map { crn ->
      val offenderResponse = offenderDetailsList[crn]
      val accessResponse = userAccessList[crn]

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

  @Deprecated("This method directly couples to the AP Delius Context API.", replaceWith = ReplaceWith("getOffenderSummariesByCrns(crns, userDistinguishedName, ignoreLaoRestrictions, true)"))
  @SuppressWarnings("CyclomaticComplexMethod")
  fun getOffenderSummariesByCrns(crns: List<String>, userDistinguishedName: String, ignoreLaoRestrictions: Boolean = false): List<PersonSummaryInfoResult> {
    if (crns.isEmpty()) {
      return emptyList()
    }

    val offenders = when (val response = apDeliusContextApiClient.getSummariesForCrns(crns)) {
      is ClientResult.Success -> response.body
      is ClientResult.Failure.StatusCode -> response.throwException()
      is ClientResult.Failure -> response.throwException()
    }

    val laoResponse = if (!ignoreLaoRestrictions) {
      when (val response = apDeliusContextApiClient.getUserAccessForCrns(userDistinguishedName, crns)) {
        is ClientResult.Success -> response.body
        is ClientResult.Failure.StatusCode -> response.throwException()
        is ClientResult.Failure -> response.throwException()
      }
    } else {
      null
    }

    return crns.map { crn ->
      val caseSummary = offenders.cases.find { it.crn == crn } ?: return@map PersonSummaryInfoResult.NotFound(crn)

      val isLao = laoResponse?.let {
        laoResponse.access.find { caseAccess ->
          caseAccess.crn == crn &&
            (caseAccess.userExcluded || caseAccess.userRestricted)
        } != null
      } ?: false

      if (isLao) {
        PersonSummaryInfoResult.Success.Restricted(crn, caseSummary.nomsId)
      } else {
        PersonSummaryInfoResult.Success.Full(crn, caseSummary)
      }
    }
  }

  fun getOffenderByCrn(crn: String, userDistinguishedName: String, ignoreLaoRestrictions: Boolean = false): AuthorisableActionResult<OffenderDetailSummary> {
    return getOffender(
      ignoreLaoRestrictions,
      { offenderDetailsDataSource.getOffenderDetailSummary(crn) },
      { offenderDetailsDataSource.getUserAccessForOffenderCrn(userDistinguishedName, crn) },
    )
  }

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
    val offenderResponse = offenderDetailsDataSource.getOffenderDetailSummary(crn)

    val offender = when (offenderResponse) {
      is ClientResult.Success -> offenderResponse.body
      is ClientResult.Failure -> offenderResponse.throwException()
    }

    return offender.currentExclusion || offender.currentRestriction
  }

  fun canAccessOffender(username: String, crn: String): Boolean {
    return when (val accessResponse = offenderDetailsDataSource.getUserAccessForOffenderCrn(username, crn)) {
      is ClientResult.Success -> !accessResponse.body.userExcluded && !accessResponse.body.userRestricted
      is ClientResult.Failure.StatusCode -> {
        if (accessResponse.status.equals(HttpStatus.FORBIDDEN)) {
          try {
            accessResponse.deserializeTo<UserOffenderAccess>()
            return false
          } catch (exception: Exception) {
            accessResponse.throwException()
          }
        }

        accessResponse.throwException()
      }
      is ClientResult.Failure -> accessResponse.throwException()
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

  @Deprecated(message = "The 'jwt' parameter is no longer needed.", replaceWith = ReplaceWith(expression = "getRiskByCrn(crn, userDistinguishedName)"))
  fun getRiskByCrn(crn: String, jwt: String, userDistinguishedName: String): AuthorisableActionResult<PersonRisks> =
    getRiskByCrn(crn, userDistinguishedName)

  fun getRiskByCrn(crn: String, deliusUsername: String): AuthorisableActionResult<PersonRisks> {
    return when (getOffenderByCrn(crn, deliusUsername)) {
      is AuthorisableActionResult.NotFound -> AuthorisableActionResult.NotFound()
      is AuthorisableActionResult.Unauthorised -> AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.Success -> AuthorisableActionResult.Success(
        offenderRisksDataSource.getPersonRisks(crn),
      )
    }
  }

  fun getPrisonCaseNotesByNomsNumber(nomsNumber: String): AuthorisableActionResult<List<CaseNote>> {
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
          HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
          HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
          else -> caseNotesPageResponse.throwException()
        }
        is ClientResult.Failure -> caseNotesPageResponse.throwException()
      }

      allCaseNotes.addAll(
        currentPage.content.filter { caseNote ->
          prisonCaseNotesConfig.excludedCategories.none { it.excluded(caseNote.type, caseNote.subType) }
        },
      )
    } while (currentPage != null && currentPage.totalPages > currentPageIndex!! + 1)

    return AuthorisableActionResult.Success(allCaseNotes)
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

  fun getAcctAlertsByNomsNumber(nomsNumber: String): AuthorisableActionResult<List<Alert>> {
    val alertsResult = prisonsApiClient.getAlerts(nomsNumber, "HA")

    val alerts = when (alertsResult) {
      is ClientResult.Success -> alertsResult.body
      is ClientResult.Failure.StatusCode -> when (alertsResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> alertsResult.throwException()
      }
      is ClientResult.Failure -> alertsResult.throwException()
    }

    return AuthorisableActionResult.Success(alerts)
  }

  fun getOASysNeeds(crn: String): AuthorisableActionResult<NeedsDetails> {
    val needsResult = apOASysContextApiClient.getNeedsDetails(crn)

    val needs = when (needsResult) {
      is ClientResult.Success -> needsResult.body
      is ClientResult.Failure.StatusCode -> when (needsResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> {
          log.warn("Failed to fetch OASys needs details - got response status ${needsResult.status}, returning 404")
          throw NotFoundProblem(crn, "OASys")
        }
      }
      is ClientResult.Failure.Other -> {
        log.warn("Failed to fetch OASys needs details, returning 404", needsResult.exception)
        throw NotFoundProblem(crn, "OASys")
      }
      is ClientResult.Failure -> needsResult.throwException()
    }

    return AuthorisableActionResult.Success(needs)
  }

  fun getOASysOffenceDetails(crn: String): AuthorisableActionResult<OffenceDetails> {
    val offenceDetailsResult = apOASysContextApiClient.getOffenceDetails(crn)

    val offenceDetails = when (offenceDetailsResult) {
      is ClientResult.Success -> offenceDetailsResult.body
      is ClientResult.Failure.StatusCode -> when (offenceDetailsResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> offenceDetailsResult.throwException()
      }
      is ClientResult.Failure -> offenceDetailsResult.throwException()
    }

    return AuthorisableActionResult.Success(offenceDetails)
  }

  fun getOASysRiskManagementPlan(crn: String): AuthorisableActionResult<RiskManagementPlan> {
    val riskManagementPlanResult = apOASysContextApiClient.getRiskManagementPlan(crn)

    val riskManagement = when (riskManagementPlanResult) {
      is ClientResult.Success -> riskManagementPlanResult.body
      is ClientResult.Failure.StatusCode -> when (riskManagementPlanResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> riskManagementPlanResult.throwException()
      }
      is ClientResult.Failure -> riskManagementPlanResult.throwException()
    }

    return AuthorisableActionResult.Success(riskManagement)
  }

  fun getOASysRoshSummary(crn: String): AuthorisableActionResult<RoshSummary> {
    val roshSummaryResult = apOASysContextApiClient.getRoshSummary(crn)

    val roshSummary = when (roshSummaryResult) {
      is ClientResult.Success -> roshSummaryResult.body
      is ClientResult.Failure.StatusCode -> when (roshSummaryResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> roshSummaryResult.throwException()
      }
      is ClientResult.Failure -> roshSummaryResult.throwException()
    }

    return AuthorisableActionResult.Success(roshSummary)
  }

  fun getOASysRiskToTheIndividual(crn: String): AuthorisableActionResult<RisksToTheIndividual> {
    val risksToTheIndividualResult = apOASysContextApiClient.getRiskToTheIndividual(crn)

    val riskToTheIndividual = when (risksToTheIndividualResult) {
      is ClientResult.Success -> risksToTheIndividualResult.body
      is ClientResult.Failure.StatusCode -> when (risksToTheIndividualResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> risksToTheIndividualResult.throwException()
      }
      is ClientResult.Failure -> risksToTheIndividualResult.throwException()
    }

    return AuthorisableActionResult.Success(riskToTheIndividual)
  }

  fun getConvictions(crn: String): AuthorisableActionResult<List<Conviction>> {
    val convictionsResult = communityApiClient.getConvictions(crn)

    val convictions = when (convictionsResult) {
      is ClientResult.Success -> convictionsResult.body
      is ClientResult.Failure.StatusCode -> when (convictionsResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> convictionsResult.throwException()
      }
      is ClientResult.Failure -> convictionsResult.throwException()
    }

    return AuthorisableActionResult.Success(convictions)
  }

  fun getDocuments(crn: String): AuthorisableActionResult<GroupedDocuments> {
    val documentsResult = communityApiClient.getDocuments(crn)

    val documents = when (documentsResult) {
      is ClientResult.Success -> documentsResult.body
      is ClientResult.Failure.StatusCode -> when (documentsResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> documentsResult.throwException()
      }
      is ClientResult.Failure -> documentsResult.throwException()
    }

    return AuthorisableActionResult.Success(documents)
  }

  fun getDocument(
    crn: String,
    documentId: String,
    outputStream: OutputStream,
  ) = communityApiClient.getDocument(crn, documentId, outputStream)

  @SuppressWarnings("CyclomaticComplexMethod", "NestedBlockDepth", "ReturnCount")
  fun getInfoForPerson(crn: String, deliusUsername: String, ignoreLaoRestrictions: Boolean): PersonInfoResult {
    val offenderResponse = offenderDetailsDataSource.getOffenderDetailSummary(crn)

    val offender = when (offenderResponse) {
      is ClientResult.Success -> offenderResponse.body

      is ClientResult.Failure.StatusCode -> if (offenderResponse.status.equals(HttpStatus.NOT_FOUND)) {
        return PersonInfoResult.NotFound(crn)
      } else {
        return PersonInfoResult.Unknown(crn, offenderResponse.toException())
      }

      is ClientResult.Failure -> return PersonInfoResult.Unknown(crn, offenderResponse.toException())
    }

    if (!ignoreLaoRestrictions) {
      if (offender.currentExclusion || offender.currentRestriction) {
        val access =
          when (val accessResponse = offenderDetailsDataSource.getUserAccessForOffenderCrn(deliusUsername, crn)) {
            is ClientResult.Success -> accessResponse.body
            is ClientResult.Failure.StatusCode -> {
              if (accessResponse.status.equals(HttpStatus.FORBIDDEN)) {
                try {
                  accessResponse.deserializeTo<UserOffenderAccess>()
                  return PersonInfoResult.Success.Restricted(crn, offender.otherIds.nomsNumber)
                } catch (exception: Exception) {
                  Sentry.captureException(RuntimeException("LAO calls returns but failed to marshal the response", exception))
                  return PersonInfoResult.Success.Restricted(crn, offender.otherIds.nomsNumber)
                }
              }

              accessResponse.throwException()
            }
            is ClientResult.Failure -> accessResponse.throwException()
          }

        if (access.userExcluded || access.userRestricted) {
          return PersonInfoResult.Success.Restricted(crn, offender.otherIds.nomsNumber)
        }
      }
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
}
