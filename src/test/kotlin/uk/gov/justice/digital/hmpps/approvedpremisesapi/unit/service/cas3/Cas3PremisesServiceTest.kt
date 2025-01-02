package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingSummaryForAvailabilityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Availability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3PremisesService
import java.time.LocalDate
import java.util.UUID

class Cas3PremisesServiceTest {
  private val premisesRepositoryMock = mockk<PremisesRepository>()
  private val lostBedsRepositoryMock = mockk<LostBedsRepository>()
  private val lostBedReasonRepositoryMock = mockk<LostBedReasonRepository>()
  private val lostBedCancellationRepositoryMock = mockk<LostBedCancellationRepository>()
  private val bookingRepositoryMock = mockk<BookingRepository>()
  private val localAuthorityAreaRepositoryMock = mockk<LocalAuthorityAreaRepository>()
  private val probationRegionRepositoryMock = mockk<ProbationRegionRepository>()
  private val probationDeliveryUnitRepositoryMock = mockk<ProbationDeliveryUnitRepository>()
  private val characteristicServiceMock = mockk<CharacteristicService>()

  private val temporaryAccommodationPremisesFactory = TemporaryAccommodationPremisesEntityFactory()
    .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
    }

  private val premisesService = Cas3PremisesService(
    premisesRepositoryMock,
    lostBedsRepositoryMock,
    lostBedReasonRepositoryMock,
    lostBedCancellationRepositoryMock,
    bookingRepositoryMock,
    localAuthorityAreaRepositoryMock,
    probationRegionRepositoryMock,
    probationDeliveryUnitRepositoryMock,
    characteristicServiceMock,
  )

  @Test
  fun `getAvailabilityForRange returns correctly for Temporary Accommodation premises`() {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(6)

    val premises = temporaryAccommodationPremisesFactory.produce()

    val room = RoomEntityFactory()
      .withYieldedPremises { premises }
      .produce()

    val bed = BedEntityFactory()
      .withYieldedRoom { room }
      .produce()

    val voidBedspaceEntity = LostBedsEntityFactory()
      .withPremises(premises)
      .withStartDate(startDate.plusDays(1))
      .withEndDate(startDate.plusDays(2))
      .withYieldedReason { LostBedReasonEntityFactory().produce() }
      .withYieldedBed { bed }
      .produce()

    val pendingBookingEntity = BookingSummaryForAvailabilityFactory()
      .withArrivalDate(startDate.plusDays(1))
      .withDepartureDate(startDate.plusDays(3))
      .withCancelled(false)
      .withArrived(false)
      .withIsNotArrived(false)
      .produce()

    val arrivedBookingEntity = BookingSummaryForAvailabilityFactory()
      .withArrivalDate(startDate)
      .withDepartureDate(startDate.plusDays(2))
      .withCancelled(false)
      .withArrived(true)
      .withIsNotArrived(false)
      .produce()

    val nonArrivedBookingEntity = BookingSummaryForAvailabilityFactory()
      .withArrivalDate(startDate.plusDays(3))
      .withDepartureDate(startDate.plusDays(5))
      .withCancelled(false)
      .withArrived(false)
      .withIsNotArrived(true)
      .produce()

    val cancelledBookingEntity = BookingSummaryForAvailabilityFactory()
      .withArrivalDate(startDate.plusDays(4))
      .withDepartureDate(startDate.plusDays(6))
      .withCancelled(true)
      .withArrived(false)
      .withIsNotArrived(false)
      .produce()

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      pendingBookingEntity,
      arrivedBookingEntity,
      nonArrivedBookingEntity,
      cancelledBookingEntity,
    )
    every { lostBedsRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      voidBedspaceEntity,
    )

    val result = premisesService.getAvailabilityForRange(premises, startDate, endDate)

    assertThat(result).containsValues(
      Availability(date = startDate, pendingBookings = 0, arrivedBookings = 1, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(1), pendingBookings = 1, arrivedBookings = 1, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 1),
      Availability(date = startDate.plusDays(2), pendingBookings = 1, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(3), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 1, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(4), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 1, cancelledBookings = 1, lostBeds = 0),
      Availability(date = startDate.plusDays(5), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 1, lostBeds = 0),
    )
  }

  @Test
  fun `getAvailabilityForRange returns correctly when there are no bookings or void bedspaces`() {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(3)

    val premises = temporaryAccommodationPremisesFactory.produce()

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf()
    every { lostBedsRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf()

    val result = premisesService.getAvailabilityForRange(premises, startDate, endDate)

    assertThat(result).containsValues(
      Availability(date = startDate, 0, 0, 0, 0, 0),
      Availability(date = startDate.plusDays(1), 0, 0, 0, 0, 0),
      Availability(date = startDate.plusDays(2), 0, 0, 0, 0, 0),
    )
  }

  @Test
  fun `getAvailabilityForRange returns correctly when there are bookings`() {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(6)

    val premises = temporaryAccommodationPremisesFactory.produce()

    val voidBedspaceEntityOne = LostBedsEntityFactory()
      .withPremises(premises)
      .withStartDate(startDate.plusDays(1))
      .withEndDate(startDate.plusDays(2))
      .withYieldedReason { LostBedReasonEntityFactory().produce() }
      .withBed(
        BedEntityFactory().apply {
          withYieldedRoom {
            RoomEntityFactory().apply {
              withYieldedPremises { premises }
            }.produce()
          }
        }.produce(),
      )
      .produce()

    val voidBedspaceEntityTwo = LostBedsEntityFactory()
      .withPremises(premises)
      .withStartDate(startDate.plusDays(1))
      .withEndDate(startDate.plusDays(2))
      .withYieldedReason { LostBedReasonEntityFactory().produce() }
      .withBed(
        BedEntityFactory().apply {
          withYieldedRoom {
            RoomEntityFactory().apply {
              withYieldedPremises { premises }
            }.produce()
          }
        }.produce(),
      )
      .produce()

    val pendingBookingEntity = BookingSummaryForAvailabilityFactory()
      .withArrivalDate(startDate.plusDays(1))
      .withDepartureDate(startDate.plusDays(3))
      .withArrived(false)
      .withCancelled(false)
      .withIsNotArrived(false)
      .produce()

    val arrivedBookingEntity = BookingSummaryForAvailabilityFactory()
      .withArrivalDate(startDate)
      .withDepartureDate(startDate.plusDays(2))
      .withArrived(true)
      .withCancelled(false)
      .withIsNotArrived(false)
      .produce()

    val nonArrivedBookingEntity = BookingSummaryForAvailabilityFactory()
      .withArrivalDate(startDate.plusDays(3))
      .withDepartureDate(startDate.plusDays(5))
      .withArrived(false)
      .withCancelled(false)
      .withIsNotArrived(true)
      .produce()

    val cancelledBookingEntity = BookingSummaryForAvailabilityFactory()
      .withArrivalDate(startDate.plusDays(4))
      .withDepartureDate(startDate.plusDays(6))
      .withArrived(false)
      .withCancelled(true)
      .withIsNotArrived(false)
      .produce()

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      pendingBookingEntity,
      arrivedBookingEntity,
      nonArrivedBookingEntity,
      cancelledBookingEntity,
    )
    every { lostBedsRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      voidBedspaceEntityOne,
      voidBedspaceEntityTwo,
    )

    val result = premisesService.getAvailabilityForRange(premises, startDate, endDate)

    assertThat(result).containsValues(
      Availability(date = startDate, pendingBookings = 0, arrivedBookings = 1, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(1), pendingBookings = 1, arrivedBookings = 1, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 2),
      Availability(date = startDate.plusDays(2), pendingBookings = 1, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(3), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 1, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(4), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 1, cancelledBookings = 1, lostBeds = 0),
      Availability(date = startDate.plusDays(5), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 1, lostBeds = 0),
    )
  }

  @Test
  fun `getAvailabilityForRange returns correctly when there are cancelled void bedspaces`() {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(6)

    val premises = temporaryAccommodationPremisesFactory.produce()

    val voidBedspaceEntity = LostBedsEntityFactory()
      .withPremises(premises)
      .withStartDate(startDate.plusDays(1))
      .withEndDate(startDate.plusDays(2))
      .withYieldedReason { LostBedReasonEntityFactory().produce() }
      .withBed(
        BedEntityFactory().apply {
          withYieldedRoom {
            RoomEntityFactory().apply {
              withYieldedPremises { premises }
            }.produce()
          }
        }.produce(),
      )
      .produce()

    val voidBedspaceCancellation = LostBedCancellationEntityFactory()
      .withYieldedLostBed { voidBedspaceEntity }
      .produce()

    voidBedspaceEntity.cancellation = voidBedspaceCancellation

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf()
    every { lostBedsRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      voidBedspaceEntity,
    )

    val result = premisesService.getAvailabilityForRange(premises, startDate, endDate)

    assertThat(result).containsValues(
      Availability(date = startDate, pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(1), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(2), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(3), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(4), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(5), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 0),
    )
  }

  @Test
  fun `createVoidBedspaces returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
    val premisesEntity = temporaryAccommodationPremisesFactory.produce()

    val reasonId = UUID.randomUUID()

    every { lostBedReasonRepositoryMock.findByIdOrNull(reasonId) } returns null

    val result = premisesService.createVoidBedspaces(
      premises = premisesEntity,
      startDate = LocalDate.parse("2022-08-28"),
      endDate = LocalDate.parse("2022-08-25"),
      reasonId = reasonId,
      referenceNumber = "12345",
      notes = "notes",
      bedId = UUID.randomUUID(),
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.endDate", "beforeStartDate"),
      entry("$.reason", "doesNotExist"),
    )
  }

  @Test
  fun `createVoidBedspaces returns FieldValidationError with correct param to message map when a void bedspace reason with the incorrect service scope is supplied`() {
    val premisesEntity = temporaryAccommodationPremisesFactory.produce()

    val reasonId = UUID.randomUUID()

    every { lostBedReasonRepositoryMock.findByIdOrNull(reasonId) } returns LostBedReasonEntityFactory()
      .withServiceScope(ServiceName.approvedPremises.value)
      .produce()

    val result = premisesService.createVoidBedspaces(
      premises = premisesEntity,
      startDate = LocalDate.parse("2022-08-25"),
      endDate = LocalDate.parse("2022-08-28"),
      reasonId = reasonId,
      referenceNumber = "12345",
      notes = "notes",
      bedId = UUID.randomUUID(),
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.reason", "incorrectLostBedReasonServiceScope"),
    )
  }

  @Test
  fun `createVoidBedspaces returns Success with correct result when validation passed`() {
    val premisesEntity = temporaryAccommodationPremisesFactory.produce()

    val room = RoomEntityFactory()
      .withPremises(premisesEntity)
      .produce()

    val bed = BedEntityFactory()
      .withYieldedRoom { room }
      .produce()

    premisesEntity.rooms += room
    room.beds += bed

    val voidBedspaceReason = LostBedReasonEntityFactory()
      .withServiceScope(ServiceName.temporaryAccommodation.value)
      .produce()

    every { lostBedReasonRepositoryMock.findByIdOrNull(voidBedspaceReason.id) } returns voidBedspaceReason

    every { lostBedsRepositoryMock.save(any()) } answers { it.invocation.args[0] as LostBedsEntity }

    val result = premisesService.createVoidBedspaces(
      premises = premisesEntity,
      startDate = LocalDate.parse("2022-08-25"),
      endDate = LocalDate.parse("2022-08-28"),
      reasonId = voidBedspaceReason.id,
      referenceNumber = "12345",
      notes = "notes",
      bedId = bed.id,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.premises).isEqualTo(premisesEntity)
    assertThat(result.entity.reason).isEqualTo(voidBedspaceReason)
    assertThat(result.entity.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
    assertThat(result.entity.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
    assertThat(result.entity.referenceNumber).isEqualTo("12345")
    assertThat(result.entity.notes).isEqualTo("notes")
  }

  @Test
  fun `updateVoidBedspaces returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
    val premisesEntity = temporaryAccommodationPremisesFactory.produce()

    val reasonId = UUID.randomUUID()

    val voidBedspaceEntity = LostBedsEntityFactory()
      .withYieldedPremises { premisesEntity }
      .withYieldedReason {
        LostBedReasonEntityFactory()
          .withServiceScope(ServiceName.temporaryAccommodation.value)
          .produce()
      }
      .withBed(
        BedEntityFactory().apply {
          withYieldedRoom {
            RoomEntityFactory().apply {
              withPremises(premisesEntity)
            }.produce()
          }
        }.produce(),
      )
      .produce()

    every { lostBedsRepositoryMock.findByIdOrNull(voidBedspaceEntity.id) } returns voidBedspaceEntity
    every { lostBedReasonRepositoryMock.findByIdOrNull(reasonId) } returns null

    val result = premisesService.updateVoidBedspaces(
      voidBedspaceId = voidBedspaceEntity.id,
      startDate = LocalDate.parse("2022-08-28"),
      endDate = LocalDate.parse("2022-08-25"),
      reasonId = reasonId,
      referenceNumber = "12345",
      notes = "notes",
    )

    assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
    val resultEntity = (result as AuthorisableActionResult.Success).entity
    assertThat(resultEntity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((resultEntity as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.endDate", "beforeStartDate"),
      entry("$.reason", "doesNotExist"),
    )
  }

  @Test
  fun `updateVoidBedspaces returns FieldValidationError with correct param to message map when a void bedspace reason with the incorrect service scope is supplied`() {
    val premisesEntity = temporaryAccommodationPremisesFactory.produce()

    val reasonId = UUID.randomUUID()

    val voidBedspaceEntity = LostBedsEntityFactory()
      .withYieldedPremises { premisesEntity }
      .withYieldedReason {
        LostBedReasonEntityFactory()
          .withServiceScope(ServiceName.temporaryAccommodation.value)
          .produce()
      }
      .withBed(
        BedEntityFactory().apply {
          withYieldedRoom {
            RoomEntityFactory().apply {
              withPremises(premisesEntity)
            }.produce()
          }
        }.produce(),
      )
      .produce()

    every { lostBedsRepositoryMock.findByIdOrNull(voidBedspaceEntity.id) } returns voidBedspaceEntity
    every { lostBedReasonRepositoryMock.findByIdOrNull(reasonId) } returns LostBedReasonEntityFactory()
      .withServiceScope(ServiceName.approvedPremises.value)
      .produce()

    val result = premisesService.updateVoidBedspaces(
      voidBedspaceId = voidBedspaceEntity.id,
      startDate = LocalDate.parse("2022-08-25"),
      endDate = LocalDate.parse("2022-08-28"),
      reasonId = reasonId,
      referenceNumber = "12345",
      notes = "notes",
    )

    assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
    val resultEntity = (result as AuthorisableActionResult.Success).entity
    assertThat(resultEntity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((resultEntity as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.reason", "incorrectLostBedReasonServiceScope"),
    )
  }

  @Test
  fun `updateVoidBedspaces returns Success with correct result when validation passed`() {
    val premisesEntity = temporaryAccommodationPremisesFactory.produce()

    val voidBedspaceReason = LostBedReasonEntityFactory()
      .withServiceScope(ServiceName.temporaryAccommodation.value)
      .produce()

    val voidBedspacesEntity = LostBedsEntityFactory()
      .withYieldedPremises { premisesEntity }
      .withYieldedReason {
        LostBedReasonEntityFactory()
          .withServiceScope(ServiceName.temporaryAccommodation.value)
          .produce()
      }
      .withBed(
        BedEntityFactory().apply {
          withYieldedRoom {
            RoomEntityFactory().apply {
              withPremises(premisesEntity)
            }.produce()
          }
        }.produce(),
      )
      .produce()

    every { lostBedsRepositoryMock.findByIdOrNull(voidBedspacesEntity.id) } returns voidBedspacesEntity
    every { lostBedReasonRepositoryMock.findByIdOrNull(voidBedspaceReason.id) } returns voidBedspaceReason

    every { lostBedsRepositoryMock.save(any()) } answers { it.invocation.args[0] as LostBedsEntity }

    val result = premisesService.updateVoidBedspaces(
      voidBedspaceId = voidBedspacesEntity.id,
      startDate = LocalDate.parse("2022-08-25"),
      endDate = LocalDate.parse("2022-08-28"),
      reasonId = voidBedspaceReason.id,
      referenceNumber = "12345",
      notes = "notes",
    )
    assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
    val resultEntity = (result as AuthorisableActionResult.Success).entity
    assertThat(resultEntity).isInstanceOf(ValidatableActionResult.Success::class.java)
    resultEntity as ValidatableActionResult.Success
    assertThat(resultEntity.entity.premises).isEqualTo(premisesEntity)
    assertThat(resultEntity.entity.reason).isEqualTo(voidBedspaceReason)
    assertThat(resultEntity.entity.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
    assertThat(resultEntity.entity.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
    assertThat(resultEntity.entity.referenceNumber).isEqualTo("12345")
    assertThat(resultEntity.entity.notes).isEqualTo("notes")
  }

  @Test
  fun `renamePremises returns NotFound if the premises does not exist`() {
    every { premisesRepositoryMock.findTemporaryAccommodationPremisesByIdOrNull(any()) } returns null

    val result = premisesService.renamePremises(UUID.randomUUID(), "unknown-premises")

    assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
  }

  @Test
  fun `renamePremises returns FieldValidationError if the new name is not unique for the service`() {
    val premises = temporaryAccommodationPremisesFactory.produce()

    every { premisesRepositoryMock.findTemporaryAccommodationPremisesByIdOrNull(any()) } returns premises
    every { premisesRepositoryMock.nameIsUniqueForType<TemporaryAccommodationPremisesEntity>(any(), any()) } returns false

    val result = premisesService.renamePremises(premises.id, "non-unique-name-premises")

    assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
    result as AuthorisableActionResult.Success
    assertThat(result.entity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    val resultEntity = result.entity as ValidatableActionResult.FieldValidationError
    assertThat(resultEntity.validationMessages).contains(
      entry("$.name", "notUnique"),
    )
  }

  @Test
  fun `renamePremises returns Success containing updated premises otherwise`() {
    val premises = temporaryAccommodationPremisesFactory.produce()

    every { premisesRepositoryMock.findTemporaryAccommodationPremisesByIdOrNull(any()) } returns premises
    every { premisesRepositoryMock.nameIsUniqueForType<TemporaryAccommodationPremisesEntity>(any(), any()) } returns true
    every { premisesRepositoryMock.save(any()) } returnsArgument 0

    val result = premisesService.renamePremises(premises.id, "renamed-premises")

    assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
    result as AuthorisableActionResult.Success
    assertThat(result.entity).isInstanceOf(ValidatableActionResult.Success::class.java)
    val resultEntity = result.entity as ValidatableActionResult.Success
    assertThat(resultEntity.entity).matches {
      it.id == premises.id &&
        it.name == "renamed-premises"
    }

    verify(exactly = 1) {
      premisesRepositoryMock.save(
        match {
          it.id == premises.id &&
            it.name == "renamed-premises"
        },
      )
    }
  }
}