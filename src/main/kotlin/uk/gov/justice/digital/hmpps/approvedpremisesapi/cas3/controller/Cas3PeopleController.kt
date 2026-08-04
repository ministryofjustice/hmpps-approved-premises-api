package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderRisksService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService

@Cas3Controller
class Cas3PeopleController(
  private val offenderRiskService: OffenderRisksService,
  private val caseService: CaseService,
  private val sentryService: SentryService,
) {

  @Operation(
    summary = "Returns a risk profile for a Person.",
    description = "Returns a 404 if a case for the CRN has not been previously registered in CAS.",
  )
  @GetMapping("/people/{crn}/risk-profile")
  fun getPersonRiskProfile(@PathVariable crn: String): ResponseEntity<PersonRisks> {
    val case = caseService.getCase(crn)
      ?: throw NotFoundProblem(crn, "Case").also { sentryService.captureException(it) }
    val personRisks = offenderRiskService.getPersonRisks(case)
    return ResponseEntity.ok(personRisks)
  }
}
