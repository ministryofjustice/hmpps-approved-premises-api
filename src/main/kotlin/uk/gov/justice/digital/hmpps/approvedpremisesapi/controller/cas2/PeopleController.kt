package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.PeopleCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskOfSeriousHarm
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskToSelf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Service("Cas2PeopleController")
class PeopleController(
  private val offenderService: Cas2OffenderService,
  private val oasysService: OASysService,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val nomisUserService: NomisUserService,
) : PeopleCas2Delegate {

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun peopleSearchGet(nomsNumber: String): ResponseEntity<Person> {
    val currentUser = nomisUserService.getUserForRequest()

    val probationOffenderResult = offenderService.getPersonByNomsNumber(nomsNumber, currentUser)

    when (probationOffenderResult) {
      is ProbationOffenderSearchResult.NotFound -> throw NotFoundProblem(nomsNumber, "Offender")
      is ProbationOffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is ProbationOffenderSearchResult.Unknown ->
        throw probationOffenderResult.throwable
          ?: RuntimeException("Could not retrieve person info for Prison Number: $nomsNumber")

      is ProbationOffenderSearchResult.Success.Full -> return ResponseEntity.ok(
        personTransformer.transformProbationOffenderToPersonApi(probationOffenderResult, nomsNumber),
      )

      null -> throw NotFoundProblem(nomsNumber, "Offender")
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
    val risks = when (val risksResult = offenderService.getRiskByCrn(crn)) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Success -> risksResult.entity
    }

    return ResponseEntity.ok(risksTransformer.transformDomainToApi(risks, crn))
  }

  private fun getOffenderDetails(crn: String): OffenderDetailSummary {
    val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrnDeprecated(crn)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
    }

    return offenderDetails
  }
}
