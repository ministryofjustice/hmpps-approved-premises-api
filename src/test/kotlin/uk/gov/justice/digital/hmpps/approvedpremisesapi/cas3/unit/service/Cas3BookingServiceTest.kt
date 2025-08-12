package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service

import io.mockk.Runs
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
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3ConfirmationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ConfirmationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3TurnaroundRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DestinationProviderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3DomainEventService as Cas3DomainEventService

class Cas3BookingServiceTest {
  private val mockCas3PremisesService = mockk<Cas3PremisesService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockCas3DomainEventService = mockk<Cas3DomainEventService>()
  private val mockWorkingDayService = mockk<WorkingDayService>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockArrivalRepository = mockk<ArrivalRepository>()
  private val mockCancellationRepository = mockk<CancellationRepository>()
  private val mockCas3ConfirmationRepository = mockk<Cas3ConfirmationRepository>()
  private val mockExtensionRepository = mockk<ExtensionRepository>()
  private val mockDepartureRepository = mockk<DepartureRepository>()
  private val mockDepartureReasonRepository = mockk<DepartureReasonRepository>()
  private val mockMoveOnCategoryRepository = mockk<MoveOnCategoryRepository>()
  private val mockDestinationProviderRepository = mockk<DestinationProviderRepository>()
  private val mockCancellationReasonRepository = mockk<CancellationReasonRepository>()
  private val mockBedRepository = mockk<BedRepository>()
  private val mockCas3VoidBedspacesRepository = mockk<Cas3VoidBedspacesRepository>()
  private val mockCas3TurnaroundRepository = mockk<Cas3TurnaroundRepository>()
  private val mockAssessmentRepository = mockk<AssessmentRepository>()
  private val mockUserAccessService = mockk<UserAccessService>()
  private val mockAssessmentService = mockk<AssessmentService>()
  private val mockFeatureFlagService = mockk<FeatureFlagService>()

  fun createCas3BookingService(): Cas3BookingService = Cas3BookingService(
    bookingRepository = mockBookingRepository,
    bedRepository = mockBedRepository,
    cas3ConfirmationRepository = mockCas3ConfirmationRepository,
    assessmentRepository = mockAssessmentRepository,
    arrivalRepository = mockArrivalRepository,
    departureRepository = mockDepartureRepository,
    departureReasonRepository = mockDepartureReasonRepository,
    moveOnCategoryRepository = mockMoveOnCategoryRepository,
    cancellationRepository = mockCancellationRepository,
    cancellationReasonRepository = mockCancellationReasonRepository,
    cas3VoidBedspacesRepository = mockCas3VoidBedspacesRepository,
    cas3TurnaroundRepository = mockCas3TurnaroundRepository,
    extensionRepository = mockExtensionRepository,
    cas3PremisesService = mockCas3PremisesService,
    assessmentService = mockAssessmentService,
    userAccessService = mockUserAccessService,
    offenderService = mockOffenderService,
    workingDayService = mockWorkingDayService,
    cas3DomainEventService = mockCas3DomainEventService,
    featureFlagService = mockFeatureFlagService,
  )

  private val cas3BookingService = createCas3BookingService()

  private val user = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .produce()

  @Nested
  inner class GetBookingPremises {
    @Test
    fun `getBookingForPremises returns BookingNotFound when booking with provided ID does not exist`() {
      val bookingId = UUID.randomUUID()
      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withId(UUID.randomUUID())
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
        .produce()

      every { mockBookingRepository.findByIdOrNull(bookingId) } returns null

      assertThat(cas3BookingService.getBookingForPremises(premises, bookingId))
        .isEqualTo(GetBookingForPremisesResult.BookingNotFound)
    }

    @Test
    fun `getBookingForPremises returns BookingNotFound when booking does not belong to Premises`() {
      val bookingId = UUID.fromString("75ed7091-1767-4901-8c2b-371dd0f5864c")

      val premisesEntityFactory = TemporaryAccommodationPremisesEntityFactory()
        .withId(UUID.randomUUID())
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }

      val keyWorker = ContextStaffMemberFactory().produce()

      every { mockBookingRepository.findByIdOrNull(bookingId) } returns BookingEntityFactory()
        .withId(bookingId)
        .withPremises(premisesEntityFactory.withId(UUID.randomUUID()).produce())
        .withStaffKeyWorkerCode(keyWorker.code)
        .produce()

      assertThat(cas3BookingService.getBookingForPremises(premisesEntityFactory.withId(UUID.randomUUID()).produce(), bookingId))
        .isEqualTo(GetBookingForPremisesResult.BookingNotFound)
    }

