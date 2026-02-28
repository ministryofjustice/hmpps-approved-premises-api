package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem

@Cas2v2Controller
class Cas2v2PeopleController(
  private val cas2OffenderService: Cas2OffenderService,
) {

  @Suppress("ThrowsCount")
  @GetMapping("/people/search-by-crn/{crn}")
  fun searchByCrnGet(
    @PathVariable crn: String,
  ): ResponseEntity<Person> {
    when (val cas2v2OffenderSearchResult = cas2OffenderService.getPersonByNomisIdOrCrn(crn)) {
      is Cas2OffenderSearchResult.NotFound -> throw NotFoundProblem(crn, "Offender", "a CRN")
      is Cas2OffenderSearchResult.Unknown -> throw cas2v2OffenderSearchResult.throwable ?: BadRequestProblem(errorDetail = "Could not retrieve person info for CRN: $crn")
      is Cas2OffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is Cas2OffenderSearchResult.Success.Full -> return ResponseEntity.ok(cas2v2OffenderSearchResult.person)
    }
  }

  @Suppress("ThrowsCount")
  @GetMapping("/people/search-by-noms/{nomsNumber}")
  fun searchByNomisIdGet(
    @PathVariable nomsNumber: String,
  ): ResponseEntity<Person> {
    when (val cas2v2OffenderSearchResult = cas2OffenderService.getPersonByNomisIdOrCrn(nomsNumber)) {
      is Cas2OffenderSearchResult.NotFound -> throw NotFoundProblem(nomsNumber, "Offender", "a nomsNumber (prison number)")
      is Cas2OffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is Cas2OffenderSearchResult.Unknown -> throw cas2v2OffenderSearchResult.throwable ?: BadRequestProblem(errorDetail = "Could not retrieve person info for Prison Number: $nomsNumber")
      is Cas2OffenderSearchResult.Success.Full -> return ResponseEntity.ok(cas2v2OffenderSearchResult.person)
    }
  }
}
