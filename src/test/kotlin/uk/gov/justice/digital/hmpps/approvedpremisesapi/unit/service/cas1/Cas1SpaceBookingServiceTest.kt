package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssignKeyWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewEmergencyTransfer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewPlannedTransfer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingResidency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PageCriteriaFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableCas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableCas1SpaceBookingEntityRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository.Constants.MOVE_ON_CATEGORY_NOT_APPLICABLE_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingManagementDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.DepartureInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.UpdateSpaceBookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class Cas1SpaceBookingServiceTest {
  private val cas1PremisesService = mockk<Cas1PremisesService>()
  private val placementRequestService = mockk<PlacementRequestService>()
  private val spaceBookingRepository = mockk<Cas1SpaceBookingRepository>()
  private val cas1BookingDomainEventService = mockk<Cas1BookingDomainEventService>()
  private val cas1BookingEmailService = mockk<Cas1BookingEmailService>()
  private val cas1SpaceBookingManagementDomainEventService = mockk<Cas1SpaceBookingManagementDomainEventService>()
  private val moveOnCategoryRepository = mockk<MoveOnCategoryRepository>()
  private val departureReasonRepository = mockk<DepartureReasonRepository>()
  private val cas1ApplicationStatusService = mockk<Cas1ApplicationStatusService>()
  private val staffMemberService = mockk<StaffMemberService>()
  private val cancellationReasonRepository = mockk<CancellationReasonRepository>()
  private val nonArrivalReasonRepository = mockk<NonArrivalReasonRepository>()
  private val lockablePlacementRequestRepository = mockk<LockablePlacementRequestRepository>()
  private val lockableCas1SpaceBookingRepository = mockk<LockableCas1SpaceBookingEntityRepository>()
  private val userService = mockk<UserService>()
  private val cas1ChangeRequestService = mockk<Cas1ChangeRequestService>()
  private val characteristicService = mockk<CharacteristicService>()

  private val service = Cas1SpaceBookingService(
    cas1PremisesService,
    placementRequestService,
    spaceBookingRepository,
    cas1BookingDomainEventService,
    cas1BookingEmailService,
    cas1SpaceBookingManagementDomainEventService,
    departureReasonRepository,
    moveOnCategoryRepository,
    cas1ApplicationStatusService,
    staffMemberService,
    cancellationReasonRepository,
    nonArrivalReasonRepository,
    lockablePlacementRequestRepository,
    lockableCas1SpaceBookingRepository,
    userService,
    cas1ChangeRequestService,
    characteristicService,
    Clock.systemDefaultZone(),
  )

  companion object CONSTANTS {
    val PREMISES_ID: UUID = UUID.randomUUID()
    val DESTINATION_PREMISES_ID: UUID = UUID.randomUUID()
  }

  @Nested
  inner class CreateNewBooking {

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withSupportsSpaceBookings(true)
      .produce()

    private val placementRequest = PlacementRequestEntityFactory()
      .withDefaults()
      .produce()

    private val user = UserEntityFactory()
      .withDefaults()
      .produce()

    @Test
    fun `Returns validation error if no premises with the given ID exists`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest
      every { lockablePlacementRequestRepository.acquirePessimisticLock(placementRequest.id) } returns
        LockablePlacementRequestEntity(placementRequest.id)

      val result = service.createNewBooking(
        premisesId = UUID.randomUUID(),
        placementRequestId = placementRequest.id,
        arrivalDate = LocalDate.now(),
        departureDate = LocalDate.now().plusDays(1),
        createdBy = user,
        characteristics = emptyList(),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.premisesId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if premises supplied does not support space bookings`() {
      val premisesDoesntSupportSpaceBookings = ApprovedPremisesEntityFactory()
        .withSupportsSpaceBookings(false)
        .withDefaults()
        .produce()

      every { cas1PremisesService.findPremiseById(any()) } returns premisesDoesntSupportSpaceBookings
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest
      every { lockablePlacementRequestRepository.acquirePessimisticLock(placementRequest.id) } returns
        LockablePlacementRequestEntity(placementRequest.id)

      val result = service.createNewBooking(
        premisesId = premisesDoesntSupportSpaceBookings.id,
        placementRequestId = placementRequest.id,
        arrivalDate = LocalDate.now(),
        departureDate = LocalDate.now().plusDays(1),
        createdBy = user,
        characteristics = emptyList(),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.premisesId" && value == "doesNotSupportSpaceBookings"
      }
    }

    @Test
    fun `Returns validation error if no placement request with the given ID exists`() {
      every { cas1PremisesService.findPremiseById(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(any()) } returns null
      every { lockablePlacementRequestRepository.acquirePessimisticLock(any()) } returns
        LockablePlacementRequestEntity(placementRequest.id)

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = UUID.randomUUID(),
        arrivalDate = LocalDate.now(),
        departureDate = LocalDate.now().plusDays(1),
        createdBy = user,
        characteristics = emptyList(),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.placementRequestId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if the departure date is before the arrival date`() {
      every { cas1PremisesService.findPremiseById(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest
      every { lockablePlacementRequestRepository.acquirePessimisticLock(placementRequest.id) } returns
        LockablePlacementRequestEntity(placementRequest.id)

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        arrivalDate = LocalDate.now().plusDays(1),
        departureDate = LocalDate.now(),
        createdBy = user,
        characteristics = emptyList(),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.departureDate" && value == "shouldBeAfterArrivalDate"
      }
    }

    @Test
    fun `Returns conflict error if a space booking already exists for the same premises and placement request`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withPremises(premises)
        .withPlacementRequest(placementRequest)
        .produce()

      every { cas1PremisesService.findPremiseById(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest
      every { spaceBookingRepository.findByPlacementRequestId(placementRequest.id) } returns listOf(existingSpaceBooking)
      every { lockablePlacementRequestRepository.acquirePessimisticLock(placementRequest.id) } returns
        LockablePlacementRequestEntity(placementRequest.id)

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        arrivalDate = LocalDate.now(),
        departureDate = LocalDate.now().plusDays(1),
        createdBy = user,
        characteristics = emptyList(),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.conflictingEntityId).isEqualTo(placementRequest.id)
      assertThat(result.message).contains("A Space Booking already exists")
    }

    @Test
    fun `Returns conflict error if a legacy booking already exists for the same premises and placement request`() {
      val legacyBooking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      val placementRequestwithLegacyBooking = placementRequest.copy(
        booking = legacyBooking,
      )

      every { cas1PremisesService.findPremiseById(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequestwithLegacyBooking
      every { lockablePlacementRequestRepository.acquirePessimisticLock(placementRequest.id) } returns
        LockablePlacementRequestEntity(placementRequest.id)

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        arrivalDate = LocalDate.now(),
        departureDate = LocalDate.now().plusDays(1),
        createdBy = user,
        characteristics = emptyList(),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.conflictingEntityId).isEqualTo(legacyBooking.id)
      assertThat(result.message).contains("A legacy Booking already exists")
    }

    @Test
    fun `Creates new booking if all data is valid, updates application status, raises domain event and sends email`() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withSupportsSpaceBookings(true)
        .produce()
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withEventNumber("42")
        .produce()

      val placementApplication = PlacementApplicationEntityFactory().withDefaults().produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withPlacementApplication(placementApplication)
        .produce()

      val arrivalDate = LocalDate.now()
      val durationInDays = 1
      val departureDate = arrivalDate.plusDays(durationInDays.toLong())

      every { cas1PremisesService.findPremiseById(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest
      every { spaceBookingRepository.findByPlacementRequestId(placementRequest.id) } returns emptyList()
      every { lockablePlacementRequestRepository.acquirePessimisticLock(placementRequest.id) } returns
        LockablePlacementRequestEntity(placementRequest.id)

      every { cas1ApplicationStatusService.spaceBookingMade(any()) } returns Unit

      every {
        cas1BookingDomainEventService.spaceBookingMade(
          application,
          any(),
          user,
          placementRequest,
        )
      } returns Unit

      every { cas1BookingEmailService.spaceBookingMade(any(), any()) } returns Unit

      val persistedBookingCaptor = slot<Cas1SpaceBookingEntity>()
      every { spaceBookingRepository.saveAndFlush(capture(persistedBookingCaptor)) } returnsArgument 0

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
        createdBy = user,
        characteristics = listOf(
          CharacteristicEntityFactory().withName("c1").produce(),
          CharacteristicEntityFactory().withName("c2").produce(),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      val persistedBooking = persistedBookingCaptor.captured
      assertThat(persistedBooking.premises).isEqualTo(premises)
      assertThat(persistedBooking.placementRequest).isEqualTo(placementRequest)
      assertThat(persistedBooking.application).isEqualTo(application)
      assertThat(persistedBooking.createdAt).isWithinTheLastMinute()
      assertThat(persistedBooking.createdBy).isEqualTo(user)
      assertThat(persistedBooking.expectedArrivalDate).isEqualTo(arrivalDate)
      assertThat(persistedBooking.expectedDepartureDate).isEqualTo(departureDate)
      assertThat(persistedBooking.actualArrivalDate).isNull()
      assertThat(persistedBooking.actualArrivalTime).isNull()
      assertThat(persistedBooking.actualDepartureDate).isNull()
      assertThat(persistedBooking.actualDepartureTime).isNull()
      assertThat(persistedBooking.canonicalArrivalDate).isEqualTo(arrivalDate)
      assertThat(persistedBooking.canonicalDepartureDate).isEqualTo(departureDate)
      assertThat(persistedBooking.crn).isEqualTo(application.crn)
      assertThat(persistedBooking.keyWorkerStaffCode).isNull()
      assertThat(persistedBooking.keyWorkerAssignedAt).isNull()
      assertThat(persistedBooking.criteria).hasSize(2)
      assertThat(persistedBooking.nonArrivalReason).isNull()
      assertThat(persistedBooking.nonArrivalNotes).isNull()
      assertThat(persistedBooking.nonArrivalReason).isNull()
      assertThat(persistedBooking.deliusEventNumber).isEqualTo("42")

      verify { cas1ApplicationStatusService.spaceBookingMade(persistedBooking) }
    }

    @Test
    fun `Creates a new booking if data is valid and legacy and space bookings are cancelled`() {
      val legacyBookingWithCancellation = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      val cancellationEntity = CancellationEntityFactory()
        .withBooking(legacyBookingWithCancellation)
        .withReason(CancellationReasonEntityFactory().produce())
        .produce()

      legacyBookingWithCancellation.cancellations = mutableListOf(cancellationEntity)

      val spaceBookingWithCancellation = Cas1SpaceBookingEntityFactory()
        .withPremises(premises)
        .withPlacementRequest(placementRequest)
        .withCancellationOccurredAt(LocalDate.now())
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withEventNumber("42")
        .produce()

      val placementApplication = PlacementApplicationEntityFactory().withDefaults().produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withBooking(legacyBookingWithCancellation)
        .withSpaceBookings(mutableListOf(spaceBookingWithCancellation))
        .withApplication(application)
        .withPlacementApplication(placementApplication)
        .produce()

      val arrivalDate = LocalDate.now()
      val durationInDays = 1
      val departureDate = arrivalDate.plusDays(durationInDays.toLong())

      every { cas1PremisesService.findPremiseById(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest
      every { spaceBookingRepository.findByPlacementRequestId(placementRequest.id) } returns emptyList()
      every { lockablePlacementRequestRepository.acquirePessimisticLock(placementRequest.id) } returns
        LockablePlacementRequestEntity(placementRequest.id)

      every { cas1ApplicationStatusService.spaceBookingMade(any()) } returns Unit

      every {
        cas1BookingDomainEventService.spaceBookingMade(
          application,
          any(),
          user,
          placementRequest,
        )
      } returns Unit

      every { cas1BookingEmailService.spaceBookingMade(any(), any()) } returns Unit

      val persistedBookingCaptor = slot<Cas1SpaceBookingEntity>()
      every { spaceBookingRepository.saveAndFlush(capture(persistedBookingCaptor)) } returnsArgument 0

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
        createdBy = user,
        characteristics = listOf(
          CharacteristicEntityFactory().withName("c1").produce(),
          CharacteristicEntityFactory().withName("c2").produce(),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      val persistedBooking = persistedBookingCaptor.captured
      verify { cas1ApplicationStatusService.spaceBookingMade(persistedBooking) }
    }
  }

  @Nested
  inner class Search {

    @Test
    fun `approved premises not found return error`() {
      every { cas1PremisesService.findPremiseById(PREMISES_ID) } returns null

      val result = service.search(
        PREMISES_ID,
        Cas1SpaceBookingService.SpaceBookingFilterCriteria(
          residency = null,
          crnOrName = null,
          keyWorkerStaffCode = null,
        ),
        PageCriteriaFactory(Cas1SpaceBookingSummarySortField.canonicalArrivalDate)
          .produce(),
      )

      assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
    }

    @ParameterizedTest
    @CsvSource(
      "personName,personName",
      "canonicalArrivalDate,canonicalArrivalDate",
      "canonicalDepartureDate,canonicalDepartureDate",
      "keyWorkerName,keyWorkerName",
      "tier,tier",
    )
    fun `delegate to repository, defining correct sort column`(
      inputSortField: Cas1SpaceBookingSummarySortField,
      sqlSortField: String,
    ) {
      every { cas1PremisesService.findPremiseById(PREMISES_ID) } returns ApprovedPremisesEntityFactory().withDefaults()
        .produce()

      val results = PageImpl(
        listOf(
          mockk<Cas1SpaceBookingSearchResult>(),
          mockk<Cas1SpaceBookingSearchResult>(),
          mockk<Cas1SpaceBookingSearchResult>(),
        ),
      )
      val pageableCaptor = slot<Pageable>()

      every {
        spaceBookingRepository.search(
          "current",
          "theCrnOrName",
          "keyWorkerStaffCode",
          PREMISES_ID,
          capture(pageableCaptor),
        )
      } returns results

      val result = service.search(
        PREMISES_ID,
        Cas1SpaceBookingService.SpaceBookingFilterCriteria(
          residency = Cas1SpaceBookingResidency.current,
          crnOrName = "theCrnOrName",
          keyWorkerStaffCode = "keyWorkerStaffCode",
        ),
        PageCriteriaFactory(inputSortField).produce(),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.results).hasSize(3)

      assertThat(pageableCaptor.captured.sort.toList()[0].property).isEqualTo(sqlSortField)
    }
  }

  @Nested
  inner class GetBooking {

    @Test
    fun `Returns not found error if premises with the given ID doesn't exist`() {
      every { cas1PremisesService.premiseExistsById(any()) } returns false

      val result = service.getBooking(UUID.randomUUID(), UUID.randomUUID())

      assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
      assertThat((result as CasResult.NotFound).entityType).isEqualTo("premises")
    }

    @Test
    fun `Returns not found error if booking with the given ID doesn't exist`() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      every { cas1PremisesService.premiseExistsById(premises.id) } returns true
      every { spaceBookingRepository.findByIdOrNull(any()) } returns null

      val result = service.getBooking(premises.id, UUID.randomUUID())

      assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
      assertThat((result as CasResult.NotFound).entityType).isEqualTo("booking")
    }

    @Test
    fun `Returns booking info if exists`() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .produce()

      every { cas1PremisesService.premiseExistsById(premises.id) } returns true
      every { spaceBookingRepository.findByIdOrNull(spaceBooking.id) } returns spaceBooking

      val result = service.getBooking(premises.id, spaceBooking.id)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      assertThat((result as CasResult.Success).value).isEqualTo(spaceBooking)
    }
  }

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
    fun `Updates space booking with arrival information and raises domain event`() {
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

  @Nested
  inner class GetWithdrawalState {

    @Test
    fun `is withdrawable if no arrival and not cancelled`() {
      val result = service.getWithdrawableState(
        Cas1SpaceBookingEntityFactory()
          .withActualArrivalDate(null)
          .withCancellationOccurredAt(null)
          .withNonArrivalConfirmedAt(null)
          .produce(),
        UserEntityFactory().withDefaults().produce(),
      )

      assertThat(result.withdrawable).isEqualTo(true)
      assertThat(result.withdrawn).isEqualTo(false)
      assertThat(result.blockingReason).isNull()
    }

    @Test
    fun `is not withdrawable if has arrival`() {
      val result = service.getWithdrawableState(
        Cas1SpaceBookingEntityFactory()
          .withActualArrivalDate(LocalDate.now())
          .withCancellationOccurredAt(null)
          .withNonArrivalConfirmedAt(null)
          .produce(),
        UserEntityFactory().withDefaults().produce(),
      )

      assertThat(result.withdrawable).isEqualTo(false)
      assertThat(result.withdrawn).isEqualTo(false)
      assertThat(result.blockingReason).isEqualTo(BlockingReason.ArrivalRecordedInCas1)
    }

    @Test
    fun `is not withdrawable if has a non-arrival`() {
      val result = service.getWithdrawableState(
        Cas1SpaceBookingEntityFactory()
          .withActualArrivalDate(null)
          .withCancellationOccurredAt(null)
          .withNonArrivalConfirmedAt(Instant.now())
          .produce(),
        UserEntityFactory().withDefaults().produce(),
      )

      assertThat(result.withdrawable).isEqualTo(false)
      assertThat(result.withdrawn).isEqualTo(false)
      assertThat(result.blockingReason).isEqualTo(BlockingReason.NonArrivalRecordedInCas1)
    }

    @Test
    fun `is not withdrawable if already cancelled`() {
      val result = service.getWithdrawableState(
        Cas1SpaceBookingEntityFactory()
          .withActualArrivalDate(null)
          .withCancellationOccurredAt(LocalDate.now())
          .withNonArrivalConfirmedAt(null)
          .produce(),
        UserEntityFactory().withDefaults().produce(),
      )

      assertThat(result.withdrawable).isEqualTo(false)
      assertThat(result.withdrawn).isEqualTo(true)
      assertThat(result.blockingReason).isNull()
    }

    @Test
    fun `user without CAS1_SPACE_BOOKING_WITHDRAW or CAS1_PLACEMENT_APPEAL_ASSESS permission cannot directly withdraw`() {
      val result = service.getWithdrawableState(
        Cas1SpaceBookingEntityFactory().produce(),
        UserEntityFactory.mockUserWithoutPermission(UserPermission.CAS1_SPACE_BOOKING_WITHDRAW, UserPermission.CAS1_PLACEMENT_APPEAL_ASSESS),
      )

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(false)
    }

    @Test
    fun `user with CAS1_SPACE_BOOKING_WITHDRAW can directly withdraw`() {
      val result = service.getWithdrawableState(
        Cas1SpaceBookingEntityFactory().produce(),
        UserEntityFactory.mockUserWithPermission(UserPermission.CAS1_SPACE_BOOKING_WITHDRAW),
      )

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(true)
    }

    @Test
    fun `user with CAS1_PLACEMENT_APPEAL_ASSESS can directly withdraw`() {
      val result = service.getWithdrawableState(
        Cas1SpaceBookingEntityFactory().produce(),
        UserEntityFactory.mockUserWithPermission(UserPermission.CAS1_PLACEMENT_APPEAL_ASSESS),
      )

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(true)
    }
  }

  @Nested
  inner class Withdraw {
    val user = UserEntityFactory().withDefaults().produce()
    val premises = ApprovedPremisesEntityFactory().withDefaults().produce()
    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    val reason = CancellationReasonEntityFactory().withServiceScope("*").produce()
    val reasonId = reason.id

    @Test
    fun `withdraw is idempotent if the booking is already cancelled`() {
      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(LocalDate.now())
        .produce()

      val result = service.withdraw(
        spaceBooking = spaceBooking,
        occurredAt = LocalDate.parse("2022-08-25"),
        userProvidedReasonId = UUID.randomUUID(),
        userProvidedReasonNotes = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.SpaceBooking,
          spaceBooking.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)

      verify(exactly = 0) { spaceBookingRepository.save(any()) }
    }

    @Test
    fun `withdraw returns FieldValidationError if reason id can't be resolved`() {
      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(null)
        .produce()

      val reasonId = UUID.randomUUID()

      every { cancellationReasonRepository.findByIdOrNull(reasonId) } returns null

      val result = service.withdraw(
        spaceBooking = spaceBooking,
        occurredAt = LocalDate.parse("2022-08-25"),
        userProvidedReasonId = reasonId,
        userProvidedReasonNotes = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.SpaceBooking,
          spaceBooking.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.reason", "doesNotExist"),
      )
    }

    @Test
    fun `withdraw returns FieldValidationError when reason is 'Other' and 'userProvidedReasonNotes' is blank`() {
      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(null)
        .produce()

      val reasonId = UUID.randomUUID()

      val reasonEntity = CancellationReasonEntityFactory()
        .withServiceScope(ServiceName.approvedPremises.value)
        .withName("Other")
        .produce()

      every { cancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity

      val result = service.withdraw(
        spaceBooking = spaceBooking,
        occurredAt = LocalDate.parse("2022-08-25"),
        userProvidedReasonId = reasonId,
        userProvidedReasonNotes = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.SpaceBooking,
          spaceBooking.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.otherReason", "empty"),
      )
    }

    @Test
    fun success() {
      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .withCancellationOccurredAt(null)
        .produce()

      val reasonId = UUID.randomUUID()

      val reason = CancellationReasonEntityFactory()
        .withServiceScope(ServiceName.approvedPremises.value)
        .withName("Some Reason")
        .produce()

      every { cancellationReasonRepository.findByIdOrNull(reasonId) } returns reason

      val spaceBookingCaptor = slot<Cas1SpaceBookingEntity>()
      every { spaceBookingRepository.save(capture(spaceBookingCaptor)) } returns spaceBooking

      every { cas1ChangeRequestService.spaceBookingWithdrawn(spaceBooking) } returns Unit
      every { cas1BookingEmailService.spaceBookingWithdrawn(spaceBooking, application, WithdrawalTriggeredByUser(user)) } returns Unit
      every { cas1BookingDomainEventService.spaceBookingCancelled(spaceBooking, user, reason) } returns Unit
      every { cas1ApplicationStatusService.spaceBookingCancelled(spaceBooking) } returns Unit

      val result = service.withdraw(
        spaceBooking = spaceBooking,
        occurredAt = LocalDate.parse("2022-08-25"),
        userProvidedReasonId = reasonId,
        userProvidedReasonNotes = "the user provided notes",
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.SpaceBooking,
          spaceBooking.id,
        ),
      )

      assertThatCasResult(result).isSuccess()

      val persistedBooking = spaceBookingCaptor.captured
      assertThat(persistedBooking.cancellationOccurredAt).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(persistedBooking.cancellationRecordedAt).isWithinTheLastMinute()
      assertThat(persistedBooking.cancellationReason).isEqualTo(reason)
      assertThat(persistedBooking.cancellationReasonNotes).isEqualTo("the user provided notes")

      verify { cas1ChangeRequestService.spaceBookingWithdrawn(spaceBooking) }
      verify { cas1BookingDomainEventService.spaceBookingCancelled(spaceBooking, user, reason) }
      verify { cas1ApplicationStatusService.spaceBookingCancelled(spaceBooking) }
      verify { cas1BookingEmailService.spaceBookingWithdrawn(spaceBooking, application, WithdrawalTriggeredByUser(user)) }
    }
  }

  @Nested
  inner class UpdateSpaceBooking {

    private val newArrivalDate = LocalDate.of(2025, 1, 2)
    private val newDepartureDate = LocalDate.now().plusMonths(1)

    private val user = UserEntityFactory()
      .withDefaults()
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withId(PREMISES_ID)
      .produce()

    private val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .withPremises(premises)
      .produce()

    @Test
    fun `should return validation error if no premises exist with the given premisesId`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateSpaceBooking(
        UpdateSpaceBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.premisesId" && value == "doesNotExist"
      }
    }

    @Test
    fun `should return validation error if no space booking exist with the given bookingId`() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns null

      val result = service.updateSpaceBooking(
        UpdateSpaceBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.bookingId" && value == "doesNotExist"
      }
    }

    @Test
    fun `should return validation error when booking status is canceled`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(LocalDate.now().minusWeeks(2))
        .withPremises(premises)
        .produce()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateSpaceBooking(
        UpdateSpaceBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.bookingId" && value == "This Booking is cancelled and as such cannot be modified"
      }
    }

    @Test
    fun `should return validation error when booking status is departed`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withActualDepartureDate(LocalDate.now().minusWeeks(2))
        .withPremises(premises)
        .produce()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateSpaceBooking(
        UpdateSpaceBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.bookingId" && value == "hasDepartedOrNonArrival"
      }
    }

    @Test
    fun `should return validation error when booking status is nonArrival`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withNonArrivalConfirmedAt(Instant.now())
        .withPremises(premises)
        .produce()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateSpaceBooking(
        UpdateSpaceBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.bookingId" && value == "hasDepartedOrNonArrival"
      }
    }

    @Test
    fun `should return validation error when premisesId does not match the existing booking`() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateSpaceBooking(
        UpdateSpaceBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.bookingId" && value == "premisesMismatch"
      }
    }

    @Test
    fun `should return validation error before arrival when new departure date is before updated arrival date`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 6, 5)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 6, 15)

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateSpaceBooking(
        UpdateSpaceBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = LocalDate.of(2025, 6, 17),
          departureDate = LocalDate.of(2025, 6, 16),
          updatedBy = user,
          characteristics = null,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is before the arrival date.")
    }

    @Test
    fun `should return validation error before arrival when new departure date is before existing arrival date`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 6, 5)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 6, 15)

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateSpaceBooking(
        UpdateSpaceBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = null,
          departureDate = LocalDate.of(2025, 6, 4),
          updatedBy = user,
          characteristics = null,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is before the arrival date.")
    }

    @Test
    fun `should return validation error before arrival when existing departure date is before new arrival date`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 6, 5)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 6, 15)

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateSpaceBooking(
        UpdateSpaceBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = LocalDate.of(2025, 6, 16),
          departureDate = null,
          updatedBy = user,
          characteristics = null,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is before the arrival date.")
    }

    @Test
    fun `should return validation error after arrival when new departure date is before actual arrival date`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 6, 15)
      existingSpaceBooking.actualArrivalDate = LocalDate.of(2025, 6, 20)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 6, 25)

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateSpaceBooking(
        UpdateSpaceBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = null,
          departureDate = LocalDate.of(2025, 6, 18),
          updatedBy = user,
          characteristics = null,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is before the arrival date.")
    }

    @Test
    fun `should update only departure dates when booking status is hasArrival`() {
      existingSpaceBooking.actualArrivalDate = LocalDate.of(2025, 3, 5)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 1, 10)
      val updateSpaceBookingDetails = UpdateSpaceBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = newArrivalDate,
        departureDate = LocalDate.of(2025, 4, 26),
        updatedBy = user,
        characteristics = null,
      )

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any(), any(), any(), any(), any(), any()) } just Runs
      every { cas1BookingEmailService.spaceBookingAmended(any(), any()) } returns Unit

      val result = service.updateSpaceBooking(updateSpaceBookingDetails)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.expectedArrivalDate).isEqualTo(existingSpaceBooking.expectedArrivalDate)
      assertThat(updatedSpaceBooking.canonicalArrivalDate).isEqualTo(existingSpaceBooking.canonicalArrivalDate)
      assertThat(updatedSpaceBooking.expectedDepartureDate).isEqualTo(updateSpaceBookingDetails.departureDate)
      assertThat(updatedSpaceBooking.canonicalDepartureDate).isEqualTo(updateSpaceBookingDetails.departureDate)

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          booking = updatedSpaceBookingCaptor.captured,
          changedBy = user,
          bookingChangedAt = any(),
          previousArrivalDateIfChanged = null,
          previousDepartureDateIfChanged = LocalDate.of(2025, 1, 10),
          previousCharacteristicsIfChanged = null,
        )
      }

      verify(exactly = 1) {
        cas1BookingEmailService.spaceBookingAmended(
          spaceBooking = updatedSpaceBookingCaptor.captured,
          application = updatedSpaceBookingCaptor.captured.application!!,
        )
      }
    }

    @Test
    fun `should correctly update booking dates and characteristics`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 1, 10)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 3, 15)
      val originalRoomCharacteristic = CharacteristicEntityFactory().withModelScope("room").withPropertyName("IsArsenCapable").produce()
      existingSpaceBooking.criteria = mutableListOf(originalRoomCharacteristic)

      val updateSpaceBookingDetails = UpdateSpaceBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = newArrivalDate,
        departureDate = newDepartureDate,
        updatedBy = user,
        characteristics = listOf(
          CharacteristicEntityFactory()
            .withPropertyName("hasEnSuite")
            .withModelScope("room")
            .produce(),
        ),
      )

      existingSpaceBooking.actualArrivalDate = null

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any(), any(), any(), any(), any(), any()) } just Runs
      every { cas1BookingEmailService.spaceBookingAmended(any(), any()) } returns Unit

      val result = service.updateSpaceBooking(updateSpaceBookingDetails)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.expectedArrivalDate).isEqualTo(updateSpaceBookingDetails.arrivalDate)
      assertThat(updatedSpaceBooking.canonicalArrivalDate).isEqualTo(updateSpaceBookingDetails.arrivalDate)
      assertThat(updatedSpaceBooking.expectedDepartureDate).isEqualTo(updateSpaceBookingDetails.departureDate)
      assertThat(updatedSpaceBooking.canonicalDepartureDate).isEqualTo(updateSpaceBookingDetails.departureDate)

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          booking = updatedSpaceBookingCaptor.captured,
          changedBy = user,
          bookingChangedAt = any(),
          previousArrivalDateIfChanged = LocalDate.of(2025, 1, 10),
          previousDepartureDateIfChanged = LocalDate.of(2025, 3, 15),
          previousCharacteristicsIfChanged = listOf(originalRoomCharacteristic),
        )
      }

      verify(exactly = 1) {
        cas1BookingEmailService.spaceBookingAmended(
          spaceBooking = updatedSpaceBookingCaptor.captured,
          application = updatedSpaceBookingCaptor.captured.application!!,
        )
      }
    }

    @Test
    fun `should remove all room characteristics when no characteristics are provided`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 1, 10)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 3, 15)
      val originalRoomCharacteristic = CharacteristicEntityFactory().withModelScope("room").withPropertyName("IsArsenCapable").produce()
      existingSpaceBooking.criteria = mutableListOf(originalRoomCharacteristic)

      val updateSpaceBookingDetails = UpdateSpaceBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = newArrivalDate,
        departureDate = newDepartureDate,
        updatedBy = user,
        characteristics = emptyList(),
      )

      assertThat(existingSpaceBooking.criteria).isNotEmpty()

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any(), any(), any(), any(), any(), any()) } just Runs
      every { cas1BookingEmailService.spaceBookingAmended(any(), any()) } returns Unit

      val result = service.updateSpaceBooking(updateSpaceBookingDetails)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.criteria).isEmpty()

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          booking = updatedSpaceBookingCaptor.captured,
          changedBy = user,
          bookingChangedAt = any(),
          previousArrivalDateIfChanged = LocalDate.of(2025, 1, 10),
          previousDepartureDateIfChanged = LocalDate.of(2025, 3, 15),
          previousCharacteristicsIfChanged = listOf(originalRoomCharacteristic),
        )
      }

      verify(exactly = 1) {
        cas1BookingEmailService.spaceBookingAmended(
          spaceBooking = updatedSpaceBookingCaptor.captured,
          application = updatedSpaceBookingCaptor.captured.application!!,
        )
      }
    }

    @Test
    fun `should not send booking amended email when only characteristics are changed`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 1, 10)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 3, 15)
      val originalRoomCharacteristic = CharacteristicEntityFactory().withModelScope("room").withPropertyName("IsArsenCapable").produce()
      existingSpaceBooking.criteria = mutableListOf(originalRoomCharacteristic)

      val updateSpaceBookingDetails = UpdateSpaceBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = null,
        departureDate = null,
        updatedBy = user,
        characteristics = emptyList(),
      )

      assertThat(existingSpaceBooking.criteria).isNotEmpty()

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any(), any(), any(), any(), any(), any()) } just Runs

      val result = service.updateSpaceBooking(updateSpaceBookingDetails)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.criteria).isEmpty()

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          booking = updatedSpaceBookingCaptor.captured,
          changedBy = user,
          bookingChangedAt = any(),
          previousArrivalDateIfChanged = null,
          previousDepartureDateIfChanged = null,
          previousCharacteristicsIfChanged = listOf(originalRoomCharacteristic),
        )
      }

      verify { cas1BookingEmailService wasNot Called }
    }

    @Test
    fun `should not send booking amended email when application is missing`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withPremises(premises)
        .withApplication(null)
        .produce()

      val originalRoomCharacteristic = CharacteristicEntityFactory().withModelScope("room").withPropertyName("IsArsenCapable").produce()
      existingSpaceBooking.criteria = mutableListOf(originalRoomCharacteristic)

      val updateSpaceBookingDetails = UpdateSpaceBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = null,
        departureDate = null,
        updatedBy = user,
        characteristics = emptyList(),
      )

      assertThat(existingSpaceBooking.criteria).isNotEmpty()

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any(), any(), any(), any(), any(), any()) } just Runs

      val result = service.updateSpaceBooking(updateSpaceBookingDetails)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.criteria).isEmpty()

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          booking = updatedSpaceBookingCaptor.captured,
          changedBy = user,
          bookingChangedAt = any(),
          previousArrivalDateIfChanged = null,
          previousDepartureDateIfChanged = null,
          previousCharacteristicsIfChanged = listOf(originalRoomCharacteristic),
        )
      }

      verify { cas1BookingEmailService wasNot Called }
    }
  }

  @Nested
  inner class EmergencyTransfer {

    private val user = UserEntityFactory()
      .withDefaults()
      .produce()

    private val currentPremises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withId(PREMISES_ID)
      .produce()

    private val destinationPremises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withId(DESTINATION_PREMISES_ID)
      .produce()

    private var existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .withPremises(currentPremises)
      .produce()

    @Test
    fun `should throw validation error when destination premises not exist`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val destinationPremisesId = UUID.randomUUID()

      val result = service.emergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = destinationPremisesId,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThatCasResult(result)
        .isNotFound("Premises", destinationPremisesId)
    }

    @Test
    fun `should return validation error if arrivalDate is not today's date`() {
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.emergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now().minusDays(1),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThatCasResult(result)
        .isGeneralValidationError("The provided arrival date must be today's date")
    }

    @Test
    fun `should return validation error if departureDate is not after arrival date`() {
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.emergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().minusMonths(2),
        ),
      )

      assertThatCasResult(result)
        .isGeneralValidationError("The provided departure date must be after the arrival date")
    }

    @Test
    fun `should return validation error if booking does not exist`() {
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns null

      val bookingId = UUID.randomUUID()

      val result = service.emergencyTransfer(
        PREMISES_ID,
        bookingId,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThatCasResult(result)
        .isNotFound("Space Booking", bookingId)
    }

    @Test
    fun `Should return a conflict error when attempting an emergency transfer for a booking that does not belong to the given premises`() {
      val anotherPremisesId = UUID.randomUUID()
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.emergencyTransfer(
        anotherPremisesId,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.conflictingEntityId).isEqualTo(existingSpaceBooking.premises.id)
      assertThat(result.message).contains("The booking is not associated with the specified premises $anotherPremisesId")
    }

    @Test
    fun `should return a conflict error when attempting an emergency transfer for a person who has already departed the premises`() {
      existingSpaceBooking.actualDepartureDate = LocalDate.now()
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.emergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.conflictingEntityId).isEqualTo(existingSpaceBooking.id)
      assertThat(result.message).contains("The booking is not eligible for an emergency transfer")
    }

    @Test
    fun `should return a conflict error when attempting an emergency transfer for a booking that has already transferred`() {
      existingSpaceBooking.transferredTo = Cas1SpaceBookingEntityFactory().produce()
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.emergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.conflictingEntityId).isEqualTo(existingSpaceBooking.id)
      assertThat(result.message).contains("The booking is not eligible for an emergency transfer")
    }

    @Test
    fun `should return a conflict error when attempting an emergency transfer for a booking that has already been canceled`() {
      existingSpaceBooking.cancellationOccurredAt = LocalDate.now()
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.emergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.conflictingEntityId).isEqualTo(existingSpaceBooking.id)
      assertThat(result.message).contains("The booking is not eligible for an emergency transfer")
    }

    @Test
    fun `should return a conflict error when attempting an emergency transfer for a booking that has a confirmed non-arrival`() {
      existingSpaceBooking.nonArrivalConfirmedAt = Instant.now()
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.emergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.conflictingEntityId).isEqualTo(existingSpaceBooking.id)
      assertThat(result.message).contains("The booking is not eligible for an emergency transfer")
    }

    @Test
    fun `should successfully create an emergency booking and update the existing booking`() {
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val capturedBookings = mutableListOf<Cas1SpaceBookingEntity>()

      every { spaceBookingRepository.saveAndFlush(capture(capturedBookings)) } answers { firstArg() }
      every { cas1SpaceBookingManagementDomainEventService.emergencyTransferCreated(any(), any(), any()) } returns Unit

      assertThat(existingSpaceBooking.transferredTo).isNull()

      val result = service.emergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)

      verify(exactly = 2) { spaceBookingRepository.saveAndFlush(any()) }

      assertEquals(2, capturedBookings.size)

      val emergencyBooking = capturedBookings.first()
      existingSpaceBooking = capturedBookings.last()

      assertThat(existingSpaceBooking.transferredTo).isEqualTo(emergencyBooking)
      assertThat(existingSpaceBooking.expectedDepartureDate).isEqualTo(emergencyBooking.expectedArrivalDate)
      assertThat(existingSpaceBooking.canonicalDepartureDate).isEqualTo(emergencyBooking.expectedArrivalDate)

      assertThat(emergencyBooking.premises.id).isEqualTo(DESTINATION_PREMISES_ID)
      assertThat(emergencyBooking.expectedArrivalDate).isEqualTo(LocalDate.now())
      assertThat(emergencyBooking.expectedDepartureDate).isEqualTo(LocalDate.now().plusMonths(2))
      assertThat(emergencyBooking.transferType).isEqualTo(TransferType.EMERGENCY)

      verify {
        cas1SpaceBookingManagementDomainEventService.emergencyTransferCreated(
          createdBy = user,
          from = existingSpaceBooking,
          to = emergencyBooking,
        )
      }
    }
  }

  @Nested
  inner class PlannedTransfer {

    private val user = UserEntityFactory()
      .withDefaults()
      .produce()

    private val currentPremises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withId(PREMISES_ID)
      .produce()

    private val destinationPremises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withId(DESTINATION_PREMISES_ID)
      .produce()

    private var existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .withPremises(currentPremises)
      .withActualArrivalDate(LocalDate.of(2025, 1, 10))
      .produce()

    private var existingChangeRequest = Cas1ChangeRequestEntityFactory()
      .withSpaceBooking(existingSpaceBooking)
      .produce()

    @Test
    fun `should throw validation error when destination premises not exist`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val destinationPremisesId = UUID.randomUUID()

      val result = service.plannedTransfer(
        existingSpaceBooking.id,
        user,
        Cas1NewPlannedTransfer(
          destinationPremisesId = destinationPremisesId,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
          changeRequestId = UUID.randomUUID(),
          characteristics = emptyList(),
        ),
      )

      assertThatCasResult(result)
        .isNotFound("Premises", destinationPremisesId)
    }

    @Test
    fun `should return validation error if arrivalDate is not in the future`() {
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest

      val result = service.plannedTransfer(
        existingSpaceBooking.id,
        user,
        Cas1NewPlannedTransfer(
          destinationPremisesId = destinationPremises.id,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
          changeRequestId = UUID.randomUUID(),
          characteristics = emptyList(),
        ),
      )

      assertThatCasResult(result)
        .isGeneralValidationError("The provided arrival date (${LocalDate.now()}) must be in the future")
    }

    @Test
    fun `should return validation error if departureDate is not after arrival date`() {
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest

      val result = service.plannedTransfer(
        existingSpaceBooking.id,
        user,
        Cas1NewPlannedTransfer(
          destinationPremisesId = destinationPremises.id,
          arrivalDate = LocalDate.now().plusWeeks(2),
          departureDate = LocalDate.now().plusWeeks(1),
          changeRequestId = UUID.randomUUID(),
          characteristics = emptyList(),
        ),
      )

      assertThatCasResult(result)
        .isGeneralValidationError("The provided departure date (${LocalDate.now().plusWeeks(1)}) must be after the arrival date (${LocalDate.now().plusWeeks(2)})")
    }

    @Test
    fun `should return validation error if booking does not exist`() {
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns null

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest

      val result = service.plannedTransfer(
        existingSpaceBooking.id,
        user,
        Cas1NewPlannedTransfer(
          destinationPremisesId = destinationPremises.id,
          arrivalDate = LocalDate.now().plusDays(2),
          departureDate = LocalDate.now().plusWeeks(1),
          changeRequestId = UUID.randomUUID(),
          characteristics = emptyList(),
        ),
      )

      assertThatCasResult(result)
        .isNotFound("Space Booking", existingSpaceBooking.id)
    }

    @Test
    fun `Should return validation error when given change request does not belongs to the booking`() {
      val anotherChangeRequest = Cas1ChangeRequestEntityFactory().produce()
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns anotherChangeRequest

      every { characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises) } returns emptyList()

      every { spaceBookingRepository.saveAndFlush(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.approvedPlannedTransfer(any(), any(), any(), any()) } returns Unit

      val bookingId = UUID.randomUUID()

      val result = service.plannedTransfer(
        bookingId,
        user,
        Cas1NewPlannedTransfer(
          destinationPremisesId = destinationPremises.id,
          arrivalDate = LocalDate.now().plusDays(2),
          departureDate = LocalDate.now().plusWeeks(1),
          changeRequestId = existingChangeRequest.id,
          characteristics = emptyList(),
        ),
      )

      assertThatCasResult(result)
        .isGeneralValidationError("The booking is not associated with the specified change request ${anotherChangeRequest.id}")
    }

    @Test
    fun `Should return validation error when given change request marked as resolved`() {
      existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withPremises(currentPremises)
        .withActualArrivalDate(LocalDate.now())
        .produce()

      existingChangeRequest = Cas1ChangeRequestEntityFactory()
        .withSpaceBooking(existingSpaceBooking)
        .withResolved(true)
        .produce()

      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest

      every { characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises) } returns emptyList()

      every { spaceBookingRepository.saveAndFlush(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.approvedPlannedTransfer(any(), any(), any(), any()) } returns Unit

      val result = service.plannedTransfer(
        existingSpaceBooking.id,
        user,
        Cas1NewPlannedTransfer(
          destinationPremisesId = destinationPremises.id,
          arrivalDate = LocalDate.now().plusDays(2),
          departureDate = LocalDate.now().plusWeeks(1),
          changeRequestId = existingChangeRequest.id,
          characteristics = emptyList(),
        ),
      )

      assertThatCasResult(result)
        .isGeneralValidationError("A decision has already been made for the change request")
    }

    @Test
    fun `Should return validation error when given space booking has no recorded arrival date`() {
      existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withPremises(currentPremises)
        .withActualArrivalDate(null)
        .produce()

      existingChangeRequest = Cas1ChangeRequestEntityFactory()
        .withSpaceBooking(existingSpaceBooking)
        .produce()

      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest

      every { characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises) } returns emptyList()

      every { spaceBookingRepository.saveAndFlush(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.approvedPlannedTransfer(any(), any(), any(), any()) } returns Unit

      val result = service.plannedTransfer(
        existingSpaceBooking.id,
        user,
        Cas1NewPlannedTransfer(
          destinationPremisesId = destinationPremises.id,
          arrivalDate = LocalDate.now().plusDays(2),
          departureDate = LocalDate.now().plusWeeks(1),
          changeRequestId = existingChangeRequest.id,
          characteristics = emptyList(),
        ),
      )

      assertThatCasResult(result)
        .isGeneralValidationError("Arrival must be recorded for the associated space booking")
    }

    @Test
    fun `should successfully create an transferred booking and update the existing booking`() {
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest

      every { characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises) } returns emptyList()

      val capturedBookings = mutableListOf<Cas1SpaceBookingEntity>()

      every { spaceBookingRepository.saveAndFlush(capture(capturedBookings)) } answers { firstArg() }

      every { cas1ChangeRequestService.approvedPlannedTransfer(any(), any(), any(), any()) } returns Unit

      assertThat(existingSpaceBooking.transferredTo).isNull()

      val result = service.plannedTransfer(
        existingSpaceBooking.id,
        user,
        Cas1NewPlannedTransfer(
          destinationPremisesId = destinationPremises.id,
          arrivalDate = LocalDate.now().plusDays(2),
          departureDate = LocalDate.now().plusMonths(1),
          changeRequestId = existingChangeRequest.id,
          characteristics = emptyList(),
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)

      verify(exactly = 2) { spaceBookingRepository.saveAndFlush(any()) }

      assertEquals(2, capturedBookings.size)

      val transferredBooking = capturedBookings.first()
      existingSpaceBooking = capturedBookings.last()

      assertThat(existingSpaceBooking.transferredTo).isEqualTo(transferredBooking)
      assertThat(existingSpaceBooking.expectedDepartureDate).isEqualTo(transferredBooking.expectedArrivalDate)
      assertThat(existingSpaceBooking.canonicalDepartureDate).isEqualTo(transferredBooking.expectedArrivalDate)

      assertThat(transferredBooking.premises.id).isEqualTo(DESTINATION_PREMISES_ID)
      assertThat(transferredBooking.expectedArrivalDate).isEqualTo(LocalDate.now().plusDays(2))
      assertThat(transferredBooking.expectedDepartureDate).isEqualTo(LocalDate.now().plusMonths(1))
      assertThat(transferredBooking.transferType).isEqualTo(TransferType.PLANNED)

      verify {
        cas1ChangeRequestService.approvedPlannedTransfer(
          changeRequest = existingChangeRequest,
          user = user,
          from = existingSpaceBooking,
          to = transferredBooking,
        )
      }
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
