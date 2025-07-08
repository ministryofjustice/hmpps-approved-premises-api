package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskOfSeriousHarm
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskToSelf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2OAsysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@RestController
@RequestMapping(
  "\${openapi.communityAccommodationServicesTier2CAS2.base-path:/cas2}",
  produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE],
)
class Cas2PeopleController(
  private val offenderService: Cas2OffenderService,
  private val oasysService: OASysService,
  private val oaSysSectionsTransformer: Cas2OAsysSectionsTransformer,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val cas2UserService: Cas2UserService,
) {

  @SuppressWarnings("TooGenericExceptionThrown", "ThrowsCount")
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/search"],
  )
  fun peopleSearchGet(@RequestParam nomsNumber: String): ResponseEntity<Person> {
    val currentUser = cas2UserService.getUserForRequest()

    val probationOffenderResult = offenderService.getPersonByNomsNumber(nomsNumber, currentUser)

    when (probationOffenderResult) {
      is ProbationOffenderSearchResult.NotFound -> throw NotFoundProblem(nomsNumber, "Offender")
      is ProbationOffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is ProbationOffenderSearchResult.Unknown ->
        throw probationOffenderResult.throwable
          ?: RuntimeException("Could not retrieve person info for Prison Number: $nomsNumber")

      is ProbationOffenderSearchResult.Success.Full -> return ResponseEntity.ok(
        personTransformer.transformProbationOffenderToPersonApi(probationOffenderResult),
      )

      null -> throw NotFoundProblem(nomsNumber, "Offender")
    }
  }

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/{crn}/oasys/risk-to-self"],
  )
  fun peopleCrnOasysRiskToSelfGet(@PathVariable crn: String): ResponseEntity<OASysRiskToSelf> {
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

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/{crn}/oasys/rosh"],
  )
  fun peopleCrnOasysRoshGet(@PathVariable crn: String): ResponseEntity<OASysRiskOfSeriousHarm> {
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

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/{crn}/risks"],
  )
  fun peopleCrnRisksGet(@PathVariable crn: String): ResponseEntity<PersonRisks> {
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
