package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Service
class BookingService(
  private val premisesService: PremisesService,
  private val bookingRepository: BookingRepository,
  private val arrivalRepository: ArrivalRepository,
  private val cancellationRepository: CancellationRepository,
  private val extensionRepository: ExtensionRepository,
  private val departureRepository: DepartureRepository,
  private val departureReasonRepository: DepartureReasonRepository,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val destinationProviderRepository: DestinationProviderRepository,
  private val nonArrivalRepository: NonArrivalRepository
) {
  fun createBooking(bookingEntity: BookingEntity): BookingEntity = bookingRepository.save(bookingEntity)
  fun updateBooking(bookingEntity: BookingEntity): BookingEntity = bookingRepository.save(bookingEntity)
  fun getBooking(id: UUID) = bookingRepository.findByIdOrNull(id)
  fun createArrival(arrivalEntity: ArrivalEntity): ArrivalEntity = arrivalRepository.save(arrivalEntity)
  fun createCancellation(cancellationEntity: CancellationEntity): CancellationEntity = cancellationRepository.save(cancellationEntity)
  fun createNonArrival(nonArrivalEntity: NonArrivalEntity): NonArrivalEntity = nonArrivalRepository.save(nonArrivalEntity)

  fun createDeparture(
    booking: BookingEntity,
    dateTime: OffsetDateTime,
    reasonId: UUID,
    moveOnCategoryId: UUID,
    destinationProviderId: UUID,
    notes: String?
  ): ValidatableActionResult<DepartureEntity> {
    if (booking.departure != null) {
      return ValidatableActionResult.GeneralValidationError("This Booking already has a Departure set")
    }

    val validationIssues = mutableMapOf<String, String>()

    if (booking.arrivalDate.toLocalDateTime().isAfter(dateTime)) {
      validationIssues["dateTime"] = "Must be after the Booking's arrival date (${booking.arrivalDate})"
    }

    val reason = departureReasonRepository.findByIdOrNull(reasonId)
    if (reason == null) {
      validationIssues["reasonId"] = "Reason does not exist"
    }

    val moveOnCategory = moveOnCategoryRepository.findByIdOrNull(moveOnCategoryId)
    if (reason == null) {
      validationIssues["moveOnCategoryId"] = "Move on Category does not exist"
    }

    val destinationProvider = destinationProviderRepository.findByIdOrNull(destinationProviderId)
    if (destinationProvider == null) {
      validationIssues["destinationProviderId"] = "Destination Provider does not exist"
    }

    if (validationIssues.any()) {
      return ValidatableActionResult.FieldValidationError(validationIssues)
    }

    val departureEntity = departureRepository.save(
      DepartureEntity(
        id = UUID.randomUUID(),
        dateTime = dateTime,
        reason = reason!!,
        moveOnCategory = moveOnCategory!!,
        destinationProvider = destinationProvider!!,
        notes = notes,
        booking = booking
      )
    )

    return ValidatableActionResult.Success(departureEntity)
  }

  @Transactional
  fun createExtension(bookingEntity: BookingEntity, extensionEntity: ExtensionEntity): ExtensionEntity {
    val extension = extensionRepository.save(extensionEntity)
    bookingEntity.departureDate = extensionEntity.newDepartureDate
    bookingEntity.extensions.add(extension)
    updateBooking(bookingEntity)

    return extensionEntity
  }

  fun getBookingForPremises(premisesId: UUID, bookingId: UUID): GetBookingForPremisesResult {
    val premises = premisesService.getPremises(premisesId)
      ?: return GetBookingForPremisesResult.PremisesNotFound

    val booking = getBooking(bookingId)
      ?: return GetBookingForPremisesResult.BookingNotFound

    if (booking.premises.id != premises.id) {
      return GetBookingForPremisesResult.BookingNotFound
    }

    return GetBookingForPremisesResult.Success(booking)
  }
}

sealed interface GetBookingForPremisesResult {
  data class Success(val booking: BookingEntity) : GetBookingForPremisesResult
  object PremisesNotFound : GetBookingForPremisesResult
  object BookingNotFound : GetBookingForPremisesResult
}
