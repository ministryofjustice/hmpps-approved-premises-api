package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCreatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.TransferInfo
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Calls to this service are orchestrated via the [Cas1SpaceBookingService]
 */
@Service
class Cas1SpaceBookingCreateService(
  private val placementRequestService: PlacementRequestService,
  private val cas1PremisesService: Cas1PremisesService,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1ApplicationStatusService: Cas1ApplicationStatusService,
  private val cas1BookingDomainEventService: Cas1BookingDomainEventService,
  private val cas1BookingEmailService: Cas1BookingEmailService,
  private val clock: Clock,
) {

  fun create(validatedDetails: ValidatedCreateBooking): Cas1SpaceBookingEntity {
    val bookingToCreate = validatedDetails.bookingToCreate
    val createdBy = bookingToCreate.createdBy!!
    val application = bookingToCreate.placementRequest!!.application

    val spaceBooking = cas1SpaceBookingRepository.save(validatedDetails.bookingToCreate)

    cas1ApplicationStatusService.spaceBookingMade(spaceBooking)

    cas1BookingDomainEventService.spaceBookingMade(Cas1BookingCreatedEvent(spaceBooking, createdBy))

    cas1BookingEmailService.spaceBookingMade(spaceBooking, application)

    cas1BookingDomainEventService.spaceBookingMade(
      Cas1BookingCreatedEvent(
        booking = spaceBooking,
        createdBy = createdBy,
        transferredFrom = validatedDetails.transferredFrom,
      ),
    )

    return spaceBooking
  }

  fun validate(
    details: CreateBookingDetails,
  ): CasResult<ValidatedCreateBooking> = validatedCasResult {
    val premises = cas1PremisesService.findPremiseById(details.premisesId)
    if (premises == null) {
      "$.premisesId" hasValidationError "doesNotExist"
      return errors()
    }

    val placementRequestId = details.placementRequestId
    val placementRequest = placementRequestService.getPlacementRequestOrNull(placementRequestId)
    if (placementRequest == null) {
      "$.placementRequestId" hasValidationError "doesNotExist"
    }

    if (!premises.supportsSpaceBookings) {
      "$.premisesId" hasValidationError "doesNotSupportSpaceBookings"
    }

    if (details.expectedArrivalDate >= details.expectedDepartureDate) {
      "$.departureDate" hasValidationError "shouldBeAfterArrivalDate"
    }

    if (hasErrors()) {
      return errors()
    } else {
      return success(
        ValidatedCreateBooking(
          bookingToCreate = toSpaceBooking(details),
          transferredFrom = details.transferredFrom,
        )
      )
    }
  }

  private fun toSpaceBooking(details: CreateBookingDetails): Cas1SpaceBookingEntity {
    val placementRequest = placementRequestService.getPlacementRequestOrNull(details.placementRequestId)!!
    val premises = cas1PremisesService.findPremiseById(details.premisesId)!!
    val createdBy = details.createdBy

    val application = placementRequest.application
    return Cas1SpaceBookingEntity(
      id = UUID.randomUUID(),
      premises = premises,
      application = application,
      offlineApplication = null,
      placementRequest = placementRequest,
      createdBy = createdBy,
      createdAt = OffsetDateTime.now(clock),
      expectedArrivalDate = details.expectedArrivalDate,
      expectedDepartureDate = details.expectedDepartureDate,
      actualArrivalDate = null,
      actualArrivalTime = null,
      actualDepartureDate = null,
      actualDepartureTime = null,
      canonicalArrivalDate = details.expectedArrivalDate,
      canonicalDepartureDate = details.expectedDepartureDate,
      crn = placementRequest.application.crn,
      keyWorkerStaffCode = null,
      keyWorkerName = null,
      keyWorkerAssignedAt = null,
      cancellationOccurredAt = null,
      cancellationRecordedAt = null,
      cancellationReason = null,
      cancellationReasonNotes = null,
      departureMoveOnCategory = null,
      departureReason = null,
      departureNotes = null,
      criteria = details.characteristics.toMutableList(),
      nonArrivalConfirmedAt = null,
      nonArrivalNotes = null,
      nonArrivalReason = null,
      deliusEventNumber = application.eventNumber,
      migratedManagementInfoFrom = null,
      transferredFrom = details.transferredFrom?.booking,
      transferredTo = null,
      transferType = details.transferredFrom?.type,
      deliusId = null,
    )
  }

  data class ValidatedCreateBooking(
    val bookingToCreate: Cas1SpaceBookingEntity,
    val transferredFrom: TransferInfo?
  )

  data class CreateBookingDetails(
    val premisesId: UUID,
    val placementRequestId: UUID,
    val expectedArrivalDate: LocalDate,
    val expectedDepartureDate: LocalDate,
    val createdBy: UserEntity,
    val characteristics: List<CharacteristicEntity>,
    val transferredFrom: TransferInfo?,
  )
}
