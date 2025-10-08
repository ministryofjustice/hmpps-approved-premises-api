package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ArchiveBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BedspaceArchiveService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BedspacesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BedspaceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas3Controller
@RequestMapping("/cas3/v2", headers = ["X-Service-Name=temporary-accommodation"])
class Cas3v2BedspaceArchiveController(
  private val cas3v2PremisesService: Cas3v2PremisesService,
  private val cas3v2BedspacesService: Cas3v2BedspacesService,
  private val cas3v2BedspaceArchiveService: Cas3v2BedspaceArchiveService,
  private val cas3BedspaceTransformer: Cas3BedspaceTransformer,
) {

  @Transactional
  @PostMapping("/premises/{premisesId}/bedspaces/{bedspaceId}/archive")
  fun archiveBedspace(
    @PathVariable premisesId: UUID,
    @PathVariable bedspaceId: UUID,
    @RequestBody body: Cas3ArchiveBedspace,
  ): ResponseEntity<Cas3Bedspace> {
    val premises = extractEntityFromCasResult(cas3v2PremisesService.getValidatedPremises(premisesId))
    val archivedBedspace = extractEntityFromCasResult(
      cas3v2BedspaceArchiveService.archiveBedspace(bedspaceId, premises, body.endDate),
    )
    val bedspaceStatus = cas3v2BedspacesService.getBedspaceStatus(archivedBedspace)
    return ResponseEntity.ok(cas3BedspaceTransformer.transformJpaToApi(archivedBedspace, bedspaceStatus))
  }
}
