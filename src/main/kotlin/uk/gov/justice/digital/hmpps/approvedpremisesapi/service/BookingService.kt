package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
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
  private val nonArrivalRepository: NonArrivalRepository,
  private val nonArrivalReasonRepository: NonArrivalReasonRepository,
  private val cancellationReasonRepository: CancellationReasonRepository
) {
  fun createBooking(bookingEntity: BookingEntity): BookingEntity = bookingRepository.save(bookingEntity)
  fun updateBooking(bookingEntity: BookingEntity): BookingEntity = bookingRepository.save(bookingEntity)
  fun getBooking(id: UUID) = bookingRepository.findByIdOrNull(id)

  fun createArrival(
    booking: BookingEntity,
    arrivalDate: LocalDate,
    expectedDepartureDate: LocalDate,
    notes: String?
  ): ValidatableActionResult<ArrivalEntity> {
    if (booking.arrival != null) {
      return ValidatableActionResult.GeneralValidationError("This Booking already has an Arrival set")
    }

    if (expectedDepartureDate.isBefore(arrivalDate)) {
      return ValidatableActionResult.FieldValidationError(mapOf("$.expectedDepartureDate" to "Cannot be before arrivalDate"))
    }

    val arrivalEntity = arrivalRepository.save(
      ArrivalEntity(
        id = UUID.randomUUID(),
        arrivalDate = arrivalDate,
        expectedDepartureDate = expectedDepartureDate,
        notes = notes,
        booking = booking
      )
    )

    return ValidatableActionResult.Success(arrivalEntity)
  }

  fun createNonArrival(
    booking: BookingEntity,
    date: LocalDate,
    reasonId: UUID,
    notes: String?
  ): ValidatableActionResult<NonArrivalEntity> {
    if (booking.nonArrival != null) {
      return ValidatableActionResult.GeneralValidationError("This Booking already has a Non Arrival set")
    }

    val validationIssues = mutableMapOf<String, String>()

    if (booking.arrivalDate.isAfter(date)) {
      validationIssues["$.date"] = "Cannot be before Booking's arrivalDate"
    }

    val reason = nonArrivalReasonRepository.findByIdOrNull(reasonId)
    if (reason == null) {
      validationIssues["$.reason"] = "This reason does not exist"
    }

    if (validationIssues.any()) {
      return ValidatableActionResult.FieldValidationError(validationIssues)
    }

    val nonArrivalEntity = nonArrivalRepository.save(
      NonArrivalEntity(
        id = UUID.randomUUID(),
        date = date,
        notes = notes,
        reason = reason!!,
        booking = booking
      )
    )

    return ValidatableActionResult.Success(nonArrivalEntity)
  }

  fun createCancellation(
    booking: BookingEntity,
    date: LocalDate,
    reasonId: UUID,
    notes: String?
  ): ValidatableActionResult<CancellationEntity> {
    if (booking.cancellation != null) {
      return ValidatableActionResult.GeneralValidationError("This Booking already has a Cancellation set")
    }

    val validationIssues = mutableMapOf<String, String>()

    val reason = cancellationReasonRepository.findByIdOrNull(reasonId)
    if (reason == null) {
      validationIssues["$.reason"] = "This reason does not exist"
    }

    if (validationIssues.any()) {
      return ValidatableActionResult.FieldValidationError(validationIssues)
    }

    val cancellationEntity = cancellationRepository.save(
      CancellationEntity(
        id = UUID.randomUUID(),
        date = date,
        reason = reason!!,
        notes = notes,
        booking = booking
      )
    )

    return ValidatableActionResult.Success(cancellationEntity)
  }

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
      validationIssues["$.dateTime"] = "Must be after the Booking's arrival date (${booking.arrivalDate})"
    }

    val reason = departureReasonRepository.findByIdOrNull(reasonId)
    if (reason == null) {
      validationIssues["$.reasonId"] = "Reason does not exist"
    }

    val moveOnCategory = moveOnCategoryRepository.findByIdOrNull(moveOnCategoryId)
    if (reason == null) {
      validationIssues["$.moveOnCategoryId"] = "Move on Category does not exist"
    }

    val destinationProvider = destinationProviderRepository.findByIdOrNull(destinationProviderId)
    if (destinationProvider == null) {
      validationIssues["$.destinationProviderId"] = "Destination Provider does not exist"
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
  fun createExtension(
    booking: BookingEntity,
    newDepartureDate: LocalDate,
    notes: String?
  ): ValidatableActionResult<ExtensionEntity> {
    if (booking.departureDate.isAfter(newDepartureDate)) {
      return ValidatableActionResult.FieldValidationError(mapOf("$.newDepartureDate" to "Must be after the Booking's current departure date (${booking.departureDate})"))
    }

    val extensionEntity = ExtensionEntity(
      id = UUID.randomUUID(),
      previousDepartureDate = booking.departureDate,
      newDepartureDate = newDepartureDate,
      notes = notes,
      booking = booking
    )

    val extension = extensionRepository.save(extensionEntity)
    booking.departureDate = extensionEntity.newDepartureDate
    booking.extensions.add(extension)
    updateBooking(booking)

    return ValidatableActionResult.Success(extensionEntity)
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
