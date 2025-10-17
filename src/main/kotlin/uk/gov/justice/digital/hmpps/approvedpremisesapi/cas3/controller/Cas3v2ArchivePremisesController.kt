package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ArchivePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2ArchiveService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas3Controller
@RequestMapping("/cas3/v2", headers = ["X-Service-Name=temporary-accommodation"])
class Cas3v2ArchivePremisesController(
  private val cas3v2PremisesService: Cas3v2PremisesService,
  private val cas3PremisesTransformer: Cas3PremisesTransformer,
  private val archiveService: Cas3v2ArchiveService,
) {

  @Transactional
  @PostMapping("/premises/{premisesId}/archive")
  fun archivePremises(
    @PathVariable premisesId: UUID,
    @RequestBody body: Cas3ArchivePremises,
  ): ResponseEntity<Cas3Premises> {
    val premises = extractEntityFromCasResult(cas3v2PremisesService.getValidatedPremises(premisesId))

    val archivedPremises = extractEntityFromCasResult(
      archiveService.archivePremises(premises, body.endDate),
    )

    return ResponseEntity.ok(cas3PremisesTransformer.toCas3Premises(archivedPremises))
  }
}
