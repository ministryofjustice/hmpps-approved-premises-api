package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2v2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2.PeopleCas2v2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskOfSeriousHarm
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskToSelf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2DeliusUserLaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService as DeliusOffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2OffenderService as NomsOffenderService

@Service("Cas2v2PeopleController")
class Cas2v2PeopleController(
  private val nomsOffenderService: NomsOffenderService,
  private val deliusOffenderService: DeliusOffenderService,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val cas2v2UserService: Cas2v2UserService,
  private val oasysService: OASysService,
) : PeopleCas2v2Delegate {

  override fun searchByCrnGet(crn: String): ResponseEntity<Person> {
    val deliusUser = cas2v2UserService.getUserForRequest()
    val personInfo = deliusOffenderService.getPersonInfoResult(crn, deliusUser.cas2DeliusUserLaoStrategy())

    when (personInfo) {
      is PersonInfoResult.NotFound -> throw NotFoundProblem(crn, "Offender")
      is PersonInfoResult.Unknown -> throw personInfo.throwable ?: BadRequestProblem(errorDetail = "Could not retrieve person info for CRN: $crn")
      is PersonInfoResult.Success -> return ResponseEntity.ok(
        personTransformer.transformModelToPersonApi(personInfo),
      )
    }
  }

  override fun searchByNomisIdGet(nomsNumber: String): ResponseEntity<Person> {
    val currentUser = cas2v2UserService.getUserForRequest()
    val caseLoadId = currentUser.activeNomisCaseloadId ?: return ResponseEntity.notFound().build()
    val probationOffenderResult = nomsOffenderService.getPersonByNomsNumberAndActiveCaseLoadId(nomsNumber, caseLoadId)

    when (probationOffenderResult) {
      is ProbationOffenderSearchResult.NotFound -> throw NotFoundProblem(nomsNumber, "Offender")
      is ProbationOffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is ProbationOffenderSearchResult.Unknown ->
        throw probationOffenderResult.throwable
          ?: BadRequestProblem(errorDetail = "Could not retrieve person info for Prison Number: $nomsNumber")

      is ProbationOffenderSearchResult.Success.Full -> return ResponseEntity.ok(
        personTransformer.transformProbationOffenderToPersonApi(probationOffenderResult, nomsNumber),
      )
    }
  }

  override fun peopleCrnOasysRiskToSelfGet(crn: String): ResponseEntity<OASysRiskToSelf> {
    getOffenderDetails(crn)

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
    getOffenderDetails(crn)

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

  override fun peopleCrnRisksGet(crn: String): ResponseEntity<PersonRisks> {
    val risks = when (val risksResult = nomsOffenderService.getRiskByCrn(crn)) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Success -> risksResult.entity
    }

    return ResponseEntity.ok(risksTransformer.transformDomainToApi(risks, crn))
  }

  private fun getOffenderDetails(crn: String): OffenderDetailSummary {
    val offenderDetails = when (val offenderDetailsResult = nomsOffenderService.getOffenderByCrnDeprecated(crn)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
    }

    return offenderDetails
  }
}
