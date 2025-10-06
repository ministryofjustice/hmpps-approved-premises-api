package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesBedspaceTotals
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Bedspaces
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UpdateBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BedspacesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BedspaceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas3Controller
@RequestMapping("/cas3/v2", headers = ["X-Service-Name=temporary-accommodation"])
class Cas3v2BedspaceController(
  private val cas3v2PremisesService: Cas3v2PremisesService,
  private val cas3v2BedspacesService: Cas3v2BedspacesService,
  private val cas3UserAccessService: Cas3UserAccessService,
  private val cas3BedspaceTransformer: Cas3BedspaceTransformer,
) {

  @PostMapping("/premises/{premisesId}/bedspaces")
  fun createBedspace(
    @PathVariable premisesId: UUID,
    @RequestBody newBedspace: Cas3NewBedspace,
  ): ResponseEntity<Cas3Bedspace> {
    val premises = getAndCheckUserCanViewPremises(premisesId)
    val bedspace = extractEntityFromCasResult(cas3v2BedspacesService.createBedspace(premises, newBedspace.reference, newBedspace.startDate, newBedspace.notes, newBedspace.characteristicIds))
    val bedspaceStatus = cas3v2BedspacesService.getBedspaceStatus(bedspace)
    return ResponseEntity.status(HttpStatus.CREATED).body(
      cas3BedspaceTransformer.transformJpaToApi(
        jpa = bedspace,
        status = bedspaceStatus,
      ),
    )
  }

  @GetMapping("/premises/{premisesId}/bedspaces")
  fun getBedspaces(@PathVariable premisesId: UUID): ResponseEntity<Cas3Bedspaces> {
    val premises = getAndCheckUserCanViewPremises(premisesId)
    val bedspaces = premises.bedspaces
    val bedspacesArchiveHistory = cas3v2BedspacesService.getBedspacesArchiveHistory(bedspaces.map { it.id })
    val result = Cas3Bedspaces(
      bedspaces = bedspaces.map { bedspace ->
        val bedspaceStatus = cas3v2BedspacesService.getBedspaceStatus(bedspace)
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
    val premises = getAndCheckUserCanViewPremises(premisesId)

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
    val premises = getAndCheckUserCanViewPremises(premisesId)
    val updatedBedspace = extractEntityFromCasResult(
      cas3v2BedspacesService.updateBedspace(premises, bedspaceId, updateBedspace.reference, updateBedspace.notes, updateBedspace.characteristicIds),
    )
    val bedspaceStatus = cas3v2BedspacesService.getBedspaceStatus(updatedBedspace)
    return ResponseEntity.ok(cas3BedspaceTransformer.transformJpaToApi(updatedBedspace, bedspaceStatus))
  }

  private fun getAndCheckUserCanViewPremises(premisesId: UUID): Cas3PremisesEntity {
    val premises = cas3v2PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")
    if (!cas3UserAccessService.currentUserCanViewPremises(premises.probationDeliveryUnit.probationRegion.id)) {
      throw ForbiddenProblem()
    }
    return premises
  }
}
