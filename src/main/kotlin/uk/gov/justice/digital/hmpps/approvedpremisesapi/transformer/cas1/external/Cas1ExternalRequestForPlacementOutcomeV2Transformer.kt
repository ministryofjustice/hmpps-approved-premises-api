package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementDtoV2
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementWithdrawalDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingShortSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.toApiType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate

@Component
class Cas1ExternalRequestForPlacementOutcomeV2Transformer {

  fun transformToRequestForPlacement(rfp: RequestForPlacement) = Cas1ExternalRequestForPlacementDtoV2(
    status = rfp.status,
    statusLastUpdated = rfp.statusSetDateTime,
    submittedBy = rfp.submittedBy,
    submittedAt = rfp.submittedAt.toLocalDate(),
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
    spaceBooking: Cas1SpaceBookingEntity,
  ): Cas1ExternalPlacementV2Dto {
    val statusAndWhen = spaceBooking.getSpaceBookingStatus()
    val premises = spaceBooking.premises
    return Cas1ExternalPlacementV2Dto(
      status = statusAndWhen.status.toApiType(),
      statusLastUpdated = statusAndWhen.occurredAt,
      canonicalArrivalDate = spaceBooking.canonicalArrivalDate,
      canonicalDepartureDate = spaceBooking.canonicalDepartureDate,
      actualArrivalDate = spaceBooking.actualArrivalDate,
      actualDepartureDate = spaceBooking.actualDepartureDate,
      expectedArrivalDate = spaceBooking.expectedArrivalDate,
      expectedDepartureDate = spaceBooking.expectedDepartureDate,
      cancellationReason = getCancellationReason(
        reasonName = spaceBooking.cancellationReason?.name,
        reasonNotes = spaceBooking.cancellationReasonNotes,
      ),
      premises = Cas1ExternalPremisesV2Dto(
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
      ),
    )
  }

  fun transformToPlacement(
    placement: Cas1SpaceBookingShortSummary,
    premises: ApprovedPremisesEntity,
  ) = Cas1ExternalPlacementV2Dto(
    status = placement.status,
    statusLastUpdated = placement.statusSetDateTime,
    canonicalArrivalDate = placement.actualArrivalDate ?: placement.expectedArrivalDate,
    canonicalDepartureDate = placement.actualDepartureDate ?: placement.expectedDepartureDate,
    actualArrivalDate = placement.actualArrivalDate,
    actualDepartureDate = placement.actualDepartureDate,
    expectedArrivalDate = placement.expectedArrivalDate,
    expectedDepartureDate = placement.expectedDepartureDate,
    cancellationReason = getCancellationReason(
      reasonName = placement.cancellation?.reason?.name,
      reasonNotes = placement.cancellation?.reasonNotes,
    ),
    premises = Cas1ExternalPremisesV2Dto(
      addressLine1 = premises.addressLine1,
      addressLine2 = premises.addressLine2,
      town = premises.town,
      postcode = premises.postcode,
    ),
  )

  private fun getCancellationReason(reasonName: String?, reasonNotes: String?) = if (reasonName == CancellationReasonRepository.CAS1_OTHER_NAME) {
    reasonNotes
  } else {
    reasonName
  }
}
