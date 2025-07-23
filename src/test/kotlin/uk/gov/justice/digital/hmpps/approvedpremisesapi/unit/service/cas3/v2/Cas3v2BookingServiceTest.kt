package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3.v2

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
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingAndPersons
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2TurnaroundRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.v2.Cas3v2BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3DomainEventService as Cas3DomainEventService

class Cas3v2BookingServiceTest {
  private val mockOffenderService = mockk<OffenderService>()
  private val mockCas3DomainEventService = mockk<Cas3DomainEventService>()
  private val mockWorkingDayService = mockk<WorkingDayService>()

  private val mockBookingRepository = mockk<Cas3v2BookingRepository>()
  private val mockBedspaceRepository = mockk<Cas3BedspacesRepository>()
  private val mockCas3v2TurnaroundRepository = mockk<Cas3v2TurnaroundRepository>()
  private val mockCas3VoidBedspacesRepository = mockk<Cas3VoidBedspacesRepository>()
  private val mockAssessmentRepository = mockk<AssessmentRepository>()
  private val mockUserAccessService = mockk<UserAccessService>()
  private val mockUserService = mockk<UserService>()
  private val mockOffenderDetailService = mockk<OffenderDetailService>()

  private fun createCas3BookingService(): Cas3v2BookingService = Cas3v2BookingService(
    cas3BookingRepository = mockBookingRepository,
    cas3BedspaceRepository = mockBedspaceRepository,
    cas3v2TurnaroundRepository = this.mockCas3v2TurnaroundRepository,
    assessmentRepository = mockAssessmentRepository,
    cas3VoidBedspacesRepository = mockCas3VoidBedspacesRepository,
    offenderService = mockOffenderService,
    workingDayService = mockWorkingDayService,
    cas3DomainEventService = mockCas3DomainEventService,
    userAccessService = mockUserAccessService,
    userService = mockUserService,
    offenderDetailService = mockOffenderDetailService,
  )

  private val cas3BookingService = createCas3BookingService()

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

    private val personInfo = PersonInfoResult.Success.Full(
      crn = bookingEntity.crn,
      offenderDetailSummary = OffenderDetailsSummaryFactory().produce(),
      inmateDetail = InmateDetailFactory().produce(),
    )

    @Test
    fun `returns a booking where booking's premises matches supplied premisesId`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanManagePremisesBookings(user, bookingEntity.premises) } returns true
      every {
        mockOffenderDetailService.getPersonInfoResult(
          bookingEntity.crn,
          user.deliusUsername,
          user.hasQualification(
            UserQualification.LAO,
          ),
        )
      } returns personInfo

      val result = cas3BookingService.getBooking(
        bookingId = bookingEntity.id,
        premisesId = premises.id,
      )

      assertThatCasResult(result).isSuccess().with {
        result as CasResult.Success
        assertThat(result.value).isEqualTo(Cas3BookingAndPersons(bookingEntity, personInfo))
      }
    }

    @Test
    fun `returns a booking where premisesId not supplied`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanManagePremisesBookings(user, bookingEntity.premises) } returns true
      every {
        mockOffenderDetailService.getPersonInfoResult(
          bookingEntity.crn,
          user.deliusUsername,
          user.hasQualification(
            UserQualification.LAO,
          ),
        )
      } returns personInfo

      val result = cas3BookingService.getBooking(
        bookingId = bookingEntity.id,
        premisesId = null,
      )

      assertThatCasResult(result).isSuccess().with {
        result as CasResult.Success
        assertThat(result.value).isEqualTo(Cas3BookingAndPersons(bookingEntity, personInfo))
      }
    }

    @Test
    fun `returns a GeneralValidationError when booking's premises does not match the premisesId`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanManagePremisesBookings(user, bookingEntity.premises) } returns true
      every {
        mockOffenderDetailService.getPersonInfoResult(
          bookingEntity.crn,
          user.deliusUsername,
          user.hasQualification(
            UserQualification.LAO,
          ),
        )
      } returns personInfo

      val result = cas3BookingService.getBooking(
        bookingId = bookingEntity.id,
        premisesId = UUID.randomUUID(),
      )

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      result as CasResult.GeneralValidationError

      assertThat(result.message).isEqualTo("The supplied premisesId does not match the booking's premises")
    }

    @Test
    fun `returns NotFound if the booking cannot be found`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns null

      val result = cas3BookingService.getBooking(
        bookingId = bookingEntity.id,
        premisesId = premises.id,
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
      )

      assertThatCasResult(authorisableResult).isFieldValidationError().hasMessage("$.bedspaceId", "empty")
    }

    @Test
    fun `createBooking returns FieldValidationError if Departure Date is before Arrival Date`() {
      val crn = "CRN123"
      val bedId = UUID.fromString("3b2f46de-a785-45ab-ac02-5e532c600647")
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(15)
      val departureDate = arrivalDate.minusDays(1)
      val assessmentId = UUID.randomUUID()

      every { mockBedspaceRepository.findByIdOrNull(bedId) } returns null

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedId, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedId,
          arrivalDate,
          departureDate,
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
      )

      assertThatCasResult(authorisableResult).isFieldValidationError().hasMessage("$.departureDate", "beforeBookingArrivalDate")
    }

    @Test
    fun `createBooking returns FieldValidationError if Bedspace does not exist`() {
      val crn = "CRN123"
      val bedId = UUID.fromString("3b2f46de-a785-45ab-ac02-5e532c600647")
      val bedspaceStartDate = LocalDate.now().randomDateBefore(30)
      val arrivalDate = bedspaceStartDate.randomDateAfter(3)
      val departureDate = arrivalDate.randomDateAfter(12)
      val assessmentId = UUID.randomUUID()

      every { mockBedspaceRepository.findByIdOrNull(bedId) } returns null
      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedId, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedId,
          arrivalDate,
          departureDate,
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

      every { mockBedspaceRepository.findByIdOrNull(bedId) } returns bedspace

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedId, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedId,
          arrivalDate,
          departureDate,
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

      every { mockBedspaceRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as Cas3BookingEntity }

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bed.id, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
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
        bed.id,
        assessmentId,
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

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
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

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
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

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
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

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
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
              it.workingDayCount == 0
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

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
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

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
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

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
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

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
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

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
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

      every { mockBookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspace.id, departureDate) } returns listOf()
      every {
        mockCas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
          bedspace.id,
          arrivalDate,
          departureDate,
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

    private fun createPremisesAndBedspace(bedspaceStartDate: LocalDate, bedspaceEndDate: LocalDate? = null): Pair<Cas3PremisesEntity, Cas3BedspacesEntity> {
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
  }
}
