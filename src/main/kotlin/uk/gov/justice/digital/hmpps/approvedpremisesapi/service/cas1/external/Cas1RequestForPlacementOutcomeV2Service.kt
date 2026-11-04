package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementDtoV2
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingManagementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external.Cas1ExternalRequestForPlacementOutcomeV2Transformer
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1RequestForPlacementOutcomeV2Service(
  private val cas1RequestForPlacementService: Cas1RequestForPlacementService,
  private val cas1PremisesService: Cas1PremisesService,
  private val transformer: Cas1ExternalRequestForPlacementOutcomeV2Transformer,
  private val spaceBookingManagementService: Cas1SpaceBookingManagementService,
  private val spaceBookingTransformer: Cas1SpaceBookingTransformer,
) {

  /**
   * One entry per request for placement, or per placement if a request has many
   */
  data class RequestForPlacementOutcome(
    val requestForPlacement: Cas1ExternalRequestForPlacementDtoV2,
    val placement: Cas1ExternalPlacementV2Dto?,
    val statusLastUpdated: LocalDate,
  )

  fun getOutcomes(applicationId: UUID): List<RequestForPlacementOutcome> {
    val rfps = (cas1RequestForPlacementService.getRequestsForPlacementByApplication(applicationId, requestingUser = null) as CasResult.Success).value

    return rfps.flatMap { rfp ->
      if (rfp.placements.isEmpty()) {
        listOf(
          RequestForPlacementOutcome(
            requestForPlacement = transformer.transformToRequestForPlacement(rfp),
            placement = null,
            statusLastUpdated = rfp.statusSetDate,
          ),
        )
      } else {
        rfp.placements.map { placement ->
          RequestForPlacementOutcome(
            requestForPlacement = transformer.transformToRequestForPlacement(rfp),
            placement = transformer.transformToPlacement(
              placement = placement,
              premises = checkNotNull(cas1PremisesService.findPremisesById(placement.premises.id)) { "Premises ${placement.premises.id} not found" },
            ),
            statusLastUpdated = placement.statusSetDate,
          )
        }
      }
    }
  }

  fun getCurrentResidentPlacement(crn: String) = spaceBookingManagementService.findCurrentResidency(crn)?.let { booking ->
    transformer.transformToPlacement(
      placement = spaceBookingTransformer.transformToCas1SpaceBookingShortSummary(booking),
      premises = booking.premises,
    )
  }
}
