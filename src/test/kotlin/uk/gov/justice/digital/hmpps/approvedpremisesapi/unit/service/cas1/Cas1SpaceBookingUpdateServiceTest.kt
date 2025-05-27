package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks.ClockConfiguration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.UpdateType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingUpdateService.UpdateBookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingChangedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1SpaceBookingServiceTest.CONSTANTS.PREMISES_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1SpaceBookingUpdateServiceTest {
  private val cas1PremisesService = mockk<Cas1PremisesService>()
  private val spaceBookingRepository = mockk<Cas1SpaceBookingRepository>()
  private val cas1BookingDomainEventService = mockk<Cas1BookingDomainEventService>()
  private val cas1BookingEmailService = mockk<Cas1BookingEmailService>()
  private val clock = ClockConfiguration.FixedClock()

  private val service = Cas1SpaceBookingUpdateService(
    cas1PremisesService,
    spaceBookingRepository,
    cas1BookingDomainEventService,
    cas1BookingEmailService,
    clock,
  )

  @Nested
  inner class Validate {

    private val newArrivalDate = LocalDate.of(2025, 1, 2)
    private val newDepartureDate = LocalDate.now().plusMonths(1)

    private val user = UserEntityFactory()
      .withDefaults()
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withId(PREMISES_ID)
      .produce()

    private val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .withPremises(premises)
      .produce()

    @Test
    fun `should return validation error if no premises exist with the given premisesId`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.validate(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.premisesId", "doesNotExist")
    }

    @Test
    fun `should return validation error if no space booking exist with the given bookingId`() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns null

      val result = service.validate(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bookingId", "doesNotExist")
    }

    @Test
    fun `should return validation error when booking status is canceled`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(LocalDate.now().minusWeeks(2))
        .withPremises(premises)
        .produce()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.validate(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bookingId", "This Booking is cancelled and as such cannot be modified")
    }

    @Test
    fun `should return validation error when booking status is departed`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withActualDepartureDate(LocalDate.now().minusWeeks(2))
        .withPremises(premises)
        .produce()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.validate(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bookingId", "hasDepartedOrNonArrival")
    }

    @Test
    fun `should return validation error when booking status is nonArrival`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withNonArrivalConfirmedAt(Instant.now())
        .withPremises(premises)
        .produce()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.validate(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bookingId", "hasDepartedOrNonArrival")
    }

    @Test
    fun `should return validation error when premisesId does not match the existing booking`() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.validate(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.premisesId", "premisesMismatch")
    }

    @Test
    fun `should return validation error before arrival when new departure date is before updated arrival date`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 6, 5)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 6, 15)

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.validate(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = LocalDate.of(2025, 6, 17),
          departureDate = LocalDate.of(2025, 6, 16),
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is before the arrival date.")
    }

    @Test
    fun `should return validation error before arrival when new departure date is before existing arrival date`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 6, 5)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 6, 15)

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.validate(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = null,
          departureDate = LocalDate.of(2025, 6, 4),
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is before the arrival date.")
    }

    @Test
    fun `should return validation error before arrival when existing departure date is before new arrival date`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 6, 5)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 6, 15)

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.validate(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = LocalDate.of(2025, 6, 16),
          departureDate = null,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is before the arrival date.")
    }

    @Test
    fun `should return validation error after arrival when new departure date is before actual arrival date`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 6, 15)
      existingSpaceBooking.actualArrivalDate = LocalDate.of(2025, 6, 20)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 6, 25)

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.validate(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = null,
          departureDate = LocalDate.of(2025, 6, 18),
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is before the arrival date.")
    }

    @Test
    fun valid() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 1, 10)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 3, 15)
      val originalRoomCharacteristic =
        CharacteristicEntityFactory().withModelScope("room").withPropertyName("IsArsenCapable").produce()
      existingSpaceBooking.criteria = mutableListOf(originalRoomCharacteristic)

      val updateBookingDetails = UpdateBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = newArrivalDate,
        departureDate = newDepartureDate,
        updatedBy = user,
        characteristics = listOf(
          CharacteristicEntityFactory()
            .withPropertyName("hasEnSuite")
            .withModelScope("room")
            .produce(),
        ),
        updateType = UpdateType.AMENDMENT,
      )

      existingSpaceBooking.actualArrivalDate = null

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.validate(updateBookingDetails)

      assertThatCasResult(result).isSuccess()
    }
  }

  @Nested
  inner class Update {

    private val newArrivalDate = LocalDate.of(2025, 1, 2)
    private val newDepartureDate = LocalDate.now().plusMonths(1)

    private val user = UserEntityFactory()
      .withDefaults()
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withId(PREMISES_ID)
      .produce()

    private val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .withPremises(premises)
      .produce()

    @Test
    fun `should update only departure dates when booking status is hasArrival`() {
      existingSpaceBooking.actualArrivalDate = LocalDate.of(2025, 3, 5)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 1, 10)
      val updateBookingDetails = UpdateBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = newArrivalDate,
        departureDate = LocalDate.of(2025, 4, 26),
        updatedBy = user,
        characteristics = null,
        updateType = UpdateType.AMENDMENT,
      )

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } just Runs
      every { cas1BookingEmailService.spaceBookingAmended(any(), any(), any()) } returns Unit

      service.update(updateBookingDetails)

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.expectedArrivalDate).isEqualTo(existingSpaceBooking.expectedArrivalDate)
      assertThat(updatedSpaceBooking.canonicalArrivalDate).isEqualTo(existingSpaceBooking.canonicalArrivalDate)
      assertThat(updatedSpaceBooking.expectedDepartureDate).isEqualTo(updateBookingDetails.departureDate)
      assertThat(updatedSpaceBooking.canonicalDepartureDate).isEqualTo(updateBookingDetails.departureDate)

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          Cas1BookingChangedEvent(
            booking = updatedSpaceBookingCaptor.captured,
            changedBy = user,
            bookingChangedAt = OffsetDateTime.now(clock),
            previousArrivalDateIfChanged = null,
            previousDepartureDateIfChanged = LocalDate.of(2025, 1, 10),
            previousCharacteristicsIfChanged = null,
          ),
        )
      }

      verify(exactly = 1) {
        cas1BookingEmailService.spaceBookingAmended(
          spaceBooking = updatedSpaceBookingCaptor.captured,
          application = updatedSpaceBookingCaptor.captured.application!!,
          updateType = UpdateType.AMENDMENT,
        )
      }
    }

    @Test
    fun `should correctly update booking dates and characteristics`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 1, 10)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 3, 15)
      val originalRoomCharacteristic = CharacteristicEntityFactory().withModelScope("room").withPropertyName("IsArsenCapable").produce()
      existingSpaceBooking.criteria = mutableListOf(originalRoomCharacteristic)

      val updateBookingDetails = UpdateBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = newArrivalDate,
        departureDate = newDepartureDate,
        updatedBy = user,
        characteristics = listOf(
          CharacteristicEntityFactory()
            .withPropertyName("hasEnSuite")
            .withModelScope("room")
            .produce(),
        ),
        updateType = UpdateType.AMENDMENT,
      )

      existingSpaceBooking.actualArrivalDate = null

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } just Runs
      every { cas1BookingEmailService.spaceBookingAmended(any(), any(), any()) } returns Unit

      service.update(updateBookingDetails)

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.expectedArrivalDate).isEqualTo(updateBookingDetails.arrivalDate)
      assertThat(updatedSpaceBooking.canonicalArrivalDate).isEqualTo(updateBookingDetails.arrivalDate)
      assertThat(updatedSpaceBooking.expectedDepartureDate).isEqualTo(updateBookingDetails.departureDate)
      assertThat(updatedSpaceBooking.canonicalDepartureDate).isEqualTo(updateBookingDetails.departureDate)

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          Cas1BookingChangedEvent(
            booking = updatedSpaceBookingCaptor.captured,
            changedBy = user,
            bookingChangedAt = OffsetDateTime.now(clock),
            previousArrivalDateIfChanged = LocalDate.of(2025, 1, 10),
            previousDepartureDateIfChanged = LocalDate.of(2025, 3, 15),
            previousCharacteristicsIfChanged = listOf(originalRoomCharacteristic),
          ),
        )
      }

      verify(exactly = 1) {
        cas1BookingEmailService.spaceBookingAmended(
          spaceBooking = updatedSpaceBookingCaptor.captured,
          application = updatedSpaceBookingCaptor.captured.application!!,
          updateType = UpdateType.AMENDMENT,
        )
      }
    }

    @Test
    fun `should remove all room characteristics when no characteristics are provided`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 1, 10)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 3, 15)
      val originalRoomCharacteristic = CharacteristicEntityFactory().withModelScope("room").withPropertyName("IsArsenCapable").produce()
      existingSpaceBooking.criteria = mutableListOf(originalRoomCharacteristic)

      val updateBookingDetails = UpdateBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = newArrivalDate,
        departureDate = newDepartureDate,
        updatedBy = user,
        characteristics = emptyList(),
        updateType = UpdateType.AMENDMENT,
      )

      assertThat(existingSpaceBooking.criteria).isNotEmpty()

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } just Runs
      every { cas1BookingEmailService.spaceBookingAmended(any(), any(), any()) } returns Unit

      service.update(updateBookingDetails)

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.criteria).isEmpty()

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          Cas1BookingChangedEvent(
            booking = updatedSpaceBookingCaptor.captured,
            changedBy = user,
            bookingChangedAt = OffsetDateTime.now(clock),
            previousArrivalDateIfChanged = LocalDate.of(2025, 1, 10),
            previousDepartureDateIfChanged = LocalDate.of(2025, 3, 15),
            previousCharacteristicsIfChanged = listOf(originalRoomCharacteristic),
          ),
        )
      }

      verify(exactly = 1) {
        cas1BookingEmailService.spaceBookingAmended(
          spaceBooking = updatedSpaceBookingCaptor.captured,
          application = updatedSpaceBookingCaptor.captured.application!!,
          updateType = UpdateType.AMENDMENT,
        )
      }
    }

    @Test
    fun `should not send booking amended email when only characteristics are changed`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 1, 10)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 3, 15)
      val originalRoomCharacteristic = CharacteristicEntityFactory().withModelScope("room").withPropertyName("IsArsenCapable").produce()
      existingSpaceBooking.criteria = mutableListOf(originalRoomCharacteristic)

      val updateBookingDetails = UpdateBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = null,
        departureDate = null,
        updatedBy = user,
        characteristics = emptyList(),
        updateType = UpdateType.AMENDMENT,
      )

      assertThat(existingSpaceBooking.criteria).isNotEmpty()

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } just Runs

      service.update(updateBookingDetails)

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.criteria).isEmpty()

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          Cas1BookingChangedEvent(
            booking = updatedSpaceBookingCaptor.captured,
            changedBy = user,
            bookingChangedAt = OffsetDateTime.now(clock),
            previousArrivalDateIfChanged = null,
            previousDepartureDateIfChanged = null,
            previousCharacteristicsIfChanged = listOf(originalRoomCharacteristic),
          ),
        )
      }

      verify { cas1BookingEmailService wasNot Called }
    }

    @Test
    fun `should not send booking amended email when application is missing`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withPremises(premises)
        .withApplication(null)
        .produce()

      val originalRoomCharacteristic = CharacteristicEntityFactory().withModelScope("room").withPropertyName("IsArsenCapable").produce()
      existingSpaceBooking.criteria = mutableListOf(originalRoomCharacteristic)

      val updateBookingDetails = UpdateBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = null,
        departureDate = null,
        updatedBy = user,
        characteristics = emptyList(),
        updateType = UpdateType.AMENDMENT,
      )

      assertThat(existingSpaceBooking.criteria).isNotEmpty()

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } just Runs

      service.update(updateBookingDetails)

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.criteria).isEmpty()

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          Cas1BookingChangedEvent(
            booking = updatedSpaceBookingCaptor.captured,
            changedBy = user,
            bookingChangedAt = OffsetDateTime.now(clock),
            previousArrivalDateIfChanged = null,
            previousDepartureDateIfChanged = null,
            previousCharacteristicsIfChanged = listOf(originalRoomCharacteristic),
          ),
        )
      }

      verify { cas1BookingEmailService wasNot Called }
    }
  }
}
