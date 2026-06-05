package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskOfSeriousHarm
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskToSelf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcOffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcOAsysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Cas2HdcController
class Cas2HdcPeopleController(
  private val offenderService: Cas2HdcOffenderService,
  private val oasysService: OASysService,
  private val oaSysSectionsTransformer: Cas2HdcOAsysSectionsTransformer,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val cas2HdcUserService: Cas2HdcUserService,
) {

  @SuppressWarnings("TooGenericExceptionThrown", "ThrowsCount")
  @GetMapping("/people/search")
  fun peopleSearchGet(@RequestParam nomsNumber: String): ResponseEntity<Person> {
    val currentUser = cas2HdcUserService.getUserForRequest(Cas2ServiceOrigin.HDC)

    when (val probationOffenderResult = offenderService.getPersonByNomsNumber(nomsNumber, currentUser)) {
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

  @GetMapping("/people/{crn}/oasys/risk-to-self")
  fun peopleCrnOasysRiskToSelfGet(@PathVariable crn: String): ResponseEntity<OASysRiskToSelf> {
    ensureCanAccessOffender(crn)

    return ResponseEntity.ok(
      oaSysSectionsTransformer.transformRiskToIndividual(
        extractEntityFromCasResult(oasysService.getRiskToTheIndividual(crn)),
      ),
    )
  }

  @GetMapping("/people/{crn}/oasys/rosh")
  fun peopleCrnOasysRoshGet(@PathVariable crn: String): ResponseEntity<OASysRiskOfSeriousHarm> {
    ensureCanAccessOffender(crn)

    return ResponseEntity.ok(
      oaSysSectionsTransformer.transformRiskOfSeriousHarm(
        extractEntityFromCasResult(oasysService.getRoshSummary(crn)),
      ),
    )
  }

  @GetMapping("/people/{crn}/risks")
  fun peopleCrnRisksGet(@PathVariable crn: String): ResponseEntity<PersonRisks> {
    val risks = when (val risksResult = offenderService.getRiskByCrn(crn)) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Success -> risksResult.entity
    }

    return ResponseEntity.ok(risksTransformer.transformDomainToApi(risks, crn))
  }

  private fun ensureCanAccessOffender(crn: String) {
    ensureEntityFromCasResultIsSuccess(offenderService.getOffenderByCrn(crn))
  }
}
