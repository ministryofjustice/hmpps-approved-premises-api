package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestForPlacementOutcomeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external.Cas1ExternalApplicationTransformer
import java.util.UUID

@Service
class Cas1RequestForPlacementOutcomeService(
  private val cas1RequestForPlacementService: Cas1RequestForPlacementService,
  private val cas1PremisesService: Cas1PremisesService,
  private val transformer: Cas1ExternalApplicationTransformer,
) {

  fun getPlacementPairs(applicationId: UUID): List<Cas1RequestForPlacementOutcomeDto> {
    val rfps = (cas1RequestForPlacementService.getRequestsForPlacementByApplication(applicationId, requestingUser = null) as CasResult.Success).value

    return rfps.flatMap { rfp ->
      val withdrawalDate = if (rfp.isWithdrawn) rfp.withdrawalDate?.toLocalDate() else null

      if (rfp.placements.isEmpty()) {
        val rejectionReason = rfp.rejectionReason

        listOf(
          transformer.transformToCas1PlacementPair(
            rfp = rfp,
            rejectionReason = rejectionReason,
            withdrawalDate = withdrawalDate,
          ),
        )
      } else {
        rfp.placements.map { placement ->
          val premisesEntity = cas1PremisesService.findPremisesById(placement.premises.id)

          transformer.transformToCas1PlacementPair(
            rfp = rfp,
            rejectionReason = null,
            withdrawalDate = withdrawalDate,
            placement = placement,
            premises = premisesEntity,
          )
        }
      }
    }
  }
}
