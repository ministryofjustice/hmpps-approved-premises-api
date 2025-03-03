package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PeopleApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskOfSeriousHarm
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskToSelf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSections
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonAcctAlert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PrisonCaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderRisksService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AdjudicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AlertTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NeedsDetailsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OffenceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PrisonCaseNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PrisonerAlertTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Service
class PeopleController(
  private val httpAuthService: HttpAuthService,
  private val offenderService: OffenderService,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val prisonCaseNoteTransformer: PrisonCaseNoteTransformer,
  private val adjudicationTransformer: AdjudicationTransformer,
  private val alertTransformer: AlertTransformer,
  private val prisonerAlertTransformer: PrisonerAlertTransformer,
  private val needsDetailsTransformer: NeedsDetailsTransformer,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val offenceTransformer: OffenceTransformer,
  private val userService: UserService,
  private val featureFlagService: FeatureFlagService,
  private val oasysService: OASysService,
  private val offenderRisksService: OffenderRisksService,
) : PeopleApiDelegate {

  override fun peopleSearchGet(crn: String): ResponseEntity<Person> {
    val user = userService.getUserForRequest()

    val personInfo = offenderService.getPersonInfoResult(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    when (personInfo) {
      is PersonInfoResult.NotFound -> throw NotFoundProblem(crn, "Offender")
      is PersonInfoResult.Unknown -> throw personInfo.throwable ?: RuntimeException("Could not retrieve person info for CRN: $crn")
      is PersonInfoResult.Success -> return ResponseEntity.ok(
        personTransformer.transformModelToPersonApi(personInfo),
      )
    }
  }

  override fun peopleCrnRisksGet(crn: String): ResponseEntity<PersonRisks> {
    val principal = httpAuthService.getDeliusPrincipalOrThrow()

    when (offenderService.getPersonSummaryInfoResult(crn, LaoStrategy.CheckUserAccess(principal.name))) {
      is PersonSummaryInfoResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is PersonSummaryInfoResult.Success.Restricted -> throw ForbiddenProblem()
      is PersonSummaryInfoResult.Unknown -> throw NotFoundProblem(crn, "Person")
      is PersonSummaryInfoResult.Success.Full -> Unit
    }

    val risks = offenderRisksService.getPersonRisks(crn)

    return ResponseEntity.ok(risksTransformer.transformDomainToApi(risks, crn))
  }

  override fun peopleCrnPrisonCaseNotesGet(
    crn: String,
    xServiceName: ServiceName,
  ): ResponseEntity<List<PrisonCaseNote>> {
    val offenderDetails = getOffenderDetails(crn)

    if (offenderDetails.otherIds.nomsNumber == null) {
      throw NotFoundProblem(crn, "Case Notes")
    }

    val nomsNumber = offenderDetails.otherIds.nomsNumber

    val prisonCaseNotesResult = offenderService.getFilteredPrisonCaseNotesByNomsNumber(
      nomsNumber,
      getCas1SpecificNoteTypes = xServiceName == ServiceName.approvedPremises,
    )

    return ResponseEntity.ok(extractEntityFromCasResult(prisonCaseNotesResult).map(prisonCaseNoteTransformer::transformModelToApi))
  }

  override fun peopleCrnAdjudicationsGet(crn: String, xServiceName: ServiceName): ResponseEntity<List<Adjudication>> {
    val offenderDetails = getOffenderDetails(crn)

    if (offenderDetails.otherIds.nomsNumber == null) {
      throw NotFoundProblem(crn, "Adjudications")
    }

    val nomsNumber = offenderDetails.otherIds.nomsNumber

    val adjudicationsResult = offenderService.getAdjudicationsByNomsNumber(nomsNumber)
    val adjudications = when (adjudicationsResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Inmate")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> adjudicationsResult.entity
    }

    return ResponseEntity.ok(
      adjudicationTransformer.transformToApi(
        adjudications,
        getLast12MonthsOnly = xServiceName == ServiceName.approvedPremises,
      ),
    )
  }

  override fun peopleCrnAcctAlertsGet(crn: String): ResponseEntity<List<PersonAcctAlert>> {
    val offenderDetails = getOffenderDetails(crn)

    if (offenderDetails.otherIds.nomsNumber == null) {
      throw NotFoundProblem(crn, "ACCT Alerts")
    }

    val nomsNumber = offenderDetails.otherIds.nomsNumber

    val usePrisonerAlertsApi = featureFlagService.getBooleanFlag("get-alerts-from-prisoner-alerts-api")

    if (usePrisonerAlertsApi) {
      val acctAlertsResult = offenderService.getAcctPrisonerAlertsByNomsNumber(nomsNumber)

      return ResponseEntity.ok(extractEntityFromCasResult(acctAlertsResult).map(prisonerAlertTransformer::transformToApi))
    } else {
      val acctAlertsResult = offenderService.getAcctAlertsByNomsNumber(nomsNumber)

      return ResponseEntity.ok(extractEntityFromCasResult(acctAlertsResult).map(alertTransformer::transformToApi))
    }
  }

  override fun peopleCrnOasysSelectionGet(crn: String): ResponseEntity<List<OASysSection>> {
    ensureUserCanAccessOffenderInfo(crn)

    return ResponseEntity.ok(
      needsDetailsTransformer.transformToApi(
        extractEntityFromCasResult(oasysService.getOASysNeeds(crn)),
      ),
    )
  }

  override fun peopleCrnOasysSectionsGet(crn: String, selectedSections: List<Int>?): ResponseEntity<OASysSections> {
    ensureUserCanAccessOffenderInfo(crn)

    val needs = extractEntityFromCasResult(oasysService.getOASysNeeds(crn))

    return runBlocking(context = Dispatchers.IO) {
      val offenceDetailsResult = async {
        oasysService.getOASysOffenceDetails(crn)
      }
      val roshSummaryResult = async {
        oasysService.getOASysRoshSummary(crn)
      }
      val riskToTheIndividualResult = async {
        oasysService.getOASysRiskToTheIndividual(crn)
      }
      val riskManagementPlanResult = async {
        oasysService.getOASysRiskManagementPlan(crn)
      }

      val offenceDetails = extractEntityFromCasResult(offenceDetailsResult.await())
      val roshSummary = extractEntityFromCasResult(roshSummaryResult.await())
      val riskToTheIndividual = extractEntityFromCasResult(riskToTheIndividualResult.await())
      val riskManagementPlan = extractEntityFromCasResult(riskManagementPlanResult.await())

      ResponseEntity.ok(
        oaSysSectionsTransformer.transformToApi(
          offenceDetails,
          roshSummary,
          riskToTheIndividual,
          riskManagementPlan,
          needs,
          selectedSections ?: emptyList(),
        ),
      )
    }
  }

  override fun peopleCrnOasysRiskToSelfGet(crn: String): ResponseEntity<OASysRiskToSelf> {
    ensureUserCanAccessOffenderInfo(crn)

    return runBlocking(context = Dispatchers.IO) {
      val offenceDetailsResult = async {
        oasysService.getOASysOffenceDetails(crn)
      }

      val riskToTheIndividualResult = async {
        oasysService.getOASysRiskToTheIndividual(crn)
      }

      val offenceDetails = extractEntityFromCasResult(offenceDetailsResult.await())
      val riskToTheIndividual = extractEntityFromCasResult(riskToTheIndividualResult.await())

      ResponseEntity.ok(
        oaSysSectionsTransformer.transformRiskToIndividual(offenceDetails, riskToTheIndividual),
      )
    }
  }

  override fun peopleCrnOasysRoshGet(crn: String): ResponseEntity<OASysRiskOfSeriousHarm> {
    ensureUserCanAccessOffenderInfo(crn)

    return runBlocking(context = Dispatchers.IO) {
      val offenceDetailsResult = async {
        oasysService.getOASysOffenceDetails(crn)
      }

      val roshResult = async {
        oasysService.getOASysRoshSummary(crn)
      }

      val offenceDetails = extractEntityFromCasResult(offenceDetailsResult.await())
      val rosh = extractEntityFromCasResult(roshResult.await())

      ResponseEntity.ok(
        oaSysSectionsTransformer.transformRiskOfSeriousHarm(offenceDetails, rosh),
      )
    }
  }

  override fun peopleCrnOffencesGet(crn: String): ResponseEntity<List<ActiveOffence>> {
    ensureUserCanAccessOffenderInfo(crn)

    val caseDetail = offenderService.getCaseDetail(crn)
    return ResponseEntity.ok(
      offenceTransformer.transformToApi(extractEntityFromCasResult(caseDetail)),
    )
  }

  private fun ensureUserCanAccessOffenderInfo(crn: String) {
    getOffenderDetails(crn)
  }

  private fun getOffenderDetails(crn: String): OffenderDetailSummary {
    val user = userService.getUserForRequest()

    val offenderDetails = when (
      val offenderDetailsResult = offenderService.getOffenderByCrn(
        crn = crn,
        userDistinguishedName = user.deliusUsername,
        ignoreLaoRestrictions = false,
      )
    ) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
    }

    return offenderDetails
  }
}
