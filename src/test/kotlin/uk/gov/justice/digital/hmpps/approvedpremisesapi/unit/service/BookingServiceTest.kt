package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3ConfirmationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3ConfirmationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DeliusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3DomainEventService as Cas3DomainEventService

class BookingServiceTest {
  private val mockOffenderDetailService = mockk<OffenderDetailService>()
  private val mockCas3DomainEventService = mockk<Cas3DomainEventService>()
  private val mockWorkingDayService = mockk<WorkingDayService>()

  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockArrivalRepository = mockk<ArrivalRepository>()
  private val mockCancellationRepository = mockk<CancellationRepository>()
  private val mockCas3ConfirmationRepository = mockk<Cas3ConfirmationRepository>()
  private val mockDateChangeRepository = mockk<DateChangeRepository>()
  private val mockCancellationReasonRepository = mockk<CancellationReasonRepository>()
  private val mockCas3LostBedsRepository = mockk<Cas3VoidBedspacesRepository>()
  private val mockAssessmentRepository = mockk<AssessmentRepository>()
  private val mockUserService = mockk<UserService>()
  private val mockUserAccessService = mockk<UserAccessService>()
  private val mockAssessmentService = mockk<AssessmentService>()
  private val mockCas1BookingEmailService = mockk<Cas1BookingEmailService>()
  private val mockDeliusService = mockk<DeliusService>()
  private val mockCas1ApplicationStatusService = mockk<Cas1ApplicationStatusService>()
  private val mockCas1BookingDomainEventService = mockk<Cas1BookingDomainEventService>()

  fun createBookingService(): BookingService = BookingService(
    offenderDetailService = mockOffenderDetailService,
    workingDayService = mockWorkingDayService,
    bookingRepository = mockBookingRepository,
    arrivalRepository = mockArrivalRepository,
    cancellationRepository = mockCancellationRepository,
    cas3ConfirmationRepository = mockCas3ConfirmationRepository,
    dateChangeRepository = mockDateChangeRepository,
    cancellationReasonRepository = mockCancellationReasonRepository,
    cas3VoidBedspacesRepository = mockCas3LostBedsRepository,
    userService = mockUserService,
    userAccessService = mockUserAccessService,
    cas1BookingEmailService = mockCas1BookingEmailService,
    deliusService = mockDeliusService,
    cas1BookingDomainEventService = mockCas1BookingDomainEventService,
    cas1ApplicationStatusService = mockCas1ApplicationStatusService,
  )

  private val bookingService = createBookingService()

  private val user = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .produce()

  @Test
  fun `getBookingForPremises returns BookingNotFound when booking with provided ID does not exist`() {
    val bookingId = UUID.randomUUID()
    val premises = ApprovedPremisesEntityFactory()
      .withId(UUID.randomUUID())
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    every { mockBookingRepository.findByIdOrNull(bookingId) } returns null

    assertThat(bookingService.getBookingForPremises(premises, bookingId))
      .isEqualTo(GetBookingForPremisesResult.BookingNotFound)
  }

  @Test
  fun `getBookingForPremises returns BookingNotFound when booking does not belong to Premises`() {
    val bookingId = UUID.randomUUID()

    val premisesEntityFactory = ApprovedPremisesEntityFactory()
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

    assertThat(bookingService.getBookingForPremises(premisesEntityFactory.withId(UUID.randomUUID()).produce(), bookingId))
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

    val keyWorker = ContextStaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withId(bookingId)
      .withPremises(premisesEntity)
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    every { mockBookingRepository.findByIdOrNull(bookingId) } returns bookingEntity

    assertThat(bookingService.getBookingForPremises(premisesEntity, bookingId))
      .isEqualTo(GetBookingForPremisesResult.Success(bookingEntity))
  }

