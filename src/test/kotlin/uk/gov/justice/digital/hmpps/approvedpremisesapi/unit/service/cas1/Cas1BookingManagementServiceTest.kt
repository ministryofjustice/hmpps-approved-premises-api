package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssignKeyWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableCas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableCas1SpaceBookingEntityRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository.Constants.MOVE_ON_CATEGORY_NOT_APPLICABLE_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingManagementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingManagementDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.DepartureInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.ArrivalRecorded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.stream.Stream
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

@ExtendWith(MockKExtension::class)
class Cas1BookingManagementServiceTest {

  @MockK
  lateinit var cas1PremisesService: Cas1PremisesService

  @MockK
  lateinit var spaceBookingRepository: Cas1SpaceBookingRepository

  @MockK
  lateinit var cas1SpaceBookingManagementDomainEventService: Cas1SpaceBookingManagementDomainEventService

  @MockK
  lateinit var moveOnCategoryRepository: MoveOnCategoryRepository

  @MockK
  lateinit var departureReasonRepository: DepartureReasonRepository

  @MockK
  lateinit var staffMemberService: StaffMemberService

  @MockK
  lateinit var nonArrivalReasonRepository: NonArrivalReasonRepository

  @MockK
  lateinit var lockableCas1SpaceBookingRepository: LockableCas1SpaceBookingEntityRepository

  @MockK
  lateinit var userService: UserService

  @MockK
  lateinit var cas1ChangeRequestService: Cas1ChangeRequestService

  @MockK
  lateinit var applicationEventPublisher: ApplicationEventPublisher

  @InjectMockKs
  lateinit var service: Cas1BookingManagementService

