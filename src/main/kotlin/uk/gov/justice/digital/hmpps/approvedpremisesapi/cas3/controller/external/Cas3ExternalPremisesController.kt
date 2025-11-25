package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas3ExternalController
class Cas3ExternalPremisesController(
  private val cas3PremisesService: Cas3PremisesService,
  private val cas3PremisesTransformer: Cas3PremisesTransformer,
) {

  @PreAuthorize("hasRole('ACCOMMODATION_API__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/premises/{premisesId}")
  fun getPremisesById(@PathVariable premisesId: UUID): ResponseEntity<Cas3Premises> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    val archiveHistory = extractEntityFromCasResult(cas3PremisesService.getPremisesArchiveHistory(premises.id))

    return ResponseEntity.ok(cas3PremisesTransformer.transformDomainToApi(premises, archiveHistory))
  }
}
