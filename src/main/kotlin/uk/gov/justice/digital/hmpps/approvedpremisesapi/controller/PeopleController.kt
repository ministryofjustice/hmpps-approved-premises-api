package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PeopleApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSections
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonAcctAlert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PrisonCaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AdjudicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OffenceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PrisonCaseNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PrisonerAlertTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Service
class PeopleController(
  private val offenderService: OffenderService,
  private val personTransformer: PersonTransformer,
  private val prisonCaseNoteTransformer: PrisonCaseNoteTransformer,
  private val adjudicationTransformer: AdjudicationTransformer,
  private val prisonerAlertTransformer: PrisonerAlertTransformer,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val offenceTransformer: OffenceTransformer,
  private val userService: UserService,
  private val oasysService: OASysService,
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

    val acctAlertsResult = offenderService.getAcctPrisonerAlertsByNomsNumber(nomsNumber)

    return ResponseEntity.ok(extractEntityFromCasResult(acctAlertsResult).map(prisonerAlertTransformer::transformToApi))
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
