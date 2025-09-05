package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BedspacesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BedspaceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas3Controller
@RequestMapping("/cas3/v2", headers = ["X-Service-Name=temporary-accommodation"])
class Cas3v2BedspaceController(
  private val cas3v2PremisesService: Cas3v2PremisesService,
  private val cas3v2BedspacesService: Cas3v2BedspacesService,
  private val userAccessService: UserAccessService,
  private val cas3BedspaceTransformer: Cas3BedspaceTransformer,
) {

  @PostMapping("/premises/{premisesId}/bedspaces")
  fun createBedspace(
    @PathVariable premisesId: UUID,
    @RequestBody newBedspace: Cas3NewBedspace,
  ): ResponseEntity<Cas3Bedspace> {
    val premises = cas3v2PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")
    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }
    val bedspace = cas3v2BedspacesService.createBedspace(premises, newBedspace.reference, newBedspace.startDate, newBedspace.notes, newBedspace.characteristicIds)
    return ResponseEntity.status(HttpStatus.CREATED).body(
      cas3BedspaceTransformer.transformJpaToApi(
        jpa = extractEntityFromCasResult(bedspace),
        status = Cas3BedspaceStatus.online,
      ),
    )
  }
}
