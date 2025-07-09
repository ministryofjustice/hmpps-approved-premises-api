package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2OffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
@RestController
@RequestMapping(
  "\${openapi.communityAccommodationServicesTier2CAS2Version2.base-path:/cas2v2}",
  produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE],
)
class Cas2v2PeopleController(
  private val cas2v2OffenderService: Cas2v2OffenderService,
) {

  @Suppress("ThrowsCount")
  @GetMapping("/people/search-by-crn/{crn}")
  fun searchByCrnGet(
    @PathVariable crn: String,
  ): ResponseEntity<Person> {
    when (val cas2v2OffenderSearchResult = cas2v2OffenderService.getPersonByNomisIdOrCrn(crn)) {
      is Cas2v2OffenderSearchResult.NotFound -> throw NotFoundProblem(crn, "Offender", "a CRN")
      is Cas2v2OffenderSearchResult.Unknown -> throw cas2v2OffenderSearchResult.throwable ?: BadRequestProblem(errorDetail = "Could not retrieve person info for CRN: $crn")
      is Cas2v2OffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is Cas2v2OffenderSearchResult.Success.Full -> return ResponseEntity.ok(cas2v2OffenderSearchResult.person)
    }
  }

  @Suppress("ThrowsCount")
  @GetMapping("/people/search-by-noms/{nomsNumber}")
  fun searchByNomisIdGet(
    @PathVariable nomsNumber: String,
  ): ResponseEntity<Person> {
    when (val cas2v2OffenderSearchResult = cas2v2OffenderService.getPersonByNomisIdOrCrn(nomsNumber)) {
      is Cas2v2OffenderSearchResult.NotFound -> throw NotFoundProblem(nomsNumber, "Offender", "a nomsNumber (prison number)")
      is Cas2v2OffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is Cas2v2OffenderSearchResult.Unknown -> throw cas2v2OffenderSearchResult.throwable ?: BadRequestProblem(errorDetail = "Could not retrieve person info for Prison Number: $nomsNumber")
      is Cas2v2OffenderSearchResult.Success.Full -> return ResponseEntity.ok(cas2v2OffenderSearchResult.person)
    }
  }
}
