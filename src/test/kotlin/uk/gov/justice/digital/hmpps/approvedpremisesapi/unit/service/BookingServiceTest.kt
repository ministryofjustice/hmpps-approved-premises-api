package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Called
import io.mockk.Runs
import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import io.sentry.Sentry
import io.sentry.protocol.SentryId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.DestinationProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffWithoutUsernameUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedMoveEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedMoveRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CruService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayCountService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService as Cas3DomainEventService

class BookingServiceTest {
  private val mockPremisesService = mockk<PremisesService>()
  private val mockStaffMemberService = mockk<StaffMemberService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockDomainEventService = mockk<DomainEventService>()
  private val mockCas3DomainEventService = mockk<Cas3DomainEventService>()
  private val mockCruService = mockk<CruService>()
  private val mockApplicationService = mockk<ApplicationService>()
  private val mockWorkingDayCountService = mockk<WorkingDayCountService>()

  private val mockPlacementRequestService = mockk<PlacementRequestService>()
  private val mockCommunityApiClient = mockk<CommunityApiClient>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockArrivalRepository = mockk<ArrivalRepository>()
  private val mockCancellationRepository = mockk<CancellationRepository>()
  private val mockConfirmationRepository = mockk<ConfirmationRepository>()
  private val mockExtensionRepository = mockk<ExtensionRepository>()
  private val mockDateChangeRepository = mockk<DateChangeRepository>()
  private val mockDepartureRepository = mockk<DepartureRepository>()
  private val mockNonArrivalRepository = mockk<NonArrivalRepository>()
  private val mockDepartureReasonRepository = mockk<DepartureReasonRepository>()
  private val mockMoveOnCategoryRepository = mockk<MoveOnCategoryRepository>()
  private val mockDestinationProviderRepository = mockk<DestinationProviderRepository>()
  private val mockNonArrivalReasonRepository = mockk<NonArrivalReasonRepository>()
  private val mockCancellationReasonRepository = mockk<CancellationReasonRepository>()
  private val mockBedRepository = mockk<BedRepository>()
  private val mockPlacementRequestRepository = mockk<PlacementRequestRepository>()
  private val mockLostBedsRepository = mockk<LostBedsRepository>()
  private val mockTurnaroundRepository = mockk<TurnaroundRepository>()
  private val mockBedMoveRepository = mockk<BedMoveRepository>()
  private val mockPremisesRepository = mockk<PremisesRepository>()
  private val mockAssessmentRepository = mockk<AssessmentRepository>()
  private val mockUserService = mockk<UserService>()
  private val mockUserAccessService = mockk<UserAccessService>()
  private val mockAssessmentService = mockk<AssessmentService>()
  private val mockCas1BookingEmailService = mockk<Cas1BookingEmailService>()

  fun createBookingService(arrivedAndDepartedDomainEventsDisabled: Boolean): BookingService {
    return BookingService(
      premisesService = mockPremisesService,
      staffMemberService = mockStaffMemberService,
      offenderService = mockOffenderService,
      domainEventService = mockDomainEventService,
      cas3DomainEventService = mockCas3DomainEventService,
      cruService = mockCruService,
      applicationService = mockApplicationService,
      workingDayCountService = mockWorkingDayCountService,
      placementRequestService = mockPlacementRequestService,
      communityApiClient = mockCommunityApiClient,
      bookingRepository = mockBookingRepository,
      arrivalRepository = mockArrivalRepository,
      cancellationRepository = mockCancellationRepository,
      confirmationRepository = mockConfirmationRepository,
      extensionRepository = mockExtensionRepository,
      dateChangeRepository = mockDateChangeRepository,
      departureRepository = mockDepartureRepository,
      nonArrivalRepository = mockNonArrivalRepository,
      departureReasonRepository = mockDepartureReasonRepository,
      moveOnCategoryRepository = mockMoveOnCategoryRepository,
      destinationProviderRepository = mockDestinationProviderRepository,
      nonArrivalReasonRepository = mockNonArrivalReasonRepository,
      cancellationReasonRepository = mockCancellationReasonRepository,
      bedRepository = mockBedRepository,
      placementRequestRepository = mockPlacementRequestRepository,
      lostBedsRepository = mockLostBedsRepository,
      turnaroundRepository = mockTurnaroundRepository,
      bedMoveRepository = mockBedMoveRepository,
      premisesRepository = mockPremisesRepository,
      assessmentRepository = mockAssessmentRepository,
      applicationUrlTemplate = "http://frontend/applications/#id",
      arrivedAndDepartedDomainEventsDisabled = arrivedAndDepartedDomainEventsDisabled,
      userService = mockUserService,
      userAccessService = mockUserAccessService,
      assessmentService = mockAssessmentService,
      cas1BookingEmailService = mockCas1BookingEmailService,
    )
  }

  private val bookingService = createBookingService(arrivedAndDepartedDomainEventsDisabled = false)
  private val bookingServiceWithArrivedAndDepartedDomainEventsDisabled = createBookingService(arrivedAndDepartedDomainEventsDisabled = true)

  private val user = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .produce()

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

