package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository.FindOpenChangeRequestResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1ChangeRequestService(
  private val cas1ChangeRequestRepository: Cas1ChangeRequestRepository,
  private val placementRequestRepository: PlacementRequestRepository,
  private val cas1ChangeRequestReasonRepository: Cas1ChangeRequestReasonRepository,
  private val objectMapper: ObjectMapper,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
) {
  @Transactional
  fun createChangeRequest(placementRequestId: UUID, cas1NewChangeRequest: Cas1NewChangeRequest): CasResult<Unit> = validatedCasResult {
    val placementRequest = placementRequestRepository.findByIdOrNull(placementRequestId)
      ?: return CasResult.NotFound("Placement Request", placementRequestId.toString())

    val requestReason = cas1ChangeRequestReasonRepository.findByIdOrNull(cas1NewChangeRequest.reasonId)
      ?: return CasResult.NotFound("Change Request Reason", cas1NewChangeRequest.reasonId.toString())

    val spaceBooking = cas1SpaceBookingRepository.findByIdOrNull(cas1NewChangeRequest.spaceBookingId)
      ?: return CasResult.NotFound("Space Booking", cas1NewChangeRequest.spaceBookingId.toString())

    if (!placementRequest.spaceBookings.contains(spaceBooking)) return CasResult.NotFound("Placement Request with Space Booking", spaceBooking.id.toString())

    if (cas1NewChangeRequest.type == Cas1ChangeRequestType.PLANNED_TRANSFER) {
      spaceBooking.actualArrivalDate ?: return CasResult.GeneralValidationError("Associated space booking has not been marked as arrived")
      if (spaceBooking.nonArrivalConfirmedAt != null) return CasResult.GeneralValidationError("Associated space booking has been marked as non arrived")
      if (spaceBooking.actualDepartureDate != null) return CasResult.GeneralValidationError("Associated space booking has been marked as departed")
      if (spaceBooking.cancellationOccurredAt != null) return CasResult.GeneralValidationError("Associated space booking has been cancelled")
    }

    if (cas1NewChangeRequest.type == Cas1ChangeRequestType.APPEAL) {
      if (spaceBooking.actualArrivalDate != null) return CasResult.GeneralValidationError("Associated space booking has been marked as arrived")
      if (spaceBooking.nonArrivalConfirmedAt != null) return CasResult.GeneralValidationError("Associated space booking has been marked as non arrived")
      if (spaceBooking.cancellationOccurredAt != null) return CasResult.GeneralValidationError("Associated space booking has been cancelled")
    }

    val now = OffsetDateTime.now()

    cas1ChangeRequestRepository.save(
      Cas1ChangeRequestEntity(
        id = UUID.randomUUID(),
        placementRequest = placementRequest,
        spaceBooking = spaceBooking,
        type = ChangeRequestType.valueOf(cas1NewChangeRequest.type.name),
        requestJson = objectMapper.writeValueAsString(cas1NewChangeRequest.requestJson),
        requestReason = requestReason,
        decisionJson = null,
        decision = null,
        rejectionReason = null,
        decisionMadeByUser = null,
        decisionMadeAt = null,
        createdAt = now,
        updatedAt = now,
      ),
    )

    return success(Unit)
  }

  fun findOpen(
    cruManagementAreaId: UUID?,
    pageCriteria: PageCriteria<Cas1ChangeRequestSortField>,
  ): List<FindOpenChangeRequestResult> = cas1ChangeRequestRepository.findOpen(
    cruManagementAreaId,
    pageCriteria.toPageableOrAllPages(
      sortBy = when (pageCriteria.sortBy) {
        Cas1ChangeRequestSortField.NAME -> "name"
        Cas1ChangeRequestSortField.TIER -> "tier"
        Cas1ChangeRequestSortField.CANONICAL_ARRIVAL_DATE -> "canonicalArrivalDate"
        Cas1ChangeRequestSortField.LENGTH_OF_STAY_DAYS -> "lengthOfStayDays"
      },
    ),
  )
}
