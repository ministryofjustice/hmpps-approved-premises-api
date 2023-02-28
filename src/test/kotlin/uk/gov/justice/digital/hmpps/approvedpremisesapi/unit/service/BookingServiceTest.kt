package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ConfirmationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DestinationProviderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffWithoutUsernameUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationRepository
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CruService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class BookingServiceTest {
  private val mockPremisesService = mockk<PremisesService>()
  private val mockStaffMemberService = mockk<StaffMemberService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockDomainEventService = mockk<DomainEventService>()
  private val mockCruService = mockk<CruService>()
  private val mockApplicationService = mockk<ApplicationService>()
  private val mockCommunityApiClient = mockk<CommunityApiClient>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockArrivalRepository = mockk<ArrivalRepository>()
  private val mockCancellationRepository = mockk<CancellationRepository>()
  private val mockConfirmationRepository = mockk<ConfirmationRepository>()
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
    offenderService = mockOffenderService,
    domainEventService = mockDomainEventService,
    cruService = mockCruService,
    applicationService = mockApplicationService,
    communityApiClient = mockCommunityApiClient,
    bookingRepository = mockBookingRepository,
    arrivalRepository = mockArrivalRepository,
    cancellationRepository = mockCancellationRepository,
    confirmationRepository = mockConfirmationRepository,
    extensionRepository = mockExtensionRepository,
    departureRepository = mockDepartureRepository,
    nonArrivalRepository = mockNonArrivalRepository,
    departureReasonRepository = mockDepartureReasonRepository,
    moveOnCategoryRepository = mockMoveOnCategoryRepository,
    destinationProviderRepository = mockDestinationProviderRepository,
    nonArrivalReasonRepository = mockNonArrivalReasonRepository,
    cancellationReasonRepository = mockCancellationReasonRepository,
    applicationUrlTemplate = "http://frontend/applications/#id"
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

    every { mockPremisesService.getPremises(premisesId) } returns ApprovedPremisesEntityFactory()
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

    val premisesEntityFactory = ApprovedPremisesEntityFactory()
      .withId(premisesId)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }

    every { mockPremisesService.getPremises(premisesId) } returns premisesEntityFactory.produce()

    val keyWorker = ContextStaffMemberFactory().produce()

    every { mockBookingRepository.findByIdOrNull(bookingId) } returns BookingEntityFactory()
      .withId(bookingId)
      .withPremises(premisesEntityFactory.withId(UUID.randomUUID()).produce())
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    assertThat(bookingService.getBookingForPremises(premisesId, bookingId))
      .isEqualTo(GetBookingForPremisesResult.BookingNotFound)
  }

  @Test
  fun `getBookingForPremises returns Success when booking does belong to Premises`() {
    val premisesId = UUID.fromString("8461d08b-0e3f-426a-a941-0ada4160e6db")
    val bookingId = UUID.fromString("75ed7091-1767-4901-8c2b-371dd0f5864c")

    val premisesEntity = ApprovedPremisesEntityFactory()
      .withId(premisesId)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    every { mockPremisesService.getPremises(premisesId) } returns premisesEntity

    val keyWorker = ContextStaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withId(bookingId)
      .withPremises(premisesEntity)
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    every { mockBookingRepository.findByIdOrNull(bookingId) } returns bookingEntity

    assertThat(bookingService.getBookingForPremises(premisesId, bookingId))
      .isEqualTo(GetBookingForPremisesResult.Success(bookingEntity))
  }

  @Test
  fun `createDeparture returns GeneralValidationError with correct message when Booking already has a Departure`() {
    val keyWorker = ContextStaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode(keyWorker.code)
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
  fun `createDeparture returns FieldValidationError with correct param to message map when dateTime in past supplied`() {
    val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
    val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
    val destinationProviderId = UUID.fromString("a6f5377e-e0c8-4122-b348-b30ba7e9d7a2")

    val keyWorker = ContextStaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns DepartureReasonEntityFactory().produce()
    every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns MoveOnCategoryEntityFactory().produce()
    every { mockDestinationProviderRepository.findByIdOrNull(destinationProviderId) } returns DestinationProviderEntityFactory().produce()

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
      entry("$.dateTime", "beforeBookingArrivalDate")
    )
  }

  @Test
  fun `createDeparture returns FieldValidationError with correct param to message map when invalid departure reason supplied`() {
    val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
    val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
    val destinationProviderId = UUID.fromString("a6f5377e-e0c8-4122-b348-b30ba7e9d7a2")

    val keyWorker = ContextStaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns null
    every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns MoveOnCategoryEntityFactory().produce()
    every { mockDestinationProviderRepository.findByIdOrNull(destinationProviderId) } returns DestinationProviderEntityFactory().produce()

    val result = bookingService.createDeparture(
      booking = bookingEntity,
      dateTime = OffsetDateTime.now().minusMinutes(1),
      reasonId = departureReasonId,
      moveOnCategoryId = moveOnCategoryId,
      destinationProviderId = destinationProviderId,
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.reasonId", "doesNotExist")
    )
  }

  @Test
  fun `createDeparture returns FieldValidationError with correct param to message map when the departure reason has the wrong service scope`() {
    val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
    val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
    val destinationProviderId = UUID.fromString("a6f5377e-e0c8-4122-b348-b30ba7e9d7a2")

    val keyWorker = ContextStaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns DepartureReasonEntityFactory()
      .withServiceScope(ServiceName.temporaryAccommodation.value)
      .produce()
    every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns MoveOnCategoryEntityFactory()
      .withServiceScope("*")
      .produce()
    every { mockDestinationProviderRepository.findByIdOrNull(destinationProviderId) } returns DestinationProviderEntityFactory().produce()

    val result = bookingService.createDeparture(
      booking = bookingEntity,
      dateTime = OffsetDateTime.now().minusMinutes(1),
      reasonId = departureReasonId,
      moveOnCategoryId = moveOnCategoryId,
      destinationProviderId = destinationProviderId,
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.reasonId", "incorrectDepartureReasonServiceScope")
    )
  }

  @Test
  fun `createDeparture returns FieldValidationError with correct param to message map when invalid move on category supplied`() {
    val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
    val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
    val destinationProviderId = UUID.fromString("a6f5377e-e0c8-4122-b348-b30ba7e9d7a2")

    val keyWorker = ContextStaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns DepartureReasonEntityFactory().produce()
    every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns null
    every { mockDestinationProviderRepository.findByIdOrNull(destinationProviderId) } returns DestinationProviderEntityFactory().produce()

    val result = bookingService.createDeparture(
      booking = bookingEntity,
      dateTime = OffsetDateTime.now().plusMinutes(1),
      reasonId = departureReasonId,
      moveOnCategoryId = moveOnCategoryId,
      destinationProviderId = destinationProviderId,
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.moveOnCategoryId", "doesNotExist")
    )
  }

  @Test
  fun `createDeparture returns FieldValidationError with correct param to message map when the move-on category has the wrong service scope`() {
    val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
    val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
    val destinationProviderId = UUID.fromString("a6f5377e-e0c8-4122-b348-b30ba7e9d7a2")

    val keyWorker = ContextStaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withServiceName(ServiceName.temporaryAccommodation)
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns DepartureReasonEntityFactory()
      .withServiceScope("*")
      .produce()
    every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns MoveOnCategoryEntityFactory()
      .withServiceScope(ServiceName.approvedPremises.value)
      .produce()
    every { mockDestinationProviderRepository.findByIdOrNull(destinationProviderId) } returns DestinationProviderEntityFactory().produce()

    val result = bookingService.createDeparture(
      booking = bookingEntity,
      dateTime = OffsetDateTime.now().plusMinutes(1),
      reasonId = departureReasonId,
      moveOnCategoryId = moveOnCategoryId,
      destinationProviderId = destinationProviderId,
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.moveOnCategoryId", "incorrectMoveOnCategoryServiceScope")
    )
  }

  @Test
  fun `createDeparture returns FieldValidationError with correct param to message map when invalid destination provider supplied`() {
    val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
    val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
    val destinationProviderId = UUID.fromString("a6f5377e-e0c8-4122-b348-b30ba7e9d7a2")

    val keyWorker = ContextStaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns DepartureReasonEntityFactory().produce()
    every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns MoveOnCategoryEntityFactory().produce()
    every { mockDestinationProviderRepository.findByIdOrNull(destinationProviderId) } returns null

    val result = bookingService.createDeparture(
      booking = bookingEntity,
      dateTime = OffsetDateTime.now().plusMinutes(1),
      reasonId = departureReasonId,
      moveOnCategoryId = moveOnCategoryId,
      destinationProviderId = destinationProviderId,
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.destinationProviderId", "doesNotExist")
    )
  }

  @Test
  fun `createDeparture for an Approved Premises booking returns FieldValidationError with correct param to message map when the destination provider is empty`() {
    val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
    val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")

    val keyWorker = ContextStaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns DepartureReasonEntityFactory().produce()
    every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns MoveOnCategoryEntityFactory().produce()
    every { mockDestinationProviderRepository.findByIdOrNull(any()) } returns null

    val result = bookingService.createDeparture(
      booking = bookingEntity,
      dateTime = OffsetDateTime.now().plusMinutes(1),
      reasonId = departureReasonId,
      moveOnCategoryId = moveOnCategoryId,
      destinationProviderId = null,
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.destinationProviderId", "empty")
    )
  }

  @Test
  fun `createDeparture returns Success with correct result when validation passed`() {
    val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
    val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
    val destinationProviderId = UUID.fromString("a6f5377e-e0c8-4122-b348-b30ba7e9d7a2")

    val keyWorker = ContextStaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-22"))
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    val reasonEntity = DepartureReasonEntityFactory()
      .withServiceScope("approved-premises")
      .produce()
    val moveOnCategoryEntity = MoveOnCategoryEntityFactory()
      .withServiceScope("approved-premises")
      .produce()
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
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode("123")
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
      keyWorkerStaffCode = "123",
      user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
    assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("This Booking already has an Arrival set")
  }

  @Test
  fun `createArrival returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
    val keyWorker = ContextStaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
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
      keyWorkerStaffCode = keyWorker.code,
      user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.expectedDepartureDate", "beforeBookingArrivalDate")
    )
  }

  @Test
  fun `createArrival returns Success with correct result when validation passed, does not save Domain Event when associated with Offline Application as Event Number is not present`() {
    val keyWorker = ContextStaffMemberFactory().produce()
    every { mockStaffMemberService.getStaffMemberByCode(keyWorker.code, "QCODE") } returns AuthorisableActionResult.Success(keyWorker)

    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withQCode("QCODE")
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode(keyWorker.code)
      .withOfflineApplication(OfflineApplicationEntityFactory().produce())
      .produce()

    every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as ArrivalEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

    val result = bookingService.createArrival(
      booking = bookingEntity,
      arrivalDate = LocalDate.parse("2022-08-27"),
      expectedDepartureDate = LocalDate.parse("2022-08-29"),
      notes = "notes",
      keyWorkerStaffCode = keyWorker.code,
      user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-27"))
    assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-29"))
    assertThat(result.entity.notes).isEqualTo("notes")

    verify(exactly = 0) { mockDomainEventService.savePersonArrivedEvent(any()) }
  }

  @Test
  fun `createArrival returns Success with correct result when validation passed, saves Domain Event when associated with Online Application`() {
    val keyWorker = ContextStaffMemberFactory().produce()
    every { mockStaffMemberService.getStaffMemberByCode(keyWorker.code, "QCODE") } returns AuthorisableActionResult.Success(keyWorker)

    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withQCode("QCODE")
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode(keyWorker.code)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory()
              .withUnitTestControlProbationRegion()
              .produce()
          )
          .produce()
      )
      .produce()

    every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as ArrivalEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(bookingEntity.crn)
      .produce()

    every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)

    val keyWorkerStaffUserDetails = StaffWithoutUsernameUserDetailsFactory().produce()

    every { mockCommunityApiClient.getStaffUserDetailsForStaffCode(keyWorker.code) } returns ClientResult.Success(
      HttpStatus.OK,
      keyWorkerStaffUserDetails
    )

    every { mockDomainEventService.savePersonArrivedEvent(any()) } just Runs

    val result = bookingService.createArrival(
      booking = bookingEntity,
      arrivalDate = LocalDate.parse("2022-08-27"),
      expectedDepartureDate = LocalDate.parse("2022-08-29"),
      notes = "notes",
      keyWorkerStaffCode = keyWorker.code,
      user = user
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-27"))
    assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-29"))
    assertThat(result.entity.notes).isEqualTo("notes")

    verify(exactly = 1) {
      mockDomainEventService.savePersonArrivedEvent(
        match {
          val data = (it.data as PersonArrivedEnvelope).eventDetails
          val application = bookingEntity.application as ApprovedPremisesApplicationEntity
          val approvedPremises = bookingEntity.premises as ApprovedPremisesEntity

          it.applicationId == application.id &&
            it.crn == bookingEntity.crn &&
            data.applicationId == application.id &&
            data.applicationUrl == "http://frontend/applications/${application.id}" &&
            data.personReference == PersonReference(
            crn = offenderDetails.otherIds.crn,
            noms = offenderDetails.otherIds.nomsNumber!!
          ) &&
            data.deliusEventNumber == application.eventNumber &&
            data.premises == Premises(
            id = approvedPremises.id,
            name = approvedPremises.name,
            apCode = approvedPremises.apCode,
            legacyApCode = approvedPremises.qCode,
            localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name
          )
        }
      )
    }
  }

  @Test
  fun `createArrival returns Success with correct result for Temporary Accommodation when validation passed`() {
    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode(null)
      .produce()

    every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as ArrivalEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

    val result = bookingService.createArrival(
      booking = bookingEntity,
      arrivalDate = LocalDate.parse("2022-08-27"),
      expectedDepartureDate = LocalDate.parse("2022-08-29"),
      notes = "notes",
      keyWorkerStaffCode = null,
      user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-27"))
    assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-29"))
    assertThat(result.entity.notes).isEqualTo("notes")

    verify(exactly = 0) { mockStaffMemberService.getStaffMemberByCode(any(), any()) }
  }

  @Test
  fun `createNonArrival returns GeneralValidationError with correct message when Booking already has a NonArrival`() {
    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
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
      user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce(),
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
        ApprovedPremisesEntityFactory()
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
      user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce(),
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
  fun `createNonArrival returns Success with correct result when validation passed, does not save Domain Event when associated with Offline Application as Event Number is not present`() {
    val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-24"))
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withOfflineApplication(OfflineApplicationEntityFactory().produce())
      .produce()

    val reasonEntity = NonArrivalReasonEntityFactory().produce()

    every { mockNonArrivalReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
    every { mockNonArrivalRepository.save(any()) } answers { it.invocation.args[0] as NonArrivalEntity }

    val result = bookingService.createNonArrival(
      user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce(),
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

    verify(exactly = 0) {
      mockDomainEventService.savePersonNotArrivedEvent(any())
    }
  }

  @Test
  fun `createNonArrival returns Success with correct result when validation passed, saves Domain Event when associated with Online Application`() {
    val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .produce()

    val bookingEntity = BookingEntityFactory()
      .withCrn(application.crn)
      .withArrivalDate(LocalDate.parse("2022-08-24"))
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withApplication(application)
      .produce()

    val reasonEntity = NonArrivalReasonEntityFactory().produce()

    every { mockNonArrivalReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
    every { mockNonArrivalRepository.save(any()) } answers { it.invocation.args[0] as NonArrivalEntity }

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(bookingEntity.crn)
      .produce()

    every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)

    val staffUserDetails = StaffUserDetailsFactory().produce()

    every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
      HttpStatus.OK,
      staffUserDetails
    )

    every { mockDomainEventService.savePersonNotArrivedEvent(any()) } just Runs

    val result = bookingService.createNonArrival(
      user = user,
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

    verify(exactly = 1) {
      mockDomainEventService.savePersonNotArrivedEvent(
        match {
          val data = (it.data as PersonNotArrivedEnvelope).eventDetails
          val application = bookingEntity.application as ApprovedPremisesApplicationEntity
          val approvedPremises = bookingEntity.premises as ApprovedPremisesEntity

          it.applicationId == application.id &&
            it.crn == bookingEntity.crn &&
            data.applicationId == application.id &&
            data.applicationUrl == "http://frontend/applications/${application.id}" &&
            data.personReference == PersonReference(
            crn = offenderDetails.otherIds.crn,
            noms = offenderDetails.otherIds.nomsNumber!!
          ) &&
            data.deliusEventNumber == application.eventNumber &&
            data.premises == Premises(
            id = approvedPremises.id,
            name = approvedPremises.name,
            apCode = approvedPremises.apCode,
            legacyApCode = approvedPremises.qCode,
            localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name
          ) &&
            data.expectedArrivalOn == bookingEntity.originalArrivalDate &&
            data.recordedBy == StaffMember(
            staffCode = staffUserDetails.staffCode,
            staffIdentifier = staffUserDetails.staffIdentifier,
            forenames = staffUserDetails.staff.forenames,
            surname = staffUserDetails.staff.surname,
            username = staffUserDetails.username
          )
        }
      )
    }
  }

  @Test
  fun `createCancellation returns GeneralValidationError with correct message when Booking already has a Cancellation`() {
    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
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
        ApprovedPremisesEntityFactory()
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
  fun `createCancellation returns FieldValidationError with correct param to message map when the cancellation reason has the wrong service scope`() {
    val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withServiceName(ServiceName.approvedPremises)
      .produce()

    val reasonEntity = CancellationReasonEntityFactory()
      .withServiceScope(ServiceName.temporaryAccommodation.value)
      .produce()

    every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
    every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }

    val result = bookingService.createCancellation(
      booking = bookingEntity,
      date = LocalDate.parse("2022-08-25"),
      reasonId = reasonId,
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.reason", "incorrectCancellationReasonServiceScope")
    )
  }

  @Test
  fun `createCancellation returns Success with correct result when validation passed`() {
    val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

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
  fun `createExtension returns FieldValidationError with correct param to message map when an Approved Premises booking has a new departure date before the existing departure date`() {
    val bookingEntity = BookingEntityFactory()
      .withDepartureDate(LocalDate.parse("2022-08-26"))
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
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
  fun `createExtension returns Success with correct result when a Temporary Accommodation booking has a new departure date before the existing departure date`() {
    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-10"))
      .withDepartureDate(LocalDate.parse("2022-08-26"))
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withServiceName(ServiceName.temporaryAccommodation)
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
    assertThat(result.entity.previousDepartureDate).isEqualTo(LocalDate.parse("2022-08-26"))
    assertThat(result.entity.notes).isEqualTo("notes")
  }

  @Test
  fun `createExtension returns FieldValidationError with correct param to message map when a Temporary Accommodation booking has a new departure date before the arrival date`() {
    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-26"))
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withServiceName(ServiceName.temporaryAccommodation)
      .produce()

    val result = bookingService.createExtension(
      booking = bookingEntity,
      newDepartureDate = LocalDate.parse("2022-08-25"),
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.newDepartureDate", "beforeBookingArrivalDate")
    )
  }

  @Test
  fun `createExtension returns Success with correct result when validation passed`() {
    val bookingEntity = BookingEntityFactory()
      .withDepartureDate(LocalDate.parse("2022-08-24"))
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
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

  @Test
  fun `createConfirmation returns GeneralValidationError with correct message when Booking already has a Confirmation`() {
    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withService(ServiceName.temporaryAccommodation.value)
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    val confirmationEntity = ConfirmationEntityFactory()
      .withBooking(bookingEntity)
      .produce()

    bookingEntity.confirmation = confirmationEntity

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
    assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("This Booking already has a Confirmation set")
  }

  @Test
  fun `createConfirmation returns Success with correct result when validation passed`() {
    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withService(ServiceName.temporaryAccommodation.value)
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { mockConfirmationRepository.save(any()) } answers { it.invocation.args[0] as ConfirmationEntity }

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
    assertThat(result.entity.notes).isEqualTo("notes")
  }

  @Test
  fun `createApprovedPremisesBooking returns Unauthorised if user does not have either MANAGER or MATCHER role`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val result = bookingService.createApprovedPremisesBooking(user, premises, "CRN", LocalDate.parse("2023-02-22"), LocalDate.parse("2023-02-24"))

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["MANAGER", "MATCHER"])
  fun `createApprovedPremisesBooking returns FieldValidationError if Departure Date is before Arrival Date`(role: UserRole) {
    val crn = "CRN123"

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addRoleForUnitTest(role)

    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val existingApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    every { mockApplicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises) } returns listOf(existingApplication)
    every { mockApplicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()

    val authorisableResult = bookingService.createApprovedPremisesBooking(user, premises, crn, LocalDate.parse("2023-02-23"), LocalDate.parse("2023-02-22"))
    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

    val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
    assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

    assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.departureDate", "beforeBookingArrivalDate")
    )
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["MANAGER", "MATCHER"])
  fun `createApprovedPremisesBooking returns FieldValidationError if there are no existing Applications for the CRN`(role: UserRole) {
    val crn = "CRN123"

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addRoleForUnitTest(role)

    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    every { mockApplicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()
    every { mockApplicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()

    val authorisableResult = bookingService.createApprovedPremisesBooking(user, premises, crn, LocalDate.parse("2023-02-22"), LocalDate.parse("2023-02-23"))
    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

    val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
    assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

    assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.crn", "doesNotHaveApplication")
    )
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["MANAGER", "MATCHER"])
  fun `createApprovedPremisesBooking throws if unable to get Offender Details`(role: UserRole) {
    val crn = "CRN123"

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addRoleForUnitTest(role)

    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val existingApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    every { mockApplicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises) } returns listOf(existingApplication)
    every { mockApplicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()

    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    every { mockOffenderService.getOffenderByCrn(crn, user.deliusUsername) } returns AuthorisableActionResult.Unauthorised()

    val runtimeException = assertThrows<RuntimeException> {
      bookingService.createApprovedPremisesBooking(user, premises, crn, LocalDate.parse("2023-02-22"), LocalDate.parse("2023-02-23"))
    }

    assertThat(runtimeException.message).isEqualTo("Unable to get Offender Details when creating Booking Made Domain Event: Unauthorised")
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["MANAGER", "MATCHER"])
  fun `createApprovedPremisesBooking throws if unable to get Staff Details`(role: UserRole) {
    val crn = "CRN123"

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addRoleForUnitTest(role)

    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val existingApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    every { mockApplicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises) } returns listOf(existingApplication)
    every { mockApplicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .produce()

    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    every { mockOffenderService.getOffenderByCrn(crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)
    every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/staff-details/${user.deliusUsername}", HttpStatus.NOT_FOUND, null)

    val runtimeException = assertThrows<RuntimeException> {
      bookingService.createApprovedPremisesBooking(user, premises, crn, LocalDate.parse("2023-02-22"), LocalDate.parse("2023-02-23"))
    }

    assertThat(runtimeException.message).isEqualTo("Unable to complete GET request to /staff-details/${user.deliusUsername}: 404 NOT_FOUND")
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["MANAGER", "MATCHER"])
  fun `createApprovedPremisesBooking saves Booking and creates Domain Event when associated Application is an Online Application`(role: UserRole) {
    val crn = "CRN123"
    val arrivalDate = LocalDate.parse("2023-02-22")
    val departureDate = LocalDate.parse("2023-02-23")

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addRoleForUnitTest(role)

    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val existingApplication = ApprovedPremisesApplicationEntityFactory()
      .withCrn(crn)
      .withCreatedByUser(user)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    every { mockApplicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises) } returns listOf(existingApplication)
    every { mockApplicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .produce()

    val staffUserDetails = StaffUserDetailsFactory()
      .withUsername(user.deliusUsername)
      .produce()

    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    every { mockOffenderService.getOffenderByCrn(crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)
    every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)
    every { mockCruService.cruNameFromProbationAreaCode(staffUserDetails.probationArea.code) } returns "CRU NAME"
    every { mockDomainEventService.saveBookingMadeDomainEvent(any()) } just Runs

    val authorisableResult = bookingService.createApprovedPremisesBooking(user, premises, crn, arrivalDate, departureDate)
    assertThat(authorisableResult is AuthorisableActionResult.Success)
    val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
    assertThat(validatableResult is ValidatableActionResult.Success)

    verify(exactly = 1) {
      mockBookingRepository.save(
        match {
          it.crn == crn &&
            it.premises == premises &&
            it.arrivalDate == arrivalDate &&
            it.departureDate == departureDate
        }
      )
    }

    verify(exactly = 1) {
      mockDomainEventService.saveBookingMadeDomainEvent(
        match {
          val data = (it.data as BookingMadeEnvelope).eventDetails

          it.applicationId == existingApplication.id &&
            it.crn == crn &&
            data.applicationId == existingApplication.id &&
            data.applicationUrl == "http://frontend/applications/${existingApplication.id}" &&
            data.personReference == PersonReference(
            crn = offenderDetails.otherIds.crn,
            noms = offenderDetails.otherIds.nomsNumber!!
          ) &&
            data.deliusEventNumber == existingApplication.eventNumber &&
            data.premises == Premises(
            id = premises.id,
            name = premises.name,
            apCode = premises.apCode,
            legacyApCode = premises.qCode,
            localAuthorityAreaName = premises.localAuthorityArea!!.name
          ) &&
            data.arrivalOn == arrivalDate
        }
      )
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["MANAGER", "MATCHER"])
  fun `createApprovedPremisesBooking saves Booking but does not create Domain Event when associated Application is an Offline Application as Event Number is not present`(role: UserRole) {
    val crn = "CRN123"
    val arrivalDate = LocalDate.parse("2023-02-22")
    val departureDate = LocalDate.parse("2023-02-23")

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addRoleForUnitTest(role)

    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val existingApplication = OfflineApplicationEntityFactory()
      .withCrn(crn)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    every { mockApplicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()
    every { mockApplicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises) } returns listOf(existingApplication)

    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

    val authorisableResult = bookingService.createApprovedPremisesBooking(user, premises, crn, arrivalDate, departureDate)
    assertThat(authorisableResult is AuthorisableActionResult.Success)
    val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
    assertThat(validatableResult is ValidatableActionResult.Success)

    verify(exactly = 1) {
      mockBookingRepository.save(
        match {
          it.crn == crn &&
            it.premises == premises &&
            it.arrivalDate == arrivalDate &&
            it.departureDate == departureDate
        }
      )
    }

    verify(exactly = 0) {
      mockDomainEventService.saveBookingMadeDomainEvent(any())
    }
  }

  @Test
  fun `createTemporaryAccommodationBooking returns FieldValidationError if Departure Date is before Arrival Date`() {
    val crn = "CRN123"
    val bedId = UUID.fromString("3b2f46de-a785-45ab-ac02-5e532c600647")

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val authorisableResult = bookingService.createTemporaryAccommodationBooking(user, premises, crn, LocalDate.parse("2023-02-23"), LocalDate.parse("2023-02-22"), bedId)
    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

    val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
    assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

    assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.departureDate", "beforeBookingArrivalDate")
    )
  }

  @Test
  fun `createTemporaryAccommodationBooking returns FieldValidationError if Bed does not exist`() {
    val crn = "CRN123"
    val bedId = UUID.fromString("3b2f46de-a785-45ab-ac02-5e532c600647")

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val authorisableResult = bookingService.createTemporaryAccommodationBooking(user, premises, crn, LocalDate.parse("2023-02-23"), LocalDate.parse("2023-02-22"), bedId)
    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

    val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
    assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

    assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.bedId", "doesNotExist")
    )
  }

  @Test
  fun `createTemporaryAccommodationBooking saves Booking`() {
    val crn = "CRN123"
    val arrivalDate = LocalDate.parse("2023-02-23")
    val departureDate = LocalDate.parse("2023-02-24")

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    room.beds += bed
    premises.rooms += room

    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

    val authorisableResult = bookingService.createTemporaryAccommodationBooking(user, premises, crn, arrivalDate, departureDate, bed.id)
    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

    val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
    assertThat(validatableResult is ValidatableActionResult.Success)

    verify(exactly = 1) {
      mockBookingRepository.save(
        match {
          it.crn == crn &&
            it.premises == premises &&
            it.arrivalDate == arrivalDate &&
            it.departureDate == departureDate
        }
      )
    }
  }
}
