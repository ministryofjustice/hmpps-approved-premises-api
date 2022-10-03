package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DestinationProviderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffMemberFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class BookingServiceTest {
  private val mockPremisesService = mockk<PremisesService>()
  private val mockStaffMemberService = mockk<StaffMemberService>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockArrivalRepository = mockk<ArrivalRepository>()
  private val mockCancellationRepository = mockk<CancellationRepository>()
  private val mockExtensionRepository = mockk<ExtensionRepository>()
  private val mockDepartureRepository = mockk<DepartureRepository>()
  private val mockNonArrivalRepository = mockk<NonArrivalRepository>()
  private val mockDepartureReasonRepository = mockk<DepartureReasonRepository>()
  private val mockMoveOnCategoryRepository = mockk<MoveOnCategoryRepository>()
  private val mockDestinationProviderRepository = mockk<DestinationProviderRepository>()
  private val mockNonArrivalReasonRepository = mockk<NonArrivalReasonRepository>()
  private val mockCancellationReasonRepository = mockk<CancellationReasonRepository>()

  private val bookingService = BookingService(
    premisesService = mockPremisesService,
    staffMemberService = mockStaffMemberService,
    bookingRepository = mockBookingRepository,
    arrivalRepository = mockArrivalRepository,
    cancellationRepository = mockCancellationRepository,
    extensionRepository = mockExtensionRepository,
    departureRepository = mockDepartureRepository,
    nonArrivalRepository = mockNonArrivalRepository,
    departureReasonRepository = mockDepartureReasonRepository,
    moveOnCategoryRepository = mockMoveOnCategoryRepository,
    destinationProviderRepository = mockDestinationProviderRepository,
    nonArrivalReasonRepository = mockNonArrivalReasonRepository,
    cancellationReasonRepository = mockCancellationReasonRepository
  )

  @Test
  fun `getBookingForPremises returns PremisesNotFound when premises with provided ID does not exist`() {
    val premisesId = UUID.fromString("8461d08b-0e3f-426a-a941-0ada4160e6db")
    val bookingId = UUID.fromString("75ed7091-1767-4901-8c2b-371dd0f5864c")

    every { mockPremisesService.getPremises(premisesId) } returns null

    assertThat(bookingService.getBookingForPremises(premisesId, bookingId))
      .isEqualTo(GetBookingForPremisesResult.PremisesNotFound)
  }

  @Test
  fun `getBookingForPremises returns BookingNotFound when booking with provided ID does not exist`() {
    val premisesId = UUID.fromString("8461d08b-0e3f-426a-a941-0ada4160e6db")
    val bookingId = UUID.fromString("75ed7091-1767-4901-8c2b-371dd0f5864c")

    every { mockPremisesService.getPremises(premisesId) } returns PremisesEntityFactory()
      .withId(premisesId)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    every { mockBookingRepository.findByIdOrNull(bookingId) } returns null

    assertThat(bookingService.getBookingForPremises(premisesId, bookingId))
      .isEqualTo(GetBookingForPremisesResult.BookingNotFound)
  }

  @Test
  fun `getBookingForPremises returns BookingNotFound when booking does not belong to Premises`() {
    val premisesId = UUID.fromString("8461d08b-0e3f-426a-a941-0ada4160e6db")
    val bookingId = UUID.fromString("75ed7091-1767-4901-8c2b-371dd0f5864c")

    val premisesEntityFactory = PremisesEntityFactory()
      .withId(premisesId)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }

    every { mockPremisesService.getPremises(premisesId) } returns premisesEntityFactory.produce()

    val keyWorker = StaffMemberFactory().produce()

    every { mockBookingRepository.findByIdOrNull(bookingId) } returns BookingEntityFactory()
      .withId(bookingId)
      .withPremises(premisesEntityFactory.withId(UUID.randomUUID()).produce())
      .withStaffKeyWorkerId(keyWorker.staffIdentifier)
      .produce()

    assertThat(bookingService.getBookingForPremises(premisesId, bookingId))
      .isEqualTo(GetBookingForPremisesResult.BookingNotFound)
  }

  @Test
  fun `getBookingForPremises returns Success when booking does belong to Premises`() {
    val premisesId = UUID.fromString("8461d08b-0e3f-426a-a941-0ada4160e6db")
    val bookingId = UUID.fromString("75ed7091-1767-4901-8c2b-371dd0f5864c")

    val premisesEntity = PremisesEntityFactory()
      .withId(premisesId)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    every { mockPremisesService.getPremises(premisesId) } returns premisesEntity

    val keyWorker = StaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withId(bookingId)
      .withPremises(premisesEntity)
      .withStaffKeyWorkerId(keyWorker.staffIdentifier)
      .produce()

    every { mockBookingRepository.findByIdOrNull(bookingId) } returns bookingEntity

    assertThat(bookingService.getBookingForPremises(premisesId, bookingId))
      .isEqualTo(GetBookingForPremisesResult.Success(bookingEntity))
  }

  @Test
  fun `createDeparture returns GeneralValidationError with correct message when Booking already has a Departure`() {
    val keyWorker = StaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerId(keyWorker.staffIdentifier)
      .produce()

    val departureEntity = DepartureEntityFactory()
      .withBooking(bookingEntity)
      .withYieldedReason { DepartureReasonEntityFactory().produce() }
      .withYieldedMoveOnCategory { MoveOnCategoryEntityFactory().produce() }
      .withYieldedDestinationProvider { DestinationProviderEntityFactory().produce() }
      .produce()

    bookingEntity.departure = departureEntity

    val result = bookingService.createDeparture(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00"),
      reasonId = UUID.randomUUID(),
      moveOnCategoryId = UUID.randomUUID(),
      destinationProviderId = UUID.randomUUID(),
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
    assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("This Booking already has a Departure set")
  }

  @Test
  fun `createDeparture returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
    val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
    val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
    val destinationProviderId = UUID.fromString("a6f5377e-e0c8-4122-b348-b30ba7e9d7a2")

    val keyWorker = StaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerId(keyWorker.staffIdentifier)
      .produce()

    every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns null
    every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns null
    every { mockDestinationProviderRepository.findByIdOrNull(destinationProviderId) } returns null

    val result = bookingService.createDeparture(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00"),
      reasonId = departureReasonId,
      moveOnCategoryId = moveOnCategoryId,
      destinationProviderId = destinationProviderId,
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.dateTime", "beforeBookingArrivalDate"),
      entry("$.reasonId", "doesNotExist"),
      entry("$.moveOnCategoryId", "doesNotExist"),
      entry("$.destinationProviderId", "doesNotExist")
    )
  }

  @Test
  fun `createDeparture returns Success with correct result when validation passed`() {
    val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
    val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
    val destinationProviderId = UUID.fromString("a6f5377e-e0c8-4122-b348-b30ba7e9d7a2")

    val keyWorker = StaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-22"))
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerId(keyWorker.staffIdentifier)
      .produce()

    val reasonEntity = DepartureReasonEntityFactory().produce()
    val moveOnCategoryEntity = MoveOnCategoryEntityFactory().produce()
    val destinationProviderEntity = DestinationProviderEntityFactory().produce()

    every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns reasonEntity
    every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns moveOnCategoryEntity
    every { mockDestinationProviderRepository.findByIdOrNull(destinationProviderId) } returns destinationProviderEntity

    every { mockDepartureRepository.save(any()) } answers { it.invocation.args[0] as DepartureEntity }

    val result = bookingService.createDeparture(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00"),
      reasonId = departureReasonId,
      moveOnCategoryId = moveOnCategoryId,
      destinationProviderId = destinationProviderId,
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.booking).isEqualTo(bookingEntity)
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-24T15:00:00+01:00"))
    assertThat(result.entity.reason).isEqualTo(reasonEntity)
    assertThat(result.entity.moveOnCategory).isEqualTo(moveOnCategoryEntity)
    assertThat(result.entity.destinationProvider).isEqualTo(destinationProviderEntity)
    assertThat(result.entity.reason).isEqualTo(reasonEntity)
    assertThat(result.entity.notes).isEqualTo("notes")
  }

  @Test
  fun `createArrival returns GeneralValidationError with correct message when Booking already has an Arrival`() {
    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerId(123)
      .produce()

    val arrivalEntity = ArrivalEntityFactory()
      .withBooking(bookingEntity)
      .produce()

    bookingEntity.arrival = arrivalEntity

    val result = bookingService.createArrival(
      booking = bookingEntity,
      arrivalDate = LocalDate.parse("2022-08-25"),
      expectedDepartureDate = LocalDate.parse("2022-08-26"),
      notes = "notes",
      keyWorkerStaffId = 123
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
    assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("This Booking already has an Arrival set")
  }

  @Test
  fun `createArrival returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
    val keyWorker = StaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    val result = bookingService.createArrival(
      booking = bookingEntity,
      arrivalDate = LocalDate.parse("2022-08-27"),
      expectedDepartureDate = LocalDate.parse("2022-08-26"),
      notes = "notes",
      keyWorkerStaffId = keyWorker.staffIdentifier
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.expectedDepartureDate", "beforeBookingArrivalDate")
    )
  }

  @Test
  fun `createArrival returns Success with correct result when validation passed`() {
    val keyWorker = StaffMemberFactory().produce()
    every { mockStaffMemberService.getStaffMemberById(keyWorker.staffIdentifier) } returns AuthorisableActionResult.Success(keyWorker)

    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerId(keyWorker.staffIdentifier)
      .produce()

    every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as ArrivalEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

    val result = bookingService.createArrival(
      booking = bookingEntity,
      arrivalDate = LocalDate.parse("2022-08-27"),
      expectedDepartureDate = LocalDate.parse("2022-08-29"),
      notes = "notes",
      keyWorkerStaffId = keyWorker.staffIdentifier
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-27"))
    assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-29"))
    assertThat(result.entity.notes).isEqualTo("notes")
  }

  @Test
  fun `createNonArrival returns GeneralValidationError with correct message when Booking already has a NonArrival`() {
    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    val nonArrivalEntity = NonArrivalEntityFactory()
      .withBooking(bookingEntity)
      .withYieldedReason { NonArrivalReasonEntityFactory().produce() }
      .produce()

    bookingEntity.nonArrival = nonArrivalEntity

    val result = bookingService.createNonArrival(
      booking = bookingEntity,
      date = LocalDate.parse("2022-08-25"),
      reasonId = UUID.randomUUID(),
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
    assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("This Booking already has a Non Arrival set")
  }

  @Test
  fun `createNonArrival returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
    val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-26"))
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { mockNonArrivalReasonRepository.findByIdOrNull(reasonId) } returns null

    val result = bookingService.createNonArrival(
      booking = bookingEntity,
      date = LocalDate.parse("2022-08-25"),
      reasonId = reasonId,
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.date", "afterBookingArrivalDate"),
      entry("$.reason", "doesNotExist")
    )
  }

  @Test
  fun `createNonArrival returns Success with correct result when validation passed`() {
    val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-24"))
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    val reasonEntity = NonArrivalReasonEntityFactory().produce()

    every { mockNonArrivalReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
    every { mockNonArrivalRepository.save(any()) } answers { it.invocation.args[0] as NonArrivalEntity }

    val result = bookingService.createNonArrival(
      booking = bookingEntity,
      date = LocalDate.parse("2022-08-25"),
      reasonId = reasonId,
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
    assertThat(result.entity.reason).isEqualTo(reasonEntity)
    assertThat(result.entity.notes).isEqualTo("notes")
  }

  @Test
  fun `createCancellation returns GeneralValidationError with correct message when Booking already has a Cancellation`() {
    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    val cancellationEntity = CancellationEntityFactory()
      .withBooking(bookingEntity)
      .withYieldedReason { CancellationReasonEntityFactory().produce() }
      .produce()

    bookingEntity.cancellation = cancellationEntity

    val result = bookingService.createCancellation(
      booking = bookingEntity,
      date = LocalDate.parse("2022-08-25"),
      reasonId = UUID.randomUUID(),
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
    assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("This Booking already has a Cancellation set")
  }

  @Test
  fun `createCancellation returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
    val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-26"))
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns null

    val result = bookingService.createCancellation(
      booking = bookingEntity,
      date = LocalDate.parse("2022-08-25"),
      reasonId = reasonId,
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.reason", "doesNotExist")
    )
  }

  @Test
  fun `createCancellation returns Success with correct result when validation passed`() {
    val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    val reasonEntity = CancellationReasonEntityFactory().produce()

    every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
    every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }

    val result = bookingService.createCancellation(
      booking = bookingEntity,
      date = LocalDate.parse("2022-08-25"),
      reasonId = reasonId,
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
    assertThat(result.entity.reason).isEqualTo(reasonEntity)
    assertThat(result.entity.notes).isEqualTo("notes")
  }

  @Test
  fun `createExtension returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
    val bookingEntity = BookingEntityFactory()
      .withDepartureDate(LocalDate.parse("2022-08-26"))
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    val result = bookingService.createExtension(
      booking = bookingEntity,
      newDepartureDate = LocalDate.parse("2022-08-25"),
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.newDepartureDate", "beforeExistingDepartureDate")
    )
  }

  @Test
  fun `createExtension returns Success with correct result when validation passed`() {
    val bookingEntity = BookingEntityFactory()
      .withDepartureDate(LocalDate.parse("2022-08-24"))
      .withYieldedPremises {
        PremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    every { mockExtensionRepository.save(any()) } answers { it.invocation.args[0] as ExtensionEntity }

    val result = bookingService.createExtension(
      booking = bookingEntity,
      newDepartureDate = LocalDate.parse("2022-08-25"),
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.newDepartureDate).isEqualTo(LocalDate.parse("2022-08-25"))
    assertThat(result.entity.previousDepartureDate).isEqualTo(LocalDate.parse("2022-08-24"))
    assertThat(result.entity.notes).isEqualTo("notes")
  }
}
