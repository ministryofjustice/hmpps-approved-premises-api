package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspaceCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspaceRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3v2VoidBedspaceService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BedspacesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3VoidBedspacesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas3Controller
class Cas3v2VoidBedspaceController(
  private val voidBedspaceService: Cas3v2VoidBedspaceService,
  private val cas3PremisesService: Cas3v2PremisesService,
  private val cas3UserAccessService: Cas3UserAccessService,
  private val cas3VoidBedspacesTransformer: Cas3VoidBedspacesTransformer,
  private val cas3BedspaceService: Cas3v2BedspacesService,
) {

  private fun validateUserAccess(premises: Cas3PremisesEntity) {
    if (!cas3UserAccessService.canViewVoidBedspaces(premises.probationDeliveryUnit.probationRegion.id)) {
      throw ForbiddenProblem()
    }
  }

  @GetMapping("/v2/premises/{premisesId}/bedspaces/void-bedspaces")
  fun getVoidBedspaces(@PathVariable premisesId: UUID): ResponseEntity<List<Cas3VoidBedspace>> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")
    validateUserAccess(premises)

    val voidBedspaces = voidBedspaceService.findVoidBedspaces(premises.id)
      .map(cas3VoidBedspacesTransformer::toCas3VoidBedspace)

    return ResponseEntity.ok(voidBedspaces)
  }

  @GetMapping("/v2/premises/{premisesId}/bedspaces/{bedspaceId}/void-bedspaces/{voidBedspaceId}")
  fun getVoidBedspace(
    @PathVariable premisesId: UUID,
    @PathVariable bedspaceId: UUID,
    @PathVariable voidBedspaceId: UUID,
  ): ResponseEntity<Cas3VoidBedspace> {
    val voidBedspace = voidBedspaceService.findVoidBedspace(premisesId, bedspaceId, voidBedspaceId) ?: throw NotFoundProblem(
      voidBedspaceId,
      "Cas3VoidBedspace",
    )

    validateUserAccess(voidBedspace.bedspace!!.premises)

    return ResponseEntity.ok(cas3VoidBedspacesTransformer.toCas3VoidBedspace(voidBedspace))
  }

  @PostMapping("/v2/premises/{premisesId}/bedspaces/{bedspaceId}/void-bedspaces")
  fun createVoidBedspace(
    @PathVariable premisesId: UUID,
    @PathVariable bedspaceId: UUID,
    @RequestBody body: Cas3VoidBedspaceRequest,
  ): ResponseEntity<Cas3VoidBedspace> {
    val bedspace = cas3BedspaceService.findBedspace(premisesId, bedspaceId) ?: throw NotFoundProblem(
      bedspaceId,
      "Cas3Bedspace",
    )

    if (!cas3UserAccessService.canViewVoidBedspaces(bedspace.premises.probationDeliveryUnit.probationRegion.id)) {
      throw ForbiddenProblem()
    }

    val voidBedspace = voidBedspaceService.createVoidBedspace(
      voidBedspaceStartDate = body.startDate,
      voidBedspaceEndDate = body.endDate,
      reasonId = body.reasonId,
      referenceNumber = body.referenceNumber,
      notes = body.notes,
      bedspace = bedspace,
      costCentre = body.costCentre,
    )

    val result = extractEntityFromCasResult(voidBedspace)
    return ResponseEntity.status(HttpStatus.CREATED).body(cas3VoidBedspacesTransformer.toCas3VoidBedspace(result))
  }

  @PutMapping("/v2/premises/{premisesId}/bedspaces/{bedspaceId}/void-bedspaces/{voidBedspaceId}")
  fun updateVoidBedspace(
    @PathVariable premisesId: UUID,
    @PathVariable bedspaceId: UUID,
    @PathVariable voidBedspaceId: UUID,
    @RequestBody body: Cas3VoidBedspaceRequest,
  ): ResponseEntity<Cas3VoidBedspace> {
    val voidBedspaceEntity =
      voidBedspaceService.findVoidBedspace(premisesId, bedspaceId, voidBedspaceId) ?: throw NotFoundProblem(
        voidBedspaceId,
        "Cas3VoidBedspace",
      )

    if (!cas3UserAccessService.canViewVoidBedspaces(voidBedspaceEntity.bedspace!!.premises.probationDeliveryUnit.probationRegion.id)) {
      throw ForbiddenProblem()
    }

    val updateVoidBedspaceResult = voidBedspaceService.updateVoidBedspace(
      voidBedspaceEntity,
      body.startDate,
      body.endDate,
      body.reasonId,
      body.referenceNumber,
      body.notes,
    )

    return ResponseEntity.ok(
      cas3VoidBedspacesTransformer.toCas3VoidBedspace(
        extractEntityFromCasResult(
          updateVoidBedspaceResult,
        ),
      ),
    )
  }

  @PutMapping("/v2/premises/{premisesId}/bedspaces/{bedspaceId}/void-bedspaces/{voidBedspaceId}/cancellations")
  fun cancelVoidBedspace(
    @PathVariable premisesId: UUID,
    @PathVariable bedspaceId: UUID,
    @PathVariable voidBedspaceId: UUID,
    @RequestBody cas3VoidBedspaceCancellation: Cas3VoidBedspaceCancellation,
  ): ResponseEntity<Cas3VoidBedspace> {
    val voidBedspace = voidBedspaceService.findVoidBedspace(premisesId, bedspaceId, voidBedspaceId) ?: throw NotFoundProblem(
      voidBedspaceId,
      "Cas3VoidBedspace",
    )

    if (!cas3UserAccessService.canViewVoidBedspaces(voidBedspace.bedspace!!.premises.probationDeliveryUnit.probationRegion.id)) {
      throw ForbiddenProblem()
    }

    val cancelledVoidBedspace = voidBedspaceService.cancelVoidBedspace(voidBedspace, cas3VoidBedspaceCancellation.cancellationNotes)
    val result = extractEntityFromCasResult(cancelledVoidBedspace)
    return ResponseEntity.ok(cas3VoidBedspacesTransformer.toCas3VoidBedspace(result))
  }
}
