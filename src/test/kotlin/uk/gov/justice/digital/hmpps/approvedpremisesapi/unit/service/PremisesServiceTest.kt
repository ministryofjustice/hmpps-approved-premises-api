package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Availability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import java.time.LocalDate
import java.util.UUID

class PremisesServiceTest {
  private val premisesRepositoryMock = mockk<PremisesRepository>()
  private val lostBedsRepositoryMock = mockk<LostBedsRepository>()
  private val bookingRepositoryMock = mockk<BookingRepository>()
  private val lostBedReasonRepositoryMock = mockk<LostBedReasonRepository>()
  private val localAuthorityAreaRepositoryMock = mockk<LocalAuthorityAreaRepository>()
  private val probationRegionRepositoryMock = mockk<ProbationRegionRepository>()
  private val lostBedCancellationRepositoryMock = mockk<LostBedCancellationRepository>()
  private val probationDeliveryUnitRepositoryMock = mockk<ProbationDeliveryUnitRepository>()
  private val characteristicServiceMock = mockk<CharacteristicService>()
  private val roomRepositoryMock = mockk<RoomRepository>()
  private val bedRepositoryMock = mockk<BedRepository>()

  private val premisesService = PremisesService(
    premisesRepositoryMock,
    lostBedsRepositoryMock,
    bookingRepositoryMock,
    lostBedReasonRepositoryMock,
    localAuthorityAreaRepositoryMock,
    probationRegionRepositoryMock,
    lostBedCancellationRepositoryMock,
    probationDeliveryUnitRepositoryMock,
    characteristicServiceMock,
    roomRepositoryMock,
    bedRepositoryMock,
  )

  @Test
  fun `getAvailabilityForRange returns correctly when there are no bookings or lost beds`() {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(3)

    val premises = ApprovedPremisesEntityFactory()
      .withTotalBeds(30)
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
      }.produce()

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