  @Nested
  inner class GetBooking {
    val premises = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    private val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withPremises(premises)
      .produce()

    private val personInfo = PersonInfoResult.Success.Full(
      crn = bookingEntity.crn,
      offenderDetailSummary = OffenderDetailsSummaryFactory().produce(),
      inmateDetail = InmateDetailFactory().produce(),
    )

    @Test
    fun `returns a booking`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanViewBooking(user, bookingEntity) } returns true
      every { mockOffenderDetailService.getPersonInfoResult(bookingEntity.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO)) } returns personInfo

      val result = bookingService.getBooking(bookingEntity.id)

      assertThat(result is AuthorisableActionResult.Success).isTrue()
      result as AuthorisableActionResult.Success

      assertThat(result.entity).isEqualTo(BookingService.BookingAndPersons(bookingEntity, personInfo))
    }

    @Test
    fun `returns NotFound if the booking cannot be found`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns null

      val result = bookingService.getBooking(bookingEntity.id)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue()
      result as AuthorisableActionResult.NotFound

      assertThat(result.id).isEqualTo(bookingEntity.id.toString())
      assertThat(result.entityType).isEqualTo("Booking")
    }

    @Test
    fun `returns Unauthorised if the user cannot view the booking`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanViewBooking(user, bookingEntity) } returns false

      val result = bookingService.getBooking(bookingEntity.id)

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue()
    }
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
      assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("CAS3 booking arrival not supported here, preferred method is createArrival in Cas3BookingService")
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, user)
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
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      assertThat(result.value).isEqualTo(cancellationEntity)
      verify(exactly = 0) { mockBookingRepository.save(any()) }
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
          WithdrawalTriggeredByUser(user),
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
          WithdrawalTriggeredByUser(user),
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
          WithdrawalTriggeredByUser(user),
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

      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, true) } returns Unit
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
          WithdrawalTriggeredByUser(user),
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

      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, true) } returns Unit
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
          WithdrawalTriggeredByUser(user),
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

      val cancellationSaveArgument = slot<CancellationEntity>()
      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, true) } returns Unit
      every { mockCancellationRepository.save(capture(cancellationSaveArgument)) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas1BookingDomainEventService.bookingCancelled(any(), any(), any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockBookingRepository.findAllByApplication(application) } returns emptyList()

      every { mockCas1BookingEmailService.bookingWithdrawn(application, bookingEntity, null, WithdrawalTriggeredByUser(user)) } returns Unit

      val cancelledAt = LocalDate.parse("2022-08-25")
      val notes = "notes"

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = cancelledAt,
        userProvidedReason = reasonId,
        notes = notes,
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
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
        mockCas1BookingDomainEventService.bookingCancelled(
          bookingEntity,
          user,
          cancellationSaveArgument.captured,
          reason,
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

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withApplication(application)
        .withCrn(application.crn)
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .withPlacementApplication(placementApplication)
            .produce(),
        )
        .produce()

      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, true) } returns Unit
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas1BookingDomainEventService.bookingCancelled(any(), any(), any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockBookingRepository.findAllByApplication(application) } returns emptyList()
      every { mockCas1BookingEmailService.bookingWithdrawn(application, bookingEntity, placementApplication, WithdrawalTriggeredByUser(user)) } returns Unit

      val cancelledAt = LocalDate.parse("2022-08-25")
      val notes = "notes"

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = cancelledAt,
        userProvidedReason = reasonId,
        notes = notes,
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)

      verify(exactly = 1) { mockCas1BookingEmailService.bookingWithdrawn(application, bookingEntity, placementApplication, WithdrawalTriggeredByUser(user)) }
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

      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, true) } returns Unit
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      val cancellationSaveArgument = slot<CancellationEntity>()
      every { mockCancellationRepository.save(capture(cancellationSaveArgument)) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas1BookingDomainEventService.bookingCancelled(any(), any(), any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val cancelledAt = LocalDate.parse("2022-08-25")
      val notes = "notes"

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = cancelledAt,
        userProvidedReason = reasonId,
        notes = notes,
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
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
        mockCas1BookingDomainEventService.bookingCancelled(
          bookingEntity,
          user,
          cancellationSaveArgument.captured,
          reason,
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

      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, true) } returns Unit
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val cancelledAt = LocalDate.parse("2022-08-25")
      val notes = "notes"

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = cancelledAt,
        userProvidedReason = reasonId,
        notes = notes,
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
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
        mockCas1BookingDomainEventService.bookingCancelled(any(), any(), any(), any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @ParameterizedTest
    @EnumSource(WithdrawableEntityType::class, names = ["SpaceBooking"], mode = EnumSource.Mode.EXCLUDE)
    fun `createCancellation sets correct reason if withdrawal triggered by other entity`(triggeringEntity: WithdrawableEntityType) {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      val expectedReasonId = when (triggeringEntity) {
        WithdrawableEntityType.Application -> CancellationReasonRepository.CAS1_RELATED_APP_WITHDRAWN_ID
        WithdrawableEntityType.PlacementApplication -> CancellationReasonRepository.CAS1_RELATED_PLACEMENT_APP_WITHDRAWN_ID
        WithdrawableEntityType.PlacementRequest -> CancellationReasonRepository.CAS1_RELATED_PLACEMENT_REQ_WITHDRAWN_ID
        WithdrawableEntityType.Booking -> reasonId
        WithdrawableEntityType.SpaceBooking -> reasonId
      }

      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, triggeringEntity == WithdrawableEntityType.Booking) } returns Unit
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
          WithdrawalTriggeredByUser(user),
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

    val confirmationEntity = Cas3ConfirmationEntityFactory()
      .withBooking(bookingEntity)
      .produce()

    bookingEntity.confirmation = confirmationEntity

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
    assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("This Booking already has a Confirmation set")
  }

  @Test
  fun `createConfirmation returns Success with correct result not closing referral when the request is not Temporary premises accommodation`() {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .produce()
    val bookingEntity = createApprovedPremisesAccommodationBooking(application)

    every { mockCas3ConfirmationRepository.save(any()) } answers { it.invocation.args[0] as Cas3ConfirmationEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(), user) } just Runs

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
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
      mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id)
    }
  }

  private fun createApprovedPremisesAccommodationBooking(application: ApprovedPremisesApplicationEntity?) = BookingEntityFactory()
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
      every { mockDeliusService.referralHasArrival(booking) } returns false

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.withdrawn).isTrue()
      assertThat(result.withdrawable).isFalse()
    }

    @Test
    fun `getWithdrawableState withdrawable if has no arrivals in CAS1 and Delius and not already cancelled`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true
      every { mockDeliusService.referralHasArrival(booking) } returns false

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.withdrawn).isFalse()
      assertThat(result.withdrawable).isTrue()
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `getWithdrawableState userMayDirectlyWithdraw delegates to user access service`(canWithdraw: Boolean) {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns canWithdraw
      every { mockDeliusService.referralHasArrival(booking) } returns false

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(canWithdraw)
    }

    @Test
    fun `getWithdrawableState blockingReason is null if no arrivals in CAS1 or Delius`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .withArrivals(mutableListOf())
        .produce()

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true
      every { mockDeliusService.referralHasArrival(booking) } returns false

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.blockingReason).isNull()
    }

    @Test
    fun `getWithdrawableState blockingReason is ArrivalRecordedInCas1 if has arrival recorded in CAS1`() {
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

      assertThat(result.blockingReason).isEqualTo(BlockingReason.ArrivalRecordedInCas1)
    }

    @Test
    fun `getWithdrawableState blockingReason is ArrivalRecordedInDelius if has no CAS1 arrivals and arrival recorded in Delius`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true
      every { mockDeliusService.referralHasArrival(booking) } returns true

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.blockingReason).isEqualTo(BlockingReason.ArrivalRecordedInDelius)
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

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-14"), booking.id) } returns listOf(conflictingBooking)

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-12"),
        newDepartureDate = LocalDate.parse("2023-07-14"),
      )

      assertThatCasResult(result)
        .isConflictError()
        .hasMessageContaining("A Booking already exists")
    }

    @Test
    fun `for non-AP Bookings, returns conflict error for conflicting Void Bedspace`() {
      val booking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      val conflictingLostBed = Cas3VoidBedspaceEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withStartDate(LocalDate.parse("2023-07-10"))
        .withEndDate(LocalDate.parse("2023-07-12"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-14"), booking.id) } returns emptyList()
      every {
        mockCas3LostBedsRepository.findByBedspaceIdAndOverlappingDate(
          temporaryAccommodationBed.id,
          LocalDate.parse("2023-07-12"),
          LocalDate.parse("2023-07-14"),
          null,
        )
      } returns listOf(conflictingLostBed)

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-12"),
        newDepartureDate = LocalDate.parse("2023-07-14"),
      )

      assertThatCasResult(result)
        .isConflictError()
        .hasMessageContaining("A Lost Bed already exists")
    }

    @Test
    fun `returns validation error if new arrival date is after the new departure date`() {
      val booking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-14"), booking.id) } returns emptyList()
      every { mockCas3LostBedsRepository.findByBedspaceIdAndOverlappingDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-16"), LocalDate.parse("2023-07-14"), null) } returns emptyList()

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-16"),
        newDepartureDate = LocalDate.parse("2023-07-14"),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.newDepartureDate", "beforeBookingArrivalDate")
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

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-15"),
        newDepartureDate = LocalDate.parse("2023-07-16"),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.newArrivalDate", "arrivalDateCannotBeChangedOnArrivedBooking")
    }

    @Test
    fun `returns error if booking is cancelled`() {
      val booking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .produce()
        .apply {
          cancellations = mutableListOf(
            CancellationEntityFactory()
              .withDefaults()
              .withBooking(this)
              .produce(),
          )
        }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-15"),
        newDepartureDate = LocalDate.parse("2023-07-16"),
      )

      assertThatCasResult(result).isGeneralValidationError("This Booking is cancelled and as such cannot be modified")
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

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = null,
        newDepartureDate = LocalDate.parse("2023-07-15"),
      )

      assertThatCasResult(result).isSuccess()
      result as CasResult.Success

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

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-18"),
        newDepartureDate = LocalDate.parse("2023-07-22"),
      )

      assertThatCasResult(result).isSuccess()
      result as CasResult.Success

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
    fun `emits domain event when booking has associated application, only arrival date changed`() {
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

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockCas1BookingEmailService.bookingAmended(any(), any(), any()) } just runs
      every { mockCas1BookingDomainEventService.bookingChanged(any(), any(), any(), any(), any()) } just Runs

      val newArrivalDate = LocalDate.parse("2023-07-15")
      val newDepartureDate = LocalDate.parse("2023-07-16")

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = newArrivalDate,
        newDepartureDate = newDepartureDate,
      )

      assertThatCasResult(result).isSuccess()
      result as CasResult.Success

      verify {
        mockDateChangeRepository.save(
          match {
            it.booking.id == booking.id &&
              it.changedByUser == user &&
              it.newArrivalDate == LocalDate.parse("2023-07-15") &&
              it.newDepartureDate == LocalDate.parse("2023-07-16") &&
              it.previousArrivalDate == LocalDate.parse("2023-07-14") &&
              it.previousDepartureDate == LocalDate.parse("2023-07-16")
          },
        )
      }

      verify {
        mockBookingRepository.save(
          match {
            it.id == booking.id &&
              it.arrivalDate == LocalDate.parse("2023-07-15") &&
              it.departureDate == LocalDate.parse("2023-07-16")
          },
        )
      }

      verify(exactly = 1) {
        mockCas1BookingDomainEventService.bookingChanged(
          booking = booking,
          changedBy = user,
          bookingChangedAt = any(),
          previousArrivalDateIfChanged = LocalDate.of(2023, 7, 14),
          previousDepartureDateIfChanged = null,
        )
      }

      verify(exactly = 1) {
        mockCas1BookingEmailService.bookingAmended(
          application = booking.application as ApprovedPremisesApplicationEntity,
          booking = booking,
          placementApplication = booking.placementRequest?.placementApplication,
        )
      }
    }

    @Test
    fun `emits domain event when booking has associated offline application with an event number, only departure date changed`() {
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

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockCas1BookingDomainEventService.bookingChanged(any(), any(), any(), any(), any()) } just Runs

      val newArrivalDate = LocalDate.parse("2023-07-14")
      val newDepartureDate = LocalDate.parse("2023-07-22")

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = newArrivalDate,
        newDepartureDate = newDepartureDate,
      )

      assertThatCasResult(result).isSuccess()
      result as CasResult.Success

      verify(exactly = 1) {
        mockCas1BookingDomainEventService.bookingChanged(
          booking = booking,
          changedBy = user,
          bookingChangedAt = any(),
          previousArrivalDateIfChanged = null,
          previousDepartureDateIfChanged = LocalDate.of(2023, 7, 16),
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

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockCas1BookingDomainEventService.bookingChanged(any(), any(), any(), any(), any()) } just Runs

      val newArrivalDate = LocalDate.parse("2023-07-18")
      val newDepartureDate = LocalDate.parse("2023-07-22")

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = newArrivalDate,
        newDepartureDate = newDepartureDate,
      )

      assertThatCasResult(result).isSuccess()
      result as CasResult.Success

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

      verify(exactly = 0) { mockCas1BookingDomainEventService wasNot Called }
    }
  }
}
