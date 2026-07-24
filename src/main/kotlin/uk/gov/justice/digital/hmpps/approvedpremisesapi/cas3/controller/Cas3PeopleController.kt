package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderRisksService

@Cas3Controller
class Cas3PeopleController(
  private val offenderRiskService: OffenderRisksService,
) {

  @Operation(summary = "Returns a risk profile for a Person.")
  @GetMapping("/people/{crn}/risk-profile")
  fun getPersonRiskProfile(@PathVariable crn: String): ResponseEntity<PersonRisks> {
    val personRisks = offenderRiskService.getPersonRisks(crn)
    return ResponseEntity.ok(personRisks)
  }
}