    val premises = ApprovedPremisesEntityFactory()
      .withTotalBeds(30)
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
      }.produce()

    val lostBedEntityOne = LostBedsEntityFactory()
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

    val lostBedEntityTwo = LostBedsEntityFactory()
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

    val pendingBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate.plusDays(1))
      .withDepartureDate(startDate.plusDays(3))
      .withStaffKeyWorkerCode(null)
      .produce()

    val arrivedBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate)
      .withDepartureDate(startDate.plusDays(2))
      .withStaffKeyWorkerCode("123")
      .produce()

    val arrivalEntity = ArrivalEntityFactory()
      .withBooking(arrivedBookingEntity)
      .produce()

    arrivedBookingEntity.arrival = arrivalEntity

    val nonArrivedBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate.plusDays(3))
      .withDepartureDate(startDate.plusDays(5))
      .withStaffKeyWorkerCode(null)
      .produce()

    val nonArrivalEntity = NonArrivalEntityFactory()
      .withBooking(nonArrivedBookingEntity)
      .withYieldedReason { NonArrivalReasonEntityFactory().produce() }
      .produce()

    nonArrivedBookingEntity.nonArrival = nonArrivalEntity

    val cancelledBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate.plusDays(4))
      .withDepartureDate(startDate.plusDays(6))
      .withStaffKeyWorkerCode(null)
      .produce()

    val cancelledArrivalEntity = CancellationEntityFactory()
      .withYieldedReason { CancellationReasonEntityFactory().produce() }
      .withBooking(cancelledBookingEntity)
      .produce()

    cancelledBookingEntity.cancellations = mutableListOf(cancelledArrivalEntity)

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      pendingBookingEntity,
      arrivedBookingEntity,
      nonArrivedBookingEntity,
      cancelledBookingEntity,
    )
    every { lostBedsRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      lostBedEntityOne,
      lostBedEntityTwo,
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
  fun `getAvailabilityForRange returns correctly when there are cancelled lost beds`() {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(6)

    val premises = ApprovedPremisesEntityFactory()
      .withTotalBeds(30)
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
      }.produce()

    val lostBedEntity = LostBedsEntityFactory()
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

    val lostBedCancellation = LostBedCancellationEntityFactory()
      .withYieldedLostBed { lostBedEntity }
      .produce()

    lostBedEntity.cancellation = lostBedCancellation

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf()
    every { lostBedsRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      lostBedEntity,
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
  fun `getAvailabilityForRange returns correctly for Temporary Accommodation premises`() {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(6)

    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
      }.produce()

    val room = RoomEntityFactory()
      .withYieldedPremises { premises }
      .produce()

    val bed = BedEntityFactory()
      .withYieldedRoom { room }
      .produce()

    val lostBedEntity = LostBedsEntityFactory()
      .withPremises(premises)
      .withStartDate(startDate.plusDays(1))
      .withEndDate(startDate.plusDays(2))
      .withYieldedReason { LostBedReasonEntityFactory().produce() }
      .withYieldedBed { bed }
      .produce()

    val pendingBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate.plusDays(1))
      .withDepartureDate(startDate.plusDays(3))
      .withStaffKeyWorkerCode(null)
      .produce()

    val arrivedBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate)
      .withDepartureDate(startDate.plusDays(2))
      .withStaffKeyWorkerCode("123")
      .produce()

    val arrivalEntity = ArrivalEntityFactory()
      .withBooking(arrivedBookingEntity)
      .produce()

    arrivedBookingEntity.arrival = arrivalEntity

    val nonArrivedBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate.plusDays(3))
      .withDepartureDate(startDate.plusDays(5))
      .withStaffKeyWorkerCode(null)
      .produce()

    val nonArrivalEntity = NonArrivalEntityFactory()
      .withBooking(nonArrivedBookingEntity)
      .withYieldedReason { NonArrivalReasonEntityFactory().produce() }
      .produce()

    nonArrivedBookingEntity.nonArrival = nonArrivalEntity

    val cancelledBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate.plusDays(4))
      .withDepartureDate(startDate.plusDays(6))
      .withStaffKeyWorkerCode(null)
      .produce()

    val cancelledArrivalEntity = CancellationEntityFactory()
      .withYieldedReason { CancellationReasonEntityFactory().produce() }
      .withBooking(cancelledBookingEntity)
      .produce()

    cancelledBookingEntity.cancellations = mutableListOf(cancelledArrivalEntity)

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      pendingBookingEntity,
      arrivedBookingEntity,
      nonArrivedBookingEntity,
      cancelledBookingEntity,
    )
    every { lostBedsRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      lostBedEntity,
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
  fun `createLostBeds returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
    val premisesEntity = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val reasonId = UUID.randomUUID()

    every { lostBedReasonRepositoryMock.findByIdOrNull(reasonId) } returns null

    val result = premisesService.createLostBeds(
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
  fun `createLostBeds returns FieldValidationError with correct param to message map when a lost bed reason with the incorrect service scope is supplied`() {
    val premisesEntity = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val reasonId = UUID.randomUUID()

    every { lostBedReasonRepositoryMock.findByIdOrNull(reasonId) } returns LostBedReasonEntityFactory()
      .withServiceScope(ServiceName.temporaryAccommodation.value)
      .produce()

    val result = premisesService.createLostBeds(
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
  fun `createLostBeds returns Success with correct result when validation passed`() {
    val premisesEntity = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premisesEntity)
      .produce()

    val bed = BedEntityFactory()
      .withYieldedRoom { room }
      .produce()

    premisesEntity.rooms += room
    room.beds += bed

    val lostBedReason = LostBedReasonEntityFactory()
      .withServiceScope(ServiceName.approvedPremises.value)
      .produce()

    every { lostBedReasonRepositoryMock.findByIdOrNull(lostBedReason.id) } returns lostBedReason

    every { lostBedsRepositoryMock.save(any()) } answers { it.invocation.args[0] as LostBedsEntity }

    val result = premisesService.createLostBeds(
      premises = premisesEntity,
      startDate = LocalDate.parse("2022-08-25"),
      endDate = LocalDate.parse("2022-08-28"),
      reasonId = lostBedReason.id,
      referenceNumber = "12345",
      notes = "notes",
      bedId = bed.id,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.premises).isEqualTo(premisesEntity)
    assertThat(result.entity.reason).isEqualTo(lostBedReason)
    assertThat(result.entity.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
    assertThat(result.entity.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
    assertThat(result.entity.referenceNumber).isEqualTo("12345")
    assertThat(result.entity.notes).isEqualTo("notes")
  }

  @Test
  fun `updateLostBeds returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
    val premisesEntity = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val reasonId = UUID.randomUUID()

    val lostBedsEntity = LostBedsEntityFactory()
      .withYieldedPremises { premisesEntity }
      .withYieldedReason {
        LostBedReasonEntityFactory()
          .withServiceScope(ServiceName.approvedPremises.value)
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

    every { lostBedsRepositoryMock.findByIdOrNull(lostBedsEntity.id) } returns lostBedsEntity
    every { lostBedReasonRepositoryMock.findByIdOrNull(reasonId) } returns null

    val result = premisesService.updateLostBeds(
      lostBedId = lostBedsEntity.id,
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
  fun `updateLostBeds returns FieldValidationError with correct param to message map when a lost bed reason with the incorrect service scope is supplied`() {
    val premisesEntity = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val reasonId = UUID.randomUUID()

    val lostBedsEntity = LostBedsEntityFactory()
      .withYieldedPremises { premisesEntity }
      .withYieldedReason {
        LostBedReasonEntityFactory()
          .withServiceScope(ServiceName.approvedPremises.value)
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

    every { lostBedsRepositoryMock.findByIdOrNull(lostBedsEntity.id) } returns lostBedsEntity
    every { lostBedReasonRepositoryMock.findByIdOrNull(reasonId) } returns LostBedReasonEntityFactory()
      .withServiceScope(ServiceName.temporaryAccommodation.value)
      .produce()

    val result = premisesService.updateLostBeds(
      lostBedId = lostBedsEntity.id,
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
  fun `updateLostBeds returns Success with correct result when validation passed`() {
    val premisesEntity = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val lostBedReason = LostBedReasonEntityFactory()
      .withServiceScope(ServiceName.approvedPremises.value)
      .produce()

    val lostBedsEntity = LostBedsEntityFactory()
      .withYieldedPremises { premisesEntity }
      .withYieldedReason {
        LostBedReasonEntityFactory()
          .withServiceScope(ServiceName.approvedPremises.value)
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

    every { lostBedsRepositoryMock.findByIdOrNull(lostBedsEntity.id) } returns lostBedsEntity
    every { lostBedReasonRepositoryMock.findByIdOrNull(lostBedReason.id) } returns lostBedReason

    every { lostBedsRepositoryMock.save(any()) } answers { it.invocation.args[0] as LostBedsEntity }

    val result = premisesService.updateLostBeds(
      lostBedId = lostBedsEntity.id,
      startDate = LocalDate.parse("2022-08-25"),
      endDate = LocalDate.parse("2022-08-28"),
      reasonId = lostBedReason.id,
      referenceNumber = "12345",
      notes = "notes",
    )
    assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
    val resultEntity = (result as AuthorisableActionResult.Success).entity
    assertThat(resultEntity).isInstanceOf(ValidatableActionResult.Success::class.java)
    resultEntity as ValidatableActionResult.Success
    assertThat(resultEntity.entity.premises).isEqualTo(premisesEntity)
    assertThat(resultEntity.entity.reason).isEqualTo(lostBedReason)
    assertThat(resultEntity.entity.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
    assertThat(resultEntity.entity.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
    assertThat(resultEntity.entity.referenceNumber).isEqualTo("12345")
    assertThat(resultEntity.entity.notes).isEqualTo("notes")
  }
}
