package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesBedspaceTotals
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Bedspaces
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UpdateBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3BedspacesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BedspaceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas3Controller
@RequestMapping("/cas3/v2", headers = ["X-Service-Name=temporary-accommodation"])
class Cas3v2BedspaceController(
  private val cas3PremisesService: Cas3PremisesService,
  private val cas3BedspacesService: Cas3BedspacesService,
  private val cas3BedspaceTransformer: Cas3BedspaceTransformer,
) {

  @PostMapping("/premises/{premisesId}/bedspaces")
  fun createBedspace(
    @PathVariable premisesId: UUID,
    @RequestBody newBedspace: Cas3NewBedspace,
  ): ResponseEntity<Cas3Bedspace> {
    val premises = extractEntityFromCasResult(cas3PremisesService.getValidatedPremises(premisesId))
    val bedspace = extractEntityFromCasResult(cas3BedspacesService.createBedspace(premises, newBedspace.reference, newBedspace.startDate, newBedspace.notes, newBedspace.characteristicIds))
    val bedspaceStatus = cas3BedspacesService.getBedspaceStatus(bedspace)
    return ResponseEntity.status(HttpStatus.CREATED).body(
      cas3BedspaceTransformer.transformJpaToApi(
        jpa = bedspace,
        status = bedspaceStatus,
      ),
    )
  }

  @Suppress("ThrowsCount")
  @GetMapping("/premises/{premisesId}/bedspaces/{bedspaceId}")
  fun getBedspace(
    @PathVariable premisesId: UUID,
    @PathVariable bedspaceId: UUID,
  ): ResponseEntity<Cas3Bedspace> {
    val premises = extractEntityFromCasResult(cas3PremisesService.getValidatedPremises(premisesId))
    val bedspace = extractEntityFromCasResult(cas3BedspacesService.getBedspace(premises.id, bedspaceId))
    val archiveHistory = extractEntityFromCasResult(cas3BedspacesService.getBedspaceArchiveHistory(bedspaceId))
    val bedspaceStatus = cas3BedspacesService.getBedspaceStatus(bedspace)
    return ResponseEntity.ok(cas3BedspaceTransformer.transformJpaToApi(bedspace, bedspaceStatus, archiveHistory))
  }

  @GetMapping("/premises/{premisesId}/bedspaces")
  fun getBedspaces(@PathVariable premisesId: UUID): ResponseEntity<Cas3Bedspaces> {
    val premises = extractEntityFromCasResult(cas3PremisesService.getValidatedPremises(premisesId))
    val bedspaces = premises.bedspaces
    val bedspacesArchiveHistory = cas3BedspacesService.getBedspacesArchiveHistory(bedspaces.map { it.id })
    val result = Cas3Bedspaces(
      bedspaces = bedspaces.map { bedspace ->
        val bedspaceStatus = cas3BedspacesService.getBedspaceStatus(bedspace)
        val archiveHistory = bedspacesArchiveHistory
          .firstOrNull { bedspaceArchiveHistory -> bedspaceArchiveHistory.bedspaceId == bedspace.id }
          ?.actions ?: emptyList()
        cas3BedspaceTransformer.transformJpaToApi(bedspace, bedspaceStatus, archiveHistory)
      },
      totalOnlineBedspaces = premises.countOnlineBedspaces(),
      totalUpcomingBedspaces = premises.countUpcomingBedspaces(),
      totalArchivedBedspaces = premises.countArchivedBedspaces(),
    )
    return ResponseEntity.ok(result)
  }

  @GetMapping("/premises/{premisesId}/bedspace-totals")
  fun getBedspaceTotals(@PathVariable premisesId: UUID): ResponseEntity<Cas3PremisesBedspaceTotals> {
    val premises = extractEntityFromCasResult(cas3PremisesService.getValidatedPremises(premisesId))

    val result = Cas3PremisesBedspaceTotals(
      id = premises.id,
      status = if (premises.isPremisesArchived()) Cas3PremisesStatus.archived else Cas3PremisesStatus.online,
      premisesEndDate = premises.endDate,
      totalOnlineBedspaces = premises.countOnlineBedspaces(),
      totalUpcomingBedspaces = premises.countUpcomingBedspaces(),
      totalArchivedBedspaces = premises.countArchivedBedspaces(),
    )

    return ResponseEntity.ok(result)
  }

  @PutMapping("/premises/{premisesId}/bedspaces/{bedspaceId}")
  fun updateBedspace(
    @PathVariable premisesId: UUID,
    @PathVariable bedspaceId: UUID,
    @RequestBody updateBedspace: Cas3UpdateBedspace,
  ): ResponseEntity<Cas3Bedspace> {
    val premises = extractEntityFromCasResult(cas3PremisesService.getValidatedPremises(premisesId))
    val updatedBedspace = extractEntityFromCasResult(
      cas3BedspacesService.updateBedspace(premises, bedspaceId, updateBedspace.reference, updateBedspace.notes, updateBedspace.characteristicIds),
    )
    val bedspaceStatus = cas3BedspacesService.getBedspaceStatus(updatedBedspace)
    return ResponseEntity.ok(cas3BedspaceTransformer.transformJpaToApi(updatedBedspace, bedspaceStatus))
  }
}
