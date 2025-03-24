package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2v2

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2.PeopleCas2v2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2OffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2OffenderService

@Service("Cas2v2PeopleController")
class Cas2v2PeopleController(
  private val cas2v2OffenderService: Cas2v2OffenderService,
) : PeopleCas2v2Delegate {

  override fun searchByCrnGet(crn: String): ResponseEntity<Person> {
    when (val cas2v2OffenderSearchResult = cas2v2OffenderService.getPersonByCrn(crn)) {
      is Cas2v2OffenderSearchResult.NotFound -> throw NotFoundProblem(crn, "Offender")
      is Cas2v2OffenderSearchResult.Unknown -> throw cas2v2OffenderSearchResult.throwable ?: BadRequestProblem(errorDetail = "Could not retrieve person info for CRN: $crn")
      is Cas2v2OffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is Cas2v2OffenderSearchResult.Success.Full -> return ResponseEntity.ok(cas2v2OffenderSearchResult.person)
    }
  }

  override fun searchByNomisIdGet(nomsNumber: String): ResponseEntity<Person> {
    when (val cas2v2OffenderSearchResult = cas2v2OffenderService.getPersonByNomsNumber(nomsNumber)) {
      is Cas2v2OffenderSearchResult.NotFound -> throw NotFoundProblem(nomsNumber, "Offender")
      is Cas2v2OffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is Cas2v2OffenderSearchResult.Unknown -> throw cas2v2OffenderSearchResult.throwable ?: BadRequestProblem(errorDetail = "Could not retrieve person info for Prison Number: $nomsNumber")
      is Cas2v2OffenderSearchResult.Success.Full -> return ResponseEntity.ok(cas2v2OffenderSearchResult.person)
    }
  }
}
