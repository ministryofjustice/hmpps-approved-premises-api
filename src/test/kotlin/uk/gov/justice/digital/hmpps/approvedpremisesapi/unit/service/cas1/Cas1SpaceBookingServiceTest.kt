package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewEmergencyTransfer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewPlannedTransfer
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PageCriteriaFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks.ClockConfiguration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableCas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableCas1SpaceBookingEntityRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.ActionsResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingActionsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingManagementDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.ShortenBookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.UpdateBookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.UpdateType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingChangedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCreatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1SpaceBookingServiceTest {
  private val cas1PremisesService = mockk<Cas1PremisesService>()
  private val placementRequestService = mockk<PlacementRequestService>()
  private val spaceBookingRepository = mockk<Cas1SpaceBookingRepository>()
  private val cas1BookingDomainEventService = mockk<Cas1BookingDomainEventService>()
  private val cas1BookingEmailService = mockk<Cas1BookingEmailService>()
  private val cas1SpaceBookingManagementDomainEventService = mockk<Cas1SpaceBookingManagementDomainEventService>()
  private val cas1ApplicationStatusService = mockk<Cas1ApplicationStatusService>()
  private val cancellationReasonRepository = mockk<CancellationReasonRepository>()
  private val lockablePlacementRequestRepository = mockk<LockablePlacementRequestRepository>()
  private val lockableCas1SpaceBookingRepository = mockk<LockableCas1SpaceBookingEntityRepository>()
  private val cas1ChangeRequestService = mockk<Cas1ChangeRequestService>()
  private val characteristicService = mockk<CharacteristicService>()
  private val cas1SpaceBookingActionsService = mockk<Cas1SpaceBookingActionsService>()
  private val clock = ClockConfiguration.FixedClock()

  private val service = Cas1SpaceBookingService(
    cas1PremisesService,
    placementRequestService,
    spaceBookingRepository,
    cas1BookingDomainEventService,
    cas1BookingEmailService,
    cas1SpaceBookingManagementDomainEventService,
    cas1ApplicationStatusService,
    cancellationReasonRepository,
    lockablePlacementRequestRepository,
    lockableCas1SpaceBookingRepository,
    cas1ChangeRequestService,
    characteristicService,
    cas1SpaceBookingActionsService,
    clock,
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
      every { cas1BookingDomainEventService.spaceBookingMade(any()) } returns Unit
      every { cas1BookingEmailService.spaceBookingMade(any(), any()) } returns Unit

      val persistedBookingCaptor = slot<Cas1SpaceBookingEntity>()
      every { spaceBookingRepository.save(capture(persistedBookingCaptor)) } returnsArgument 0

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

      assertThatCasResult(result).isSuccess()

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
      verify { cas1BookingDomainEventService.spaceBookingMade(Cas1BookingCreatedEvent(persistedBooking, user)) }
      verify { cas1BookingEmailService.spaceBookingMade(persistedBooking, application) }
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

      every { cas1BookingDomainEventService.spaceBookingMade(any()) } returns Unit

      every { cas1BookingEmailService.spaceBookingMade(any(), any()) } returns Unit

      val persistedBookingCaptor = slot<Cas1SpaceBookingEntity>()
      every { spaceBookingRepository.save(capture(persistedBookingCaptor)) } returnsArgument 0

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

      assertThatCasResult(result).isSuccess()

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
  inner class GetBookingForPremisesAndId {

    @Test
    fun `Returns not found error if premises with the given ID doesn't exist`() {
      every { cas1PremisesService.premiseExistsById(any()) } returns false

      val result = service.getBookingForPremisesAndId(UUID.randomUUID(), UUID.randomUUID())

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

      val result = service.getBookingForPremisesAndId(premises.id, UUID.randomUUID())

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

      val result = service.getBookingForPremisesAndId(premises.id, spaceBooking.id)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      assertThat((result as CasResult.Success).value).isEqualTo(spaceBooking)
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
      every { cas1BookingDomainEventService.spaceBookingCancelled(any()) } returns Unit
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
      verify { cas1BookingDomainEventService.spaceBookingCancelled(Cas1BookingCancelledEvent(spaceBooking, user, reason)) }
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

      val result = service.updateBooking(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
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

      val result = service.updateBooking(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bookingId", "doesNotExist")
    }

    @Test
    fun `should return validation error when booking status is canceled`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(LocalDate.now().minusWeeks(2))
        .withPremises(premises)
        .produce()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateBooking(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bookingId", "This Booking is cancelled and as such cannot be modified")
    }

    @Test
    fun `should return validation error when booking status is departed`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withActualDepartureDate(LocalDate.now().minusWeeks(2))
        .withPremises(premises)
        .produce()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateBooking(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bookingId", "hasDepartedOrNonArrival")
    }

    @Test
    fun `should return validation error when booking status is nonArrival`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withNonArrivalConfirmedAt(Instant.now())
        .withPremises(premises)
        .produce()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateBooking(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bookingId", "hasDepartedOrNonArrival")
    }

    @Test
    fun `should return validation error when premisesId does not match the existing booking`() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateBooking(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = newArrivalDate,
          departureDate = newDepartureDate,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.premisesId", "premisesMismatch")
    }

    @Test
    fun `should return validation error before arrival when new departure date is before updated arrival date`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 6, 5)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 6, 15)

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.updateBooking(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = LocalDate.of(2025, 6, 17),
          departureDate = LocalDate.of(2025, 6, 16),
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
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

      val result = service.updateBooking(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = null,
          departureDate = LocalDate.of(2025, 6, 4),
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
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

      val result = service.updateBooking(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = LocalDate.of(2025, 6, 16),
          departureDate = null,
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
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

      val result = service.updateBooking(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          arrivalDate = null,
          departureDate = LocalDate.of(2025, 6, 18),
          updatedBy = user,
          characteristics = null,
          updateType = UpdateType.AMENDMENT,
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
      val updateBookingDetails = UpdateBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = newArrivalDate,
        departureDate = LocalDate.of(2025, 4, 26),
        updatedBy = user,
        characteristics = null,
        updateType = UpdateType.AMENDMENT,
      )

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } just Runs
      every { cas1BookingEmailService.spaceBookingAmended(any(), any(), any()) } returns Unit

      val result = service.updateBooking(updateBookingDetails)

      assertThatCasResult(result).isSuccess()

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.expectedArrivalDate).isEqualTo(existingSpaceBooking.expectedArrivalDate)
      assertThat(updatedSpaceBooking.canonicalArrivalDate).isEqualTo(existingSpaceBooking.canonicalArrivalDate)
      assertThat(updatedSpaceBooking.expectedDepartureDate).isEqualTo(updateBookingDetails.departureDate)
      assertThat(updatedSpaceBooking.canonicalDepartureDate).isEqualTo(updateBookingDetails.departureDate)

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          Cas1BookingChangedEvent(
            booking = updatedSpaceBookingCaptor.captured,
            changedBy = user,
            bookingChangedAt = OffsetDateTime.now(clock),
            previousArrivalDateIfChanged = null,
            previousDepartureDateIfChanged = LocalDate.of(2025, 1, 10),
            previousCharacteristicsIfChanged = null,
          ),
        )
      }

      verify(exactly = 1) {
        cas1BookingEmailService.spaceBookingAmended(
          spaceBooking = updatedSpaceBookingCaptor.captured,
          application = updatedSpaceBookingCaptor.captured.application!!,
          updateType = UpdateType.AMENDMENT,
        )
      }
    }

    @Test
    fun `should correctly update booking dates and characteristics`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 1, 10)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 3, 15)
      val originalRoomCharacteristic = CharacteristicEntityFactory().withModelScope("room").withPropertyName("IsArsenCapable").produce()
      existingSpaceBooking.criteria = mutableListOf(originalRoomCharacteristic)

      val updateBookingDetails = UpdateBookingDetails(
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
        updateType = UpdateType.AMENDMENT,
      )

      existingSpaceBooking.actualArrivalDate = null

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } just Runs
      every { cas1BookingEmailService.spaceBookingAmended(any(), any(), any()) } returns Unit

      val result = service.updateBooking(updateBookingDetails)

      assertThatCasResult(result).isSuccess()

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.expectedArrivalDate).isEqualTo(updateBookingDetails.arrivalDate)
      assertThat(updatedSpaceBooking.canonicalArrivalDate).isEqualTo(updateBookingDetails.arrivalDate)
      assertThat(updatedSpaceBooking.expectedDepartureDate).isEqualTo(updateBookingDetails.departureDate)
      assertThat(updatedSpaceBooking.canonicalDepartureDate).isEqualTo(updateBookingDetails.departureDate)

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          Cas1BookingChangedEvent(
            booking = updatedSpaceBookingCaptor.captured,
            changedBy = user,
            bookingChangedAt = OffsetDateTime.now(clock),
            previousArrivalDateIfChanged = LocalDate.of(2025, 1, 10),
            previousDepartureDateIfChanged = LocalDate.of(2025, 3, 15),
            previousCharacteristicsIfChanged = listOf(originalRoomCharacteristic),
          ),
        )
      }

      verify(exactly = 1) {
        cas1BookingEmailService.spaceBookingAmended(
          spaceBooking = updatedSpaceBookingCaptor.captured,
          application = updatedSpaceBookingCaptor.captured.application!!,
          updateType = UpdateType.AMENDMENT,
        )
      }
    }

    @Test
    fun `should remove all room characteristics when no characteristics are provided`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 1, 10)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 3, 15)
      val originalRoomCharacteristic = CharacteristicEntityFactory().withModelScope("room").withPropertyName("IsArsenCapable").produce()
      existingSpaceBooking.criteria = mutableListOf(originalRoomCharacteristic)

      val updateBookingDetails = UpdateBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = newArrivalDate,
        departureDate = newDepartureDate,
        updatedBy = user,
        characteristics = emptyList(),
        updateType = UpdateType.AMENDMENT,
      )

      assertThat(existingSpaceBooking.criteria).isNotEmpty()

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } just Runs
      every { cas1BookingEmailService.spaceBookingAmended(any(), any(), any()) } returns Unit

      val result = service.updateBooking(updateBookingDetails)

      assertThatCasResult(result).isSuccess()

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.criteria).isEmpty()

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          Cas1BookingChangedEvent(
            booking = updatedSpaceBookingCaptor.captured,
            changedBy = user,
            bookingChangedAt = OffsetDateTime.now(clock),
            previousArrivalDateIfChanged = LocalDate.of(2025, 1, 10),
            previousDepartureDateIfChanged = LocalDate.of(2025, 3, 15),
            previousCharacteristicsIfChanged = listOf(originalRoomCharacteristic),
          ),
        )
      }

      verify(exactly = 1) {
        cas1BookingEmailService.spaceBookingAmended(
          spaceBooking = updatedSpaceBookingCaptor.captured,
          application = updatedSpaceBookingCaptor.captured.application!!,
          updateType = UpdateType.AMENDMENT,
        )
      }
    }

    @Test
    fun `should not send booking amended email when only characteristics are changed`() {
      existingSpaceBooking.expectedArrivalDate = LocalDate.of(2025, 1, 10)
      existingSpaceBooking.expectedDepartureDate = LocalDate.of(2025, 3, 15)
      val originalRoomCharacteristic = CharacteristicEntityFactory().withModelScope("room").withPropertyName("IsArsenCapable").produce()
      existingSpaceBooking.criteria = mutableListOf(originalRoomCharacteristic)

      val updateBookingDetails = UpdateBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = null,
        departureDate = null,
        updatedBy = user,
        characteristics = emptyList(),
        updateType = UpdateType.AMENDMENT,
      )

      assertThat(existingSpaceBooking.criteria).isNotEmpty()

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } just Runs

      val result = service.updateBooking(updateBookingDetails)

      assertThatCasResult(result).isSuccess()

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.criteria).isEmpty()

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          Cas1BookingChangedEvent(
            booking = updatedSpaceBookingCaptor.captured,
            changedBy = user,
            bookingChangedAt = OffsetDateTime.now(clock),
            previousArrivalDateIfChanged = null,
            previousDepartureDateIfChanged = null,
            previousCharacteristicsIfChanged = listOf(originalRoomCharacteristic),
          ),
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

      val updateBookingDetails = UpdateBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        arrivalDate = null,
        departureDate = null,
        updatedBy = user,
        characteristics = emptyList(),
        updateType = UpdateType.AMENDMENT,
      )

      assertThat(existingSpaceBooking.criteria).isNotEmpty()

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } just Runs

      val result = service.updateBooking(updateBookingDetails)

      assertThatCasResult(result).isSuccess()

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.criteria).isEmpty()

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          Cas1BookingChangedEvent(
            booking = updatedSpaceBookingCaptor.captured,
            changedBy = user,
            bookingChangedAt = OffsetDateTime.now(clock),
            previousArrivalDateIfChanged = null,
            previousDepartureDateIfChanged = null,
            previousCharacteristicsIfChanged = listOf(originalRoomCharacteristic),
          ),
        )
      }

      verify { cas1BookingEmailService wasNot Called }
    }
  }

  @Nested
  inner class ShortenSpaceBooking {

    private val originalDepartureDate = LocalDate.now().plusDays(7)
    private val newDepartureDate = LocalDate.now().plusDays(1)

    private val user = UserEntityFactory()
      .withDefaults()
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withId(PREMISES_ID)
      .produce()

    private val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .withPremises(premises)
      .withExpectedArrivalDate(LocalDate.now().minusDays(7))
      .withActualArrivalDate(LocalDate.now().minusDays(7))
      .withExpectedDepartureDate(originalDepartureDate)
      .produce()

    @Test
    fun `should return validation error if no premises exist with the given premisesId`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.shortenBooking(
        ShortenBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          departureDate = newDepartureDate,
          reason = "valid reason",
          updatedBy = user,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.premisesId", "doesNotExist")
    }

    @Test
    fun `should return validation error if no space booking exist with the given bookingId`() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns null

      val result = service.shortenBooking(
        ShortenBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          departureDate = newDepartureDate,
          reason = "valid reason",
          updatedBy = user,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bookingId", "doesNotExist")
    }

    @Test
    fun `should return validation error when booking status is cancelled`() {
      existingSpaceBooking.cancellationOccurredAt = LocalDate.now().minusWeeks(2)

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.shortenBooking(
        ShortenBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          departureDate = newDepartureDate,
          updatedBy = user,
          reason = "valid reason",
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bookingId", "This Booking is cancelled and as such cannot be modified")
    }

    @Test
    fun `should return validation error when booking status is departed`() {
      existingSpaceBooking.actualDepartureDate = LocalDate.now().minusWeeks(2)

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.shortenBooking(
        ShortenBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          departureDate = newDepartureDate,
          reason = "valid reason",
          updatedBy = user,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bookingId", "hasDepartedOrNonArrival")
    }

    @Test
    fun `should return validation error when booking status is nonArrival`() {
      existingSpaceBooking.nonArrivalConfirmedAt = Instant.now()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.shortenBooking(
        ShortenBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = PREMISES_ID,
          departureDate = newDepartureDate,
          updatedBy = user,
          reason = "valid reason",
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bookingId", "hasDepartedOrNonArrival")
    }

    @Test
    fun `should return validation error when premisesId does not match the existing booking`() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.shortenBooking(
        ShortenBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          departureDate = newDepartureDate,
          reason = "valid reason",
          updatedBy = user,
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.premisesId", "premisesMismatch")
    }

    @Test
    fun `should return validation error when new departure date is before today`() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.shortenBooking(
        ShortenBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          departureDate = LocalDate.now().minusDays(1),
          reason = "valid reason",
          updatedBy = user,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is in the past.")
    }

    @Test
    fun `should return validation error if new departure date is after expected departure date`() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.shortenBooking(
        ShortenBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          departureDate = LocalDate.now().plusDays(8),
          reason = "valid reason",
          updatedBy = user,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is after the current expected departure date.")
    }

    @Test
    fun `should return validation error if new departure date is equal to expected departure date`() {
      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.shortenBooking(
        ShortenBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          departureDate = LocalDate.now().plusDays(7),
          reason = "valid reason",
          updatedBy = user,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is the same as the current expected departure date.")
    }

    @Test
    fun `should update departure date when status is hasArrival and send emails`() {
      val shortenBookingDetails = ShortenBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        departureDate = LocalDate.now().plusDays(1),
        reason = "valid reason",
        updatedBy = user,
      )

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } returns Unit
      every { cas1BookingEmailService.spaceBookingAmended(any(), any(), any()) } returns Unit

      val result = service.shortenBooking(shortenBookingDetails)

      assertThatCasResult(result).isSuccess()

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.expectedArrivalDate).isEqualTo(existingSpaceBooking.expectedArrivalDate)
      assertThat(updatedSpaceBooking.canonicalArrivalDate).isEqualTo(existingSpaceBooking.canonicalArrivalDate)
      assertThat(updatedSpaceBooking.expectedDepartureDate).isEqualTo(shortenBookingDetails.departureDate)
      assertThat(updatedSpaceBooking.canonicalDepartureDate).isEqualTo(shortenBookingDetails.departureDate)

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          Cas1BookingChangedEvent(
            booking = updatedSpaceBookingCaptor.captured,
            changedBy = user,
            bookingChangedAt = OffsetDateTime.now(clock),
            previousArrivalDateIfChanged = null,
            previousDepartureDateIfChanged = originalDepartureDate,
            previousCharacteristicsIfChanged = null,
          ),
        )
      }

      verify(exactly = 1) {
        cas1BookingEmailService.spaceBookingAmended(
          spaceBooking = updatedSpaceBookingCaptor.captured,
          application = updatedSpaceBookingCaptor.captured.application!!,
          updateType = UpdateType.SHORTENING,
        )
      }
    }

    @Test
    fun `should update departure date when status is hasArrival and new departure date and actual arrival date are today`() {
      existingSpaceBooking.actualArrivalDate = LocalDate.now()

      val shortenBookingDetails = ShortenBookingDetails(
        bookingId = UUID.randomUUID(),
        premisesId = PREMISES_ID,
        departureDate = LocalDate.now(),
        reason = "valid reason",
        updatedBy = user,
      )

      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremiseById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } returns Unit
      every { cas1BookingEmailService.spaceBookingAmended(any(), any(), any()) } returns Unit

      val result = service.shortenBooking(shortenBookingDetails)

      assertThatCasResult(result).isSuccess()

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(updatedSpaceBooking.expectedArrivalDate).isEqualTo(existingSpaceBooking.expectedArrivalDate)
      assertThat(updatedSpaceBooking.canonicalArrivalDate).isEqualTo(existingSpaceBooking.canonicalArrivalDate)
      assertThat(updatedSpaceBooking.expectedDepartureDate).isEqualTo(shortenBookingDetails.departureDate)
      assertThat(updatedSpaceBooking.canonicalDepartureDate).isEqualTo(shortenBookingDetails.departureDate)

      verify(exactly = 1) {
        cas1BookingDomainEventService.spaceBookingChanged(
          Cas1BookingChangedEvent(
            booking = updatedSpaceBookingCaptor.captured,
            changedBy = user,
            bookingChangedAt = OffsetDateTime.now(clock),
            previousArrivalDateIfChanged = null,
            previousDepartureDateIfChanged = originalDepartureDate,
            previousCharacteristicsIfChanged = null,
          ),
        )
      }

      verify(exactly = 1) {
        cas1BookingEmailService.spaceBookingAmended(
          spaceBooking = updatedSpaceBookingCaptor.captured,
          application = updatedSpaceBookingCaptor.captured.application!!,
          updateType = UpdateType.SHORTENING,
        )
      }
    }
  }

  @Nested
  inner class CreateEmergencyTransfer {

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
      .withSupportsSpaceBookings(true)
      .produce()

    private var existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .withActualArrivalDate(LocalDate.now().minusWeeks(4))
      .withPremises(currentPremises)
      .produce()

    @BeforeEach
    fun commonMocks() {
      every { cas1PremisesService.findPremiseById(DESTINATION_PREMISES_ID) } returns destinationPremises
      every { cas1PremisesService.findPremiseById(PREMISES_ID) } returns currentPremises
    }

    @Test
    fun `should throw validation error when destination premises not exist`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val destinationPremisesId = UUID.randomUUID()

      val result = service.createEmergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = destinationPremisesId,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.premisesId", "doesNotExist")
    }

    @Test
    fun `should return validation error if arrivalDate is in the future`() {
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.createEmergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now().plusDays(1),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThatCasResult(result)
        .isGeneralValidationError("The provided arrival date must be today, or within the last 7 days")
    }

    @Test
    fun `should return validation error if arrivalDate is more than 7 days ago`() {
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.createEmergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now().minusDays(8),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThatCasResult(result).isGeneralValidationError("The provided arrival date must be today, or within the last 7 days")
    }

    @Test
    fun `should return validation error if departureDate is not after arrival date`() {
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every {
        cas1SpaceBookingActionsService.determineActions(existingSpaceBooking)
      } returns ActionsResult.forAllowedAction(SpaceBookingAction.PLANNED_TRANSFER_REQUEST)

      every { placementRequestService.getPlacementRequestOrNull(any()) } returns existingSpaceBooking.placementRequest

      val result = service.createEmergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().minusMonths(2),
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("\$.departureDate", "shouldBeAfterArrivalDate")
    }

    @Test
    fun `should return validation error if booking does not exist`() {
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns null

      val bookingId = UUID.randomUUID()

      val result = service.createEmergencyTransfer(
        PREMISES_ID,
        bookingId,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bookingId", "doesNotExist")
    }

    @Test
    fun `Should return a validation error when attempting an emergency transfer for a booking that does not belong to the given premises`() {
      val anotherPremisesId = UUID.randomUUID()
      every { cas1PremisesService.findPremiseById(anotherPremisesId) } returns ApprovedPremisesEntityFactory().withDefaults().produce()
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.createEmergencyTransfer(
        anotherPremisesId,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("\$.premisesId", "premisesMismatch")
    }

    @Test
    fun `should return a general validation error when action not allowed for transfer`() {
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every {
        cas1SpaceBookingActionsService.determineActions(existingSpaceBooking)
      } returns ActionsResult.forUnavailableAction(SpaceBookingAction.PLANNED_TRANSFER_REQUEST, "nope")

      val result = service.createEmergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusMonths(2),
        ),
      )

      assertThatCasResult(result).isGeneralValidationError("nope")
    }

    @ParameterizedTest
    @CsvSource("0", "1", "2", "3", "4", "5", "6", "7")
    fun `create an emergency booking and update the existing booking if arrival date within last 7 days`(daysAgo: Long) {
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val capturedBookings = mutableListOf<Cas1SpaceBookingEntity>()

      every {
        cas1SpaceBookingActionsService.determineActions(existingSpaceBooking)
      } returns ActionsResult.forAllowedAction(SpaceBookingAction.PLANNED_TRANSFER_REQUEST)

      every { placementRequestService.getPlacementRequestOrNull(any()) } returns existingSpaceBooking.placementRequest

      every { spaceBookingRepository.save(capture(capturedBookings)) } answers { firstArg() }
      every { cas1ApplicationStatusService.spaceBookingMade(any()) } returns Unit
      every { cas1SpaceBookingManagementDomainEventService.emergencyTransferCreated(any(), any(), any()) } returns Unit

      every { cas1ApplicationStatusService.spaceBookingMade(any()) } returns Unit
      every { cas1BookingDomainEventService.spaceBookingMade(any()) } returns Unit
      every { cas1BookingEmailService.spaceBookingMade(any(), any()) } returns Unit
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } returns Unit
      every { cas1BookingEmailService.spaceBookingAmended(any(), any(), any()) } returns Unit

      assertThat(existingSpaceBooking.transferredTo).isNull()

      val transferDate = LocalDate.now().minusDays(daysAgo)
      val departureDate = LocalDate.now().plusMonths(2)

      val result = service.createEmergencyTransfer(
        PREMISES_ID,
        existingSpaceBooking.id,
        user,
        Cas1NewEmergencyTransfer(
          destinationPremisesId = DESTINATION_PREMISES_ID,
          arrivalDate = transferDate,
          departureDate = departureDate,
        ),
      )

      assertThatCasResult(result).isSuccess()

      verify(exactly = 2) { spaceBookingRepository.save(any()) }

      assertEquals(2, capturedBookings.size)

      val emergencyBooking = capturedBookings.first()
      existingSpaceBooking = capturedBookings.last()

      assertThat(existingSpaceBooking.transferredTo).isEqualTo(emergencyBooking)
      assertThat(existingSpaceBooking.expectedDepartureDate).isEqualTo(transferDate)
      assertThat(existingSpaceBooking.canonicalDepartureDate).isEqualTo(transferDate)

      assertThat(emergencyBooking.premises.id).isEqualTo(DESTINATION_PREMISES_ID)
      assertThat(emergencyBooking.expectedArrivalDate).isEqualTo(transferDate)
      assertThat(emergencyBooking.expectedDepartureDate).isEqualTo(departureDate)
      assertThat(emergencyBooking.transferType).isEqualTo(TransferType.EMERGENCY)

      verify {
        cas1SpaceBookingManagementDomainEventService.emergencyTransferCreated(
          createdBy = user,
          from = existingSpaceBooking,
          to = emergencyBooking,
        )
      }

      val placementRequest = existingSpaceBooking.placementRequest!!
      val application = placementRequest.application
      verify { cas1ApplicationStatusService.spaceBookingMade(emergencyBooking) }
      verify { cas1BookingDomainEventService.spaceBookingMade(Cas1BookingCreatedEvent(emergencyBooking, user)) }
      verify { cas1BookingEmailService.spaceBookingMade(emergencyBooking, application) }
    }
  }

  @Nested
  inner class CreatePlannedTransfer {

    private val user = UserEntityFactory()
      .withDefaults()
      .produce()

    private val currentPremises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withId(PREMISES_ID)
      .produce()

    private val destinationPremises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withSupportsSpaceBookings(true)
      .withId(DESTINATION_PREMISES_ID)
      .produce()

    private var existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .withPremises(currentPremises)
      .withActualArrivalDate(LocalDate.of(2025, 1, 10))
      .produce()

    private var existingChangeRequest = Cas1ChangeRequestEntityFactory()
      .withSpaceBooking(existingSpaceBooking)
      .produce()

    @BeforeEach
    fun commonMocks() {
      every { placementRequestService.getPlacementRequestOrNull(any()) } returns existingSpaceBooking.placementRequest
    }

    @Test
    fun `should throw validation error when destination premises not exist`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest

      every { characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises) } returns emptyList()

      val destinationPremisesId = UUID.randomUUID()

      val result = service.createPlannedTransfer(
        existingSpaceBooking.id,
        user,
        Cas1NewPlannedTransfer(
          destinationPremisesId = destinationPremisesId,
          arrivalDate = LocalDate.now().plusDays(1),
          departureDate = LocalDate.now().plusMonths(2),
          changeRequestId = UUID.randomUUID(),
          characteristics = emptyList(),
        ),
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("\$.premisesId", "doesNotExist")
    }

    @Test
    fun `should return validation error if arrivalDate is not in the future`() {
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest

      val result = service.createPlannedTransfer(
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

      every { characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises) } returns emptyList()

      val result = service.createPlannedTransfer(
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

      assertThatCasResult(result).isFieldValidationError().hasMessage("\$.departureDate", "shouldBeAfterArrivalDate")
    }

    @Test
    fun `should return validation error if booking does not exist`() {
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns null

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest

      val result = service.createPlannedTransfer(
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

      every { cas1ChangeRequestService.approvedPlannedTransfer(any(), any()) } returns Unit

      val bookingId = UUID.randomUUID()

      val result = service.createPlannedTransfer(
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

      every { cas1ChangeRequestService.approvedPlannedTransfer(any(), any()) } returns Unit

      val result = service.createPlannedTransfer(
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
    fun `successfully create an transferred booking and update the existing booking`() {
      every { cas1PremisesService.findPremiseById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest

      every { characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises) } returns emptyList()

      every { cas1ApplicationStatusService.spaceBookingMade(any()) } returns Unit
      every { cas1BookingDomainEventService.spaceBookingMade(any()) } returns Unit
      every { cas1BookingEmailService.spaceBookingMade(any(), any()) } returns Unit
      every { cas1BookingDomainEventService.spaceBookingChanged(any()) } returns Unit
      every { cas1BookingEmailService.spaceBookingAmended(any(), any(), any()) } returns Unit

      val capturedBookings = mutableListOf<Cas1SpaceBookingEntity>()

      every { spaceBookingRepository.save(capture(capturedBookings)) } answers { firstArg() }

      every { cas1ChangeRequestService.approvedPlannedTransfer(any(), any()) } returns Unit

      assertThat(existingSpaceBooking.transferredTo).isNull()

      val result = service.createPlannedTransfer(
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

      verify(exactly = 2) { spaceBookingRepository.save(any()) }

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
        )
      }

      val placementRequest = existingSpaceBooking.placementRequest!!
      val application = placementRequest.application
      verify { cas1ApplicationStatusService.spaceBookingMade(transferredBooking) }
      verify { cas1BookingDomainEventService.spaceBookingMade(Cas1BookingCreatedEvent(transferredBooking, user)) }
      verify { cas1BookingEmailService.spaceBookingMade(transferredBooking, application) }
    }
  }
}