  @Nested
  inner class RecordArrival {
    private val existingArrivalDate = LocalDate.of(2024, 1, 1)
    private val existingArrivalTime = LocalTime.of(3, 4, 5)
    private val arrivalDate = LocalDate.of(2024, 1, 2)
    private val arrivalTime = LocalTime.of(3, 4, 5)

    private val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .produce()

    @BeforeEach
    fun before() {
      every { applicationEventPublisher.publishEvent(any(JvmType.Object::class)) } just Runs
    }

    @Test
    fun `Returns validation error if no premises with the given premisesId exists`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.recordArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        arrivalDate = arrivalDate,
        arrivalTime = arrivalTime,
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.premisesId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if no space booking with the given bookingId exists`() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )

      every { spaceBookingRepository.findByIdOrNull(any()) } returns null

      val result = service.recordArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        arrivalDate = arrivalDate,
        arrivalTime = arrivalTime,
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.bookingId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns conflict error if the space booking record already has an arrival date recorded that does not match the received arrival date or arrival time`() {
      val existingSpaceBookingWithArrivalDate = existingSpaceBooking.copy(
        actualArrivalDate = existingArrivalDate,
        actualArrivalTime = existingArrivalTime,
      )

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )

      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithArrivalDate

      val result = service.recordArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        arrivalDate = arrivalDate,
        arrivalTime = arrivalTime,
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.message).isEqualTo("An arrival is already recorded for this Space Booking")
    }

    @Test
    fun `Returns conflict error if the space booking record already has an arrival date recorded that doesn't match the requested arrival date time`() {
      val existingSpaceBookingWithArrivalDate = existingSpaceBooking.copy(
        actualArrivalDate = existingArrivalDate,
        actualArrivalTime = existingArrivalTime,
        canonicalArrivalDate = existingArrivalDate,
      )

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )

      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithArrivalDate

      val result = service.recordArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        arrivalDate = arrivalDate,
        arrivalTime = arrivalTime.plusSeconds(1),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.message).isEqualTo("An arrival is already recorded for this Space Booking")
    }

    @Test
    fun `Returns conflict error if the space booking has already been cancelled`() {
      val existingSpaceBookingWithArrivalDate = existingSpaceBooking.copy(
        cancellationOccurredAt = LocalDate.now().minusDays(10),
      )

      every { cas1PremisesService.findPremiseById(any()) } returns premises

      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithArrivalDate

      val result = service.recordArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        arrivalDate = arrivalDate,
        arrivalTime = arrivalTime,
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThat(result)
        .isConflictError()
        .hasMessageContaining("The booking has already been cancelled")
    }

    @Test
    fun `Returns success without updates if the space booking record already has the exact same arrival date and time recorded`() {
      val existingSpaceBookingWithArrivalDate = existingSpaceBooking.copy(
        actualArrivalDate = existingArrivalDate,
        actualArrivalTime = existingArrivalTime,
        canonicalArrivalDate = existingArrivalDate,
      )

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )

      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithArrivalDate

      val result = service.recordArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        arrivalDate = existingArrivalDate,
        arrivalTime = existingArrivalTime,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)

      val extractedResult = (result as CasResult.Success).value

      assertThat(extractedResult.actualArrivalDate).isEqualTo(existingArrivalDate)
      assertThat(extractedResult.actualArrivalTime).isEqualTo(existingArrivalTime)
      assertThat(extractedResult.canonicalArrivalDate).isEqualTo(existingArrivalDate)
    }

    @Test
    fun `Updates space booking with arrival information, raises domain event and email`() {
      val user = UserEntityFactory().withDefaults().produce()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )

      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0

      val arrivalInfoCaptor = slot<Cas1SpaceBookingManagementDomainEventService.ArrivalInfo>()
      every { cas1ChangeRequestService.spaceBookingHasArrival(capture(updatedSpaceBookingCaptor)) } returns Unit

      every { cas1SpaceBookingManagementDomainEventService.arrivalRecorded(capture(arrivalInfoCaptor)) } returns Unit
      every { userService.getUserForRequest() } returns user

      val result = service.recordArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        arrivalDate = arrivalDate,
        arrivalTime = arrivalTime,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      verify { cas1ChangeRequestService.spaceBookingHasArrival(any()) }

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.expectedArrivalDate).isEqualTo(existingSpaceBooking.expectedArrivalDate)
      assertThat(updatedSpaceBooking.actualArrivalDate).isEqualTo(LocalDate.of(2024, 1, 2))
      assertThat(updatedSpaceBooking.actualArrivalTime).isEqualTo(LocalTime.of(3, 4, 5))
      assertThat(updatedSpaceBooking.canonicalArrivalDate).isEqualTo(LocalDate.of(2024, 1, 2))

      val arrivalInfo = arrivalInfoCaptor.captured
      assertThat(arrivalInfo.updatedCas1SpaceBooking).isEqualTo(updatedSpaceBooking)
      assertThat(arrivalInfo.actualArrivalDate).isEqualTo(LocalDate.of(2024, 1, 2))
      assertThat(arrivalInfo.actualArrivalTime).isEqualTo(LocalTime.of(3, 4, 5))
      assertThat(arrivalInfo.recordedBy).isEqualTo(user)

      verify { applicationEventPublisher.publishEvent(ArrivalRecorded(updatedSpaceBooking)) }
    }
  }

  @Nested
  inner class RecordNonArrival {

    private val originalNonArrivalDate = LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC)

    private val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .produce()

    private val recordedBy = UserEntityFactory()
      .withDefaults()
      .produce()

    private val nonArrivalReason =
      NonArrivalReasonEntityFactory()
        .produce()

    private val cas1NonArrival =
      Cas1NonArrival(nonArrivalReason.id, "non arrival notes")

    @Test
    fun `Returns validation error if no premises with the given premisesId exists`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { nonArrivalReasonRepository.findByIdOrNull(any()) } returns nonArrivalReason

      val result = service.recordNonArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        cas1NonArrival = cas1NonArrival,
        recordedBy = recordedBy,
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.premisesId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if no booking with the given bookingId exists`() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns null
      every { nonArrivalReasonRepository.findByIdOrNull(any()) } returns nonArrivalReason

      val result = service.recordNonArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        cas1NonArrival = cas1NonArrival,
        recordedBy = recordedBy,
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.bookingId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if no non arrival reason with the given reasonId exists`() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { nonArrivalReasonRepository.findByIdOrNull(any()) } returns null

      val result = service.recordNonArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        cas1NonArrival = cas1NonArrival,
        recordedBy = recordedBy,
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.reasonId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns conflict error if the space booking record already has a non arrival recorded with different data`() {
      val existingSpaceBookingWithNonArrivalDate =
        existingSpaceBooking.copy(
          nonArrivalConfirmedAt = originalNonArrivalDate,
          nonArrivalReason = nonArrivalReason,
          nonArrivalNotes = "new non arrival notes",
        )

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithNonArrivalDate
      every { nonArrivalReasonRepository.findByIdOrNull(any()) } returns nonArrivalReason

      val result = service.recordNonArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        cas1NonArrival = cas1NonArrival,
        recordedBy = recordedBy,
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.message).isEqualTo("A non-arrival is already recorded for this Space Booking")
    }

    @Test
    fun `Returns conflict error if the space booking has already been cancelled`() {
      val existingSpaceBookingWithNonArrivalDate =
        existingSpaceBooking.copy(
          cancellationOccurredAt = LocalDate.now().minusDays(10),

        )

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithNonArrivalDate
      every { nonArrivalReasonRepository.findByIdOrNull(any()) } returns nonArrivalReason

      val result = service.recordNonArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        cas1NonArrival = cas1NonArrival,
        recordedBy = recordedBy,
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThat(result)
        .isConflictError()
        .hasMessageContaining("The booking has already been cancelled")
    }

    @Test
    fun `Returns success if the space booking record already has a non arrival recorded with same data`() {
      val existingSpaceBookingWithNonArrivalDate =
        existingSpaceBooking.copy(
          nonArrivalConfirmedAt = originalNonArrivalDate,
          nonArrivalReason = nonArrivalReason,
          nonArrivalNotes = cas1NonArrival.notes,
        )

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithNonArrivalDate
      every { nonArrivalReasonRepository.findByIdOrNull(any()) } returns nonArrivalReason

      val result = service.recordNonArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        cas1NonArrival = cas1NonArrival,
        recordedBy = recordedBy,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
    }

    @Test
    fun `Updates existing space booking with non arrival information and raises domain event`() {
      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { nonArrivalReasonRepository.findByIdOrNull(any()) } returns nonArrivalReason
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1ChangeRequestService.spaceBookingMarkedAsNonArrival(capture(updatedSpaceBookingCaptor)) } returns Unit
      every { cas1SpaceBookingManagementDomainEventService.nonArrivalRecorded(any(), any(), any(), any()) } returns Unit

      val result = service.recordNonArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        cas1NonArrival = cas1NonArrival,
        recordedBy,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      verify { cas1ChangeRequestService.spaceBookingMarkedAsNonArrival(any()) }

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(nonArrivalReason).isEqualTo(updatedSpaceBooking.nonArrivalReason)
      assertThat("non arrival notes").isEqualTo(updatedSpaceBooking.nonArrivalNotes)
      assertThat(updatedSpaceBooking.nonArrivalConfirmedAt).isWithinTheLastMinute()
    }
  }

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @Nested
  inner class RecordDeparture {
    private val actualArrivalDate = LocalDate.of(2023, 1, 20)
    private val actualArrivalTime = LocalTime.of(0, 0, 0)
    private val actualDepartureDate = LocalDate.of(2023, 2, 1)
    private val actualDepartureTime = LocalTime.of(12, 45, 0)
    private val departureReason = DepartureReasonEntity(
      id = UUID.randomUUID(),
      name = "departureReason",
      isActive = true,
      serviceScope = "approved-premises",
      legacyDeliusReasonCode = "legacyDeliusReasonCode",
      parentReasonId = null,
    )
    private val departureReasonWithInvalidScope = DepartureReasonEntity(
      id = UUID.randomUUID(),
      name = "departureReason",
      isActive = true,
      serviceScope = "temporary-accommodation",
      legacyDeliusReasonCode = "legacyDeliusReasonCode",
      parentReasonId = null,
    )
    private val departureMoveOnCategory = MoveOnCategoryEntity(
      id = UUID.randomUUID(),
      name = "moveOnCategory",
      isActive = true,
      serviceScope = "approved-premises",
      legacyDeliusCategoryCode = "legacyDeliusReasonCode",
    )
    private val departureNotApplicableMoveOnCategory = MoveOnCategoryEntity(
      id = MOVE_ON_CATEGORY_NOT_APPLICABLE_ID,
      name = "notApplicableMoveOnCategory",
      isActive = true,
      serviceScope = "approved-premises",
      legacyDeliusCategoryCode = "legacyDeliusReasonCode",
    )
    private val departureMoveOnCategoryWithInvalidScope = MoveOnCategoryEntity(
      id = UUID.randomUUID(),
      name = "moveOnCategory",
      isActive = true,
      serviceScope = "temporary-accommodation",
      legacyDeliusCategoryCode = "legacyDeliusReasonCode",
    )

    private val departureNotes = "these are departure notes"

    private val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .withActualArrivalDate(actualArrivalDate)
      .withActualArrivalTime(actualArrivalTime)
      .produce()

    private val existingSpaceBooking2 = Cas1SpaceBookingEntityFactory()
      .withActualArrivalDate(actualArrivalDate)
      .withActualArrivalTime(actualArrivalTime)
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .produce()

    @BeforeEach
    fun setup() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { departureReasonRepository.findByIdOrNull(any()) } returns departureReason
      every { moveOnCategoryRepository.findByIdOrNull(any()) } returns departureMoveOnCategory
    }

    @Test
    fun `Returns validation error if no premises exist with the given premisesId`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          actualDepartureDate,
          actualDepartureTime,
          UUID.randomUUID(),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.premisesId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if no space booking exists with the given bookingId`() {
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns null

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          actualDepartureDate,
          actualDepartureTime,
          UUID.randomUUID(),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.bookingId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if no departure reason exists with the given reasonId `() {
      every { departureReasonRepository.findByIdOrNull(any()) } returns null

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          actualDepartureDate,
          actualDepartureTime,
          UUID.randomUUID(),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.cas1NewDeparture.reasonId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if the departure reason provided is not for the approved-premises service scope`() {
      every { departureReasonRepository.findByIdOrNull(any()) } returns departureReasonWithInvalidScope

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          actualDepartureDate,
          actualDepartureTime,
          UUID.randomUUID(),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.cas1NewDeparture.reasonId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if no move on category exists with the given moveOnCategoryId`() {
      every { moveOnCategoryRepository.findByIdOrNull(any()) } returns null

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          actualDepartureDate,
          actualDepartureTime,
          UUID.randomUUID(),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.cas1NewDeparture.moveOnCategoryId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if the move on category provided is not for the approves-premises service scope`() {
      every { moveOnCategoryRepository.findByIdOrNull(any()) } returns departureMoveOnCategoryWithInvalidScope

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          actualDepartureDate,
          actualDepartureTime,
          UUID.randomUUID(),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.cas1NewDeparture.moveOnCategoryId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns conflict error if the space booking does not have an actual arrival date`() {
      val existingSpaceBookingWithArrivalDate = existingSpaceBooking.copy(actualArrivalDate = null)

      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithArrivalDate

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          actualDepartureDate,
          actualDepartureTime,
          UUID.randomUUID(),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.message).isEqualTo("An arrival is not recorded for this Space Booking.")
    }

    @Test
    fun `Returns conflict error if the space booking actual departure date is before the actual arrival date`() {
      val existingSpaceBookingWithArrivalDateInFuture = existingSpaceBooking.copy(
        actualArrivalDate = actualDepartureDate.plusDays(1),
        actualArrivalTime = null,
      )

      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithArrivalDateInFuture

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          actualDepartureDate,
          actualDepartureTime,
          UUID.randomUUID(),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.message).isEqualTo("The departure date time is before the arrival date time.")
    }

    @Test
    fun `Returns conflict error if the space booking actual departure date time is before the actual arrival date time`() {
      val existingSpaceBookingWithArrivalDateInFuture = existingSpaceBooking.copy(
        actualArrivalDate = actualDepartureDate,
        actualArrivalTime = actualDepartureTime.plusSeconds(1),
      )

      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithArrivalDateInFuture

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          actualDepartureDate,
          actualDepartureTime,
          UUID.randomUUID(),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.message).isEqualTo("The departure date time is before the arrival date time.")
    }

    @ParameterizedTest
    @MethodSource("conflictCasesForSpaceBookingRecordDeparture")
    fun `Returns conflict error if the space booking has already recorded departure information that does not match the received departure information`(
      testCaseForDeparture: TestCaseForDeparture,
    ) {
      val existingSpaceBookingWithDepartureInfo = existingSpaceBooking.copy(
        actualDepartureDate = testCaseForDeparture.existingDepartureDate,
        actualDepartureTime = testCaseForDeparture.existingDepartureTime,
        departureReason = DepartureReasonEntityFactory()
          .withId(testCaseForDeparture.existingReasonId)
          .produce(),
        departureMoveOnCategory = MoveOnCategoryEntityFactory()
          .withId(testCaseForDeparture.existingMoveOnCategoryId)
          .produce(),
      )

      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithDepartureInfo

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          departureDate = testCaseForDeparture.newDepartureDate,
          departureTime = testCaseForDeparture.newDepartureTime,
          reasonId = testCaseForDeparture.newReasonId,
          moveOnCategoryId = testCaseForDeparture.newMoveOnCategoryId,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.message).isEqualTo("A departure is already recorded for this Space Booking.")
    }

    @Test
    fun `Returns conflict error if the space booking has already been cancelled`() {
      val existingSpaceBookingWithArrivalDate = existingSpaceBooking.copy(cancellationOccurredAt = LocalDate.now().minusDays(10))

      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithArrivalDate

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          actualDepartureDate,
          actualDepartureTime,
          UUID.randomUUID(),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThat(result)
        .isConflictError()
        .hasMessageContaining("The booking has already been cancelled")
    }

    @Test
    fun `Returns success if the space booking has already recorded departure information that matches the received departure information`() {
      val reasonId = UUID.randomUUID()
      val moveOnCategoryId = UUID.randomUUID()
      val departureNotes = "these are departure notes"
      val existingSpaceBookingWithDepartureInfo = existingSpaceBooking.copy(
        actualDepartureDate = actualDepartureDate,
        actualDepartureTime = actualDepartureTime,
        canonicalDepartureDate = actualDepartureDate,
        departureReason = DepartureReasonEntityFactory()
          .withId(reasonId)
          .produce(),
        departureMoveOnCategory = MoveOnCategoryEntityFactory()
          .withId(moveOnCategoryId)
          .produce(),
      )

      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithDepartureInfo

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          actualDepartureDate,
          actualDepartureTime,
          reasonId,
          moveOnCategoryId,
          departureNotes,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      val extractedResult = (result as CasResult.Success).value
    }

    @Test
    fun `Updates existing space booking with departure information and raises domain event`() {
      val user = UserEntityFactory().withDefaults().produce()

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0

      every { cas1ChangeRequestService.spaceBookingMarkedAsDeparted(capture(updatedSpaceBookingCaptor)) } returns Unit

      val departureInfoCaptor = slot<Cas1SpaceBookingManagementDomainEventService.DepartureInfo>()
      every { cas1SpaceBookingManagementDomainEventService.departureRecorded(capture(departureInfoCaptor)) } returns Unit
      every { userService.getUserForRequest() } returns user

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          departureDate = actualDepartureDate,
          departureTime = actualDepartureTime,
          reasonId = UUID.randomUUID(),
          moveOnCategoryId = UUID.randomUUID(),
          notes = "these are departure notes",
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      verify { cas1ChangeRequestService.spaceBookingMarkedAsDeparted(any()) }

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.actualDepartureDate).isEqualTo(LocalDate.of(2023, 2, 1))
      assertThat(updatedSpaceBooking.actualDepartureTime).isEqualTo(LocalTime.of(12, 45, 0))
      assertThat(updatedSpaceBooking.canonicalDepartureDate).isEqualTo(LocalDate.of(2023, 2, 1))
      assertThat(updatedSpaceBooking.departureReason).isEqualTo(departureReason)
      assertThat(updatedSpaceBooking.departureMoveOnCategory).isEqualTo(departureMoveOnCategory)
      assertThat(updatedSpaceBooking.departureNotes).isEqualTo(departureNotes)

      val departureInfo = departureInfoCaptor.captured
      assertThat(departureInfo.spaceBooking).isEqualTo(updatedSpaceBooking)
      assertThat(departureInfo.departureReason).isEqualTo(departureReason)
      assertThat(departureInfo.moveOnCategory).isEqualTo(departureMoveOnCategory)
      assertThat(departureInfo.actualDepartureDate).isEqualTo(LocalDate.of(2023, 2, 1))
      assertThat(departureInfo.actualDepartureTime).isEqualTo(LocalTime.of(12, 45, 0))
      assertThat(departureInfo.recordedBy).isEqualTo(user)
    }

    @Test
    fun `Updates existing space booking with 'Not Applicable' move-on-category if no move-on-category is supplied`() {
      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking2
      every { moveOnCategoryRepository.findByIdOrNull(MOVE_ON_CATEGORY_NOT_APPLICABLE_ID) } returns departureNotApplicableMoveOnCategory
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1ChangeRequestService.spaceBookingMarkedAsDeparted(capture(updatedSpaceBookingCaptor)) } returns Unit
      every { cas1SpaceBookingManagementDomainEventService.departureRecorded(any()) } returns Unit

      val result = service.recordDepartureForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        departureInfo = DepartureInfo(
          departureDate = actualDepartureDate,
          departureTime = actualDepartureTime,
          reasonId = UUID.randomUUID(),
          moveOnCategoryId = null,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      verify { cas1ChangeRequestService.spaceBookingMarkedAsDeparted(any()) }

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured

      assertThat(departureNotApplicableMoveOnCategory).isEqualTo(updatedSpaceBooking.departureMoveOnCategory)
    }

    @Suppress("UnusedPrivateMember")
    private fun conflictCasesForSpaceBookingRecordDeparture(): Stream<Arguments> {
      val reasonId = UUID.randomUUID()
      val moveOnCategoryId = UUID.randomUUID()
      return listOf(
        Arguments.of(
          TestCaseForDeparture(
            newReasonId = reasonId,
            newMoveOnCategoryId = moveOnCategoryId,
            newDepartureDate = LocalDate.of(2024, 2, 1),
            newDepartureTime = LocalTime.of(12, 0, 0),
            existingReasonId = UUID.randomUUID(),
            existingMoveOnCategoryId = moveOnCategoryId,
            existingDepartureDate = LocalDate.of(2024, 2, 1),
            existingDepartureTime = LocalTime.of(12, 0, 0),
          ),
        ),
        Arguments.of(
          TestCaseForDeparture(
            newReasonId = reasonId,
            newMoveOnCategoryId = moveOnCategoryId,
            newDepartureDate = LocalDate.of(2024, 2, 1),
            newDepartureTime = LocalTime.of(12, 0, 0),
            existingReasonId = reasonId,
            existingMoveOnCategoryId = UUID.randomUUID(),
            existingDepartureDate = LocalDate.of(2024, 2, 1),
            existingDepartureTime = LocalTime.of(12, 0, 0),
          ),
        ),
        Arguments.of(
          TestCaseForDeparture(
            newReasonId = reasonId,
            newMoveOnCategoryId = moveOnCategoryId,
            newDepartureDate = LocalDate.of(2024, 2, 1),
            newDepartureTime = LocalTime.of(12, 0, 0),
            existingReasonId = reasonId,
            existingMoveOnCategoryId = moveOnCategoryId,
            existingDepartureDate = LocalDate.of(2024, 2, 2),
            existingDepartureTime = LocalTime.of(12, 0, 0),
          ),
        ),
      ).stream()
    }
  }

  @Nested
  inner class RecordKeyWorker {

    private val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .withActualArrivalDate(LocalDate.now().minusDays(1))
      .withActualArrivalTime(LocalTime.now())
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .produce()

    val keyWorker = ContextStaffMemberFactory().produce()

    @BeforeEach
    fun setup() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every {
        staffMemberService.getStaffMemberByCodeForPremise(
          keyWorker.code,
          premises.qCode,
        )
      } returns CasResult.Success(keyWorker)
    }

    @Test
    fun `Returns validation error if no premises exist with the given premisesId`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null

      val result = service.recordKeyWorkerAssignedForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        keyWorker = Cas1AssignKeyWorker(keyWorker.code),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.premisesId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if no space booking exists with the given bookingId`() {
      every { spaceBookingRepository.findByIdOrNull(any()) } returns null

      val result = service.recordKeyWorkerAssignedForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        keyWorker = Cas1AssignKeyWorker(keyWorker.code),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.bookingId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if no staff record exists with the given staff code`() {
      every {
        staffMemberService.getStaffMemberByCodeForPremise(
          keyWorker.code,
          premises.qCode,
        )
      } returns CasResult.NotFound("staff", "qcode")

      val result = service.recordKeyWorkerAssignedForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        keyWorker = Cas1AssignKeyWorker(keyWorker.code),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.keyWorker.staffCode" && value == "notFound"
      }
    }

    @Test
    fun `Updates existing space booking with key worker information and raises domain event`() {
      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1SpaceBookingManagementDomainEventService.keyWorkerAssigned(any(), any(), any(), any()) } returns Unit

      val result = service.recordKeyWorkerAssignedForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        keyWorker = Cas1AssignKeyWorker(keyWorker.code),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(existingSpaceBooking.premises).isEqualTo(updatedSpaceBooking.premises)
      assertThat(existingSpaceBooking.placementRequest).isEqualTo(updatedSpaceBooking.placementRequest)
      assertThat(existingSpaceBooking.application).isEqualTo(updatedSpaceBooking.application)
      assertThat(existingSpaceBooking.createdAt).isEqualTo(updatedSpaceBooking.createdAt)
      assertThat(existingSpaceBooking.createdBy).isEqualTo(updatedSpaceBooking.createdBy)
      assertThat(existingSpaceBooking.crn).isEqualTo(updatedSpaceBooking.crn)

      assertThat(keyWorker.code).isEqualTo(updatedSpaceBooking.keyWorkerStaffCode)
      assertThat("${keyWorker.name.forename} ${keyWorker.name.surname}").isEqualTo(updatedSpaceBooking.keyWorkerName)
      assertThat(updatedSpaceBooking.keyWorkerAssignedAt).isWithinTheLastMinute()
    }
  }

  data class TestCaseForDeparture(
    val newReasonId: UUID,
    val newMoveOnCategoryId: UUID,
    val newDepartureDate: LocalDate,
    val newDepartureTime: LocalTime,
    val existingReasonId: UUID,
    val existingMoveOnCategoryId: UUID,
    val existingDepartureDate: LocalDate,
    val existingDepartureTime: LocalTime,
  )
}
