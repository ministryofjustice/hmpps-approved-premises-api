package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementDtoV2
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementWithdrawalDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingShortSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate

@Component
class Cas1ExternalRequestForPlacementOutcomeV2Transformer {

  fun transformToRequestForPlacement(rfp: RequestForPlacement) = Cas1ExternalRequestForPlacementDtoV2(
    status = rfp.status,
    statusLastUpdated = rfp.statusSetDateTime,
    submittedBy = rfp.submittedBy,
    submittedAt = rfp.submittedAt?.toLocalDate(),
    expectedArrivalDate = rfp.canonicalPlacementPeriod.arrival,
    durationDays = rfp.canonicalPlacementPeriod.duration,
    decision = rfp.decision?.apiValue,
    rejectionReason = rfp.rejectionReason,
    withdrawal = if (rfp.isWithdrawn) {
      Cas1ExternalRequestForPlacementWithdrawalDto(
        withdrawalReason = rfp.withdrawalReason,
        withdrawalDate = rfp.withdrawalDate?.toLocalDate(),
      )
    } else {
      null
    },
  )

  fun transformToPlacement(
    placement: Cas1SpaceBookingShortSummary,
    premises: ApprovedPremisesEntity,
  ) = Cas1ExternalPlacementV2Dto(
    status = placement.status,
    canonicalArrivalDate = placement.actualArrivalDate ?: placement.expectedArrivalDate,
    canonicalDepartureDate = placement.actualDepartureDate ?: placement.expectedDepartureDate,
    actualArrivalDate = placement.actualArrivalDate,
    actualDepartureDate = placement.actualDepartureDate,
    expectedArrivalDate = placement.expectedArrivalDate,
    expectedDepartureDate = placement.expectedDepartureDate,
    cancellationReason = getCancellationReason(placement),
    premises = Cas1ExternalPremisesV2Dto(
      addressLine1 = premises.addressLine1,
      addressLine2 = premises.addressLine2,
      town = premises.town,
      postcode = premises.postcode,
    ),
  )

  private fun getCancellationReason(placement: Cas1SpaceBookingShortSummary) = if (placement.cancellation?.reason?.name == CancellationReasonRepository.CAS1_OTHER_NAME) {
    placement.cancellation.reasonNotes
  } else {
    placement.cancellation?.reason?.name
  }
}
