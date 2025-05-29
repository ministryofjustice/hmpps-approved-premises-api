package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.successOrErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingCreateService.Companion.MAX_LENGTH_YEARS
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingChangedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.TransferInfo
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Calls to this service are orchestrated via the [Cas1SpaceBookingService]
 */
@Service
class Cas1SpaceBookingUpdateService(
  private val cas1PremisesService: Cas1PremisesService,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1BookingDomainEventService: Cas1BookingDomainEventService,
  private val cas1BookingEmailService: Cas1BookingEmailService,
  private val clock: Clock,
) {

  /**
   * Any calls to this should first call and validate the response of [validate]
   */
  fun update(details: UpdateBookingDetails): Cas1SpaceBookingEntity {
    val bookingToUpdate = cas1SpaceBookingRepository.findByIdOrNull(details.bookingId)!!

    val previousArrivalDate = bookingToUpdate.expectedArrivalDate
    val previousDepartureDate = bookingToUpdate.expectedDepartureDate
    val previousCharacteristics = bookingToUpdate.criteria.toList()

    if (bookingToUpdate.hasArrival()) {
      bookingToUpdate.updateDepartureDates(details)
    } else {
      bookingToUpdate.updateArrivalDates(details)
      bookingToUpdate.updateDepartureDates(details)
    }

    if (details.characteristics != null) {
      updateRoomCharacteristics(bookingToUpdate, details.characteristics)
    }

    val updatedBooking = cas1SpaceBookingRepository.save(bookingToUpdate)

    val previousArrivalDateIfChanged = if (previousArrivalDate != updatedBooking.expectedArrivalDate) previousArrivalDate else null
    val previousDepartureDateIfChanged = if (previousDepartureDate != updatedBooking.expectedDepartureDate) previousDepartureDate else null
    val previousCharacteristicsIfChanged = if (previousCharacteristics.sortedBy { it.id } != updatedBooking.criteria.sortedBy { it.id }) previousCharacteristics else null

    cas1BookingDomainEventService.spaceBookingChanged(
      Cas1BookingChangedEvent(
        booking = updatedBooking,
        changedBy = details.updatedBy,
        bookingChangedAt = OffsetDateTime.now(clock),
        previousArrivalDateIfChanged = previousArrivalDateIfChanged,
        previousDepartureDateIfChanged = previousDepartureDateIfChanged,
        previousCharacteristicsIfChanged = previousCharacteristicsIfChanged,
        transferredTo = details.transferredTo,
      ),
    )

    if (previousArrivalDateIfChanged != null || previousDepartureDateIfChanged != null) {
      updatedBooking.application?.let { application ->
        cas1BookingEmailService.spaceBookingAmended(
          spaceBooking = updatedBooking,
          application = application,
          updateType = details.updateType,
        )
      }
    }

    return updatedBooking
  }

  fun validate(
    updateBookingDetails: UpdateBookingDetails,
  ): CasResult<Unit> = validatedCasResult {
    val premises = cas1PremisesService.findPremiseById(updateBookingDetails.premisesId)
    if (premises == null) {
      "$.premisesId" hasValidationError "doesNotExist"
      return errors()
    }
    val bookingToUpdate = cas1SpaceBookingRepository.findByIdOrNull(updateBookingDetails.bookingId)
    if (bookingToUpdate == null) {
      "$.bookingId" hasValidationError "doesNotExist"
      return errors()
    }

    if (bookingToUpdate.isCancelled()) {
      "$.bookingId" hasValidationError "This Booking is cancelled and as such cannot be modified"
    }
    if (bookingToUpdate.hasDeparted() || bookingToUpdate.hasNonArrival()) {
      "$.bookingId" hasValidationError "hasDepartedOrNonArrival"
    }
    if (bookingToUpdate.premises.id != updateBookingDetails.premisesId) {
      "$.premisesId" hasValidationError "premisesMismatch"
    }

    val effectiveArrivalDate = if (bookingToUpdate.hasArrival()) {
      bookingToUpdate.actualArrivalDate
    } else {
      updateBookingDetails.arrivalDate ?: bookingToUpdate.expectedArrivalDate
    }

    val effectiveDepartureDate = updateBookingDetails.departureDate ?: bookingToUpdate.expectedDepartureDate

    if (effectiveDepartureDate.isBefore(effectiveArrivalDate)) {
      "$.departureDate" hasValidationError "The departure date is before the arrival date."
    }

    if (ChronoUnit.YEARS.between(effectiveArrivalDate, effectiveDepartureDate) >= MAX_LENGTH_YEARS) {
      "$.departureDate" hasValidationError "mustBeLessThan2Years"
    }

    return successOrErrors()
  }

  private fun updateRoomCharacteristics(
    booking: Cas1SpaceBookingEntity,
    newRoomCharacteristics: List<CharacteristicEntity>,
  ) {
    booking.criteria.apply {
      retainAll { it.isModelScopePremises() }
      addAll(newRoomCharacteristics)
    }
  }

  private fun Cas1SpaceBookingEntity.updateDepartureDates(updateBookingDetails: UpdateBookingDetails) {
    if (updateBookingDetails.departureDate != null) {
      this.expectedDepartureDate = updateBookingDetails.departureDate
      this.canonicalDepartureDate = updateBookingDetails.departureDate
    }
  }

  private fun Cas1SpaceBookingEntity.updateArrivalDates(updateBookingDetails: UpdateBookingDetails) {
    if (updateBookingDetails.arrivalDate != null) {
      this.expectedArrivalDate = updateBookingDetails.arrivalDate
      this.canonicalArrivalDate = updateBookingDetails.arrivalDate
    }
  }

  data class UpdateBookingDetails(
    val bookingId: UUID,
    val premisesId: UUID,
    val arrivalDate: LocalDate? = null,
    val departureDate: LocalDate? = null,
    val characteristics: List<CharacteristicEntity>? = null,
    val updatedBy: UserEntity,
    val updateType: Cas1SpaceBookingService.UpdateType,
    val transferredTo: TransferInfo? = null,
  )
}