    @Test
    fun `getBookingForPremises returns Success when booking does belong to Premises`() {
      val premisesId = UUID.fromString("8461d08b-0e3f-426a-a941-0ada4160e6db")
      val bookingId = UUID.fromString("75ed7091-1767-4901-8c2b-371dd0f5864c")

      val premisesEntity = TemporaryAccommodationPremisesEntityFactory()
        .withId(premisesId)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
        .produce()

      val keyWorker = ContextStaffMemberFactory().produce()

      val bookingEntity = BookingEntityFactory()
        .withId(bookingId)
        .withPremises(premisesEntity)
        .withStaffKeyWorkerCode(keyWorker.code)
        .produce()

      every { mockBookingRepository.findByIdOrNull(bookingId) } returns bookingEntity

      assertThat(cas3BookingService.getBookingForPremises(premisesEntity, bookingId))
        .isEqualTo(GetBookingForPremisesResult.Success(bookingEntity))
    }
  }

  @Nested
  inner class FindFutureBookingsForPremises {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val fullPersonOffenderCaseSummary = CaseSummaryFactory().produce()
    private val fullPersonSummaryInfo = PersonSummaryInfoResult.Success.Full(
      crn = fullPersonOffenderCaseSummary.crn,
      summary = fullPersonOffenderCaseSummary,
    )

    private val restrictedPersonSummaryInfo = PersonSummaryInfoResult.Success.Restricted(
      randomStringMultiCaseWithNumbers(8),
      randomStringMultiCaseWithNumbers(10),
    )

    @Test
    fun `findFutureBookingsForPremises returns NotFound when premises with provided ID does not exist`() {
      val premisesId = UUID.randomUUID()

      every { mockCas3PremisesService.getPremises(premisesId) } returns null

      val result = cas3BookingService.findFutureBookingsForPremises(premisesId, listOf(BookingStatus.provisional), user)

      assertThatCasResult(result).isNotFound("Premises", premisesId.toString())
    }

    @Test
    fun `findFutureBookingsForPremises returns Unauthorised if the user cannot view the bookings`() {
      every { mockCas3PremisesService.getPremises(premises.id) } returns premises
      every { mockUserAccessService.userCanManageCas3PremisesBookings(user, premises) } returns false

      val result = cas3BookingService.findFutureBookingsForPremises(premises.id, listOf(BookingStatus.provisional), user)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `findFutureBookingsForPremises returns Success when there are current or future bookings belong to Premises`() {
      val bookingStatuses = listOf(BookingStatus.provisional, BookingStatus.confirmed, BookingStatus.arrived)

      val provisionalBookingEntity = createBooking(
        premises,
        fullPersonOffenderCaseSummary.crn,
        BookingStatus.provisional,
        LocalDate.now().plusDays(3),
        LocalDate.now().plusDays(31),
      )

      val confirmedBookingEntity = createBooking(
        premises,
        fullPersonOffenderCaseSummary.crn,
        BookingStatus.confirmed,
        LocalDate.now().plusDays(21),
        LocalDate.now().plusDays(76),
      )

      val arrivedBookingEntity = createBooking(
        premises,
        fullPersonOffenderCaseSummary.crn,
        BookingStatus.arrived,
        LocalDate.now().minusDays(3),
        LocalDate.now().plusDays(22),
      )

      val restrictedOffenderBooking = createBooking(
        premises,
        restrictedPersonSummaryInfo.crn,
        BookingStatus.arrived,
        LocalDate.now().minusDays(17),
        LocalDate.now().plusDays(31),
      )

      every { mockCas3PremisesService.getPremises(premises.id) } returns premises
      every {
        mockOffenderService.getPersonSummaryInfoResults(
          setOf(fullPersonOffenderCaseSummary.crn, restrictedPersonSummaryInfo.crn),
          user.cas3LaoStrategy(),
        )
      } returns listOf(fullPersonSummaryInfo, restrictedPersonSummaryInfo)
      every { mockUserAccessService.userCanManageCas3PremisesBookings(user, premises) } returns true
      every {
        mockBookingRepository.findFutureBookingsByPremisesIdAndStatus(
          ServiceName.temporaryAccommodation.value,
          premises.id,
          LocalDate.now(),
          bookingStatuses,
        )
      } returns listOf(provisionalBookingEntity, confirmedBookingEntity, arrivedBookingEntity, restrictedOffenderBooking)

      assertThat(cas3BookingService.findFutureBookingsForPremises(premises.id, bookingStatuses, user))
        .isEqualTo(
          CasResult.Success(
            listOf(
              Cas3BookingService.BookingAndPersons(provisionalBookingEntity, fullPersonSummaryInfo),
              Cas3BookingService.BookingAndPersons(confirmedBookingEntity, fullPersonSummaryInfo),
              Cas3BookingService.BookingAndPersons(arrivedBookingEntity, fullPersonSummaryInfo),
              Cas3BookingService.BookingAndPersons(restrictedOffenderBooking, restrictedPersonSummaryInfo),
            ),
          ),
        )
    }

    private fun createBooking(
      premises: PremisesEntity,
      crn: String,
      status: BookingStatus,
      arrivalDate: LocalDate,
      departureDate: LocalDate,
    ): BookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withCrn(crn)
      .withStatus(status)
      .withArrivalDate(arrivalDate)
      .withDepartureDate(departureDate)
      .produce()
  }

  @Nested
  inner class CreateDeparture {
    val premises = TemporaryAccommodationPremisesEntityFactory()
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
      .withServiceScope("temporary-accommodation")
      .produce()
    private val moveOnCategoryEntity = MoveOnCategoryEntityFactory()
      .withServiceScope("temporary-accommodation")
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
    fun `createDeparture returns FieldValidationError with correct param to message map when dateTime in past supplied`() {
      every { mockFeatureFlagService.getBooleanFlag("cas3-validate-booking-departure-in-future") } returns false

      val result = cas3BookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00"),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.dateTime", "beforeBookingArrivalDate"),
      )
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when departure date is in the future`() {
      every { mockFeatureFlagService.getBooleanFlag("cas3-validate-booking-departure-in-future") } returns true

      val result = cas3BookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().plusDays(1),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.dateTime", "departureDateInFuture"),
      )
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when invalid departure reason supplied`() {
      every { mockDepartureReasonRepository.findByIdOrNull(any()) } returns null
      every { mockFeatureFlagService.getBooleanFlag("cas3-validate-booking-departure-in-future") } returns false

      val result = cas3BookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().minusMinutes(1),
        reasonId = UUID.randomUUID(),
        moveOnCategoryId = moveOnCategoryId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.reasonId", "doesNotExist"),
      )
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when the departure reason has the wrong service scope`() {
      every { mockDepartureReasonRepository.findByIdOrNull(any()) } returns DepartureReasonEntityFactory()
        .withServiceScope(ServiceName.temporaryAccommodation.value)
        .produce()
      every { mockFeatureFlagService.getBooleanFlag("cas3-validate-booking-departure-in-future") } returns false

      val result = cas3BookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().minusMinutes(1),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.reasonId", "incorrectDepartureReasonServiceScope"),
      )
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when invalid move on category supplied`() {
      every { mockMoveOnCategoryRepository.findByIdOrNull(any()) } returns null
      every { mockFeatureFlagService.getBooleanFlag("cas3-validate-booking-departure-in-future") } returns false

      val result = cas3BookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().plusMinutes(1),
        reasonId = departureReasonId,
        moveOnCategoryId = UUID.randomUUID(),
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
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
      every { mockFeatureFlagService.getBooleanFlag("cas3-validate-booking-departure-in-future") } returns false

      val result = cas3BookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().plusMinutes(1),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.moveOnCategoryId", "incorrectMoveOnCategoryServiceScope"),
      )
    }

    @Test
    fun `createDeparture for a booking returns Success with correct result when validation passed and saves a domain event`() {
      val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
      val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")

      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val bookingEntity = createBooking(probationRegion)

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

      every { mockFeatureFlagService.getBooleanFlag("cas3-validate-booking-departure-in-future") } returns false

      val user = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      every { mockCas3DomainEventService.savePersonDepartedEvent(any(BookingEntity::class), user) } just Runs

      val result = cas3BookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00"),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.booking).isEqualTo(bookingEntity)
      assertThat(result.value.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-24T15:00:00+01:00"))
      assertThat(result.value.reason).isEqualTo(reasonEntity)
      assertThat(result.value.moveOnCategory).isEqualTo(moveOnCategoryEntity)
      assertThat(result.value.destinationProvider).isEqualTo(null)
      assertThat(result.value.reason).isEqualTo(reasonEntity)
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.booking.status).isEqualTo(BookingStatus.departed)

      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonDepartedEvent(bookingEntity, user)
      }
      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonDepartedEvent(any(BookingEntity::class), user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonDepartureUpdatedEvent(any(BookingEntity::class), user)
      }
    }

    @Test
    fun `createDeparture for a booking successfully create departure update event and saves a domain event`() {
      val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
      val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
      val departureDateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
      val notes = "Some notes about the departure"
      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val bookingEntity = createBooking(probationRegion)

      val reasonEntity = DepartureReasonEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()
      val moveOnCategoryEntity = MoveOnCategoryEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()

      val user = UserEntityFactory()
        .withProbationRegion(probationRegion)
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
      every { mockCas3DomainEventService.savePersonDepartureUpdatedEvent(any(BookingEntity::class), user) } just Runs
      every { mockFeatureFlagService.getBooleanFlag("cas3-validate-booking-departure-in-future") } returns false

      val result = cas3BookingService.createDeparture(
        booking = bookingEntity,
        dateTime = departureDateTime,
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        notes = notes,
        user = user,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.booking).isEqualTo(bookingEntity)
      assertThat(result.value.dateTime).isEqualTo(departureDateTime)
      assertThat(result.value.reason).isEqualTo(reasonEntity)
      assertThat(result.value.moveOnCategory).isEqualTo(moveOnCategoryEntity)
      assertThat(result.value.destinationProvider).isEqualTo(null)
      assertThat(result.value.reason).isEqualTo(reasonEntity)
      assertThat(result.value.notes).isEqualTo(notes)
      assertThat(result.value.booking.status).isEqualTo(BookingStatus.departed)

      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonDepartureUpdatedEvent(any(BookingEntity::class), user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonDepartedEvent(any(BookingEntity::class), user)
      }
    }
  }

  @Nested
  inner class CreateArrival {
    private val bookingEntity = createBooking()

    @BeforeEach
    fun setup() {
      every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as ArrivalEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockCas3DomainEventService.savePersonArrivedEvent(any(BookingEntity::class), any(UserEntity::class)) } just Runs
    }

    @Test
    fun `createArrival returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val arrivalDate = LocalDate.now().minusDays(1)
      val expectedDepartureDate = arrivalDate.minusDays(2)

      val result = cas3BookingService.createArrival(
        booking = bookingEntity,
        arrivalDate = arrivalDate,
        expectedDepartureDate = expectedDepartureDate,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.expectedDepartureDate", "beforeBookingArrivalDate")

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

      every { mockCas3DomainEventService.savePersonArrivedUpdatedEvent(any(BookingEntity::class), any(UserEntity::class)) } just Runs

      val arrivalDate = LocalDate.now().minusDays(1)
      val expectedDepartureDate = arrivalDate.plusDays(2)

      val result = cas3BookingService.createArrival(
        booking = bookingEntity,
        arrivalDate = arrivalDate,
        expectedDepartureDate = expectedDepartureDate,
        notes = "notes",
        user = userEntity,
      )

      assertThatCasResult(result).isSuccess().with { arrival ->
        assertThat(arrival.arrivalDate).isEqualTo(arrivalDate)
        assertThat(arrival.arrivalDateTime).isEqualTo(arrivalDate.toLocalDateTime().toInstant())
        assertThat(arrival.expectedDepartureDate).isEqualTo(expectedDepartureDate)
        assertThat(arrival.notes).isEqualTo("notes")
        assertThat(arrival.booking.status).isEqualTo(BookingStatus.arrived)
      }

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
    fun `createArrival returns Success with correct result when validation passed and saves domain event`() {
      val userEntity = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val arrivalDate = LocalDate.now().minusDays(1)
      val expectedDepartureDate = arrivalDate.plusDays(2)

      val result = cas3BookingService.createArrival(
        booking = bookingEntity,
        arrivalDate = arrivalDate,
        expectedDepartureDate = expectedDepartureDate,
        notes = "notes",
        user = userEntity,
      )

      assertThatCasResult(result).isSuccess().with { arrival ->
        assertThat(arrival.arrivalDate).isEqualTo(arrivalDate)
        assertThat(arrival.arrivalDateTime).isEqualTo(arrivalDate.toLocalDateTime().toInstant())
        assertThat(arrival.expectedDepartureDate).isEqualTo(expectedDepartureDate)
        assertThat(arrival.notes).isEqualTo("notes")
        assertThat(arrival.booking.status).isEqualTo(BookingStatus.arrived)
      }

      verify(exactly = 1) { mockArrivalRepository.save(any()) }
      verify(exactly = 1) { mockBookingRepository.save(any()) }
      verify(exactly = 1) { mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, userEntity) }
    }

    @Test
    fun `createArrival returns Success with correct result when validation passed and saves domain event without staff detail`() {
      every { mockCas3DomainEventService.savePersonArrivedEvent(any(BookingEntity::class), user) } just Runs

      val arrivalDate = LocalDate.now().minusDays(1)
      val expectedDepartureDate = arrivalDate.plusDays(2)

      val result = cas3BookingService.createArrival(
        booking = bookingEntity,
        arrivalDate = arrivalDate,
        expectedDepartureDate = expectedDepartureDate,
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with { arrival ->
        assertThat(arrival.arrivalDate).isEqualTo(arrivalDate)
        assertThat(arrival.arrivalDateTime).isEqualTo(arrivalDate.toLocalDateTime().toInstant())
        assertThat(arrival.expectedDepartureDate).isEqualTo(expectedDepartureDate)
        assertThat(arrival.notes).isEqualTo("notes")
        assertThat(arrival.booking.status).isEqualTo(BookingStatus.arrived)
      }

      verify(exactly = 1) { mockArrivalRepository.save(any()) }
      verify(exactly = 1) { mockBookingRepository.save(any()) }
      verify(exactly = 1) { mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, user) }
    }

    private fun createBooking() = BookingEntityFactory()
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
  }

  @Nested
  inner class CreateCancellation {

    @Test
    fun `createCancellation returns Success with correct result and emits a domain event`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

      val bookingEntity = createBookingEntity()

      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(BookingEntity::class), user) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = cas3BookingService.createCancellation(
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
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation returns Success with correct result and emits cancelled-updated domain event`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createBookingEntity()
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()
      val cancellationEntity = CancellationEntityFactory().withBooking(bookingEntity).withReason(reasonEntity).produce()
      bookingEntity.cancellations += cancellationEntity
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(BookingEntity::class), user) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = cas3BookingService.createCancellation(
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
        mockCas3DomainEventService.saveBookingCancelledEvent(any(BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation returns Success and move the assessment to read-to-place state`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(BookingEntity::class), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } returns CasResult.Success(assessmentEntity!!)
      mockkStatic(Sentry::class)

      val result = cas3BookingService.createCancellation(
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
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, null, any())
      }
      verify(exactly = 0) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns Success and not throwing error when acceptAssessment is failed with forbidden error`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(BookingEntity::class), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } returns CasResult.Unauthorised()
      mockkStatic(Sentry::class)
      every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

      val result = cas3BookingService.createCancellation(
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
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, null, any())
      }
      verify(exactly = 1) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns Success and not throwing error when acceptAssessment is failed with runtime exception`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(BookingEntity::class), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } throws RuntimeException("some-exception")
      mockkStatic(Sentry::class)

      val result = cas3BookingService.createCancellation(
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
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, null, any())
      }
      verify(exactly = 1) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns exception when acceptAssessment is failed with unexpected exception`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(BookingEntity::class), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } throws Throwable("some-exception")
      mockkStatic(Sentry::class)

      assertThatExceptionOfType(Throwable::class.java)
        .isThrownBy {
          cas3BookingService.createCancellation(
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
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, null, any())
      }
      verify(exactly = 0) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns exception when unexpected exception occurred and acceptAssessment is not invoked`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } throws RuntimeException("som-exception")
      mockkStatic(Sentry::class)

      assertThatExceptionOfType(RuntimeException::class.java)
        .isThrownBy {
          cas3BookingService.createCancellation(
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
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 0) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 0) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, null, any())
      }
      verify(exactly = 0) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns Success and not throwing error when accept assessment is failed with bad request error`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(BookingEntity::class), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } returns CasResult.GeneralValidationError("Error")
      mockkStatic(Sentry::class)
      every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

      val result = cas3BookingService.createCancellation(
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
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, null, any())
      }
      verify(exactly = 1) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns Success and not invoke acceptAssessment when assessment is empty`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createBookingWithAssessment(false)
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(BookingEntity::class), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = cas3BookingService.createCancellation(
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
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 0) {
        mockAssessmentService.acceptAssessment(user, any(), any(), null, null, null, any())
      }
      verify(exactly = 0) {
        Sentry.captureException(any())
      }
    }

    private fun createBookingEntity(): BookingEntity {
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

    private fun createBookingWithAssessment(withAssessment: Boolean = true): BookingEntity {
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

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf()
      every { mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(any(), any(), any(), any()) } returns listOf()
    }

    @Test
    fun `Success with correct result when a booking has a new departure date before the existing departure date`() {
      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-10"))
        .withDepartureDate(LocalDate.parse("2022-08-26"))
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      val result = cas3BookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = LocalDate.parse("2022-08-25"),
        notes = "notes",
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.newDepartureDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.previousDepartureDate).isEqualTo(LocalDate.parse("2022-08-26"))
      assertThat(result.entity.notes).isEqualTo("notes")
    }

    @Test
    fun `FieldValidationError when a booking has a new departure date before the arrival date`() {
      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-26"))
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      val result = cas3BookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = LocalDate.parse("2022-08-25"),
        notes = "notes",
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.newDepartureDate", "beforeBookingArrivalDate"),
      )
    }
  }

  @Nested
  inner class CreateConfirmation {

    @Test
    fun `createConfirmation returns GeneralValidationError with correct message when Booking already has a Confirmation`() {
      val bookingEntity = BookingEntityFactory()
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
        .produce()

      val confirmationEntity = Cas3ConfirmationEntityFactory()
        .withBooking(bookingEntity)
        .produce()

      bookingEntity.confirmation = confirmationEntity

      val result = cas3BookingService.createConfirmation(
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
      val bookingEntity = createBooking(null)

      every { mockCas3ConfirmationRepository.save(any()) } answers { it.invocation.args[0] as Cas3ConfirmationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(BookingEntity::class), user) } just Runs

      val result = cas3BookingService.createConfirmation(
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
      val bookingEntity = createBooking(null)

      every { mockCas3ConfirmationRepository.save(any()) } answers { it.invocation.args[0] as Cas3ConfirmationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(BookingEntity::class), user) } just Runs

      val result = cas3BookingService.createConfirmation(
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
        mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(any())
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

      val bookingEntity = createBooking(application)

      every { mockCas3ConfirmationRepository.save(any()) } answers { it.invocation.args[0] as Cas3ConfirmationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(BookingEntity::class), user) } just Runs
      every { mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id) } returns assessment
      every { mockAssessmentService.closeAssessment(user, assessment.id) } returns CasResult.Success(assessment)
      mockkStatic(Sentry::class)

      val result = cas3BookingService.createConfirmation(
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
        mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id)
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
      val bookingEntity = createBooking(application)

      every { mockCas3ConfirmationRepository.save(any()) } answers { it.invocation.args[0] as Cas3ConfirmationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(BookingEntity::class), user) } just Runs
      every { mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id) } returns null

      val result = cas3BookingService.createConfirmation(
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
        mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id)
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

      val bookingEntity = createBooking(application)

      every { mockCas3ConfirmationRepository.save(any()) } answers { it.invocation.args[0] as Cas3ConfirmationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(BookingEntity::class), user) } just Runs
      every { mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id) } returns assessment
      every { mockAssessmentService.closeAssessment(user, assessment.id) } returns CasResult.Unauthorised()
      mockkStatic(Sentry::class)
      every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

      val result = cas3BookingService.createConfirmation(
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
        mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id)
      }
      verify(exactly = 1) {
        Sentry.captureException(any())
      }
    }

    private fun createBooking(application: TemporaryAccommodationApplicationEntity?) = BookingEntityFactory()
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
  }

  @Nested
  inner class CreateBooking {

    @BeforeEach
    fun setup() {
      every { mockBedRepository.findArchivedBedByBedIdAndDate(any(), any()) } returns null
      every { mockOffenderService.getPersonSummaryInfoResult(eq("CRN123"), eq(LaoStrategy.NeverRestricted)) } returns PersonSummaryInfoResult.Success.Full(
        "CRN123",
        CaseSummaryFactory().withCrn("CRN123").withName(Name("John", "Smith", emptyList())).produce(),
      )
      every { mockFeatureFlagService.getBooleanFlag("cas3-validate-booking-arrival-after-bedspace-start-date") } returns true
    }

    @Test
    fun `createBooking returns FieldValidationError if Bed ID is not provided`() {
      val crn = "CRN123"
      val assessmentId = UUID.randomUUID()
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)

      val (premises, _) = createPremisesAndBedspace(bedspaceStartDate)

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        bedspaceStartDate.plusDays(10),
        bedspaceStartDate.plusDays(90),
        bedId = null,
        assessmentId,
        false,
      )

      assertThatCasResult(authorisableResult).isFieldValidationError().hasMessage("$.bedId", "empty")
    }

    @Test
    fun `createBooking returns FieldValidationError if Departure Date is before Arrival Date`() {
      val crn = "CRN123"
      val bedId = UUID.fromString("3b2f46de-a785-45ab-ac02-5e532c600647")
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(15)
      val departureDate = arrivalDate.minusDays(1)
      val assessmentId = UUID.randomUUID()

      every { mockBedRepository.findByIdOrNull(bedId) } returns null

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedId, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedId,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, _) = createPremisesAndBedspace(bedspaceStartDate)

      val authorisableResult = cas3BookingService.createBooking(
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

      assertThatCasResult(authorisableResult).isFieldValidationError().hasMessage("$.departureDate", "beforeBookingArrivalDate")
    }

    @Test
    fun `createBooking returns FieldValidationError if Bed does not exist`() {
      val crn = "CRN123"
      val bedId = UUID.fromString("3b2f46de-a785-45ab-ac02-5e532c600647")
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(3)
      val departureDate = arrivalDate.randomDateAfter(12)
      val assessmentId = UUID.randomUUID()

      every { mockBedRepository.findByIdOrNull(bedId) } returns null
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedId, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedId,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, _) = createPremisesAndBedspace(bedspaceStartDate)

      val authorisableResult = cas3BookingService.createBooking(
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

      assertThatCasResult(authorisableResult).isFieldValidationError().hasMessage("$.bedId", "doesNotExist")
    }

    @Test
    fun `createBooking returns FieldValidationError if Bedspace Start Date is after Booking Arrival Date`() {
      val crn = "CRN123"
      val bedId = UUID.randomUUID()
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateBefore(20)
      val departureDate = arrivalDate.plusDays(84)

      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)
      val assessment = createAssessment(user)

      every { mockBedRepository.findByIdOrNull(bedId) } returns bedspace

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedId, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedId,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedId,
        assessment.id,
        false,
      )

      assertThatCasResult(authorisableResult).isFieldValidationError().hasMessage("$.arrivalDate", "bookingArrivalDateBeforeBedspaceStartDate")
    }

    @Test
    fun `createBooking returns FieldValidationError if Application is provided and does not exist`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(15)
      val departureDate = arrivalDate.randomDateAfter(45)
      val assessmentId = UUID.randomUUID()

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, bed) = createPremisesAndBedspace(bedspaceStartDate)

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null
      every { mockCas3TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val authorisableResult = cas3BookingService.createBooking(
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

      assertThatCasResult(authorisableResult).isFieldValidationError().hasMessage("$.assessmentId", "doesNotExist")
    }

    @Test
    fun `createBooking saves Booking and creates domain event`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(3)
      val departureDate = arrivalDate.randomDateAfter(33)

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)
      val assessment = createAssessment(user)

      every { mockBedRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedspace.id, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { mockCas3TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessment.id,
        false,
      )

      assertThatCasResult(authorisableResult).isSuccess()

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == assessment.application &&
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
              it.application == assessment.application
          },
          user,
        )
      }
    }

    @Test
    fun `createBooking does not attach the application if no ID is provided`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(7)
      val departureDate = arrivalDate.randomDateAfter(65)

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)

      every { mockBedRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedspace.id, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()

      every { mockCas3TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      mockkStatic(Sentry::class)

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        null,
        false,
      )

      assertThatCasResult(authorisableResult).isSuccess()

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
    fun `createBooking automatically creates a Turnaround of zero days if 'enableTurnarounds' is false`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(14)
      val departureDate = arrivalDate.randomDateAfter(10)

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)
      val assessment = createAssessment(user)

      every { mockBedRepository.findByIdOrNull(bedspace.id) } returns bedspace

      val bookingSlot = slot<BookingEntity>()
      every { mockBookingRepository.save(capture(bookingSlot)) } answers { bookingSlot.captured }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedspace.id, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { mockCas3TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessment.id,
        false,
      )

      assertThatCasResult(authorisableResult).isSuccess()

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == assessment.application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3TurnaroundRepository.save(
          match {
            it.booking == bookingSlot.captured &&
              it.workingDayCount == 0
          },
        )
      }
    }

    @Test
    fun `createBooking automatically creates a Turnaround of the number of working days specified on the premises if 'enableTurnarounds' is true`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(19)
      val departureDate = arrivalDate.randomDateAfter(41)

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)
      val assessment = createAssessment(user)

      every { mockBedRepository.findByIdOrNull(bedspace.id) } returns bedspace

      val bookingSlot = slot<BookingEntity>()
      every { mockBookingRepository.save(capture(bookingSlot)) } answers { bookingSlot.captured }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedspace.id, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { mockCas3TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessment.id,
        true,
      )

      assertThatCasResult(authorisableResult).isSuccess()

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == assessment.application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3TurnaroundRepository.save(
          match {
            it.booking == bookingSlot.captured &&
              it.workingDayCount == premises.turnaroundWorkingDays
          },
        )
      }
    }

    @Test
    fun `createBooking saves Booking and creates domain event and log error when closing assessment failed with authentication error`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(12)
      val departureDate = arrivalDate.randomDateAfter(27)

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)
      val assessment = createAssessment(user)

      every { mockBedRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedspace.id, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { mockCas3TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      mockkStatic(Sentry::class)
      every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessment.id,
        false,
      )

      assertThatCasResult(authorisableResult).isSuccess()

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == assessment.application &&
              it.status == BookingStatus.provisional &&
              it.offenderName == "John Smith"
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
              it.application == assessment.application
          },
          user,
        )
      }
    }

    @Test
    fun `createBooking saves Booking and creates domain event and log error when closing assessment failed with field validation error`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(5)
      val departureDate = arrivalDate.randomDateAfter(31)

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)
      val assessment = createAssessment(user)

      every { mockBedRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedspace.id, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { mockCas3TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessment.id,
        false,
      )

      assertThatCasResult(authorisableResult).isSuccess()

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == assessment.application &&
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
              it.application == assessment.application
          },
          user,
        )
      }
    }

    @Test
    fun `createBooking saves Booking and creates domain event and log error when closing assessment failed with runtime exception`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(16)
      val departureDate = arrivalDate.randomDateAfter(77)

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)
      val assessment = createAssessment(user)

      every { mockBedRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedspace.id, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { mockCas3TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessment.id,
        false,
      )

      assertThatCasResult(authorisableResult).isSuccess()

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == assessment.application &&
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
              it.application == assessment.application
          },
          user,
        )
      }
    }

    @Test
    fun `createBooking not closing assessments and returns FieldValidationError if Application is provided and does not exist`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(2)
      val departureDate = arrivalDate.randomDateAfter(54)
      val assessmentId = UUID.randomUUID()

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)

      every { mockBedRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedspace.id, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null
      every { mockCas3TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessmentId,
        false,
      )

      assertThatCasResult(authorisableResult).isFieldValidationError().hasMessage("$.assessmentId", "doesNotExist")
    }

    @Test
    fun `createBooking fail to save booking and not closing assessment when saving booking triggered Database exception`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(13)
      val departureDate = arrivalDate.randomDateAfter(61)

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)
      val assessment = createAssessment(user)

      every { mockBedRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } throws RuntimeException("DB exception")

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedspace.id, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { mockCas3TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      assertThatExceptionOfType(RuntimeException::class.java)
        .isThrownBy {
          cas3BookingService.createBooking(
            user,
            premises,
            crn,
            "NOMS123",
            arrivalDate,
            departureDate,
            bedspace.id,
            assessment.id,
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
              it.application == assessment.application &&
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
              it.application == assessment.application
          },
          user,
        )
      }
    }

    @Test
    fun `createBooking throws conflict error when bedspace is archived for the requested bed`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(14)
      val departureDate = arrivalDate.randomDateAfter(35)

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate, LocalDate.now())
      val assessment = createAssessment(user)

      every { mockBedRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedspace.id, departureDate, null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { mockCas3TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      every { mockBedRepository.findArchivedBedByBedIdAndDate(any(), any()) } returns bedspace

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessment.id,
        false,
      )

      assertThatCasResult(authorisableResult).isConflictError().hasMessage("BedSpace is archived from ${LocalDate.now()} which overlaps with the desired dates")

      verify(exactly = 0) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == assessment.application &&
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
              it.application == assessment.application
          },
          user,
        )
      }

      verify {
        mockBedRepository.findArchivedBedByBedIdAndDate(any(), any())
      }
    }

    private fun createPremisesAndBedspace(bedspaceStartDate: LocalDate, bedspaceEndDate: LocalDate? = null): Pair<TemporaryAccommodationPremisesEntity, BedEntity> {
      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bedspace = BedEntityFactory()
        .withRoom(room)
        .withStartDate(bedspaceStartDate)
        .withEndDate(bedspaceEndDate)
        .produce()

      return Pair(premises, bedspace)
    }

    private fun createAssessment(user: UserEntity): TemporaryAccommodationAssessmentEntity {
      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      return assessment
    }
  }

  @Nested
  inner class CreateTurnaround {
    @Test
    fun `createTurnaround returns FieldValidationError if the number of working days is a negative integer`() {
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

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf()
      every { mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(any(), any(), any(), any()) } returns listOf()
      every { mockCas3TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3TurnaroundEntity }

      val negativeDaysResult = cas3BookingService.createTurnaround(booking, -1)

      assertThat(negativeDaysResult).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((negativeDaysResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
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

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf()
      every { mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(any(), any(), any(), any()) } returns listOf()
      every { mockCas3TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3TurnaroundEntity }

      val result = cas3BookingService.createTurnaround(booking, 2)

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.booking).isEqualTo(booking)
      assertThat(result.entity.workingDayCount).isEqualTo(2)
    }
  }

  private fun createBooking(probationRegion: ProbationRegionEntity): BookingEntity {
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
}
