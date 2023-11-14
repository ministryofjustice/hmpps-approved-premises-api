package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.AdjudicationsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CaseNotesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ExcludedCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.Agency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.Results
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderIds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderLanguages
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderProfile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Alert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import java.io.OutputStream
import java.time.LocalDate

@Service
class OffenderService(
  private val communityApiClient: CommunityApiClient,
  private val hmppsTierApiClient: HMPPSTierApiClient,
  private val prisonsApiClient: PrisonsApiClient,
  private val caseNotesClient: CaseNotesClient,
  private val apOASysContextApiClient: ApOASysContextApiClient,
  private val adjudicationsApiClient: AdjudicationsApiClient,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  prisonCaseNotesConfigBindingModel: PrisonCaseNotesConfigBindingModel,
  adjudicationsConfigBindingModel: PrisonAdjudicationsConfigBindingModel,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val ignoredRegisterTypesForFlags = listOf("RVHR", "RHRH", "RMRH", "RLRH", "MAPP")
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
      adjudicationsApiPageSize = adjudicationsConfigBindingModel.prisonApiPageSize ?: throw RuntimeException("No prison-adjudications.adjudications-api-page-size configuration provided"),
    )
  }

  fun getOffenderSummariesByCrns(crns: List<String>, userDistinguishedName: String, ignoreLao: Boolean = false): List<PersonSummaryInfoResult> {
    val offenders = when (val response = apDeliusContextApiClient.getSummariesForCrns(crns)) {
      is ClientResult.Success -> response.body
      is ClientResult.Failure -> response.throwException()
    }

    val laoResponse = if (!ignoreLao) {
      when (val response = apDeliusContextApiClient.getUserAccessForCrns(userDistinguishedName, crns)) {
        is ClientResult.Success -> response.body
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

  fun getOffenderByCrn(
    crn: String,
    userDistinguishedName: String,
    ignoreLao: Boolean = false,
  ): AuthorisableActionResult<OffenderDetailSummary> =
    when (val response = apDeliusContextApiClient.getSummariesForCrns(listOf(crn))) {
      is ClientResult.Success -> response.body
      is ClientResult.Failure.StatusCode -> response.throwException()
      is ClientResult.Failure -> response.throwException()
    }.cases.firstOrNull { it.crn == crn }?.let {
      if (
        !ignoreLao &&
        (it.currentExclusion == true || it.currentRestriction == true) &&
        isLao(userDistinguishedName, crn)
      ) {
        AuthorisableActionResult.Unauthorised()
      } else {
        AuthorisableActionResult.Success(it.asOffenderDetail())
      }
    } ?: AuthorisableActionResult.NotFound("Person", crn)

  fun CaseSummary.asOffenderDetail() = OffenderDetailSummary(
    firstName = name.forename,
    middleNames = name.middleNames,
    surname = name.surname,
    dateOfBirth = dateOfBirth,
    gender = gender ?: "",
    otherIds = OffenderIds(crn = crn, nomsNumber = nomsId, pncNumber = pnc),
    offenderProfile = OffenderProfile(
      ethnicity = profile?.ethnicity,
      nationality = profile?.nationality,
      genderIdentity = profile?.genderIdentity,
      religion = profile?.religion,
      offenderLanguages = OffenderLanguages(),
    ),
    softDeleted = false,
    currentExclusion = currentExclusion ?: false,
    currentRestriction = currentRestriction ?: false,
  )

  fun isLao(deliusUsername: String, crn: String): Boolean =
    when (val response = apDeliusContextApiClient.getUserAccessForCrns(deliusUsername, listOf(crn))) {
      is ClientResult.Success -> response.body
      is ClientResult.Failure -> response.throwException()
    }.access.firstOrNull { it.crn == crn }?.isLao() ?: throw NotFoundProblem(crn, "Person")

  fun canAccessOffender(username: String, crn: String): Boolean = !isLao(username, crn)

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

  fun getCaseDetail(crn: String, userDistinguishedName: String) =
    when (val res = apDeliusContextApiClient.getCaseDetail(crn)) {
      is ClientResult.Success -> res.body
      is ClientResult.Failure.StatusCode -> if (res.status == HttpStatus.NOT_FOUND) null else res.throwException()
      is ClientResult.Failure -> res.throwException()
    }?.let {
      if (
        (it.case.currentExclusion == true || it.case.currentRestriction == true) &&
        isLao(userDistinguishedName, crn)
      ) {
        AuthorisableActionResult.Unauthorised()
      } else {
        AuthorisableActionResult.Success(it)
      }
    } ?: AuthorisableActionResult.NotFound("Person", crn)

  fun getRiskByCrn(crn: String, userDistinguishedName: String): AuthorisableActionResult<PersonRisks> = try {
    when (val res = apDeliusContextApiClient.getCaseDetail(crn)) {
      is ClientResult.Success -> res.body
      is ClientResult.Failure.StatusCode -> if (res.status == HttpStatus.NOT_FOUND) null else res.throwException()
      is ClientResult.Failure -> res.throwException()
    }.let {
      if (it == null) {
        AuthorisableActionResult.NotFound("Person", crn)
      } else if (
        (it.case.currentExclusion == true || it.case.currentRestriction == true) &&
        isLao(userDistinguishedName, crn)
      ) {
        AuthorisableActionResult.Unauthorised()
      } else {
        AuthorisableActionResult.Success(
          PersonRisks(
            roshRisks = getRoshRisksEnvelope(crn),
            mappa = it.mappaForRisks(),
            tier = getRiskTierEnvelope(crn),
            flags = it.flags(),
          ),
        )
      }
    }
  } catch (e: Exception) {
    AuthorisableActionResult.Success(
      PersonRisks(
        roshRisks = getRoshRisksEnvelope(crn),
        mappa = RiskWithStatus(status = RiskStatus.Error),
        tier = getRiskTierEnvelope(crn),
        flags = RiskWithStatus(status = RiskStatus.Error),
      ),
    )
  }

  fun CaseDetail.mappaForRisks(): RiskWithStatus<Mappa> =
    mappaDetail?.let {
      RiskWithStatus(
        Mappa(
          "CAT ${mappaDetail.categoryDescription}/LEVEL ${mappaDetail.levelDescription}",
          mappaDetail.lastUpdated.toLocalDate(),
        ),
      )
    } ?: RiskWithStatus(status = RiskStatus.NotFound)

  fun CaseDetail.flags(): RiskWithStatus<List<String>> {
    val registrations = registrations.filter { !ignoredRegisterTypesForFlags.contains(it.code) }
    return if (registrations.isEmpty()) {
      RiskWithStatus(status = RiskStatus.NotFound)
    } else {
      RiskWithStatus(registrations.map { it.description })
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

      val adjudicationsPageResponse = adjudicationsApiClient.getAdjudicationsPage(nomsNumber, currentPageIndex, adjudicationsConfig.adjudicationsApiPageSize)
      currentPage = when (adjudicationsPageResponse) {
        is ClientResult.Success -> adjudicationsPageResponse.body
        is ClientResult.Failure.StatusCode -> when (adjudicationsPageResponse.status) {
          HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
          HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
          else -> adjudicationsPageResponse.throwException()
        }
        is ClientResult.Failure -> adjudicationsPageResponse.throwException()
      }

      allAdjudications.addAll(currentPage.results.content)
      allAgencies.addAll(currentPage.agencies)
    } while (currentPage != null && currentPage.results.content.size == adjudicationsConfig.adjudicationsApiPageSize)

    return AuthorisableActionResult.Success(
      AdjudicationsPage(
        Results(content = allAdjudications),
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

  fun getConvictions(deliusUsername: String, crn: String): AuthorisableActionResult<List<ActiveOffence>> =
    when (val caseDetail = getCaseDetail(crn, deliusUsername)) {
      is AuthorisableActionResult.Success -> AuthorisableActionResult.Success(
        caseDetail.entity.offences.map {
          ActiveOffence(
            deliusEventNumber = it.eventNumber,
            offenceDescription = it.description,
            "",
            0,
            offenceDate = it.date,
          )
        },
      )
      is AuthorisableActionResult.NotFound -> AuthorisableActionResult.NotFound()
      is AuthorisableActionResult.Unauthorised -> AuthorisableActionResult.Unauthorised()
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

  fun getDocument(crn: String, documentId: String, outputStream: OutputStream) = communityApiClient.getDocument(crn, documentId, outputStream)

  fun getInfoForPerson(crn: String, deliusUsername: String, ignoreLao: Boolean): PersonInfoResult = try {
    getOffenderSummariesByCrns(listOf(crn), deliusUsername, ignoreLao)
      .filter { it.crn == crn }
      .map {
        when (it) {
          is PersonSummaryInfoResult.NotFound -> PersonInfoResult.NotFound(it.crn)
          is PersonSummaryInfoResult.Unknown -> PersonInfoResult.Unknown(it.crn)
          is PersonSummaryInfoResult.Success.Restricted -> PersonInfoResult.Success.Restricted(it.crn, it.nomsNumber)
          is PersonSummaryInfoResult.Success.Full -> {
            val inmateDetails = it.summary.nomsId?.let { nomsNumber ->
              when (val inmateDetailsResult = getInmateDetailByNomsNumber(it.crn, nomsNumber)) {
                is AuthorisableActionResult.Success -> inmateDetailsResult.entity
                else -> null
              }
            }
            PersonInfoResult.Success.Full(
              it.crn,
              it.summary.asOffenderDetail(),
              inmateDetails,
            )
          }
        }
      }.first()
  } catch (e: Exception) {
    PersonInfoResult.Unknown(crn, e)
  }

  private fun getRoshRisksEnvelope(crn: String): RiskWithStatus<RoshRisks> {
    when (val roshRisksResponse = apOASysContextApiClient.getRoshRatings(crn)) {
      is ClientResult.Success -> {
        val summary = roshRisksResponse.body.rosh

        if (summary.anyRisksAreNull()) {
          return RiskWithStatus(
            status = RiskStatus.NotFound,
            value = null,
          )
        }

        return RiskWithStatus(
          status = RiskStatus.Retrieved,
          value = RoshRisks(
            overallRisk = summary.determineOverallRiskLevel().text,
            riskToChildren = summary.riskChildrenCommunity!!.text,
            riskToPublic = summary.riskPublicCommunity!!.text,
            riskToKnownAdult = summary.riskKnownAdultCommunity!!.text,
            riskToStaff = summary.riskStaffCommunity!!.text,
            lastUpdated = roshRisksResponse.body.dateCompleted?.toLocalDate()
              ?: roshRisksResponse.body.initiationDate.toLocalDate(),
          ),
        )
      }
      is ClientResult.Failure.StatusCode -> return if (roshRisksResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(
          status = RiskStatus.NotFound,
          value = null,
        )
      } else {
        RiskWithStatus(
          status = RiskStatus.Error,
          value = null,
        )
      }
      is ClientResult.Failure -> return RiskWithStatus(
        status = RiskStatus.Error,
        value = null,
      )
    }
  }

  private fun getRiskTierEnvelope(crn: String): RiskWithStatus<RiskTier> {
    when (val tierResponse = hmppsTierApiClient.getTier(crn)) {
      is ClientResult.Success -> {
        return RiskWithStatus(
          status = RiskStatus.Retrieved,
          value = RiskTier(
            level = tierResponse.body.tierScore,
            lastUpdated = tierResponse.body.calculationDate.toLocalDate(),
          ),
        )
      }
      is ClientResult.Failure.StatusCode -> return if (tierResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(status = RiskStatus.NotFound)
      } else {
        RiskWithStatus(status = RiskStatus.Error)
      }
      is ClientResult.Failure -> {
        return RiskWithStatus(status = RiskStatus.Error)
      }
    }
  }
}
