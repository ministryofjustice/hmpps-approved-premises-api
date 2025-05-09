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
import java.time.LocalDate
import java.util.UUID

/**
 * Calls to this service are orchestrated via the [Cas1SpaceBookingService]
 */
@Service
class Cas1SpaceBookingUpdateService(
  private val cas1PremisesService: Cas1PremisesService,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
) {

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

    return successOrErrors()
  }

  data class UpdateBookingDetails(
    val bookingId: UUID,
    val premisesId: UUID,
    val arrivalDate: LocalDate? = null,
    val departureDate: LocalDate? = null,
    val characteristics: List<CharacteristicEntity>? = null,
    val updatedBy: UserEntity,
    val updateType: Cas1SpaceBookingService.UpdateType,
    val transferredTo: Cas1SpaceBookingEntity? = null,
  )
}