  @Nested
  inner class CreateDeparture {
    val premises = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val keyWorker = ContextStaffMemberFactory().produce()

    private val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withPremises(premises)
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    private val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
    private val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
    private val destinationProviderId = UUID.fromString("a6f5377e-e0c8-4122-b348-b30ba7e9d7a2")

    private val reasonEntity = DepartureReasonEntityFactory()
      .withServiceScope("approved-premises")
      .produce()
    private val moveOnCategoryEntity = MoveOnCategoryEntityFactory()
      .withServiceScope("approved-premises")
      .produce()
    private val destinationProviderEntity = DestinationProviderEntityFactory().produce()

    @BeforeEach
    fun setup() {
      every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns reasonEntity
      every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns moveOnCategoryEntity
      every { mockDestinationProviderRepository.findByIdOrNull(destinationProviderId) } returns destinationProviderEntity

      every { mockDepartureRepository.save(any()) } answers { it.invocation.args[0] as DepartureEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    }

    @Test
    fun `createDeparture returns GeneralValidationError with correct message when Booking already has a Departure`() {
      val departureEntity = DepartureEntityFactory()
        .withBooking(bookingEntity)
        .withYieldedReason { DepartureReasonEntityFactory().produce() }
        .withYieldedMoveOnCategory { MoveOnCategoryEntityFactory().produce() }
        .withYieldedDestinationProvider { DestinationProviderEntityFactory().produce() }
        .produce()

      bookingEntity.departures = mutableListOf(departureEntity)

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00"),
        reasonId = UUID.randomUUID(),
        moveOnCategoryId = UUID.randomUUID(),
        destinationProviderId = UUID.randomUUID(),
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
      assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("This Booking already has a Departure set")
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when dateTime in past supplied`() {
      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00"),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        destinationProviderId = destinationProviderId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.dateTime", "beforeBookingArrivalDate"),
      )
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when invalid departure reason supplied`() {
      every { mockDepartureReasonRepository.findByIdOrNull(any()) } returns null

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().minusMinutes(1),
        reasonId = UUID.randomUUID(),
        moveOnCategoryId = moveOnCategoryId,
        destinationProviderId = destinationProviderId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.reasonId", "doesNotExist"),
      )
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when the departure reason has the wrong service scope`() {
      every { mockDepartureReasonRepository.findByIdOrNull(any()) } returns DepartureReasonEntityFactory()
        .withServiceScope(ServiceName.temporaryAccommodation.value)
        .produce()

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().minusMinutes(1),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        destinationProviderId = destinationProviderId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.reasonId", "incorrectDepartureReasonServiceScope"),
      )
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when invalid move on category supplied`() {
      every { mockMoveOnCategoryRepository.findByIdOrNull(any()) } returns null

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().plusMinutes(1),
        reasonId = departureReasonId,
        moveOnCategoryId = UUID.randomUUID(),
        destinationProviderId = destinationProviderId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.moveOnCategoryId", "doesNotExist"),
      )
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when the move-on category has the wrong service scope`() {
      every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns DepartureReasonEntityFactory()
        .withServiceScope("*")
        .produce()
      every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns MoveOnCategoryEntityFactory()
        .withServiceScope(ServiceName.temporaryAccommodation.value)
        .produce()
      every { mockDestinationProviderRepository.findByIdOrNull(destinationProviderId) } returns DestinationProviderEntityFactory().produce()

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().plusMinutes(1),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        destinationProviderId = destinationProviderId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.moveOnCategoryId", "incorrectMoveOnCategoryServiceScope"),
      )
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when invalid destination provider supplied`() {
      every { mockDestinationProviderRepository.findByIdOrNull(destinationProviderId) } returns null

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().plusMinutes(1),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        destinationProviderId = destinationProviderId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.destinationProviderId", "doesNotExist"),
      )
    }

    @Test
    fun `createDeparture for a CAS1 booking returns FieldValidationError with correct param to message map when the destination provider is empty`() {
      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().plusMinutes(1),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        destinationProviderId = null,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.destinationProviderId", "empty"),
      )
    }

    @Test
    fun `createDeparture does not save Domain Event when associated with Offline Application without an event number`() {
      val application = OfflineApplicationEntityFactory()
        .withEventNumber(null)
        .produce()

      bookingEntity.offlineApplication = application

      val dateTime = OffsetDateTime.now()

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = dateTime,
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        destinationProviderId = destinationProviderId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.booking).isEqualTo(bookingEntity)
      assertThat(result.entity.dateTime).isEqualTo(dateTime)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.moveOnCategory).isEqualTo(moveOnCategoryEntity)
      assertThat(result.entity.destinationProvider).isEqualTo(destinationProviderEntity)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.departed)

      verify { mockDomainEventService wasNot Called }
    }

    @Test
    fun `createDeparture for a CAS1 booking returns Success, saves and emits Domain Event when associated with Online Application`() {
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
        keyWorkerStaffUserDetails,
      )

      every { mockDomainEventService.savePersonDepartedEvent(any(), any()) } just Runs

      val dateTime = OffsetDateTime.now()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withSubmittedAt(OffsetDateTime.parse("2023-02-15T15:00:00Z"))
        .withCreatedByUser(
          UserEntityFactory()
            .withUnitTestControlProbationRegion()
            .produce(),
        )
        .produce()

      bookingEntity.application = application

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = dateTime,
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        destinationProviderId = destinationProviderId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.booking).isEqualTo(bookingEntity)
      assertThat(result.entity.dateTime).isEqualTo(dateTime)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.moveOnCategory).isEqualTo(moveOnCategoryEntity)
      assertThat(result.entity.destinationProvider).isEqualTo(destinationProviderEntity)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.departed)

      verify(exactly = 1) {
        mockDomainEventService.savePersonDepartedEvent(
          emit = true,
          domainEvent = match {
            val data = (it.data as PersonDepartedEnvelope).eventDetails
            val approvedPremises = bookingEntity.premises as ApprovedPremisesEntity

            it.applicationId == application.id &&
              it.crn == bookingEntity.crn &&
              it.bookingId == bookingEntity.id
            data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ) &&
              data.departedAt == dateTime.toInstant() &&
              data.legacyReasonCode == reasonEntity.legacyDeliusReasonCode &&
              data.destination.destinationProvider == DestinationProvider(
              description = destinationProviderEntity.name,
              id = destinationProviderEntity.id,
            ) &&
              data.reason == reasonEntity.name
          },
        )
      }
    }

    @Test
    fun `createDeparture for a CAS1 booking returns Success, saves and emits Domain Event when associated with Offline Application with event number`() {
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
        keyWorkerStaffUserDetails,
      )

      every { mockDomainEventService.savePersonDepartedEvent(any(), any()) } just Runs

      val dateTime = OffsetDateTime.now()

      val application = OfflineApplicationEntityFactory()
        .withEventNumber("123")
        .produce()

      bookingEntity.offlineApplication = application

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = dateTime,
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        destinationProviderId = destinationProviderId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.booking).isEqualTo(bookingEntity)
      assertThat(result.entity.dateTime).isEqualTo(dateTime)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.moveOnCategory).isEqualTo(moveOnCategoryEntity)
      assertThat(result.entity.destinationProvider).isEqualTo(destinationProviderEntity)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.departed)

      verify(exactly = 1) {
        mockDomainEventService.savePersonDepartedEvent(
          emit = true,
          domainEvent = match {
            val data = (it.data as PersonDepartedEnvelope).eventDetails
            val approvedPremises = bookingEntity.premises as ApprovedPremisesEntity

            it.applicationId == application.id &&
              it.crn == bookingEntity.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ) &&
              data.departedAt == dateTime.toInstant() &&
              data.legacyReasonCode == reasonEntity.legacyDeliusReasonCode &&
              data.destination.destinationProvider == DestinationProvider(
              description = destinationProviderEntity.name,
              id = destinationProviderEntity.id,
            ) &&
              data.reason == reasonEntity.name
          },
        )
      }
    }

    @Test
    fun `createDeparture for a CAS1 booking returns Success, saves but does not emit domain event when associated with Application but arrivedAndDepartedDomainEventsDisabled is true`() {
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
        keyWorkerStaffUserDetails,
      )

      every { mockDomainEventService.savePersonDepartedEvent(any(), any()) } just Runs

      val dateTime = OffsetDateTime.now()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withSubmittedAt(OffsetDateTime.parse("2023-02-15T15:00:00Z"))
        .withCreatedByUser(
          UserEntityFactory()
            .withUnitTestControlProbationRegion()
            .produce(),
        )
        .produce()

      bookingEntity.application = application

      val result = bookingServiceWithArrivedAndDepartedDomainEventsDisabled.createDeparture(
        booking = bookingEntity,
        dateTime = dateTime,
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        destinationProviderId = destinationProviderId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.booking).isEqualTo(bookingEntity)
      assertThat(result.entity.dateTime).isEqualTo(dateTime)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.moveOnCategory).isEqualTo(moveOnCategoryEntity)
      assertThat(result.entity.destinationProvider).isEqualTo(destinationProviderEntity)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.departed)

      verify(exactly = 1) {
        mockDomainEventService.savePersonDepartedEvent(
          emit = false,
          domainEvent = match {
            val data = it.data.eventDetails
            val approvedPremises = bookingEntity.premises as ApprovedPremisesEntity

            it.applicationId == application.id &&
              it.crn == bookingEntity.crn &&
              it.bookingId == bookingEntity.id
            data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ) &&
              data.departedAt == dateTime.toInstant() &&
              data.legacyReasonCode == reasonEntity.legacyDeliusReasonCode &&
              data.destination.destinationProvider == DestinationProvider(
              description = destinationProviderEntity.name,
              id = destinationProviderEntity.id,
            ) &&
              data.reason == reasonEntity.name
          },
        )
      }
    }

    @Test
    fun `createDeparture for a CAS3 booking returns Success with correct result when validation passed and saves a domain event`() {
      val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
      val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")

      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val bookingEntity = createTemporaryAccommodationBooking(probationRegion)

      val reasonEntity = DepartureReasonEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()
      val moveOnCategoryEntity = MoveOnCategoryEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()

      every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns reasonEntity
      every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns moveOnCategoryEntity

      every { mockDepartureRepository.save(any()) } answers { it.invocation.args[0] as DepartureEntity }

      every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as ArrivalEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val user = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)

      every { mockCas3DomainEventService.savePersonDepartedEvent(any(), user) } just Runs

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00"),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        destinationProviderId = null,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.booking).isEqualTo(bookingEntity)
      assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-24T15:00:00+01:00"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.moveOnCategory).isEqualTo(moveOnCategoryEntity)
      assertThat(result.entity.destinationProvider).isEqualTo(null)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.departed)

      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonDepartedEvent(bookingEntity, user)
      }
      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonDepartedEvent(any(), user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonDepartureUpdatedEvent(any(), user)
      }
    }

    @Test
    fun `createDeparture for a CAS3 booking successfully create departure update event and saves a domain event`() {
      val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
      val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
      val departureDateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
      val notes = "Some notes about the departure"
      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val bookingEntity = createTemporaryAccommodationBooking(probationRegion)

      val reasonEntity = DepartureReasonEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()
      val moveOnCategoryEntity = MoveOnCategoryEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()

      val user = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      val departureEntity = DepartureEntityFactory()
        .withBooking(bookingEntity)
        .withDateTime(departureDateTime)
        .withReason(reasonEntity)
        .withMoveOnCategory(moveOnCategoryEntity)
        .withNotes(notes)
        .produce()
      bookingEntity.departures += departureEntity

      every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns reasonEntity
      every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns moveOnCategoryEntity
      every { mockDepartureRepository.save(any()) } answers { it.invocation.args[0] as DepartureEntity }
      every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as ArrivalEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockCas3DomainEventService.savePersonDepartureUpdatedEvent(any(), user) } just Runs

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = departureDateTime,
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        destinationProviderId = null,
        notes = notes,
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.booking).isEqualTo(bookingEntity)
      assertThat(result.entity.dateTime).isEqualTo(departureDateTime)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.moveOnCategory).isEqualTo(moveOnCategoryEntity)
      assertThat(result.entity.destinationProvider).isEqualTo(null)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo(notes)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.departed)

      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonDepartureUpdatedEvent(any(), user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonDepartedEvent(any(), user)
      }
    }
  }

  private fun createTemporaryAccommodationBooking(probationRegion: ProbationRegionEntity): BookingEntity {
    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-22"))
      .withOfflineApplication(OfflineApplicationEntityFactory().produce())
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withProbationRegion(probationRegion)
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withSubmittedAt(OffsetDateTime.parse("2023-02-15T15:00:00Z"))
          .withProbationRegion(probationRegion)
          .withCreatedByUser(
            UserEntityFactory()
              .withProbationRegion(probationRegion)
              .produce(),
          )
          .produce(),
      )
      .withServiceName(ServiceName.temporaryAccommodation)
      .produce()
    return bookingEntity
  }

  @Nested
  inner class CreateArrival {
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

      bookingEntity.arrivals += arrivalEntity

      val result = bookingService.createArrival(
        booking = bookingEntity,
        arrivalDate = LocalDate.parse("2022-08-25"),
        expectedDepartureDate = LocalDate.parse("2022-08-26"),
        notes = "notes",
        keyWorkerStaffCode = "123",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
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
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.expectedDepartureDate", "beforeBookingArrivalDate"),
      )
    }

    @Test
    fun `createArrival returns GeneralValidationError with correct message when Booking is CAS3 `() {
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
      every { mockCas3DomainEventService.savePersonArrivedEvent(any(), user) } just Runs

      val result = bookingService.createArrival(
        booking = bookingEntity,
        arrivalDate = LocalDate.parse("2022-08-27"),
        expectedDepartureDate = LocalDate.parse("2022-08-29"),
        notes = "notes",
        keyWorkerStaffCode = null,
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
      assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("CAS3 booking arrival not supported here, preferred method is createCas3Arrival")
      verify(exactly = 0) { mockStaffMemberService.getStaffMemberByCode(any(), any()) }
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, user)
      }
    }
  }

  @Nested
  inner class CreateCas1Arrival {
    val premises = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    private val keyWorker = ContextStaffMemberFactory().produce()

    @BeforeEach
    fun setup() {
      every { mockStaffMemberService.getStaffMemberByCode(keyWorker.code, premises.qCode) } returns AuthorisableActionResult.Success(keyWorker)
      every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as ArrivalEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    }

    @Test
    fun `createCas1Arrival return GeneralValidationError when the premises is not a CAS1 premises`() {
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
        .produce()

      val result = bookingService.createCas1Arrival(
        booking = bookingEntity,
        arrivalDateTime = Instant.parse("2022-08-27T15:00:00Z"),
        expectedDepartureDate = LocalDate.parse("2022-08-26"),
        notes = "notes",
        keyWorkerStaffCode = keyWorker.code,
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
      assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("CAS1 Arrivals cannot be set on non-CAS1 premises")
    }

    @Test
    fun `createCas1Arrival returns GeneralValidationError with correct message when Booking already has an Arrival`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withStaffKeyWorkerCode("123")
        .produce()

      val arrivalEntity = ArrivalEntityFactory()
        .withBooking(bookingEntity)
        .produce()

      bookingEntity.arrivals += arrivalEntity

      val result = bookingService.createCas1Arrival(
        booking = bookingEntity,
        arrivalDateTime = Instant.parse("2022-08-25T15:00:00Z"),
        expectedDepartureDate = LocalDate.parse("2022-08-26"),
        notes = "notes",
        keyWorkerStaffCode = "123",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
      assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("This Booking already has an Arrival set")
    }

    @Test
    fun `createCas1Arrival returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      val result = bookingService.createCas1Arrival(
        booking = bookingEntity,
        arrivalDateTime = Instant.parse("2022-08-27T15:00:00Z"),
        expectedDepartureDate = LocalDate.parse("2022-08-26"),
        notes = "notes",
        keyWorkerStaffCode = keyWorker.code,
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.expectedDepartureDate", "beforeBookingArrivalDate"),
      )
    }

    @Test
    fun `createCas1Arrival returns Success with correct result when validation passed, does not save Domain Event when associated with Offline Application without an event number`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withStaffKeyWorkerCode(keyWorker.code)
        .withOfflineApplication(
          OfflineApplicationEntityFactory()
            .withEventNumber(null)
            .produce(),
        )
        .produce()

      val result = bookingService.createCas1Arrival(
        booking = bookingEntity,
        arrivalDateTime = Instant.parse("2022-08-27T00:00:00Z"),
        expectedDepartureDate = LocalDate.parse("2022-08-29"),
        notes = "notes",
        keyWorkerStaffCode = keyWorker.code,
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-27"))
      assertThat(result.entity.arrivalDateTime).isEqualTo(Instant.parse("2022-08-27T00:00:00Z"))
      assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-29"))
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.arrived)

      verify { mockDomainEventService wasNot Called }
    }

    @Test
    fun `createCas1Arrival returns Success with correct result when validation passed, saves and emits Domain Event when associated with Online Application`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withStaffKeyWorkerCode(keyWorker.code)
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withSubmittedAt(OffsetDateTime.parse("2023-02-15T15:00:00Z"))
            .withCreatedByUser(
              UserEntityFactory()
                .withUnitTestControlProbationRegion()
                .produce(),
            )
            .produce(),
        )
        .produce()

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(offenderDetails)

      val keyWorkerStaffUserDetails = StaffWithoutUsernameUserDetailsFactory().produce()

      every { mockCommunityApiClient.getStaffUserDetailsForStaffCode(keyWorker.code) } returns ClientResult.Success(
        HttpStatus.OK,
        keyWorkerStaffUserDetails,
      )

      every { mockDomainEventService.savePersonArrivedEvent(any(), any()) } just Runs

      val arrivalDateTime = Instant.parse("2022-08-27T00:00:00Z")

      val result = bookingService.createCas1Arrival(
        booking = bookingEntity,
        arrivalDateTime = arrivalDateTime,
        expectedDepartureDate = LocalDate.parse("2022-08-29"),
        notes = "notes",
        keyWorkerStaffCode = keyWorker.code,
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-27"))
      assertThat(result.entity.arrivalDateTime).isEqualTo(arrivalDateTime)
      assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-29"))
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.arrived)

      verify(exactly = 1) {
        mockDomainEventService.savePersonArrivedEvent(
          emit = true,
          domainEvent = match {
            val data = (it.data as PersonArrivedEnvelope).eventDetails
            val application = bookingEntity.application as ApprovedPremisesApplicationEntity
            val approvedPremises = bookingEntity.premises as ApprovedPremisesEntity

            it.applicationId == application.id &&
              it.crn == bookingEntity.crn &&
              it.occurredAt == arrivalDateTime &&
              it.bookingId == bookingEntity.id &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ) &&
              data.applicationSubmittedOn == LocalDate.parse("2023-02-15") &&
              data.arrivedAt == arrivalDateTime
          },
        )
      }
    }

    @Test
    fun `createCas1Arrival returns Success with correct result when validation passed, saves and emits Domain Event when associated with Offline Application with event number`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withStaffKeyWorkerCode(keyWorker.code)
        .withOfflineApplication(
          OfflineApplicationEntityFactory()
            .withEventNumber("123")
            .produce(),
        )
        .produce()

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(offenderDetails)

      val keyWorkerStaffUserDetails = StaffWithoutUsernameUserDetailsFactory().produce()

      every { mockCommunityApiClient.getStaffUserDetailsForStaffCode(keyWorker.code) } returns ClientResult.Success(
        HttpStatus.OK,
        keyWorkerStaffUserDetails,
      )

      every { mockDomainEventService.savePersonArrivedEvent(any(), any()) } just Runs

      val arrivalDateTime = Instant.parse("2022-08-27T00:00:00Z")

      val result = bookingService.createCas1Arrival(
        booking = bookingEntity,
        arrivalDateTime = arrivalDateTime,
        expectedDepartureDate = LocalDate.parse("2022-08-29"),
        notes = "notes",
        keyWorkerStaffCode = keyWorker.code,
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-27"))
      assertThat(result.entity.arrivalDateTime).isEqualTo(arrivalDateTime)
      assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-29"))
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.arrived)

      verify(exactly = 1) {
        mockDomainEventService.savePersonArrivedEvent(
          emit = true,
          domainEvent = match {
            val data = (it.data as PersonArrivedEnvelope).eventDetails
            val application = bookingEntity.offlineApplication as OfflineApplicationEntity
            val approvedPremises = bookingEntity.premises as ApprovedPremisesEntity

            it.applicationId == application.id &&
              it.crn == bookingEntity.crn &&
              it.occurredAt == arrivalDateTime &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ) &&
              data.applicationSubmittedOn == application.createdAt.toLocalDate() &&
              data.arrivedAt == arrivalDateTime
          },
        )
      }
    }

    @Test
    fun `createCas1Arrival returns Success with correct result when validation passed, saves but does not emit domain event when arrivedAndDepartedDomainEventsDisabled is true`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withStaffKeyWorkerCode(keyWorker.code)
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withSubmittedAt(OffsetDateTime.parse("2023-02-15T15:00:00Z"))
            .withCreatedByUser(
              UserEntityFactory()
                .withUnitTestControlProbationRegion()
                .produce(),
            )
            .produce(),
        )
        .produce()

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      every {
        mockOffenderService.getOffenderByCrn(
          bookingEntity.crn,
          user.deliusUsername,
          true,
        )
      } returns AuthorisableActionResult.Success(offenderDetails)

      val keyWorkerStaffUserDetails = StaffWithoutUsernameUserDetailsFactory().produce()

      every { mockCommunityApiClient.getStaffUserDetailsForStaffCode(keyWorker.code) } returns ClientResult.Success(
        HttpStatus.OK,
        keyWorkerStaffUserDetails,
      )

      every { mockDomainEventService.savePersonArrivedEvent(any(), any()) } just Runs

      val arrivalDateTime = Instant.parse("2022-08-27T00:00:00Z")

      val result = bookingServiceWithArrivedAndDepartedDomainEventsDisabled.createCas1Arrival(
        booking = bookingEntity,
        arrivalDateTime = arrivalDateTime,
        expectedDepartureDate = LocalDate.parse("2022-08-29"),
        notes = "notes",
        keyWorkerStaffCode = keyWorker.code,
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-27"))
      assertThat(result.entity.arrivalDateTime).isEqualTo(arrivalDateTime)
      assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-29"))
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.arrived)

      verify(exactly = 1) {
        mockDomainEventService.savePersonArrivedEvent(
          emit = false,
          domainEvent = match {
            val data = (it.data).eventDetails
            val application = bookingEntity.application as ApprovedPremisesApplicationEntity
            val approvedPremises = bookingEntity.premises as ApprovedPremisesEntity

            it.applicationId == application.id &&
              it.crn == bookingEntity.crn &&
              it.occurredAt == arrivalDateTime &&
              it.bookingId == bookingEntity.id &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ) &&
              data.applicationSubmittedOn == LocalDate.parse("2023-02-15") &&
              data.arrivedAt == arrivalDateTime
          },
        )
      }
    }
  }

  @Nested
  inner class CreateCas3Arrival {
    private val bookingEntity = createTemporaryAccommodationBooking()

    @BeforeEach
    fun setup() {
      every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as ArrivalEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockCas3DomainEventService.savePersonArrivedEvent(any(), any(UserEntity::class)) } just Runs
    }

    @Test
    fun `createCas3Arrival should throw GeneralValidationError when premises is not temporary accommodation`() {
      val keyWorker = ContextStaffMemberFactory().produce()
      val approvedPremisesBooking = createApprovedPremisesBooking(createApprovedPremises(), keyWorker)

      val result = bookingService.createCas3Arrival(
        booking = approvedPremisesBooking,
        arrivalDate = LocalDate.parse("2022-08-25"),
        expectedDepartureDate = LocalDate.parse("2022-08-26"),
        notes = "notes",
        keyWorkerStaffCode = "158",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
      assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("CAS3 Arrivals cannot be set on non-CAS3 premise")
      verify(exactly = 0) { mockArrivalRepository.save(any()) }
      verify(exactly = 0) { mockBookingRepository.save(any()) }
      verify(exactly = 0) { mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, user) }
    }

    @Test
    fun `createArrival returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val result = bookingService.createCas3Arrival(
        booking = bookingEntity,
        arrivalDate = LocalDate.parse("2022-08-27"),
        expectedDepartureDate = LocalDate.parse("2022-08-26"),
        notes = "notes",
        keyWorkerStaffCode = null,
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.expectedDepartureDate", "beforeBookingArrivalDate"),
      )
      verify(exactly = 0) { mockArrivalRepository.save(any()) }
      verify(exactly = 0) { mockBookingRepository.save(any()) }
      verify(exactly = 0) { mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, user) }
    }

    @Test
    fun `createArrival should return success response when arrival exists for a Booking and save and emit the event`() {
      val userEntity = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()
      val arrivalEntity = ArrivalEntityFactory()
        .withBooking(bookingEntity)
        .produce()
      bookingEntity.arrivals += arrivalEntity

      every { mockCas3DomainEventService.savePersonArrivedUpdatedEvent(any(), any(UserEntity::class)) } just Runs

      val result = bookingService.createCas3Arrival(
        booking = bookingEntity,
        arrivalDate = LocalDate.parse("2022-08-25"),
        expectedDepartureDate = LocalDate.parse("2022-08-26"),
        notes = "notes",
        keyWorkerStaffCode = null,
        user = userEntity,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.arrivalDateTime).isEqualTo(Instant.parse("2022-08-25T00:00:00Z"))
      assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-26"))
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.arrived)

      verify(exactly = 1) { mockArrivalRepository.save(any()) }
      verify(exactly = 1) { mockBookingRepository.save(any()) }
      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonArrivedUpdatedEvent(bookingEntity, userEntity)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, userEntity)
      }
    }

    @Test
    fun `createArrival returns Success with correct result for CAS3 when validation passed and saves domain event`() {
      val userEntity = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()
      val result = bookingService.createCas3Arrival(
        booking = bookingEntity,
        arrivalDate = LocalDate.parse("2022-08-27"),
        expectedDepartureDate = LocalDate.parse("2022-08-29"),
        notes = "notes",
        keyWorkerStaffCode = null,
        user = userEntity,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-27"))
      assertThat(result.entity.arrivalDateTime).isEqualTo(Instant.parse("2022-08-27T00:00:00Z"))
      assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-29"))
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.arrived)

      verify(exactly = 1) { mockArrivalRepository.save(any()) }
      verify(exactly = 1) { mockBookingRepository.save(any()) }
      verify(exactly = 1) { mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, userEntity) }
    }

    @Test
    fun `createArrival returns Success with correct result for CAS3 when validation passed and saves domain event without staff detail`() {
      every { mockCas3DomainEventService.savePersonArrivedEvent(any(), null) } just Runs

      val result = bookingService.createCas3Arrival(
        booking = bookingEntity,
        arrivalDate = LocalDate.parse("2022-08-27"),
        expectedDepartureDate = LocalDate.parse("2022-08-29"),
        notes = "notes",
        keyWorkerStaffCode = null,
        user = null,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-27"))
      assertThat(result.entity.arrivalDateTime).isEqualTo(Instant.parse("2022-08-27T00:00:00Z"))
      assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-29"))
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.arrived)

      verify(exactly = 1) { mockArrivalRepository.save(any()) }
      verify(exactly = 1) { mockBookingRepository.save(any()) }
      verify(exactly = 1) { mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, null) }
    }

    private fun createTemporaryAccommodationBooking() = BookingEntityFactory()
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

    private fun createApprovedPremisesBooking(
      premises: ApprovedPremisesEntity,
      keyWorker: uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember,
    ) = BookingEntityFactory()
      .withPremises(premises)
      .withStaffKeyWorkerCode(keyWorker.code)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withSubmittedAt(OffsetDateTime.parse("2023-02-15T15:00:00Z"))
          .withCreatedByUser(
            UserEntityFactory()
              .withUnitTestControlProbationRegion()
              .produce(),
          )
          .produce(),
      )
      .produce()

    private fun createApprovedPremises() = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()
  }

  @Nested
  inner class CreateNonArrival {
    val premises = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    @Test
    fun `createNonArrival returns GeneralValidationError with correct message when Booking already has a NonArrival`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
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
        notes = "notes",
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
      assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("This Booking already has a Non Arrival set")
    }

    @Test
    fun `createNonArrival returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-26"))
        .withPremises(premises)
        .produce()

      every { mockNonArrivalReasonRepository.findByIdOrNull(reasonId) } returns null

      val result = bookingService.createNonArrival(
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
        booking = bookingEntity,
        date = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.date", "afterBookingArrivalDate"),
        entry("$.reason", "doesNotExist"),
      )
    }

    @Test
    fun `createNonArrival returns Success with correct result when validation passed, does not save Domain Event when associated with Offline Application with no event number`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-24"))
        .withPremises(premises)
        .withOfflineApplication(
          OfflineApplicationEntityFactory()
            .withEventNumber(null)
            .produce(),
        )
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
        notes = "notes",
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")

      verify { mockDomainEventService wasNot Called }
    }

    @Test
    fun `createNonArrival returns Success with correct result when validation passed, saves and emits Domain Event when associated with Online Application`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withCrn(application.crn)
        .withArrivalDate(LocalDate.parse("2022-08-24"))
        .withPremises(premises)
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
        staffUserDetails,
      )

      every { mockDomainEventService.savePersonNotArrivedEvent(any(), any()) } just Runs

      val result = bookingService.createNonArrival(
        user = user,
        booking = bookingEntity,
        date = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")

      verify(exactly = 1) {
        mockDomainEventService.savePersonNotArrivedEvent(
          emit = true,
          domainEvent = match {
            val data = (it.data as PersonNotArrivedEnvelope).eventDetails
            val approvedPremises = bookingEntity.premises as ApprovedPremisesEntity

            it.applicationId == application.id &&
              it.crn == bookingEntity.crn &&
              it.bookingId == bookingEntity.id &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ) &&
              data.expectedArrivalOn == bookingEntity.originalArrivalDate &&
              data.recordedBy == StaffMember(
              staffCode = staffUserDetails.staffCode,
              staffIdentifier = staffUserDetails.staffIdentifier,
              forenames = staffUserDetails.staff.forenames,
              surname = staffUserDetails.staff.surname,
              username = staffUserDetails.username,
            ) &&
              data.reason == reasonEntity.name &&
              data.legacyReasonCode == reasonEntity.legacyDeliusReasonCode
          },
        )
      }
    }

    @Test
    fun `createNonArrival returns Success with correct result when validation passed, saves and emits Domain Event when associated with offline application with event number`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = OfflineApplicationEntityFactory()
        .withEventNumber("123")
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withCrn(application.crn)
        .withArrivalDate(LocalDate.parse("2022-08-24"))
        .withPremises(premises)
        .withOfflineApplication(application)
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
        staffUserDetails,
      )

      every { mockDomainEventService.savePersonNotArrivedEvent(any(), any()) } just Runs

      val result = bookingService.createNonArrival(
        user = user,
        booking = bookingEntity,
        date = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")

      verify(exactly = 1) {
        mockDomainEventService.savePersonNotArrivedEvent(
          emit = true,
          domainEvent = match {
            val data = (it.data as PersonNotArrivedEnvelope).eventDetails
            val approvedPremises = bookingEntity.premises as ApprovedPremisesEntity

            it.applicationId == application.id &&
              it.crn == bookingEntity.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ) &&
              data.expectedArrivalOn == bookingEntity.originalArrivalDate &&
              data.recordedBy == StaffMember(
              staffCode = staffUserDetails.staffCode,
              staffIdentifier = staffUserDetails.staffIdentifier,
              forenames = staffUserDetails.staff.forenames,
              surname = staffUserDetails.staff.surname,
              username = staffUserDetails.username,
            ) &&
              data.reason == reasonEntity.name &&
              data.legacyReasonCode == reasonEntity.legacyDeliusReasonCode
          },
        )
      }
    }

    @Test
    fun `createNonArrival returns Success, saves but does not emit domain event when associated with Application but arrivedAndDepartedDomainEventsDisabled is true`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withCrn(application.crn)
        .withArrivalDate(LocalDate.parse("2022-08-24"))
        .withPremises(premises)
        .withApplication(application)
        .produce()

      val reasonEntity = NonArrivalReasonEntityFactory().produce()

      every { mockNonArrivalReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockNonArrivalRepository.save(any()) } answers { it.invocation.args[0] as NonArrivalEntity }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      every {
        mockOffenderService.getOffenderByCrn(
          bookingEntity.crn,
          user.deliusUsername,
        )
      } returns AuthorisableActionResult.Success(offenderDetails)

      val staffUserDetails = StaffUserDetailsFactory().produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )

      every { mockDomainEventService.savePersonNotArrivedEvent(any(), any()) } just Runs

      val result = bookingServiceWithArrivedAndDepartedDomainEventsDisabled.createNonArrival(
        user = user,
        booking = bookingEntity,
        date = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")

      verify(exactly = 1) {
        mockDomainEventService.savePersonNotArrivedEvent(
          emit = false,
          domainEvent = match {
            val data = (it.data as PersonNotArrivedEnvelope).eventDetails
            val approvedPremises = bookingEntity.premises as ApprovedPremisesEntity

            it.applicationId == application.id &&
              it.crn == bookingEntity.crn &&
              it.bookingId == bookingEntity.id &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ) &&
              data.expectedArrivalOn == bookingEntity.originalArrivalDate &&
              data.recordedBy == StaffMember(
              staffCode = staffUserDetails.staffCode,
              staffIdentifier = staffUserDetails.staffIdentifier,
              forenames = staffUserDetails.staff.forenames,
              surname = staffUserDetails.staff.surname,
              username = staffUserDetails.username,
            ) &&
              data.reason == reasonEntity.name &&
              data.legacyReasonCode == reasonEntity.legacyDeliusReasonCode
          },
        )
      }
    }
  }

  @Nested
  inner class CreateCas1Cancellation {
    val premises = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    val reason = CancellationReasonEntityFactory().withServiceScope("*").produce()
    val reasonId = reason.id

    @Test
    fun `createCancellation is idempotent if the booking is already cancelled`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      val cancellationEntity = CancellationEntityFactory()
        .withBooking(bookingEntity)
        .withReason(reason)
        .produce()

      bookingEntity.cancellations = mutableListOf(cancellationEntity)

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = UUID.randomUUID(),
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      assertThat(result.value).isEqualTo(cancellationEntity)
      verify { mockBookingRepository.save(any()) wasNot called }
    }

    @Test
    fun `createCancellation returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-26"))
        .withPremises(premises)
        .produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns null

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = reasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.reason", "doesNotExist"),
      )
    }

    @Test
    fun `createCancellation returns FieldValidationError with correct param to message map when reason is 'Other' and 'otherReason' is blank`() {
      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-26"))
        .withPremises(premises)
        .produce()

      val reasonEntity = CancellationReasonEntityFactory()
        .withServiceScope(ServiceName.approvedPremises.value)
        .withName("Other")
        .produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = reasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.otherReason", "empty"),
      )
    }

    @Test
    fun `createCancellation returns FieldValidationError with correct param to message map when the cancellation reason has the wrong service scope`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withServiceName(ServiceName.approvedPremises)
        .produce()

      val otherReason = CancellationReasonEntityFactory()
        .withServiceScope(ServiceName.temporaryAccommodation.value)
        .produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns otherReason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = reasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.reason", "incorrectCancellationReasonServiceScope"),
      )
    }

    @Test
    fun `createCancellation returns Success with correct result when validation passed`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = reasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.reason).isEqualTo(reason)
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation returns Success with correct result with other reason validation passed`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      val otherReason = CancellationReasonEntityFactory()
        .withServiceScope(ServiceName.approvedPremises.value)
        .withName("Other")
        .produce()

      every { mockCancellationReasonRepository.findByIdOrNull(otherReason.id) } returns otherReason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = otherReason.id,
        notes = "notes",
        otherReason = "Other reason",
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.reason).isEqualTo(otherReason)
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.otherReason).isEqualTo("Other reason")
      assertThat(result.value.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation emits domain event when linked to Application`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withApplication(application)
        .withCrn(application.crn)
        .produce()
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockDomainEventService.saveBookingCancelledEvent(any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)

      val staffUserDetails = StaffUserDetailsFactory().produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )

      every { mockBookingRepository.findAllByApplication(application) } returns emptyList()
      every { mockApplicationService.updateApprovedPremisesApplicationStatus(any(), any()) } returns Unit

      every { mockCas1BookingEmailService.bookingWithdrawn(application, bookingEntity) } returns Unit

      val cancelledAt = LocalDate.parse("2022-08-25")
      val notes = "notes"

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = cancelledAt,
        userProvidedReason = reasonId,
        notes = notes,
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.reason).isEqualTo(reason)
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockDomainEventService.saveBookingCancelledEvent(
          match {
            val data = it.data.eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = premises.id,
              name = premises.name,
              apCode = premises.apCode,
              legacyApCode = premises.qCode,
              localAuthorityAreaName = premises.localAuthorityArea!!.name,
            ) &&
              data.cancelledAt == Instant.parse("2022-08-25T00:00:00.00Z") &&
              data.cancellationReason == reason.name &&
              data.bookingId == bookingEntity.id
          },
        )
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation triggers emails when linked to Application`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withApplication(application)
        .withCrn(application.crn)
        .produce()
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockDomainEventService.saveBookingCancelledEvent(any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)

      val staffUserDetails = StaffUserDetailsFactory().produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )

      every { mockBookingRepository.findAllByApplication(application) } returns emptyList()
      every { mockApplicationService.updateApprovedPremisesApplicationStatus(any(), any()) } returns Unit
      every { mockCas1BookingEmailService.bookingWithdrawn(application, bookingEntity) } returns Unit

      val cancelledAt = LocalDate.parse("2022-08-25")
      val notes = "notes"

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = cancelledAt,
        userProvidedReason = reasonId,
        notes = notes,
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)

      verify(exactly = 1) { mockCas1BookingEmailService.bookingWithdrawn(application, bookingEntity) }
    }

    @Test
    fun `createCancellation emits domain event when linked to an offline application with an eventNumber`() {
      val application = OfflineApplicationEntityFactory()
        .withEventNumber("123")
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withOfflineApplication(application)
        .withCrn(application.crn)
        .produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockDomainEventService.saveBookingCancelledEvent(any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)

      val staffUserDetails = StaffUserDetailsFactory().produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )

      val cancelledAt = LocalDate.parse("2022-08-25")
      val notes = "notes"

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = cancelledAt,
        userProvidedReason = reasonId,
        notes = notes,
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.reason).isEqualTo(reason)
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockDomainEventService.saveBookingCancelledEvent(
          match {
            val data = it.data.eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = premises.id,
              name = premises.name,
              apCode = premises.apCode,
              legacyApCode = premises.qCode,
              localAuthorityAreaName = premises.localAuthorityArea!!.name,
            ) &&
              data.cancelledAt == Instant.parse("2022-08-25T00:00:00.00Z") &&
              data.cancellationReason == reason.name
          },
        )
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation does not emit domain event when linked to an offline application without a eventNumber`() {
      val application = OfflineApplicationEntityFactory()
        .withEventNumber(null)
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withOfflineApplication(application)
        .withCrn(application.crn)
        .produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockDomainEventService.saveBookingCancelledEvent(any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)

      val staffUserDetails = StaffUserDetailsFactory().produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )

      val cancelledAt = LocalDate.parse("2022-08-25")
      val notes = "notes"

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = cancelledAt,
        userProvidedReason = reasonId,
        notes = notes,
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.reason).isEqualTo(reason)
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 0) {
        mockDomainEventService.saveBookingCancelledEvent(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation returns Success and creates new Placement Request when cancellation reason is 'Booking successfully appealed' and cancelled Booking was linked to Placement Request`() {
      val bookingSuccessfullyAppealedReasonId = UUID.fromString("acba3547-ab22-442d-acec-2652e49895f2")
      val bookingSuccessfullyAppealedReason = CancellationReasonEntityFactory()
        .withId(bookingSuccessfullyAppealedReasonId)
        .withServiceScope("*")
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      val originalPlacementRequirements = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val originalPlacementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(originalPlacementRequirements)
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withPlacementRequest(originalPlacementRequest)
        .produce()

      every {
        mockCancellationReasonRepository.findByIdOrNull(bookingSuccessfullyAppealedReasonId)
      } returns bookingSuccessfullyAppealedReason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every {
        mockPlacementRequestService.createPlacementRequest(
          placementRequirements = originalPlacementRequirements,
          placementDates = PlacementDates(
            expectedArrival = originalPlacementRequest.expectedArrival,
            duration = originalPlacementRequest.duration,
          ),
          notes = originalPlacementRequest.notes,
          isParole = false,
          null,
        )
      } answers {
        val placementRequirementsArgument = it.invocation.args[0] as PlacementRequirementsEntity
        PlacementRequestEntityFactory()
          .withPlacementRequirements(placementRequirementsArgument)
          .withApplication(application)
          .withAssessment(assessment)
          .withAllocatedToUser(user)
          .produce()
      }

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = bookingSuccessfullyAppealedReasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.reason).isEqualTo(bookingSuccessfullyAppealedReason)
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockPlacementRequestService.createPlacementRequest(
          placementRequirements = originalPlacementRequirements,
          placementDates = PlacementDates(
            expectedArrival = originalPlacementRequest.expectedArrival,
            duration = originalPlacementRequest.duration,
          ),
          notes = originalPlacementRequest.notes,
          isParole = false,
          null,
        )
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @ParameterizedTest
    @EnumSource(WithdrawableEntityType::class)
    fun `createCancellation sets correct reason if withdrawal triggered by other entity`(triggeringEntity: WithdrawableEntityType) {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      val expectedReasonId = when (triggeringEntity) {
        WithdrawableEntityType.Application -> CancellationReasonRepository.CAS1_RELATED_APP_WITHDRAWN_ID
        WithdrawableEntityType.PlacementApplication -> CancellationReasonRepository.CAS1_RELATED_PLACEMENT_APP_WITHDRAWN_ID
        WithdrawableEntityType.PlacementRequest -> CancellationReasonRepository.CAS1_RELATED_PLACEMENT_REQ_WITHDRAWN_ID
        WithdrawableEntityType.Booking -> reasonId
      }

      every { mockCancellationReasonRepository.findByIdOrNull(expectedReasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = reasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          triggeringEntity,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.reason).isEqualTo(reason)

      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation application status updated to 'AWAITING_PLACEMENT' if no other non-active bookings`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withApplication(application)
        .produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockDomainEventService.saveBookingCancelledEvent(any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)

      val staffUserDetails = StaffUserDetailsFactory().produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )

      every {
        mockApplicationService.updateApprovedPremisesApplicationStatus(
          application.id,
          ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT,
        )
      } returns Unit

      every { mockCas1BookingEmailService.bookingWithdrawn(application, bookingEntity) } returns Unit

      val cancelledBooking1 = BookingEntityFactory()
        .withPremises(premises)
        .withCancellations(
          mutableListOf(
            CancellationEntityFactory()
              .withBooking(
                BookingEntityFactory()
                  .withPremises(premises)
                  .withApplication(application)
                  .produce(),
              )
              .withDefaultReason().produce(),
          ),
        )
        .produce()

      val cancelledBooking2 = BookingEntityFactory()
        .withPremises(premises)
        .withCancellations(
          mutableListOf(
            CancellationEntityFactory()
              .withBooking(
                BookingEntityFactory()
                  .withPremises(premises)
                  .withApplication(application)
                  .produce(),
              )
              .withDefaultReason().produce(),
          ),
        )
        .produce()

      every { mockBookingRepository.findAllByApplication(application) } returns listOf(
        cancelledBooking1,
        cancelledBooking2,
      )

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = reasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      verify {
        mockApplicationService.updateApprovedPremisesApplicationStatus(
          application.id,
          ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT,
        )
      }
    }

    @Test
    fun `createCancellation application status not updated if there are other active bookings`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withApplication(application)
        .produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockDomainEventService.saveBookingCancelledEvent(any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)

      val staffUserDetails = StaffUserDetailsFactory().produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )

      every {
        mockApplicationService.updateApprovedPremisesApplicationStatus(
          application.id,
          ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT,
        )
      } returns Unit

      every { mockCas1BookingEmailService.bookingWithdrawn(application, bookingEntity) } returns Unit

      val cancelledBooking1 = BookingEntityFactory()
        .withPremises(premises)
        .withCancellations(
          mutableListOf(
            CancellationEntityFactory()
              .withBooking(
                BookingEntityFactory()
                  .withPremises(premises)
                  .withApplication(application)
                  .produce(),
              )
              .withDefaultReason().produce(),
          ),
        )
        .produce()

      val activeBooking2 = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      every { mockBookingRepository.findAllByApplication(application) } returns listOf(
        cancelledBooking1,
        activeBooking2,
      )

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = reasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      verify { mockApplicationService wasNot Called }
    }

    @Test
    fun `createCancellation application status not updated if user didn't trigger cancellation`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withApplication(application)
        .produce()

      every { mockCancellationReasonRepository.findByIdOrNull(CancellationReasonRepository.CAS1_RELATED_PLACEMENT_APP_WITHDRAWN_ID) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockDomainEventService.saveBookingCancelledEvent(any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)

      val staffUserDetails = StaffUserDetailsFactory().produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )

      every {
        mockApplicationService.updateApprovedPremisesApplicationStatus(
          application.id,
          ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT,
        )
      } returns Unit
      every { mockCas1BookingEmailService.bookingWithdrawn(application, bookingEntity) } returns Unit

      every { mockBookingRepository.findAllByApplication(application) } returns emptyList()

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = reasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.PlacementApplication,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      verify { mockApplicationService wasNot Called }
    }
  }

  @Nested
  inner class CreateCas3Cancellation {

    @Test
    fun `createCancellation returns Success with correct result and emits a domain event for CAS3`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

      val bookingEntity = createTemporaryAccommodationBookingEntity()

      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), user) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation returns Success with correct result and emits cancelled-updated domain event for CAS3`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingEntity()
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()
      val cancellationEntity = CancellationEntityFactory().withBooking(bookingEntity).withReason(reasonEntity).produce()
      bookingEntity.cancellations += cancellationEntity
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledEvent(any(), user)
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation returns Success and move the assessment to read-to-place state`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any()) } returns AuthorisableActionResult.Success(ValidatableActionResult.Success(assessmentEntity!!))
      mockkStatic(Sentry::class)

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, any())
      }
      verify(exactly = 0) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns Success and not throwing error when acceptAssessment is failed with forbidden error`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any()) } returns AuthorisableActionResult.Unauthorised()
      mockkStatic(Sentry::class)
      every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, any())
      }
      verify(exactly = 1) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns Success and not throwing error when acceptAssessment is failed with runtime exception`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any()) } throws RuntimeException("some-exception")
      mockkStatic(Sentry::class)

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, any())
      }
      verify(exactly = 1) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns exception when acceptAssessment is failed with unexpected exception`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any()) } throws Throwable("some-exception")
      mockkStatic(Sentry::class)

      assertThatExceptionOfType(Throwable::class.java)
        .isThrownBy {
          bookingService.createCas3Cancellation(
            booking = bookingEntity,
            cancelledAt = LocalDate.parse("2022-08-25"),
            reasonId = reasonId,
            notes = "notes",
            user = user,
          )
        }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, any())
      }
      verify(exactly = 0) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns exception when unexpected exception occurred and acceptAssessment is not invoked`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } throws RuntimeException("som-exception")
      mockkStatic(Sentry::class)

      assertThatExceptionOfType(RuntimeException::class.java)
        .isThrownBy {
          bookingService.createCas3Cancellation(
            booking = bookingEntity,
            cancelledAt = LocalDate.parse("2022-08-25"),
            reasonId = reasonId,
            notes = "notes",
            user = user,
          )
        }

      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 0) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 0) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, any())
      }
      verify(exactly = 0) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns Success and not throwing error when accept assessment is failed with bad request error`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any()) } returns AuthorisableActionResult.Success(ValidatableActionResult.GeneralValidationError("Error"))
      mockkStatic(Sentry::class)
      every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, any())
      }
      verify(exactly = 1) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns Success and not invoke acceptAssessment when assessment is empty`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment(false)
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 0) {
        mockAssessmentService.acceptAssessment(user, any(), any(), null, null, any())
      }
      verify(exactly = 0) {
        Sentry.captureException(any())
      }
    }

    private fun createTemporaryAccommodationBookingEntity(): BookingEntity {
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
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()
      return bookingEntity
    }

    private fun createTemporaryAccommodationBookingWithAssessment(withAssessment: Boolean = true): BookingEntity {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .produce()

      val assessmentEntity = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      if (withAssessment) {
        application.assessments += assessmentEntity
      }

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
        .withServiceName(ServiceName.temporaryAccommodation)
        .withApplication(application)
        .produce()

      return bookingEntity
    }
  }

  @Nested
  inner class CreateExtension {
    val approvedPremises = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    private val approvedPremisesRoom = RoomEntityFactory()
      .withPremises(approvedPremises)
      .produce()

    private val approvedPremisesBed = BedEntityFactory()
      .withRoom(approvedPremisesRoom)
      .produce()

    private val temporaryAccommodationPremises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    private val temporaryAccommodationRoom = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremises)
      .produce()

    private val temporaryAccommodationBed = BedEntityFactory()
      .withRoom(temporaryAccommodationRoom)
      .produce()

    @BeforeEach
    fun setup() {
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockExtensionRepository.save(any()) } answers { it.invocation.args[0] as ExtensionEntity }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(any(), any(), any(), any()) } returns listOf()
    }

    @Test
    fun `createExtension returns Success with correct result when an Approved Premises booking has a new departure date before the existing departure date`() {
      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-10"))
        .withDepartureDate(LocalDate.parse("2022-08-26"))
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .produce()

      val result = bookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = LocalDate.parse("2022-08-25"),
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.newDepartureDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.previousDepartureDate).isEqualTo(LocalDate.parse("2022-08-26"))
      assertThat(result.entity.notes).isEqualTo("notes")
    }

    @Test
    fun `createExtension returns Success with correct result when a Temporary Accommodation booking has a new departure date before the existing departure date`() {
      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-10"))
        .withDepartureDate(LocalDate.parse("2022-08-26"))
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      val result = bookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = LocalDate.parse("2022-08-25"),
        notes = "notes",
        user = user,
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
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      val result = bookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = LocalDate.parse("2022-08-25"),
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.newDepartureDate", "beforeBookingArrivalDate"),
      )
    }

    @Test
    fun `createExtension returns Success with correct result when validation passed`() {
      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-20"))
        .withDepartureDate(LocalDate.parse("2022-08-24"))
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .produce()

      val result = bookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = LocalDate.parse("2022-08-25"),
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.newDepartureDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.previousDepartureDate).isEqualTo(LocalDate.parse("2022-08-24"))
      assertThat(result.entity.notes).isEqualTo("notes")
    }

    @Test
    fun `createExtension emits domain event when Booking has associated Application`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-20"))
        .withDepartureDate(LocalDate.parse("2022-08-24"))
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .withApplication(application)
        .withCrn(application.crn)
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(application.crn)
        .produce()

      val staffUserDetails = StaffUserDetailsFactory()
        .withUsername(user.deliusUsername)
        .withForenames("John Jacob")
        .withSurname("Johnson")
        .produce()

      every { mockOffenderService.getOffenderByCrn(application.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)

      every { mockDomainEventService.saveBookingChangedEvent(any()) } just Runs

      val newDepartureDate = LocalDate.parse("2022-08-25")

      val result = bookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = newDepartureDate,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.newDepartureDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.previousDepartureDate).isEqualTo(LocalDate.parse("2022-08-24"))
      assertThat(result.entity.notes).isEqualTo("notes")

      verify(exactly = 1) {
        mockDomainEventService.saveBookingChangedEvent(
          match {
            val data = (it.data as BookingChangedEnvelope).eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ) &&
              data.arrivalOn == bookingEntity.arrivalDate &&
              data.departureOn == newDepartureDate
          },
        )
      }
    }

    @Test
    fun `createExtension emits domain event when Booking has associated offline Application`() {
      val application = OfflineApplicationEntityFactory()
        .withEventNumber("123")
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-20"))
        .withDepartureDate(LocalDate.parse("2022-08-24"))
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .withOfflineApplication(application)
        .withCrn(application.crn)
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(application.crn)
        .produce()

      val staffUserDetails = StaffUserDetailsFactory()
        .withUsername(user.deliusUsername)
        .withForenames("John Jacob")
        .withSurname("Johnson")
        .produce()

      every { mockOffenderService.getOffenderByCrn(application.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)

      every { mockDomainEventService.saveBookingChangedEvent(any()) } just Runs

      val newDepartureDate = LocalDate.parse("2022-08-25")

      val result = bookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = newDepartureDate,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.newDepartureDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.previousDepartureDate).isEqualTo(LocalDate.parse("2022-08-24"))
      assertThat(result.entity.notes).isEqualTo("notes")

      verify(exactly = 1) {
        mockDomainEventService.saveBookingChangedEvent(
          match {
            val data = (it.data as BookingChangedEnvelope).eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ) &&
              data.arrivalOn == bookingEntity.arrivalDate &&
              data.departureOn == newDepartureDate
          },
        )
      }
    }

    @Test
    fun `createExtension does not emit domain event when Booking has associated Application without an eventNumber`() {
      val application = OfflineApplicationEntityFactory()
        .withEventNumber(null)
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-20"))
        .withDepartureDate(LocalDate.parse("2022-08-24"))
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .withOfflineApplication(application)
        .withCrn(application.crn)
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(application.crn)
        .produce()

      val staffUserDetails = StaffUserDetailsFactory()
        .withUsername(user.deliusUsername)
        .withForenames("John Jacob")
        .withSurname("Johnson")
        .produce()

      every { mockOffenderService.getOffenderByCrn(application.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockExtensionRepository.save(any()) } answers { it.invocation.args[0] as ExtensionEntity }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(any(), any(), any(), any()) } returns listOf()

      every { mockDomainEventService.saveBookingChangedEvent(any()) } just Runs

      val newDepartureDate = LocalDate.parse("2022-08-25")

      val result = bookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = newDepartureDate,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.newDepartureDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.previousDepartureDate).isEqualTo(LocalDate.parse("2022-08-24"))
      assertThat(result.entity.notes).isEqualTo("notes")

      verify(exactly = 0) {
        mockDomainEventService.saveBookingChangedEvent(any())
      }
    }
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
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
    assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("This Booking already has a Confirmation set")
  }

  @Test
  fun `createConfirmation returns Success with correct result when validation passed and emits domain event`() {
    val bookingEntity = createTemporaryAccommodationBooking(null)

    every { mockConfirmationRepository.save(any()) } answers { it.invocation.args[0] as ConfirmationEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

    every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(), user) } just Runs

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
    assertThat(result.entity.notes).isEqualTo("notes")
    assertThat(bookingEntity.confirmation).isEqualTo(result.entity)
    assertThat(result.entity.booking.status).isEqualTo(BookingStatus.confirmed)

    verify(exactly = 1) {
      mockCas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)
    }
    verify(exactly = 1) {
      mockBookingRepository.save(bookingEntity)
    }
  }

  @Test
  fun `createConfirmation returns Success with correct result and not closing the referral when booking is done without application`() {
    val bookingEntity = createTemporaryAccommodationBooking(null)

    every { mockConfirmationRepository.save(any()) } answers { it.invocation.args[0] as ConfirmationEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

    every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(), user) } just Runs

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
    assertThat(result.entity.notes).isEqualTo("notes")
    assertThat(bookingEntity.confirmation).isEqualTo(result.entity)
    assertThat(result.entity.booking.status).isEqualTo(BookingStatus.confirmed)

    verify(exactly = 1) {
      mockCas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)
    }
    verify(exactly = 1) {
      mockBookingRepository.save(bookingEntity)
    }
    verify(exactly = 0) {
      mockAssessmentService.closeAssessment(user, any())
    }
    verify(exactly = 0) {
      mockAssessmentRepository.findByApplication_IdAndReallocatedAtNull(any())
    }
  }

  @Test
  fun `createConfirmation returns Success with correct result when validation passed and emits domain event and close referral`() {
    val application = TemporaryAccommodationApplicationEntityFactory()
      .withProbationRegion(user.probationRegion)
      .withCreatedByUser(user)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    val bookingEntity = createTemporaryAccommodationBooking(application)

    every { mockConfirmationRepository.save(any()) } answers { it.invocation.args[0] as ConfirmationEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

    every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(), user) } just Runs
    every { mockAssessmentRepository.findByApplication_IdAndReallocatedAtNull(bookingEntity.application!!.id) } returns assessment
    every { mockAssessmentService.closeAssessment(user, assessment.id) } returns AuthorisableActionResult.Success(ValidatableActionResult.Success(assessment))
    mockkStatic(Sentry::class)

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
    assertThat(result.entity.notes).isEqualTo("notes")
    assertThat(bookingEntity.confirmation).isEqualTo(result.entity)
    assertThat(result.entity.booking.status).isEqualTo(BookingStatus.confirmed)

    verify(exactly = 1) {
      mockCas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)
    }
    verify(exactly = 1) {
      mockBookingRepository.save(bookingEntity)
    }
    verify(exactly = 1) {
      mockAssessmentService.closeAssessment(user, assessment.id)
    }
    verify(exactly = 1) {
      mockAssessmentRepository.findByApplication_IdAndReallocatedAtNull(bookingEntity.application!!.id)
    }
    verify(exactly = 0) {
      Sentry.captureException(any())
    }
  }

  @Test
  fun `createConfirmation returns Success with correct result and not closing the referral when assessment not found`() {
    val application = TemporaryAccommodationApplicationEntityFactory()
      .withProbationRegion(user.probationRegion)
      .withCreatedByUser(user)
      .produce()
    val bookingEntity = createTemporaryAccommodationBooking(application)

    every { mockConfirmationRepository.save(any()) } answers { it.invocation.args[0] as ConfirmationEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(), user) } just Runs
    every { mockAssessmentRepository.findByApplication_IdAndReallocatedAtNull(bookingEntity.application!!.id) } returns null

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
    assertThat(result.entity.notes).isEqualTo("notes")
    assertThat(bookingEntity.confirmation).isEqualTo(result.entity)
    assertThat(result.entity.booking.status).isEqualTo(BookingStatus.confirmed)

    verify(exactly = 1) {
      mockCas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)
    }
    verify(exactly = 1) {
      mockBookingRepository.save(bookingEntity)
    }
    verify(exactly = 1) {
      mockAssessmentRepository.findByApplication_IdAndReallocatedAtNull(bookingEntity.application!!.id)
    }
    verify(exactly = 0) {
      mockAssessmentService.closeAssessment(user, any())
    }
  }

  @Test
  fun `createConfirmation returns Success with correct result when closing referral failed`() {
    val application = TemporaryAccommodationApplicationEntityFactory()
      .withProbationRegion(user.probationRegion)
      .withCreatedByUser(user)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    val bookingEntity = createTemporaryAccommodationBooking(application)

    every { mockConfirmationRepository.save(any()) } answers { it.invocation.args[0] as ConfirmationEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(), user) } just Runs
    every { mockAssessmentRepository.findByApplication_IdAndReallocatedAtNull(bookingEntity.application!!.id) } returns assessment
    every { mockAssessmentService.closeAssessment(user, assessment.id) } returns AuthorisableActionResult.Unauthorised()
    mockkStatic(Sentry::class)
    every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
    assertThat(result.entity.notes).isEqualTo("notes")
    assertThat(bookingEntity.confirmation).isEqualTo(result.entity)
    assertThat(result.entity.booking.status).isEqualTo(BookingStatus.confirmed)

    verify(exactly = 1) {
      mockCas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)
    }
    verify(exactly = 1) {
      mockBookingRepository.save(bookingEntity)
    }
    verify(exactly = 1) {
      mockAssessmentService.closeAssessment(user, assessment.id)
    }
    verify(exactly = 1) {
      mockAssessmentRepository.findByApplication_IdAndReallocatedAtNull(bookingEntity.application!!.id)
    }
    verify(exactly = 1) {
      Sentry.captureException(any())
    }
  }

  @Test
  fun `createConfirmation returns Success with correct result not closing referral when the request is not Temporary premises accommodation`() {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .produce()
    val bookingEntity = createApprovedPremisesAccommodationBooking(application)

    every { mockConfirmationRepository.save(any()) } answers { it.invocation.args[0] as ConfirmationEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(), user) } just Runs

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
    assertThat(result.entity.notes).isEqualTo("notes")
    assertThat(bookingEntity.confirmation).isEqualTo(result.entity)
    assertThat(result.entity.booking.status).isEqualTo(BookingStatus.confirmed)

    verify(exactly = 0) {
      mockCas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)
    }
    verify(exactly = 1) {
      mockBookingRepository.save(bookingEntity)
    }
    verify(exactly = 0) {
      mockAssessmentService.closeAssessment(user, any())
    }
    verify(exactly = 0) {
      mockAssessmentRepository.findByApplication_IdAndReallocatedAtNull(bookingEntity.application!!.id)
    }
  }

  private fun createTemporaryAccommodationBooking(application: TemporaryAccommodationApplicationEntity?) =
    BookingEntityFactory()
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withService(ServiceName.temporaryAccommodation.value)
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withApplication(application.let { application })
      .produce()

  private fun createApprovedPremisesAccommodationBooking(application: ApprovedPremisesApplicationEntity?) =
    BookingEntityFactory()
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withService(ServiceName.approvedPremises.value)
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withApplication(application.let { application })
      .produce()

  @Nested
  inner class CreateApprovedPremisesAdHocBooking {
    private val crn = "CRN123"
    private val arrivalDate = LocalDate.parse("2023-02-22")
    private val departureDate = LocalDate.parse("2023-02-23")

    private val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    private val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    private val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    private var user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    private val staffUserDetails = StaffUserDetailsFactory()
      .withUsername(user.deliusUsername)
      .produce()

    private val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCrn(crn)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    private val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(application.crn)
      .produce()

    private val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(user)
      .produce()

    private val bookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withCrn(crn)
      .withApplication(application)
      .produce()

    @BeforeEach
    public fun setup() {
      every { mockPlacementRequestRepository.save(any()) } answers { callOriginal() }
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf()
      every { mockApplicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises) } returns listOf(application)
      every { mockApplicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()
      every { mockBookingRepository.save(any()) } answers { bookingEntity }
      every { mockOffenderService.getOffenderByCrn(application.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)
      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed
      every { mockCruService.cruNameFromProbationAreaCode(staffUserDetails.probationArea.code) } returns "CRU NAME"
      every { mockDomainEventService.saveBookingMadeDomainEvent(any()) } just Runs
      every { mockCas1BookingEmailService.bookingMade(any(), any()) } just Runs
    }

    @Test
    fun `createApprovedPremisesAdHocBooking returns Unauthorised if user does not have either MANAGER or MATCHER role`() {
      val result = bookingService.createApprovedPremisesAdHocBooking(
        user,
        "CRN",
        "NOMS123",
        LocalDate.parse("2023-02-22"),
        LocalDate.parse("2023-02-24"),
        premises,
        bedId = UUID.randomUUID(),
        "eventNumber",
      )

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
    fun `createApprovedPremisesAdHocBooking returns FieldValidationError if Departure Date is before Arrival Date`(role: UserRole) {
      user.addRoleForUnitTest(role)

      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-22")

      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf()

      val authorisableResult = bookingService.createApprovedPremisesAdHocBooking(user, crn, "NOMS123", arrivalDate, departureDate, premises, bed.id, "eventNumber")
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.departureDate", "beforeBookingArrivalDate"),
      )
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
    fun `createApprovedPremisesAdHocBooking returns FieldValidationError if Bed does not exist`(role: UserRole) {
      user.addRoleForUnitTest(role)

      val bedId = UUID.fromString("5c0d77ff-3ec8-45e1-9e1f-a68e73bf45ec")

      every { mockBedRepository.findByIdOrNull(bedId) } returns null
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bedId, arrivalDate, departureDate, null) } returns listOf()

      val authorisableResult = bookingService.createApprovedPremisesAdHocBooking(user, crn, "NOMS123", arrivalDate, departureDate, premises, bedId, "eventNumber")
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.bedId", "doesNotExist"),
      )
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
    fun `createApprovedPremisesAdHocBooking returns FieldValidationError if Bed does not belong to an Approved Premise`(role: UserRole) {
      user.addRoleForUnitTest(role)

      val tempPremises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val tempRoom = RoomEntityFactory()
        .withPremises(tempPremises)
        .produce()

      val tempBed = BedEntityFactory()
        .withRoom(tempRoom)
        .produce()

      val bedId = tempBed.id

      every { mockBedRepository.findByIdOrNull(bedId) } returns tempBed
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bedId, arrivalDate, departureDate, null) } returns listOf()

      val authorisableResult = bookingService.createApprovedPremisesAdHocBooking(user, crn, "NOMS123", arrivalDate, departureDate, tempPremises, bedId, "eventNumber")
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.bedId", "mustBelongToApprovedPremises"),
      )
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
    fun `createApprovedPremisesAdHocBooking returns FieldValidationError if Bed does not belong to the provided Premise`(role: UserRole) {
      user.addRoleForUnitTest(role)

      val otherPremises = ApprovedPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val bedId = bed.id

      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bedId, arrivalDate, departureDate, null) } returns listOf()

      val authorisableResult = bookingService.createApprovedPremisesAdHocBooking(user, crn, "NOMS123", arrivalDate, departureDate, otherPremises, bedId, "eventNumber")
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.bedId", "mustBelongToProvidedPremise"),
      )
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
    fun `createApprovedPremisesAdHocBooking returns FieldValidationError if eventNumber is null`(role: UserRole) {
      user.addRoleForUnitTest(role)

      val authorisableResult = bookingService.createApprovedPremisesAdHocBooking(user, crn, "NOMS123", arrivalDate, departureDate, premises, bed.id, null)
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.eventNumber", "empty"),
      )
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
    fun `createApprovedPremisesAdHocBooking throws if unable to get Staff Details`(role: UserRole) {
      user.addRoleForUnitTest(role)

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Failure.StatusCode(HttpMethod.GET, "/staff-details/${user.deliusUsername}", HttpStatus.NOT_FOUND, null)

      val runtimeException = assertThrows<RuntimeException> {
        bookingService.createApprovedPremisesAdHocBooking(user, crn, "NOMS123", arrivalDate, departureDate, premises, bed.id, "eventNumber")
      }

      assertThat(runtimeException.message).isEqualTo("Unable to complete GET request to /staff-details/${user.deliusUsername}: 404 NOT_FOUND")
    }

    @Test
    fun `createApprovedPremisesAdHocBooking succeeds when creating a double Booking`() {
      user.addRoleForUnitTest(UserRole.CAS1_MANAGER)

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf(
        BookingEntityFactory()
          .withPremises(premises)
          .withBed(bed)
          .withArrivalDate(LocalDate.parse("2023-02-20"))
          .withDepartureDate(LocalDate.parse("2023-02-22"))
          .produce(),
      )
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val authorisableResult = bookingService.createApprovedPremisesAdHocBooking(user, crn, "NOMS123", arrivalDate, departureDate, premises, bed.id, "eventNumber")
      assertThat(authorisableResult is AuthorisableActionResult.Success)
      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.adhoc == true
          },
        )
      }

      verify(exactly = 1) {
        mockDomainEventService.saveBookingMadeDomainEvent(
          match {
            val data = (it.data as BookingMadeEnvelope).eventDetails

            it.applicationId == application.id &&
              it.crn == crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = premises.id,
              name = premises.name,
              apCode = premises.apCode,
              legacyApCode = premises.qCode,
              localAuthorityAreaName = premises.localAuthorityArea!!.name,
            ) &&
              data.arrivalOn == arrivalDate
          },
        )
      }

      val booking = (validatableResult as ValidatableActionResult.Success).entity
      verify(exactly = 1) { mockCas1BookingEmailService.bookingMade(application, booking) }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
    fun `createApprovedPremisesAdHocBooking saves Booking and creates Domain Event when associated Application is an Online Application`(role: UserRole) {
      user.addRoleForUnitTest(role)

      val existingApplication = ApprovedPremisesApplicationEntityFactory()
        .withCrn(crn)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { mockApplicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises) } returns listOf(existingApplication)
      every { mockApplicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val authorisableResult = bookingService.createApprovedPremisesAdHocBooking(user, crn, "NOMS123", arrivalDate, departureDate, premises, bed.id, "eventNumber")
      assertThat(authorisableResult is AuthorisableActionResult.Success)
      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.adhoc == true
          },
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
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == existingApplication.eventNumber &&
              data.premises == Premises(
              id = premises.id,
              name = premises.name,
              apCode = premises.apCode,
              legacyApCode = premises.qCode,
              localAuthorityAreaName = premises.localAuthorityArea!!.name,
            ) &&
              data.arrivalOn == arrivalDate
          },
        )
      }

      val booking = (validatableResult as ValidatableActionResult.Success).entity
      verify(exactly = 1) { mockCas1BookingEmailService.bookingMade(existingApplication, booking) }
    }

    @Test
    fun `createApprovedPremisesAdHocBooking saves Booking and creates Domain Event when associated Application is an Online Application and no Bed ID is provided`() {
      user.addRoleForUnitTest(UserRole.CAS1_MANAGER)

      val existingApplication = ApprovedPremisesApplicationEntityFactory()
        .withCrn(crn)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { mockApplicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises) } returns listOf(existingApplication)
      every { mockApplicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val authorisableResult = bookingService.createApprovedPremisesAdHocBooking(user, crn, "NOMS123", arrivalDate, departureDate, premises, bedId = null, "eventNumber")
      assertThat(authorisableResult is AuthorisableActionResult.Success)
      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.adhoc == true
          },
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
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == existingApplication.eventNumber &&
              data.premises == Premises(
              id = premises.id,
              name = premises.name,
              apCode = premises.apCode,
              legacyApCode = premises.qCode,
              localAuthorityAreaName = premises.localAuthorityArea!!.name,
            ) &&
              data.arrivalOn == arrivalDate
          },
        )
      }

      val booking = (validatableResult as ValidatableActionResult.Success).entity
      verify(exactly = 1) { mockCas1BookingEmailService.bookingMade(existingApplication, booking) }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
    fun `createApprovedPremisesAdHocBooking saves Booking and creates Domain Event when associated Application is an Offline Application`(role: UserRole) {
      user.addRoleForUnitTest(UserRole.CAS1_MANAGER)

      val existingApplication = OfflineApplicationEntityFactory()
        .withCrn(crn)
        .withCreatedAt(OffsetDateTime.now())
        .produce()

      every { mockApplicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()
      every { mockApplicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises) } returns listOf(existingApplication)

      val authorisableResult = bookingService.createApprovedPremisesAdHocBooking(user, crn, "NOMS123", arrivalDate, departureDate, premises, bed.id, "eventNumber")
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success).isTrue

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.adhoc == true
          },
        )
      }

      verify(exactly = 1) {
        mockDomainEventService.saveBookingMadeDomainEvent(any())
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
    fun `createApprovedPremisesAdHocBooking saves Booking and creates Domain Event when associated Application is an Offline Application without an eventNumber`(role: UserRole) {
      user.addRoleForUnitTest(UserRole.CAS1_MANAGER)

      val existingApplication = OfflineApplicationEntityFactory()
        .withCrn(crn)
        .withCreatedAt(OffsetDateTime.now())
        .withEventNumber(null)
        .produce()

      every { mockApplicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()
      every { mockApplicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises) } returns listOf(existingApplication)

      val authorisableResult = bookingService.createApprovedPremisesAdHocBooking(user, crn, "NOMS123", arrivalDate, departureDate, premises, bed.id, "eventNumber")
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success).isTrue

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.adhoc == true
          },
        )
      }

      verify(exactly = 1) {
        mockDomainEventService.saveBookingMadeDomainEvent(any())
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
    fun `createApprovedPremisesAdHocBooking saves Booking and creates an Offline Application when neither an Offline Application or normal Application are present`(role: UserRole) {
      user.addRoleForUnitTest(role)

      every { mockApplicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()
      every { mockApplicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()
      every {
        mockApplicationService.createOfflineApplication(
          match { it.crn == crn && it.service == ServiceName.approvedPremises.value },
        )
      } answers { it.invocation.args[0] as OfflineApplicationEntity }

      val authorisableResult = bookingService.createApprovedPremisesAdHocBooking(user, crn, "NOMS123", arrivalDate, departureDate, premises, bed.id, "eventNumber")
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success).isTrue

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.adhoc == true
          },
        )
      }

      verify {
        mockApplicationService.createOfflineApplication(
          match { it.crn == crn && it.service == ServiceName.approvedPremises.value && it.eventNumber == "eventNumber" },
        )
      }
    }

    @Test
    fun `createApprovedPremisesAdHocBooking saves Booking and creates an Offline Application when neither an Offline Application or normal Application are present and the user is null`() {
      every { mockApplicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()
      every { mockApplicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises) } returns emptyList()
      every {
        mockApplicationService.createOfflineApplication(
          match { it.crn == crn && it.service == ServiceName.approvedPremises.value },
        )
      } answers { it.invocation.args[0] as OfflineApplicationEntity }

      val authorisableResult = bookingService.createApprovedPremisesAdHocBooking(null, crn, "NOMS123", arrivalDate, departureDate, premises, bed.id, null)
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success).isTrue

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.adhoc == true
          },
        )
      }

      verify {
        mockApplicationService.createOfflineApplication(
          match { it.crn == crn && it.service == ServiceName.approvedPremises.value && it.eventNumber == null },
        )
      }

      verify(exactly = 0) {
        mockDomainEventService.saveBookingMadeDomainEvent(
          any(),
        )
      }
    }
  }

  @Nested
  inner class CreateTemporaryAccommodationBooking {

    @Test
    fun `createTemporaryAccommodationBooking returns FieldValidationError if Bed ID is not provided`() {
      val crn = "CRN123"
      val assessmentId = UUID.randomUUID()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        LocalDate.parse("2023-02-23"),
        LocalDate.parse("2023-02-22"),
        bedId = null,
        assessmentId,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.bedId", "empty"),
      )
    }

    @Test
    fun `createTemporaryAccommodationBooking returns FieldValidationError if Departure Date is before Arrival Date`() {
      val crn = "CRN123"
      val bedId = UUID.fromString("3b2f46de-a785-45ab-ac02-5e532c600647")
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-22")
      val assessmentId = UUID.randomUUID()

      every { mockBedRepository.findByIdOrNull(bedId) } returns null

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedId, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bedId,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        LocalDate.parse("2023-02-23"),
        LocalDate.parse("2023-02-22"),
        bedId,
        assessmentId,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.departureDate", "beforeBookingArrivalDate"),
      )
    }

    @Test
    fun `createTemporaryAccommodationBooking returns FieldValidationError if Bed does not exist`() {
      val crn = "CRN123"
      val bedId = UUID.fromString("3b2f46de-a785-45ab-ac02-5e532c600647")
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-22")
      val assessmentId = UUID.randomUUID()

      every { mockBedRepository.findByIdOrNull(bedId) } returns null
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedId, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bedId,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedId,
        assessmentId,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.bedId", "doesNotExist"),
      )
    }

    @Test
    fun `createTemporaryAccommodationBooking returns FieldValidationError if Application is provided and does not exist`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")
      val assessmentId = UUID.randomUUID()

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

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null
      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        assessmentId,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.assessmentId", "doesNotExist"),
      )
    }

    @Test
    fun `createTemporaryAccommodationBooking saves Booking and creates domain event`() {
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

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        application.id,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application
          },
          user,
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking does not attach the application if no ID is provided`() {
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

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      mockkStatic(Sentry::class)

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        null,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == null &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 0) {
        mockAssessmentRepository.findByIdOrNull(any())
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking automatically creates a Turnaround of zero days if 'enableTurnarounds' is false`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .withTurnaroundWorkingDayCount(4)
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      val bookingSlot = slot<BookingEntity>()
      every { mockBookingRepository.save(capture(bookingSlot)) } answers { bookingSlot.captured }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        application.id,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockTurnaroundRepository.save(
          match {
            it.booking == bookingSlot.captured &&
              it.workingDayCount == 0
          },
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking automatically creates a Turnaround of the number of working days specified on the premises if 'enableTurnarounds' is true`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .withTurnaroundWorkingDayCount(4)
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      val bookingSlot = slot<BookingEntity>()
      every { mockBookingRepository.save(capture(bookingSlot)) } answers { bookingSlot.captured }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        application.id,
        true,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockTurnaroundRepository.save(
          match {
            it.booking == bookingSlot.captured &&
              it.workingDayCount == premises.turnaroundWorkingDayCount
          },
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking saves Booking and creates domain event and log error when closing assessment failed with authentication error`() {
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

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      mockkStatic(Sentry::class)
      every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        application.id,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application
          },
          user,
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking saves Booking and creates domain event and log error when closing assessment failed with field validation error`() {
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

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        application.id,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application
          },
          user,
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking saves Booking and creates domain event and log error when closing assessment failed with runtime exception`() {
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

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        application.id,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application
          },
          user,
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking not closing assessments and returns FieldValidationError if Application is provided and does not exist`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")
      val assessmentId = UUID.randomUUID()

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

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null
      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        assessmentId,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.assessmentId", "doesNotExist"),
      )
    }

    @Test
    fun `createTemporaryAccommodationBooking fail to save booking and not closing assessment when saving booking triggered Database exception`() {
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

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } throws RuntimeException("DB exception")

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      assertThatExceptionOfType(RuntimeException::class.java)
        .isThrownBy {
          bookingService.createTemporaryAccommodationBooking(
            user,
            premises,
            crn,
            "NOMS123",
            arrivalDate,
            departureDate,
            bed.id,
            application.id,
            false,
          )
        }

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application
          },
          user,
        )
      }
    }
  }

  @Nested
  inner class CreateApprovedPremisesBookingFromPlacementRequest {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    private val otherUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(otherUser)
      .withSubmittedAt(OffsetDateTime.now())
      .withReleaseType(ReleaseTypeOption.licence.toString())
      .withSentenceType(SentenceTypeOption.nonStatutory.toString())
      .withSituation(SituationOption.bailSentence.toString())
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(otherUser)
      .produce()

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest returns Not Found if Placement Request can't be found`() {
      val placementRequestId = UUID.fromString("43d5ba3c-3eb1-4966-bcd1-c6d16be9f178")
      val bedId = UUID.fromString("d69c0e07-f362-4727-86a6-45aaa73c14af")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequestId) } returns null

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequestId,
        bedId = bedId,
        premisesId = null,
        arrivalDate = LocalDate.parse("2023-03-28"),
        departureDate = LocalDate.parse("2023-03-30"),
      )

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest returns Unauthorised if Placement Request is not allocated to the User`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(otherUser)
        .produce()

      val bedId = UUID.fromString("d69c0e07-f362-4727-86a6-45aaa73c14af")

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bedId,
        premisesId = null,
        arrivalDate = LocalDate.parse("2023-03-28"),
        departureDate = LocalDate.parse("2023-03-30"),
      )

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest returns GeneralValidationError if Bed does not belong to an Approved Premises`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
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

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf()

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bed.id,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.FieldValidationError).isTrue
      val validationError = result.entity as ValidatableActionResult.FieldValidationError

      assertThat(validationError.validationMessages["$.bedId"]).isEqualTo("mustBelongToApprovedPremises")
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest returns GeneralValidationError if a premisesId is specified and the Premises is not an Approved Premises`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockPremisesRepository.findByIdOrNull(premises.id) } returns premises

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = null,
        premisesId = premises.id,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.FieldValidationError).isTrue
      val validationError = result.entity as ValidatableActionResult.FieldValidationError

      assertThat(validationError.validationMessages["$.premisesId"]).isEqualTo("mustBeAnApprovedPremises")
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest returns GeneralValidationError if a premisesId is specified and the Premises does not exist`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      val premisesId = UUID.randomUUID()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockPremisesRepository.findByIdOrNull(premisesId) } returns null

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = null,
        premisesId = premisesId,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.FieldValidationError).isTrue
      val validationError = result.entity as ValidatableActionResult.FieldValidationError

      assertThat(validationError.validationMessages["$.premisesId"]).isEqualTo("doesNotExist")
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest returns GeneralValidationError if a bedId or premisesId is not specified`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = null,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val error = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(error.message).isEqualTo("You must specify either a bedId or a premisesId")
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest returns ConflictError if a Booking has already been made from the Placement Request`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .withBooking(
          BookingEntityFactory()
            .withBed(bed)
            .withPremises(premises)
            .produce(),
        )
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf()

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bed.id,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.ConflictError).isTrue
      val validationError = result.entity as ValidatableActionResult.ConflictError

      assertThat(validationError.message).isEqualTo("A Booking has already been made for this Placement Request")
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest returns GeneralError if the Placement Request has been withdrawn`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withIsWithdrawn(true)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .withBooking(
          BookingEntityFactory()
            .withBed(bed)
            .withPremises(premises)
            .produce(),
        )
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf()

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bed.id,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validationError = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validationError.message).isEqualTo("placementRequestIsWithdrawn")
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest returns ConflictError if Bed has a conflicting Booking`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      val premises = ApprovedPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf(
        BookingEntityFactory()
          .withArrivalDate(arrivalDate)
          .withDepartureDate(departureDate)
          .withPremises(premises)
          .withBed(bed)
          .produce(),
      )
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf()

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bed.id,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.ConflictError).isTrue
      val conflictError = result.entity as ValidatableActionResult.ConflictError

      assertThat(conflictError.message).isEqualTo("A Booking already exists for dates from $arrivalDate to $departureDate which overlaps with the desired dates")
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest returns ConflictError if Bed has a conflicting Lost Bed`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      val premises = ApprovedPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf(
        LostBedsEntityFactory()
          .withStartDate(arrivalDate)
          .withEndDate(departureDate)
          .withPremises(premises)
          .withBed(bed)
          .withYieldedReason { LostBedReasonEntityFactory().produce() }
          .produce(),
      )

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bed.id,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.ConflictError).isTrue
      val conflictError = result.entity as ValidatableActionResult.ConflictError

      assertThat(conflictError.message).isEqualTo("A Lost Bed already exists for dates from $arrivalDate to $departureDate which overlaps with the desired dates")
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest saves Booking, creates Domain Event and sends email`() {
      val arrivalDate = LocalDate.parse("2023-02-22")
      val departureDate = LocalDate.parse("2023-02-23")

      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

      val premises = ApprovedPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(application.crn)
        .produce()

      val staffUserDetails = StaffUserDetailsFactory()
        .withUsername(user.deliusUsername)
        .produce()

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockPlacementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { mockOffenderService.getOffenderByCrn(application.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)
      every { mockCruService.cruNameFromProbationAreaCode(staffUserDetails.probationArea.code) } returns "CRU NAME"
      every { mockDomainEventService.saveBookingMadeDomainEvent(any()) } just Runs
      every { mockCas1BookingEmailService.bookingMade(any(), any()) } just Runs

      val authorisableResult = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bed.id,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      val booking = assertBookingCreated(authorisableResult, application, premises, arrivalDate, departureDate)
      assertDomainEventSent(placementRequest, offenderDetails, premises, arrivalDate)
      assertEmailsSent(application, booking)
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest saves Booking, creates Domain Event and sends email when a premisesId is provided`() {
      val arrivalDate = LocalDate.parse("2023-02-22")
      val departureDate = LocalDate.parse("2023-02-23")

      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

      val premises = ApprovedPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      every { mockPremisesRepository.findByIdOrNull(premises.id) } returns premises

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(application.crn)
        .produce()

      val staffUserDetails = StaffUserDetailsFactory()
        .withUsername(user.deliusUsername)
        .produce()

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockPlacementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { mockOffenderService.getOffenderByCrn(application.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)
      every { mockCruService.cruNameFromProbationAreaCode(staffUserDetails.probationArea.code) } returns "CRU NAME"
      every { mockDomainEventService.saveBookingMadeDomainEvent(any()) } just Runs
      every { mockCas1BookingEmailService.bookingMade(any(), any()) } just Runs

      val authorisableResult = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = null,
        premisesId = premises.id,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      val booking = assertBookingCreated(authorisableResult, application, premises, arrivalDate, departureDate)
      assertDomainEventSent(placementRequest, offenderDetails, premises, arrivalDate)
      assertEmailsSent(application, booking)
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest saves Booking, creates Domain Event and sends email when a cancelled booking exists`() {
      val arrivalDate = LocalDate.parse("2023-02-22")
      val departureDate = LocalDate.parse("2023-02-23")

      val existingPremises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val existingRoom = RoomEntityFactory()
        .withPremises(existingPremises)
        .produce()

      val existingBed = BedEntityFactory()
        .withRoom(existingRoom)
        .produce()

      val existingBooking = BookingEntityFactory()
        .withPremises(existingPremises)
        .withBed(existingBed)
        .produce()

      val cancellation = CancellationEntityFactory()
        .withYieldedReason { CancellationReasonEntityFactory().produce() }
        .withBooking(existingBooking)
        .produce()

      existingBooking.cancellations = mutableListOf(cancellation)

      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .withBooking(existingBooking)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

      val premises = ApprovedPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      every { mockPremisesRepository.findByIdOrNull(premises.id) } returns premises

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(application.crn)
        .produce()

      val staffUserDetails = StaffUserDetailsFactory()
        .withUsername(user.deliusUsername)
        .produce()

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockPlacementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { mockOffenderService.getOffenderByCrn(application.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)
      every { mockCruService.cruNameFromProbationAreaCode(staffUserDetails.probationArea.code) } returns "CRU NAME"
      every { mockDomainEventService.saveBookingMadeDomainEvent(any()) } just Runs
      every { mockCas1BookingEmailService.bookingMade(any(), any()) } just Runs

      val authorisableResult = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = null,
        premisesId = premises.id,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      val booking = assertBookingCreated(authorisableResult, application, premises, arrivalDate, departureDate)
      assertDomainEventSent(placementRequest, offenderDetails, premises, arrivalDate)
      assertEmailsSent(application, booking)
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest saves successfully when the user is not assigned to the placement request and is a Workflow Manager`() {
      val arrivalDate = LocalDate.parse("2023-02-22")
      val departureDate = LocalDate.parse("2023-02-23")

      val workflowManager = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce().apply {
          this.roles.add(
            UserRoleAssignmentEntityFactory()
              .withUser(this)
              .withRole(UserRole.CAS1_WORKFLOW_MANAGER)
              .produce(),
          )
        }

      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

      val premises = ApprovedPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      every { mockPremisesRepository.findByIdOrNull(premises.id) } returns premises

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(application.crn)
        .produce()

      val staffUserDetails = StaffUserDetailsFactory()
        .withUsername(user.deliusUsername)
        .produce()

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockPlacementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { mockOffenderService.getOffenderByCrn(application.crn, workflowManager.deliusUsername, true) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockCommunityApiClient.getStaffUserDetails(workflowManager.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)
      every { mockCruService.cruNameFromProbationAreaCode(staffUserDetails.probationArea.code) } returns "CRU NAME"
      every { mockDomainEventService.saveBookingMadeDomainEvent(any()) } just Runs
      every { mockCas1BookingEmailService.bookingMade(any(), any()) } just Runs

      val authorisableResult = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = workflowManager,
        placementRequestId = placementRequest.id,
        bedId = null,
        premisesId = premises.id,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      val booking = assertBookingCreated(authorisableResult, application, premises, arrivalDate, departureDate)
      assertDomainEventSent(placementRequest, offenderDetails, premises, arrivalDate)
      assertEmailsSent(application, booking)
    }

    private fun assertBookingCreated(
      authorisableResult: AuthorisableActionResult<ValidatableActionResult<BookingEntity>>,
      application: ApprovedPremisesApplicationEntity,
      premises: ApprovedPremisesEntity,
      arrivalDate: LocalDate,
      departureDate: LocalDate,
    ): BookingEntity {
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success).isTrue

      val createdBooking = (validatableResult as ValidatableActionResult.Success).entity

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == application.crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.status == BookingStatus.confirmed &&
              it.adhoc == false
          },
        )
      }

      verify(exactly = 1) {
        mockPlacementRequestRepository.save(
          match {
            it.booking == createdBooking
          },
        )
      }

      return createdBooking
    }

    private fun assertDomainEventSent(
      placementRequest: PlacementRequestEntity,
      offenderDetails: OffenderDetailSummary,
      premises: ApprovedPremisesEntity,
      arrivalDate: LocalDate,
    ) {
      verify(exactly = 1) {
        mockDomainEventService.saveBookingMadeDomainEvent(
          match {
            val data = (it.data as BookingMadeEnvelope).eventDetails

            it.applicationId == placementRequest.application.id &&
              it.crn == application.crn &&
              data.applicationId == placementRequest.application.id &&
              data.applicationUrl == "http://frontend/applications/${placementRequest.application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == placementRequest.application.eventNumber &&
              data.premises == Premises(
              id = premises.id,
              name = premises.name,
              apCode = premises.apCode,
              legacyApCode = premises.qCode,
              localAuthorityAreaName = premises.localAuthorityArea!!.name,
            ) &&
              data.arrivalOn == arrivalDate &&
              data.applicationSubmittedOn == application.submittedAt!!.toInstant() &&
              data.releaseType == application.releaseType &&
              data.sentenceType == application.sentenceType &&
              data.situation == application.situation
          },
        )
      }
    }

    private fun assertEmailsSent(application: ApprovedPremisesApplicationEntity, booking: BookingEntity) {
      verify(exactly = 1) { mockCas1BookingEmailService.bookingMade(application, booking) }
    }
  }

  @Test
  fun `createTurnaround returns FieldValidationError if the number of working days is not a positive integer`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withProbationRegion(
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce(),
      )
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory()
          .produce(),
      )
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .withBed(bed)
      .produce()

    every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
    every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf()
    every { mockLostBedsRepository.findByBedIdAndOverlappingDate(any(), any(), any(), any()) } returns listOf()
    every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

    val negativeDaysResult = bookingService.createTurnaround(booking, -1)
    val zeroDaysResult = bookingService.createTurnaround(booking, 0)

    assertThat(negativeDaysResult).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((negativeDaysResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.workingDays", "isNotAPositiveInteger"),
    )

    assertThat(zeroDaysResult).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((zeroDaysResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.workingDays", "isNotAPositiveInteger"),
    )
  }

  @Test
  fun `createTurnaround returns Success with the persisted entity if the number of working days is a positive integer`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withProbationRegion(
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce(),
      )
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory()
          .produce(),
      )
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .withBed(bed)
      .produce()

    every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
    every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf()
    every { mockLostBedsRepository.findByBedIdAndOverlappingDate(any(), any(), any(), any()) } returns listOf()
    every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

    val result = bookingService.createTurnaround(booking, 2)

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.booking).isEqualTo(booking)
    assertThat(result.entity.workingDayCount).isEqualTo(2)
  }

  @Nested
  inner class GetWithdrawableState {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val premises = ApprovedPremisesEntityFactory()
      .withDefaultProbationRegion()
      .withDefaultLocalAuthorityArea()
      .produce()

    @Test
    fun `getWithdrawableState not withdrawable if has arrivals`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      booking.arrivals.add(
        ArrivalEntityFactory().withBooking(booking).produce(),
      )

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.withdrawable).isFalse()
    }

    @Test
    fun `getWithdrawableState not withdrawable if already cancelled`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      booking.cancellations.add(
        CancellationEntityFactory().withBooking(booking).withDefaults().produce(),
      )

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.withdrawable).isFalse()
    }

    @Test
    fun `getWithdrawableState withdrawable if has not arrivals and not already cancelled`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.withdrawable).isTrue()
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `getWithdrawableState userMayDirectlyWithdraw delegates to user access service`(canWithdraw: Boolean) {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns canWithdraw

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(canWithdraw)
    }

    @Test
    fun `getWithdrawableState doesn't block ancestor withdrawals if no arrivals`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .withArrivals(mutableListOf())
        .produce()

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.blockAncestorWithdrawals).isEqualTo(false)
    }

    @Test
    fun `getWithdrawableState block ancestor withdrawals if have arrivals`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      booking.arrivals.add(
        ArrivalEntityFactory()
          .withBooking(booking)
          .produce(),
      )

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.blockAncestorWithdrawals).isEqualTo(true)
    }
  }

  @Nested
  inner class MoveBooking {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addRoleForUnitTest(UserRole.CAS1_MANAGER)

    val premises = ApprovedPremisesEntityFactory()
      .withProbationRegion(
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce(),
      )
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory()
          .produce(),
      )
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    private val newRoom = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    private val newBed = BedEntityFactory()
      .withRoom(newRoom)
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .withBed(bed)
      .produce()

    @BeforeEach
    fun setup() {
      every { mockBedRepository.findByIdOrNull(newBed.id) } answers { newBed }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockBedMoveRepository.save(any()) } answers { it.invocation.args[0] as BedMoveEntity }
    }

    @Test
    fun `it returns unauthorised when the user is does not have a CAS1_MANAGER role`() {
      val result = bookingService.moveBooking(
        booking,
        newBed.id,
        "Some Notes",
        UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
    }

    @Test
    fun `it returns NotFound when the bed cannot be found`() {
      every { mockBedRepository.findByIdOrNull(any()) } answers { null }

      val result = bookingService.moveBooking(
        booking,
        newBed.id,
        "Some Notes",
        user,
      )

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `it returns a validation error when the bed does not belong to the same premises`() {
      every { mockBedRepository.findByIdOrNull(any()) } answers {
        val premises = ApprovedPremisesEntityFactory()
          .withProbationRegion(
            ProbationRegionEntityFactory()
              .withApArea(
                ApAreaEntityFactory()
                  .produce(),
              )
              .produce(),
          )
          .withLocalAuthorityArea(
            LocalAuthorityEntityFactory()
              .produce(),
          )
          .produce()

        val room = RoomEntityFactory()
          .withPremises(premises)
          .produce()

        BedEntityFactory()
          .withRoom(room)
          .produce()
      }

      val result = bookingService.moveBooking(
        booking,
        newBed.id,
        "Some Notes",
        user,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.FieldValidationError).isTrue
      val validationError = result.entity as ValidatableActionResult.FieldValidationError

      assertThat(validationError.validationMessages["$.bedId"]).isEqualTo("mustBelongToTheSamePremises")
    }

    @Test
    fun `it returns a validation error when the bed does not belong to an approved premises`() {
      every { mockBedRepository.findByIdOrNull(any()) } answers {
        val premises = TemporaryAccommodationPremisesEntityFactory()
          .withProbationRegion(
            ProbationRegionEntityFactory()
              .withApArea(
                ApAreaEntityFactory()
                  .produce(),
              )
              .produce(),
          )
          .withLocalAuthorityArea(
            LocalAuthorityEntityFactory()
              .produce(),
          )
          .produce()

        val room = RoomEntityFactory()
          .withPremises(premises)
          .produce()

        BedEntityFactory()
          .withRoom(room)
          .produce()
      }

      val result = bookingService.moveBooking(
        booking,
        newBed.id,
        "Some Notes",
        user,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.FieldValidationError).isTrue
      val validationError = result.entity as ValidatableActionResult.FieldValidationError

      assertThat(validationError.validationMessages["$.bedId"]).isEqualTo("mustBelongToApprovedPremises")
    }

    @Test
    fun `updates the booking and creates a new BedMoveEntity`() {
      val result = bookingService.moveBooking(
        booking,
        newBed.id,
        "Some Notes",
        user,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      val validatableActionResult = result.entity as ValidatableActionResult.Success
      val updatedBookingEntity = validatableActionResult.entity

      assertThat(updatedBookingEntity.bed).isEqualTo(newBed)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.id == booking.id &&
              it.bed!!.id == newBed.id
          },
        )
      }

      verify(exactly = 1) {
        mockBedMoveRepository.save(
          match {
            it.booking.id == booking.id &&
              it.previousBed!!.id == bed.id &&
              it.newBed.id == newBed.id &&
              it.notes == "Some Notes"
          },
        )
      }
    }
  }

  @Nested
  inner class CreateDateChange {
    private val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    private val approvedPremises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    private val approvedPremisesRoom = RoomEntityFactory()
      .withPremises(approvedPremises)
      .produce()

    private val approvedPremisesBed = BedEntityFactory()
      .withRoom(approvedPremisesRoom)
      .produce()

    private val temporaryAccommodationPremises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    private val temporaryAccommodationRoom = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremises)
      .produce()

    private val temporaryAccommodationBed = BedEntityFactory()
      .withRoom(temporaryAccommodationRoom)
      .produce()

    @Test
    fun `for non-AP Bookings, returns conflict error for conflicting Booking`() {
      val booking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      val conflictingBooking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .withArrivalDate(LocalDate.parse("2023-07-10"))
        .withDepartureDate(LocalDate.parse("2023-07-12"))
        .produce()

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-14"), booking.id) } returns listOf(conflictingBooking)

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-12"),
        newDepartureDate = LocalDate.parse("2023-07-14"),
      )

      assertThat(result is ValidatableActionResult.ConflictError).isTrue
      result as ValidatableActionResult.ConflictError
      assertThat(result.message).contains("A Booking already exists")
    }

    @Test
    fun `for non-AP Bookings, returns conflict error for conflicting Lost Bed`() {
      val booking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      val conflictingLostBed = LostBedsEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withStartDate(LocalDate.parse("2023-07-10"))
        .withEndDate(LocalDate.parse("2023-07-12"))
        .withYieldedReason { LostBedReasonEntityFactory().produce() }
        .produce()

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-14"), booking.id) } returns emptyList()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-12"), LocalDate.parse("2023-07-14"), null) } returns listOf(conflictingLostBed)

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-12"),
        newDepartureDate = LocalDate.parse("2023-07-14"),
      )

      assertThat(result is ValidatableActionResult.ConflictError).isTrue
      result as ValidatableActionResult.ConflictError
      assertThat(result.message).contains("A Lost Bed already exists")
    }

    @Test
    fun `returns validation error if new arrival date is after the new departure date`() {
      val booking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-14"), booking.id) } returns emptyList()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-16"), LocalDate.parse("2023-07-14"), null) } returns emptyList()

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-16"),
        newDepartureDate = LocalDate.parse("2023-07-14"),
      )

      assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
      result as ValidatableActionResult.FieldValidationError
      assertThat(result.validationMessages).containsEntry("$.newDepartureDate", "beforeBookingArrivalDate")
    }

    @Test
    fun `returns validation error if booking already has an arrival and attempting to change arrival date`() {
      val booking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .produce()
        .apply {
          arrivals += ArrivalEntityFactory()
            .withBooking(this)
            .produce()
        }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-15"),
        newDepartureDate = LocalDate.parse("2023-07-16"),
      )

      assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
      result as ValidatableActionResult.FieldValidationError
      assertThat(result.validationMessages).containsEntry("$.newArrivalDate", "arrivalDateCannotBeChangedOnArrivedBooking")
    }

    @Test
    fun `returns success when changing arrived booking by reducing departure date`() {
      val booking = BookingEntityFactory()
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .produce()
        .apply {
          arrivals += ArrivalEntityFactory()
            .withBooking(this)
            .produce()
        }

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = null,
        newDepartureDate = LocalDate.parse("2023-07-15"),
      )

      assertThat(result is ValidatableActionResult.Success).isTrue
      result as ValidatableActionResult.Success

      verify {
        mockDateChangeRepository.save(
          match {
            it.booking.id == booking.id &&
              it.changedByUser == user &&
              it.newArrivalDate == LocalDate.parse("2023-07-14") &&
              it.newDepartureDate == LocalDate.parse("2023-07-15") &&
              it.previousArrivalDate == LocalDate.parse("2023-07-14") &&
              it.previousDepartureDate == LocalDate.parse("2023-07-16")
          },
        )
      }

      verify {
        mockBookingRepository.save(
          match {
            it.id == booking.id &&
              it.arrivalDate == LocalDate.parse("2023-07-14") &&
              it.departureDate == LocalDate.parse("2023-07-15")
          },
        )
      }
    }

    @Test
    fun `returns success when changing non-arrived booking`() {
      val booking = BookingEntityFactory()
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .produce()

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-18"),
        newDepartureDate = LocalDate.parse("2023-07-22"),
      )

      assertThat(result is ValidatableActionResult.Success).isTrue
      result as ValidatableActionResult.Success

      verify {
        mockDateChangeRepository.save(
          match {
            it.booking.id == booking.id &&
              it.changedByUser == user &&
              it.newArrivalDate == LocalDate.parse("2023-07-18") &&
              it.newDepartureDate == LocalDate.parse("2023-07-22") &&
              it.previousArrivalDate == LocalDate.parse("2023-07-14") &&
              it.previousDepartureDate == LocalDate.parse("2023-07-16")
          },
        )
      }

      verify {
        mockBookingRepository.save(
          match {
            it.id == booking.id &&
              it.arrivalDate == LocalDate.parse("2023-07-18") &&
              it.departureDate == LocalDate.parse("2023-07-22")
          },
        )
      }
    }

    @Test
    fun `emits domain event when booking has associated application`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val booking = BookingEntityFactory()
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .withApplication(application)
        .withCrn(application.crn)
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(application.crn)
        .produce()

      val staffUserDetails = StaffUserDetailsFactory()
        .withUsername(user.deliusUsername)
        .withForenames("John Jacob")
        .withSurname("Johnson")
        .produce()

      every { mockOffenderService.getOffenderByCrn(application.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockDomainEventService.saveBookingChangedEvent(any()) } just Runs

      val newArrivalDate = LocalDate.parse("2023-07-18")
      val newDepartureDate = LocalDate.parse("2023-07-22")

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = newArrivalDate,
        newDepartureDate = newDepartureDate,
      )

      assertThat(result is ValidatableActionResult.Success).isTrue
      result as ValidatableActionResult.Success

      verify {
        mockDateChangeRepository.save(
          match {
            it.booking.id == booking.id &&
              it.changedByUser == user &&
              it.newArrivalDate == LocalDate.parse("2023-07-18") &&
              it.newDepartureDate == LocalDate.parse("2023-07-22") &&
              it.previousArrivalDate == LocalDate.parse("2023-07-14") &&
              it.previousDepartureDate == LocalDate.parse("2023-07-16")
          },
        )
      }

      verify {
        mockBookingRepository.save(
          match {
            it.id == booking.id &&
              it.arrivalDate == LocalDate.parse("2023-07-18") &&
              it.departureDate == LocalDate.parse("2023-07-22")
          },
        )
      }

      verify(exactly = 1) {
        mockDomainEventService.saveBookingChangedEvent(
          match {
            val data = (it.data as BookingChangedEnvelope).eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ) &&
              data.arrivalOn == newArrivalDate &&
              data.departureOn == newDepartureDate
          },
        )
      }
    }

    @Test
    fun `emits domain event when booking has associated offline application with an event number`() {
      val application = OfflineApplicationEntityFactory()
        .withEventNumber("123")
        .produce()

      val booking = BookingEntityFactory()
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .withOfflineApplication(application)
        .withCrn(application.crn)
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(application.crn)
        .produce()

      val staffUserDetails = StaffUserDetailsFactory()
        .withUsername(user.deliusUsername)
        .withForenames("John Jacob")
        .withSurname("Johnson")
        .produce()

      every { mockOffenderService.getOffenderByCrn(application.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockDomainEventService.saveBookingChangedEvent(any()) } just Runs

      val newArrivalDate = LocalDate.parse("2023-07-18")
      val newDepartureDate = LocalDate.parse("2023-07-22")

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = newArrivalDate,
        newDepartureDate = newDepartureDate,
      )

      assertThat(result is ValidatableActionResult.Success).isTrue
      result as ValidatableActionResult.Success

      verify(exactly = 1) {
        mockDomainEventService.saveBookingChangedEvent(
          match {
            val data = (it.data as BookingChangedEnvelope).eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.premises == Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ) &&
              data.arrivalOn == newArrivalDate &&
              data.departureOn == newDepartureDate
          },
        )
      }
    }

    @Test
    fun `does not emit domain event when booking has associated offline application without an event number`() {
      val application = OfflineApplicationEntityFactory()
        .withEventNumber(null)
        .produce()

      val booking = BookingEntityFactory()
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .withOfflineApplication(application)
        .withCrn(application.crn)
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(application.crn)
        .produce()

      val staffUserDetails = StaffUserDetailsFactory()
        .withUsername(user.deliusUsername)
        .withForenames("John Jacob")
        .withSurname("Johnson")
        .produce()

      every { mockOffenderService.getOffenderByCrn(application.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)

      every { mockWorkingDayCountService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockDomainEventService.saveBookingChangedEvent(any()) } just Runs

      val newArrivalDate = LocalDate.parse("2023-07-18")
      val newDepartureDate = LocalDate.parse("2023-07-22")

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = newArrivalDate,
        newDepartureDate = newDepartureDate,
      )

      assertThat(result is ValidatableActionResult.Success).isTrue
      result as ValidatableActionResult.Success

      verify {
        mockDateChangeRepository.save(
          match {
            it.booking.id == booking.id &&
              it.changedByUser == user &&
              it.newArrivalDate == LocalDate.parse("2023-07-18") &&
              it.newDepartureDate == LocalDate.parse("2023-07-22") &&
              it.previousArrivalDate == LocalDate.parse("2023-07-14") &&
              it.previousDepartureDate == LocalDate.parse("2023-07-16")
          },
        )
      }

      verify {
        mockBookingRepository.save(
          match {
            it.id == booking.id &&
              it.arrivalDate == LocalDate.parse("2023-07-18") &&
              it.departureDate == LocalDate.parse("2023-07-22")
          },
        )
      }

      verify(exactly = 0) {
        mockDomainEventService.saveBookingChangedEvent(
          any(),
        )
      }
    }
  }
}
