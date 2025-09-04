package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.v2.Cas3v2ConfirmationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3DepartureRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ExtensionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2ConfirmationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2TurnaroundRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3v2BookingServiceTest {
  private val mockOffenderService = mockk<OffenderService>()
  private val mockCas3DomainEventService = mockk<Cas3v2DomainEventService>()
  private val mockWorkingDayService = mockk<WorkingDayService>()
  private val mockBookingRepository = mockk<Cas3v2BookingRepository>()
  private val mockArrivalRepository = mockk<Cas3ArrivalRepository>()
  private val mockBedspaceRepository = mockk<Cas3BedspacesRepository>()
  private val mockCancellationRepository = mockk<Cas3CancellationRepository>()
  private val mockCancellationReasonRepository = mockk<CancellationReasonRepository>()
  private val mockCas3v2TurnaroundRepository = mockk<Cas3v2TurnaroundRepository>()
  private val mockCas3v2ConfirmationRepository = mockk<Cas3v2ConfirmationRepository>()
  private val mockCas3VoidBedspacesRepository = mockk<Cas3VoidBedspacesRepository>()
  private val mockAssessmentRepository = mockk<AssessmentRepository>()
  private val mockUserAccessService = mockk<UserAccessService>()
  private val mockUserService = mockk<UserService>()
  private val mockAssessmentService = mockk<AssessmentService>()
  private val mockDepartureRepository = mockk<Cas3DepartureRepository>()
  private val mockDepartureReasonRepository = mockk<DepartureReasonRepository>()
  private val mockMoveOnCategoryRepository = mockk<MoveOnCategoryRepository>()
  private val mockFeatureFlagService = mockk<FeatureFlagService>()
  private val mockExtensionRepository = mockk<Cas3ExtensionRepository>()

  private fun createCas3v2BookingService(): Cas3v2BookingService = Cas3v2BookingService(
    cas3BookingRepository = mockBookingRepository,
    cas3BedspaceRepository = mockBedspaceRepository,
    cas3CancellationRepository = mockCancellationRepository,
    cancellationReasonRepository = mockCancellationReasonRepository,
    cas3ArrivalRepository = mockArrivalRepository,
    cas3DepartureRepository = mockDepartureRepository,
    departureReasonRepository = mockDepartureReasonRepository,
    cas3v2ConfirmationRepository = mockCas3v2ConfirmationRepository,
    moveOnCategoryRepository = mockMoveOnCategoryRepository,
    cas3v2TurnaroundRepository = mockCas3v2TurnaroundRepository,
    assessmentRepository = mockAssessmentRepository,
    cas3VoidBedspacesRepository = mockCas3VoidBedspacesRepository,
    offenderService = mockOffenderService,
    workingDayService = mockWorkingDayService,
    cas3ExtensionRepository = mockExtensionRepository,
    cas3DomainEventService = mockCas3DomainEventService,
    userAccessService = mockUserAccessService,
    assessmentService = mockAssessmentService,
    featureFlagService = mockFeatureFlagService,
  )

  private val cas3BookingService = createCas3v2BookingService()

  private val user = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .produce()

  @Nested
  inner class GetBooking {
    val premises = Cas3PremisesEntityFactory()
      .withDefaults()
      .produce()

    private val bookingEntity = Cas3BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withPremises(premises)
      .withBedspace(Cas3BedspaceEntityFactory().withPremises(premises).produce())
      .produce()

    @Test
    fun `returns a booking where booking's premises matches supplied premisesId`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanManagePremisesBookings(user, bookingEntity.premises) } returns true

      val result = cas3BookingService.getBooking(
        bookingId = bookingEntity.id,
        premisesId = premises.id,
        user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(bookingEntity)
      }
    }

    @Test
    fun `returns a booking where premisesId not supplied`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanManagePremisesBookings(user, bookingEntity.premises) } returns true

      val result = cas3BookingService.getBooking(
        bookingId = bookingEntity.id,
        premisesId = null,
        user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(bookingEntity)
      }
    }

    @Test
    fun `returns a GeneralValidationError when booking's premises does not match the premisesId`() {
      every { mockUserService.getUserForRequest() } returns user
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity
      every { mockUserAccessService.userCanManagePremisesBookings(user, bookingEntity.premises) } returns true

      val result = cas3BookingService.getBooking(
        bookingId = bookingEntity.id,
        premisesId = UUID.randomUUID(),
        user,
      )

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      result as CasResult.GeneralValidationError

      assertThat(result.message).isEqualTo("The supplied premisesId does not match the booking's premises")
    }

    @Test
    fun `returns NotFound if the booking cannot be found`() {
      every { mockUserService.getUserForRequest() } returns user
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns null

      val result = cas3BookingService.getBooking(
        bookingId = bookingEntity.id,
        premisesId = premises.id,
        user,
      )

      assertThat(result is CasResult.NotFound).isTrue()
      result as CasResult.NotFound

      assertThat(result.id).isEqualTo(bookingEntity.id.toString())
      assertThat(result.entityType).isEqualTo("Booking")
    }

    @Test
    fun `returns Unauthorised if the user cannot view the booking`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanManagePremisesBookings(user, bookingEntity.premises) } returns false

      val result = cas3BookingService.getBooking(
        bookingId = bookingEntity.id,
        premisesId = premises.id,
        user,
      )

      assertThat(result is CasResult.Unauthorised).isTrue()
    }
  }

  @Nested
  inner class CreateBooking {

    @BeforeEach
    fun setup() {
      every { mockBedspaceRepository.findArchivedBedspaceByBedspaceIdAndDate(any(), any()) } returns null
      every { mockOffenderService.getPersonSummaryInfoResult(eq("CRN123"), eq(LaoStrategy.NeverRestricted)) } returns PersonSummaryInfoResult.Success.Full(
        "CRN123",
        CaseSummaryFactory().withCrn("CRN123").withName(Name("John", "Smith", emptyList())).produce(),
      )
    }

    @Test
    fun `createBooking returns FieldValidationError if Bedspace ID is not provided`() {
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
        bedspaceId = null,
        assessmentId,
        enableTurnarounds = false,
      )

      assertThatCasResult(authorisableResult).isFieldValidationError().hasMessage("$.bedspaceId", "empty")
    }

    @Test
    fun `createBooking returns FieldValidationError if Departure Date is before Arrival Date`() {
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)
      val crn = "CRN123"
      val arrivalDate = bedspaceStartDate.randomDateAfter(15)
      val departureDate = arrivalDate.minusDays(1)
      val assessmentId = UUID.randomUUID()

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns null
      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null
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
        bedspace.id,
        assessmentId,
        enableTurnarounds = false,
      )

      assertThatCasResult(authorisableResult).isFieldValidationError().hasMessage("$.departureDate", "beforeBookingArrivalDate")
    }

    @Test
    fun `createBooking returns FieldValidationError if Bedspace does not exist`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)
      val arrivalDate = bedspaceStartDate.randomDateAfter(3)
      val departureDate = arrivalDate.randomDateAfter(12)
      val assessmentId = UUID.randomUUID()

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns null
      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null

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
        bedspace.id,
        assessmentId,
        enableTurnarounds = false,
      )

      assertThatCasResult(authorisableResult).isFieldValidationError().hasMessage("$.bedspaceId", "doesNotExist")
    }

    @Test
    fun `createBooking returns FieldValidationError if Bedspace Start Date is after Booking Arrival Date`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)
      val arrivalDate = bedspaceStartDate.randomDateBefore(20)
      val departureDate = arrivalDate.plusDays(84)
      val assessment = createAssessment(user)

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns bedspace
      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
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
        bedspace.id,
        assessment.id,
        enableTurnarounds = false,
      )

      assertThatCasResult(authorisableResult).isFieldValidationError().hasMessage("$.arrivalDate", "bookingArrivalDateBeforeBedspaceStartDate")
    }

    @Test
    fun `createBooking returns FieldValidationError if Application is provided and does not exist`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate)
      val arrivalDate = bedspaceStartDate.randomDateAfter(15)
      val departureDate = arrivalDate.randomDateAfter(45)
      val assessmentId = UUID.randomUUID()

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns bedspace
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null
      every { this@Cas3v2BookingServiceTest.mockCas3v2TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2TurnaroundEntity }

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
        enableTurnarounds = false,
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

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { this@Cas3v2BookingServiceTest.mockCas3v2TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessment.id,
        enableTurnarounds = false,
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
              it.status == Cas3BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(
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

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
        )
      } returns listOf()

      every { this@Cas3v2BookingServiceTest.mockCas3v2TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(any(), user) } just Runs

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
        enableTurnarounds = false,
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
              it.status == Cas3BookingStatus.provisional
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

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns bedspace

      val bookingSlot = slot<Cas3BookingEntity>()
      every { mockBookingRepository.save(capture(bookingSlot)) } answers { bookingSlot.captured }

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { this@Cas3v2BookingServiceTest.mockCas3v2TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessment.id,
        enableTurnarounds = false,
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
              it.status == Cas3BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        this@Cas3v2BookingServiceTest.mockCas3v2TurnaroundRepository.save(
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

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns bedspace

      val bookingSlot = slot<Cas3BookingEntity>()
      every { mockBookingRepository.save(capture(bookingSlot)) } answers { bookingSlot.captured }

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { this@Cas3v2BookingServiceTest.mockCas3v2TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessment.id,
        enableTurnarounds = true,
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
              it.status == Cas3BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3v2TurnaroundRepository.save(
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

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { this@Cas3v2BookingServiceTest.mockCas3v2TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(any(), user) } just Runs

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
        enableTurnarounds = false,
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
              it.status == Cas3BookingStatus.provisional &&
              it.offenderName == "John Smith"
          },
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(
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

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { this@Cas3v2BookingServiceTest.mockCas3v2TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessment.id,
        enableTurnarounds = false,
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
              it.status == Cas3BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(
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

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { this@Cas3v2BookingServiceTest.mockCas3v2TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessment.id,
        enableTurnarounds = false,
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
              it.status == Cas3BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(
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

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null
      every { this@Cas3v2BookingServiceTest.mockCas3v2TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2TurnaroundEntity }

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
        enableTurnarounds = false,
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

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } throws RuntimeException("DB exception")

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { this@Cas3v2BookingServiceTest.mockCas3v2TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(any(), user) } just Runs

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
            enableTurnarounds = false,
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
              it.status == Cas3BookingStatus.provisional
          },
        )
      }

      verify(exactly = 0) {
        mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(
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
    fun `createBooking throws conflict error when bedspace is archived for the requested bedspace`() {
      val crn = "CRN123"
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(14)
      val departureDate = arrivalDate.randomDateAfter(35)

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val (premises, bedspace) = createPremisesAndBedspace(bedspaceStartDate, LocalDate.now())
      val assessment = createAssessment(user)

      every { mockBedspaceRepository.findByIdOrNull(bedspace.id) } returns bedspace

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate, excludeBookingId = null) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          bedspace.id,
          arrivalDate,
          departureDate,
          bookingId = null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment

      every { this@Cas3v2BookingServiceTest.mockCas3v2TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(any(), user) } just Runs

      every { mockBedspaceRepository.findArchivedBedspaceByBedspaceIdAndDate(any(), any()) } returns bedspace

      val authorisableResult = cas3BookingService.createBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedspace.id,
        assessment.id,
        enableTurnarounds = false,
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
              it.status == Cas3BookingStatus.provisional
          },
        )
      }

      verify(exactly = 0) {
        mockCas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(
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
        mockBedspaceRepository.findArchivedBedspaceByBedspaceIdAndDate(any(), any())
      }
    }
  }

  @Nested
  inner class CreateArrival {
    private val cas3BookingEntity = createCas3Booking()

    @BeforeEach
    fun setup() {
      every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as Cas3ArrivalEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockCas3DomainEventService.savePersonArrivedEvent(any(Cas3BookingEntity::class), any(UserEntity::class)) } just Runs
    }

    @Test
    fun `createArrival returns FieldValidationError with correct param to message map when arrival date is before expected departure date`() {
      val arrivalDate = LocalDate.now()
      val result = cas3BookingService.createArrival(
        booking = cas3BookingEntity,
        arrivalDate = LocalDate.now(),
        expectedDepartureDate = arrivalDate.minusDays(1),
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.expectedDepartureDate", "beforeBookingArrivalDate")

      verify(exactly = 0) { mockArrivalRepository.save(any()) }
      verify(exactly = 0) { mockBookingRepository.save(any()) }
      verify(exactly = 0) { mockCas3DomainEventService.savePersonArrivedEvent(cas3BookingEntity, user) }
    }

    @Test
    fun `createArrival returns FieldValidationError with correct param to message map when arrival date is more than 2 weeks in the past`() {
      val arrivalDate = LocalDate.now().minusDays(15)
      val result = cas3BookingService.createArrival(
        booking = cas3BookingEntity,
        arrivalDate = arrivalDate,
        expectedDepartureDate = arrivalDate.plusDays(10),
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.arrivalDate", "arrivalAfterLatestDate")

      verify(exactly = 0) { mockArrivalRepository.save(any()) }
      verify(exactly = 0) { mockBookingRepository.save(any()) }
      verify(exactly = 0) { mockCas3DomainEventService.savePersonArrivedEvent(cas3BookingEntity, user) }
    }

    @Test
    fun `createArrival should return success response when arrival exists for a Booking and save and emit the event`() {
      val userEntity = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()
      val arrivalEntity = Cas3ArrivalEntityFactory()
        .withBooking(cas3BookingEntity)
        .produce()
      cas3BookingEntity.arrivals += arrivalEntity

      every { mockCas3DomainEventService.savePersonArrivedUpdatedEvent(any(Cas3BookingEntity::class), any(UserEntity::class)) } just Runs

      val arrivalDate = LocalDate.now().minusDays(3)
      val expectedDepartureDate = arrivalDate.plusDays(1)

      val result = cas3BookingService.createArrival(
        booking = cas3BookingEntity,
        arrivalDate = arrivalDate,
        expectedDepartureDate = expectedDepartureDate,
        notes = "notes",
        user = userEntity,
      )

      assertThatCasResult(result).isSuccess()

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.arrivalDate).isEqualTo(arrivalDate)
        assertThat(it.arrivalDateTime).isEqualTo(arrivalDate.toLocalDateTime().toInstant())
        assertThat(it.expectedDepartureDate).isEqualTo(expectedDepartureDate)
        assertThat(it.notes).isEqualTo("notes")
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.arrived)
      }

      verify(exactly = 1) { mockArrivalRepository.save(any()) }
      verify(exactly = 1) { mockBookingRepository.save(any()) }
      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonArrivedUpdatedEvent(cas3BookingEntity, userEntity)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonArrivedEvent(cas3BookingEntity, userEntity)
      }
    }

    @Test
    fun `createArrival returns Success with correct result when validation passed and saves domain event`() {
      val userEntity = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val arrivalDate = LocalDate.now().minusDays(3)
      val expectedDepartureDate = arrivalDate.plusDays(1)

      val result = cas3BookingService.createArrival(
        booking = cas3BookingEntity,
        arrivalDate = arrivalDate,
        expectedDepartureDate = expectedDepartureDate,
        notes = "notes",
        user = userEntity,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.arrivalDate).isEqualTo(arrivalDate)
        assertThat(it.arrivalDateTime).isEqualTo(arrivalDate.toLocalDateTime().toInstant())
        assertThat(it.expectedDepartureDate).isEqualTo(expectedDepartureDate)
        assertThat(it.notes).isEqualTo("notes")
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.arrived)
      }

      verify(exactly = 1) { mockArrivalRepository.save(any()) }
      verify(exactly = 1) { mockBookingRepository.save(any()) }
      verify(exactly = 1) { mockCas3DomainEventService.savePersonArrivedEvent(cas3BookingEntity, userEntity) }
    }

    @Test
    fun `createArrival returns Success with correct result when validation passed and saves domain event without staff detail`() {
      every { mockCas3DomainEventService.savePersonArrivedEvent(any(Cas3BookingEntity::class), user) } just Runs

      val arrivalDate = LocalDate.now().minusDays(3)
      val expectedDepartureDate = arrivalDate.plusDays(1)

      val result = cas3BookingService.createArrival(
        booking = cas3BookingEntity,
        arrivalDate = arrivalDate,
        expectedDepartureDate = expectedDepartureDate,
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.arrivalDate).isEqualTo(arrivalDate)
        assertThat(it.arrivalDateTime).isEqualTo(arrivalDate.toLocalDateTime().toInstant())
        assertThat(it.expectedDepartureDate).isEqualTo(expectedDepartureDate)
        assertThat(it.notes).isEqualTo("notes")
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.arrived)
      }

      verify(exactly = 1) { mockArrivalRepository.save(any()) }
      verify(exactly = 1) { mockBookingRepository.save(any()) }
      verify(exactly = 1) { mockCas3DomainEventService.savePersonArrivedEvent(cas3BookingEntity, user) }
    }
  }

  @Nested
  inner class CreateCancellation {
    private val cas3BookingEntity = createCas3Booking()

    @Test
    fun `createCancellation returns Success with correct result and emits a domain event`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as Cas3CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(Cas3BookingEntity::class), user) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }

      val result = cas3BookingService.createCancellation(
        booking = cas3BookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.date).isEqualTo(LocalDate.parse("2022-08-25"))
        assertThat(it.reason).isEqualTo(reasonEntity)
        assertThat(it.notes).isEqualTo("notes")
        assertThat(cas3BookingEntity.cancellations).contains(it)
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.cancelled)
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(cas3BookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(Cas3BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(cas3BookingEntity)
      }
    }

    @Test
    fun `createCancellation returns Success with correct result and emits cancelled-updated domain event`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()
      val cancellationEntity = Cas3CancellationEntityFactory().withBooking(cas3BookingEntity).withReason(reasonEntity).produce()
      cas3BookingEntity.cancellations += cancellationEntity
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as Cas3CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(Cas3BookingEntity::class), user) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }

      val result = cas3BookingService.createCancellation(
        booking = cas3BookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.date).isEqualTo(LocalDate.parse("2022-08-25"))
        assertThat(it.reason).isEqualTo(reasonEntity)
        assertThat(it.notes).isEqualTo("notes")
        assertThat(cas3BookingEntity.cancellations).contains(it)
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.cancelled)
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(cas3BookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledEvent(any(Cas3BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockBookingRepository.save(cas3BookingEntity)
      }
    }

    @Test
    fun `createCancellation returns Success and move the assessment to read-to-place state`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingWithAssessmentEntity = createBookingWithAssessment()
      val assessmentEntity = bookingWithAssessmentEntity.application?.getLatestAssessment()
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as Cas3CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(Cas3BookingEntity::class), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } returns CasResult.Success(assessmentEntity!!)
      mockkStatic(Sentry::class)

      val result = cas3BookingService.createCancellation(
        booking = bookingWithAssessmentEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.date).isEqualTo(LocalDate.parse("2022-08-25"))
        assertThat(it.reason).isEqualTo(reasonEntity)
        assertThat(it.notes).isEqualTo("notes")
        assertThat(bookingWithAssessmentEntity.cancellations).contains(it)
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.cancelled)
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingWithAssessmentEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(Cas3BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingWithAssessmentEntity)
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
      val bookingWithAssessmentEntity = createBookingWithAssessment()
      val assessmentEntity = bookingWithAssessmentEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as Cas3CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(Cas3BookingEntity::class), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } returns CasResult.Unauthorised()
      mockkStatic(Sentry::class)
      every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

      val result = cas3BookingService.createCancellation(
        booking = bookingWithAssessmentEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.date).isEqualTo(LocalDate.parse("2022-08-25"))
        assertThat(it.reason).isEqualTo(reasonEntity)
        assertThat(it.notes).isEqualTo("notes")
        assertThat(bookingWithAssessmentEntity.cancellations).contains(it)
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.cancelled)
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingWithAssessmentEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(Cas3BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingWithAssessmentEntity)
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
      val bookingWithAssessmentEntity = createBookingWithAssessment()
      val assessmentEntity = bookingWithAssessmentEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as Cas3CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(Cas3BookingEntity::class), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } throws RuntimeException("some-exception")
      mockkStatic(Sentry::class)

      val result = cas3BookingService.createCancellation(
        booking = bookingWithAssessmentEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.date).isEqualTo(LocalDate.parse("2022-08-25"))
        assertThat(it.reason).isEqualTo(reasonEntity)
        assertThat(it.notes).isEqualTo("notes")
        assertThat(bookingWithAssessmentEntity.cancellations).contains(it)
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.cancelled)
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingWithAssessmentEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(Cas3BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingWithAssessmentEntity)
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
      val bookingWithAssessmentEntity = createBookingWithAssessment()
      val assessmentEntity = bookingWithAssessmentEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as Cas3CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(Cas3BookingEntity::class), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } throws Throwable("some-exception")
      mockkStatic(Sentry::class)

      assertThatExceptionOfType(Throwable::class.java)
        .isThrownBy {
          cas3BookingService.createCancellation(
            booking = bookingWithAssessmentEntity,
            cancelledAt = LocalDate.parse("2022-08-25"),
            reasonId = reasonId,
            notes = "notes",
            user = user,
          )
        }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingWithAssessmentEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(Cas3BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingWithAssessmentEntity)
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
      val bookingWithAssessmentEntity = createBookingWithAssessment()
      val assessmentEntity = bookingWithAssessmentEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } throws RuntimeException("som-exception")
      mockkStatic(Sentry::class)

      assertThatExceptionOfType(RuntimeException::class.java)
        .isThrownBy {
          cas3BookingService.createCancellation(
            booking = bookingWithAssessmentEntity,
            cancelledAt = LocalDate.parse("2022-08-25"),
            reasonId = reasonId,
            notes = "notes",
            user = user,
          )
        }

      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingWithAssessmentEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(Cas3BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 0) {
        mockBookingRepository.save(bookingWithAssessmentEntity)
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
      val bookingWithAssessmentEntity = createBookingWithAssessment()
      val assessmentEntity = bookingWithAssessmentEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as Cas3CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(Cas3BookingEntity::class), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } returns CasResult.GeneralValidationError("Error")
      mockkStatic(Sentry::class)
      every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

      val result = cas3BookingService.createCancellation(
        booking = bookingWithAssessmentEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.date).isEqualTo(LocalDate.parse("2022-08-25"))
        assertThat(it.reason).isEqualTo(reasonEntity)
        assertThat(it.notes).isEqualTo("notes")
        assertThat(bookingWithAssessmentEntity.cancellations).contains(it)
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.cancelled)
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingWithAssessmentEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(Cas3BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingWithAssessmentEntity)
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
      val bookingWithoutAssessmentEntity = createBookingWithAssessment(false)
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as Cas3CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(Cas3BookingEntity::class), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }

      val result = cas3BookingService.createCancellation(
        booking = bookingWithoutAssessmentEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.date).isEqualTo(LocalDate.parse("2022-08-25"))
        assertThat(it.reason).isEqualTo(reasonEntity)
        assertThat(it.notes).isEqualTo("notes")
        assertThat(bookingWithoutAssessmentEntity.cancellations).contains(it)
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.cancelled)
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingWithoutAssessmentEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(Cas3BookingEntity::class), user)
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingWithoutAssessmentEntity)
      }
      verify(exactly = 0) {
        mockAssessmentService.acceptAssessment(user, any(), any(), null, null, null, any())
      }
      verify(exactly = 0) {
        Sentry.captureException(any())
      }
    }

    private fun createBookingWithAssessment(withAssessment: Boolean = true): Cas3BookingEntity {
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

      val premises = Cas3PremisesEntityFactory()
        .withDefaults()
        .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
        .produce()
      val bedspace = Cas3BedspaceEntityFactory()
        .withPremises(premises)
        .produce()

      val bookingEntity = Cas3BookingEntityFactory()
        .withPremises(premises)
        .withBedspace(bedspace)
        .withServiceName(ServiceName.temporaryAccommodation)
        .withApplication(application)
        .produce()

      return bookingEntity
    }
  }

  @Nested
  inner class CreateDeparture {
    private val bookingEntity = createCas3Booking()
    private val departureReasonId = UUID.randomUUID()
    private val moveOnCategoryId = UUID.randomUUID()

    private val reasonEntity = DepartureReasonEntityFactory()
      .withServiceScope("temporary-accommodation")
      .produce()
    private val moveOnCategoryEntity = MoveOnCategoryEntityFactory()
      .withServiceScope("temporary-accommodation")
      .produce()

    @BeforeEach
    fun setup() {
      every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns reasonEntity
      every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns moveOnCategoryEntity
      every { mockDepartureRepository.save(any()) } answers { it.invocation.args[0] as Cas3DepartureEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
    }

    @Test
    fun `createDeparture returns FieldValidationError when departure date is before the booking arrival date`() {
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

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.dateTime", "beforeBookingArrivalDate")
    }

    @Test
    fun `createDeparture returns FieldValidationError when departure date is in the future`() {
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

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.dateTime", "departureDateInFuture")
    }

    @Test
    fun `createDeparture returns FieldValidationError when invalid departure reason supplied`() {
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

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.reasonId", "doesNotExist")
    }

    @Test
    fun `createDeparture returns FieldValidationError when the departure reason has the wrong service scope`() {
      every { mockDepartureReasonRepository.findByIdOrNull(any()) } returns DepartureReasonEntityFactory()
        .withServiceScope(ServiceName.approvedPremises.value)
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

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.reasonId", "incorrectDepartureReasonServiceScope")
    }

    @Test
    fun `createDeparture returns FieldValidationError when invalid move on category supplied`() {
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

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.moveOnCategoryId", "doesNotExist")
    }

    @Test
    fun `createDeparture returns FieldValidationError when the move-on category has the wrong service scope`() {
      every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns DepartureReasonEntityFactory()
        .withServiceScope("*")
        .produce()
      every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns MoveOnCategoryEntityFactory()
        .withServiceScope(ServiceName.approvedPremises.value)
        .produce()
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

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.moveOnCategoryId", "incorrectMoveOnCategoryServiceScope")
    }

    @Test
    fun `createDeparture for a booking returns Success with correct result when validation passed and saves a domain event`() {
      val departureReasonId = UUID.randomUUID()
      val moveOnCategoryId = UUID.randomUUID()

      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val bookingEntity = createCas3Booking(
        arrivalDate = LocalDate.parse("2022-08-22"),
      )

      val reasonEntity = DepartureReasonEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()
      val moveOnCategoryEntity = MoveOnCategoryEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()

      every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns reasonEntity
      every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns moveOnCategoryEntity
      every { mockDepartureRepository.save(any()) } answers { it.invocation.args[0] as Cas3DepartureEntity }
      every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as Cas3ArrivalEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockFeatureFlagService.getBooleanFlag("cas3-validate-booking-departure-in-future") } returns false

      val user = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      every { mockCas3DomainEventService.savePersonDepartedEvent(any(Cas3BookingEntity::class), user) } just Runs

      val result = cas3BookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00"),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.booking).isEqualTo(bookingEntity)
        assertThat(it.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-24T15:00:00+01:00"))
        assertThat(it.reason).isEqualTo(reasonEntity)
        assertThat(it.moveOnCategory).isEqualTo(moveOnCategoryEntity)
        assertThat(it.destinationProvider).isEqualTo(null)
        assertThat(it.reason).isEqualTo(reasonEntity)
        assertThat(it.notes).isEqualTo("notes")
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.departed)
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonDepartedEvent(bookingEntity, user)
      }
      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonDepartedEvent(any(Cas3BookingEntity::class), user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonDepartureUpdatedEvent(any(Cas3BookingEntity::class), user)
      }
    }

    @Test
    fun `createDeparture for a booking successfully create departure update event and saves a domain event`() {
      val departureReasonId = UUID.randomUUID()
      val moveOnCategoryId = UUID.randomUUID()
      val departureDateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
      val notes = "Some notes about the departure"
      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val reasonEntity = DepartureReasonEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()
      val moveOnCategoryEntity = MoveOnCategoryEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()

      val bookingEntity = createCas3Booking(
        arrivalDate = LocalDate.parse("2022-08-22"),
      )

      val user = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val departureEntity = Cas3DepartureEntityFactory()
        .withBooking(bookingEntity)
        .withDateTime(departureDateTime)
        .withReason(reasonEntity)
        .withMoveOnCategory(moveOnCategoryEntity)
        .withNotes(notes)
        .produce()
      bookingEntity.departures += departureEntity

      every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns reasonEntity
      every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns moveOnCategoryEntity
      every { mockDepartureRepository.save(any()) } answers { it.invocation.args[0] as Cas3DepartureEntity }
      every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as Cas3ArrivalEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockCas3DomainEventService.savePersonDepartureUpdatedEvent(any(Cas3BookingEntity::class), user) } just Runs
      every { mockFeatureFlagService.getBooleanFlag("cas3-validate-booking-departure-in-future") } returns false

      val result = cas3BookingService.createDeparture(
        booking = bookingEntity,
        dateTime = departureDateTime,
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        notes = notes,
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.booking).isEqualTo(bookingEntity)
        assertThat(it.dateTime).isEqualTo(departureDateTime)
        assertThat(it.reason).isEqualTo(reasonEntity)
        assertThat(it.moveOnCategory).isEqualTo(moveOnCategoryEntity)
        assertThat(it.destinationProvider).isEqualTo(null)
        assertThat(it.reason).isEqualTo(reasonEntity)
        assertThat(it.notes).isEqualTo(notes)
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.departed)
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonDepartureUpdatedEvent(any(Cas3BookingEntity::class), user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonDepartedEvent(any(Cas3BookingEntity::class), user)
      }
    }
  }

  @Nested
  inner class CreateConfirmation {
    private val bookingEntity = createCas3Booking()

    @Test
    fun `createConfirmation returns GeneralValidationError with correct message when Booking already has a Confirmation`() {
      val confirmationEntity = Cas3v2ConfirmationEntityFactory()
        .withBooking(bookingEntity)
        .produce()

      bookingEntity.confirmation = confirmationEntity

      val result = cas3BookingService.createConfirmation(
        booking = bookingEntity,
        dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
        notes = "notes",
        user = user,
      )
      assertThatCasResult(result).isGeneralValidationError("This Booking already has a Confirmation set")
    }

    @Test
    fun `createConfirmation returns Success with correct result when validation passes and emits domain event`() {
      every { mockCas3v2ConfirmationRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2ConfirmationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(Cas3BookingEntity::class), user) } just Runs

      val result = cas3BookingService.createConfirmation(
        booking = bookingEntity,
        dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
        assertThat(it.notes).isEqualTo("notes")
        assertThat(it).isEqualTo(bookingEntity.confirmation)
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.confirmed)
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createConfirmation returns Success with correct result and does not close the referral when booking is done without application`() {
      every { mockCas3v2ConfirmationRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2ConfirmationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(Cas3BookingEntity::class), user) } just Runs

      val result = cas3BookingService.createConfirmation(
        booking = bookingEntity,
        dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
        assertThat(it.notes).isEqualTo("notes")
        assertThat(it).isEqualTo(bookingEntity.confirmation)
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.confirmed)
      }

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
    fun `createConfirmation returns Success with correct result when validation passes and emits domain event and closes referral`() {
      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val bookingEntity = createCas3Booking(application = application)

      every { mockCas3v2ConfirmationRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2ConfirmationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(Cas3BookingEntity::class), user) } just Runs
      every { mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id) } returns assessment
      every { mockAssessmentService.closeAssessment(user, assessment.id) } returns CasResult.Success(assessment)

      mockkStatic(Sentry::class)

      val result = cas3BookingService.createConfirmation(
        booking = bookingEntity,
        dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
        assertThat(it.notes).isEqualTo("notes")
        assertThat(it).isEqualTo(bookingEntity.confirmation)
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.confirmed)
      }

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
    fun `createConfirmation returns Success with correct result and does not close the referral when assessment is not found`() {
      val bookingEntity = createCas3Booking(
        application = TemporaryAccommodationApplicationEntityFactory()
          .withProbationRegion(user.probationRegion)
          .withCreatedByUser(user)
          .produce(),
      )

      every { mockCas3v2ConfirmationRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2ConfirmationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(Cas3BookingEntity::class), user) } just Runs
      every { mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id) } returns null

      val result = cas3BookingService.createConfirmation(
        booking = bookingEntity,
        dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
        notes = "notes",
        user = user,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
        assertThat(it.notes).isEqualTo("notes")
        assertThat(it).isEqualTo(bookingEntity.confirmation)
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.confirmed)
      }

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
    fun `createConfirmation returns Success with correct result when closing referral fails`() {
      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val bookingEntity = createCas3Booking(application = application)

      every { mockCas3v2ConfirmationRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2ConfirmationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(Cas3BookingEntity::class), user) } just Runs
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

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
        assertThat(it.notes).isEqualTo("notes")
        assertThat(it).isEqualTo(bookingEntity.confirmation)
        assertThat(it.booking.status).isEqualTo(Cas3BookingStatus.confirmed)
      }

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
  }

  @Nested
  inner class CreateExtension {

    @BeforeEach
    fun setup() {
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }
      every { mockExtensionRepository.save(any()) } answers { it.invocation.args[0] as Cas3ExtensionEntity }
      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf()
      every { mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(any(), any(), any(), any()) } returns listOf()
    }

    @Test
    fun `Success with correct result when a booking has a new departure date before the existing departure date`() {
      val bookingEntity = createCas3Booking(
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-26"),
      )
      val result = cas3BookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = LocalDate.parse("2022-08-25"),
        notes = "notes",
      )
      assertThatCasResult(result).isSuccess().with {
        assertThat(it.newDepartureDate).isEqualTo(LocalDate.parse("2022-08-25"))
        assertThat(it.previousDepartureDate).isEqualTo(LocalDate.parse("2022-08-26"))
        assertThat(it.notes).isEqualTo("notes")
      }
    }

    @Test
    fun `FieldValidationError when a booking has a new departure date before the arrival date`() {
      val bookingEntity = createCas3Booking(
        arrivalDate = LocalDate.parse("2022-08-26"),
      )
      val result = cas3BookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = LocalDate.parse("2022-08-25"),
        notes = "notes",
      )
      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.newDepartureDate", "beforeBookingArrivalDate")
    }
  }

  @Nested
  inner class CreateTurnaround {
    val booking = createCas3Booking()

    @Test
    fun `createTurnaround returns FieldValidationError if the number of working days is a negative integer`() {
      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf()
      every { mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(any(), any(), any(), any()) } returns listOf()
      every { mockCas3v2TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2TurnaroundEntity }

      val result = cas3BookingService.createTurnaround(booking, -1)

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.workingDays", "isNotAPositiveInteger")
    }

    @Test
    fun `createTurnaround returns Success with the persisted entity if the number of working days is a positive integer`() {
      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf()
      every { mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(any(), any(), any(), any()) } returns listOf()
      every { mockCas3v2TurnaroundRepository.save(any()) } answers { it.invocation.args[0] as Cas3v2TurnaroundEntity }

      val result = cas3BookingService.createTurnaround(booking, 2)
      assertThatCasResult(result).isSuccess().with {
        assertThat(it.booking).isEqualTo(booking)
        assertThat(it.workingDayCount).isEqualTo(2)
      }
    }
  }

  private fun createCas3Booking(
    arrivalDate: LocalDate = LocalDate.now().randomDateBefore(14),
    departureDate: LocalDate = LocalDate.now().randomDateAfter(14),
    application: TemporaryAccommodationApplicationEntity? = null,
  ): Cas3BookingEntity {
    val premises = Cas3PremisesEntityFactory()
      .withDefaults()
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()
    val bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .produce()
    return Cas3BookingEntityFactory()
      .withPremises(premises)
      .withBedspace(bedspace)
      .withServiceName(ServiceName.temporaryAccommodation)
      .withArrivalDate(arrivalDate)
      .withDepartureDate(departureDate)
      .withApplication(application)
      .produce()
  }

  private fun createPremisesAndBedspace(bedspaceStartDate: LocalDate = LocalDate.now(), bedspaceEndDate: LocalDate? = null): Pair<Cas3PremisesEntity, Cas3BedspacesEntity> {
    val premises = Cas3PremisesEntityFactory()
      .withDefaults()
      .produce()

    val bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
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

  @Nested
  inner class ValidationTests {
    private val id = UUID.randomUUID()
    private val arrivalDate = LocalDate.now().plusDays(1)
    private val departureDate = LocalDate.now().plusDays(20)

    @Test
    fun `throws Conflict problem when booking dates conflict`() {
      val booking = Cas3BookingEntityFactory().withId(id)
        .withArrivalDate(LocalDate.now().plusDays(14))
        .withDepartureDate(departureDate)
        .withPremises(mockk<Cas3PremisesEntity>())
        .withBedspace(mockk<Cas3BedspacesEntity>())
        .produce()

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf(booking)
      every { mockWorkingDayService.addWorkingDays(any(), any()) } returns departureDate
      assertThrows<ConflictProblem> {
        cas3BookingService.throwIfBookingDatesConflict(
          arrivalDate = arrivalDate,
          departureDate = departureDate,
          thisEntityId = null,
          bedspaceId = id,
        )
      }
    }

    @Test
    fun `throws Conflict problem when void bedspace dates conflict`() {
      val voidBedspace =
        Cas3VoidBedspaceEntityFactory().withYieldedReason { mockk<Cas3VoidBedspaceReasonEntity>() }.produce()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
          any(),
          any(),
          any(),
          any(),
        )
      } returns listOf(voidBedspace)

      assertThrows<ConflictProblem> {
        cas3BookingService.throwIfVoidBedspaceDatesConflict(
          startDate = arrivalDate,
          endDate = departureDate,
          bookingId = null,
          bedspaceId = id,
        )
      }
    }
  }
}
