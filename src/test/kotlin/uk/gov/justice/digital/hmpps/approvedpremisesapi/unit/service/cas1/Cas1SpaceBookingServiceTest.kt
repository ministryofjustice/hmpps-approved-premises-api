package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PageCriteriaFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestEntityFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.ActionsResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingActionsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingCreateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingCreateService.CreateBookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingCreateService.ValidatedCreateBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.UpdateType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingUpdateService.UpdateBookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.TransferInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1SpaceBookingServiceTest {
  private val cas1PremisesService = mockk<Cas1PremisesService>()
  private val spaceBookingRepository = mockk<Cas1SpaceBookingRepository>()
  private val cas1BookingDomainEventService = mockk<Cas1BookingDomainEventService>()
  private val cas1BookingEmailService = mockk<Cas1BookingEmailService>()
  private val cas1ApplicationStatusService = mockk<Cas1ApplicationStatusService>()
  private val cancellationReasonRepository = mockk<CancellationReasonRepository>()
  private val lockablePlacementRequestRepository = mockk<LockablePlacementRequestRepository>()
  private val lockableCas1SpaceBookingRepository = mockk<LockableCas1SpaceBookingEntityRepository>()
  private val cas1ChangeRequestService = mockk<Cas1ChangeRequestService>()
  private val characteristicService = mockk<CharacteristicService>()
  private val cas1SpaceBookingActionsService = mockk<Cas1SpaceBookingActionsService>()
  private val cas1SpaceBookingCreateService = mockk<Cas1SpaceBookingCreateService>()
  private val cas1SpaceBookingUpdateService = mockk<Cas1SpaceBookingUpdateService>()
  private val offenderService = mockk<OffenderService>()
  private val cas1SpaceBookingTransformer = mockk<Cas1SpaceBookingTransformer>()

  private val service = Cas1SpaceBookingService(
    cas1PremisesService,
    spaceBookingRepository,
    cas1BookingDomainEventService,
    cas1BookingEmailService,
    cas1ApplicationStatusService,
    cancellationReasonRepository,
    lockablePlacementRequestRepository,
    lockableCas1SpaceBookingRepository,
    cas1ChangeRequestService,
    characteristicService,
    cas1SpaceBookingActionsService,
    cas1SpaceBookingCreateService,
    cas1SpaceBookingUpdateService,
    offenderService,
    cas1SpaceBookingTransformer,
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
    fun `Returns error if raised by common validation`() {
      every { lockablePlacementRequestRepository.acquirePessimisticLock(placementRequest.id) } returns
        LockablePlacementRequestEntity(placementRequest.id)

      val commonValidationError = CasResult.GeneralValidationError<ValidatedCreateBooking>("oh dear")
      every { cas1SpaceBookingCreateService.validate(any()) } returns commonValidationError

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        arrivalDate = LocalDate.now(),
        departureDate = LocalDate.now().plusDays(1),
        createdBy = user,
        characteristics = emptyList(),
      )

      assertThat(result).isEqualTo(commonValidationError)
    }

    @Test
    fun `Returns conflict error if a space booking already exists for the same premises and placement request`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withPremises(premises)
        .withPlacementRequest(placementRequest)
        .produce()

      every { lockablePlacementRequestRepository.acquirePessimisticLock(placementRequest.id) } returns
        LockablePlacementRequestEntity(placementRequest.id)

      val validatedCreateBooking = mockk<ValidatedCreateBooking>()
      every { cas1SpaceBookingCreateService.validate(any()) } returns CasResult.Success(validatedCreateBooking)
      every { spaceBookingRepository.findByPlacementRequestId(placementRequest.id) } returns listOf(existingSpaceBooking)

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
    fun `Creates new booking if all data is valid`() {
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

      every { spaceBookingRepository.findByPlacementRequestId(placementRequest.id) } returns emptyList()
      every { lockablePlacementRequestRepository.acquirePessimisticLock(placementRequest.id) } returns
        LockablePlacementRequestEntity(placementRequest.id)

      val characteristics = listOf(
        CharacteristicEntityFactory().withName("c1").produce(),
        CharacteristicEntityFactory().withName("c2").produce(),
      )

      val details = CreateBookingDetails(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        expectedArrivalDate = LocalDate.now(),
        expectedDepartureDate = LocalDate.now().plusDays(1),
        createdBy = user,
        characteristics = characteristics,
        transferredFrom = null,
      )

      val validatedCreateBooking = mockk<ValidatedCreateBooking>()
      every { cas1SpaceBookingCreateService.validate(details) } returns CasResult.Success(validatedCreateBooking)
      val createdSpaceBooking = Cas1SpaceBookingEntityFactory().produce()
      every { cas1SpaceBookingCreateService.create(validatedCreateBooking) } returns createdSpaceBooking

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        arrivalDate = LocalDate.now(),
        departureDate = LocalDate.now().plusDays(1),
        createdBy = user,
        characteristics = characteristics,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(createdSpaceBooking)
      }

      verify { cas1SpaceBookingCreateService.create(validatedCreateBooking) }
    }

    @Test
    fun `Creates a new booking if data is valid and all space bookings are cancelled`() {
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
        .withSpaceBookings(mutableListOf(spaceBookingWithCancellation))
        .withApplication(application)
        .withPlacementApplication(placementApplication)
        .produce()

      every { spaceBookingRepository.findByPlacementRequestId(placementRequest.id) } returns emptyList()
      every { lockablePlacementRequestRepository.acquirePessimisticLock(placementRequest.id) } returns
        LockablePlacementRequestEntity(placementRequest.id)

      val characteristics = listOf(
        CharacteristicEntityFactory().withName("c1").produce(),
        CharacteristicEntityFactory().withName("c2").produce(),
      )

      val details = CreateBookingDetails(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        expectedArrivalDate = LocalDate.now(),
        expectedDepartureDate = LocalDate.now().plusDays(1),
        createdBy = user,
        characteristics = characteristics,
        transferredFrom = null,
      )

      val validatedCreateBooking = mockk<ValidatedCreateBooking>()
      every { cas1SpaceBookingCreateService.validate(details) } returns CasResult.Success(validatedCreateBooking)
      val createdSpaceBooking = Cas1SpaceBookingEntityFactory().produce()
      every { cas1SpaceBookingCreateService.create(validatedCreateBooking) } returns createdSpaceBooking

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        arrivalDate = LocalDate.now(),
        departureDate = LocalDate.now().plusDays(1),
        createdBy = user,
        characteristics = characteristics,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(createdSpaceBooking)
      }
    }
  }

  @Nested
  inner class Search {

    @Test
    fun `approved premises not found return error`() {
      every { cas1PremisesService.findPremisesById(PREMISES_ID) } returns null

      val result = service.search(
        PREMISES_ID,
        Cas1SpaceBookingService.SpaceBookingFilterCriteria(
          residency = null,
          crnOrName = null,
          keyWorkerStaffCode = null,
          keyWorkerUserId = null,
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
      every { cas1PremisesService.findPremisesById(PREMISES_ID) } returns ApprovedPremisesEntityFactory().withDefaults()
        .produce()

      val results = PageImpl(
        listOf(
          mockk<Cas1SpaceBookingSearchResult>(),
          mockk<Cas1SpaceBookingSearchResult>(),
          mockk<Cas1SpaceBookingSearchResult>(),
        ),
      )
      val pageableCaptor = slot<Pageable>()

      val keyWorkerUserId = UUID.randomUUID()

      every {
        spaceBookingRepository.search(
          "current",
          "theCrnOrName",
          "keyWorkerStaffCode",
          keyWorkerUserId,
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
          keyWorkerUserId = keyWorkerUserId,
        ),
        PageCriteriaFactory(inputSortField).produce(),
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.results).hasSize(3)
      }

      assertThat(pageableCaptor.captured.sort.toList()[0].property).isEqualTo(sqlSortField)
    }
  }

  @Nested
  inner class GetBookingForPremisesAndId {

    @Test
    fun `Returns not found error if premises with the given ID doesn't exist`() {
      every { cas1PremisesService.premisesExistsById(any()) } returns false

      val result = service.getBookingForPremisesAndId(UUID.randomUUID(), UUID.randomUUID())

      assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
      assertThat((result as CasResult.NotFound).entityType).isEqualTo("premises")
    }

    @Test
    fun `Returns not found error if booking with the given ID doesn't exist`() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      every { cas1PremisesService.premisesExistsById(premises.id) } returns true
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

      every { cas1PremisesService.premisesExistsById(premises.id) } returns true
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
        appealChangeRequestId = null,
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
        appealChangeRequestId = null,
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
        appealChangeRequestId = null,
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
      val appealChangeRequestId = UUID.randomUUID()

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
        appealChangeRequestId = appealChangeRequestId,
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
      verify { cas1BookingDomainEventService.spaceBookingCancelled(Cas1BookingCancelledEvent(spaceBooking, user, reason, appealChangeRequestId)) }
      verify { cas1ApplicationStatusService.spaceBookingCancelled(spaceBooking) }
      verify { cas1BookingEmailService.spaceBookingWithdrawn(spaceBooking, application, WithdrawalTriggeredByUser(user)) }
    }
  }

  @Nested
  inner class UpdateSpaceBooking {
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
    fun `return error if common update validation fails`() {
      val validationResult = CasResult.GeneralValidationError<Unit>("common validation failed")

      val updateDetails = UpdateBookingDetails(
        bookingId = existingSpaceBooking.id,
        premisesId = PREMISES_ID,
        departureDate = LocalDate.of(2025, 4, 26),
        updatedBy = user,
        updateType = UpdateType.AMENDMENT,
      )

      every { cas1SpaceBookingUpdateService.validate(updateDetails) } returns validationResult

      val result = service.updateBooking(updateDetails)

      assertThat(result).isEqualTo(validationResult)
    }

    @Test
    fun success() {
      val updateDetails = UpdateBookingDetails(
        bookingId = existingSpaceBooking.id,
        premisesId = PREMISES_ID,
        departureDate = LocalDate.of(2025, 4, 26),
        updatedBy = user,
        updateType = UpdateType.AMENDMENT,
      )

      every { cas1SpaceBookingUpdateService.validate(updateDetails) } returns CasResult.Success(Unit)

      val updatedBooking = Cas1SpaceBookingEntityFactory().produce()
      every { cas1SpaceBookingUpdateService.update(updateDetails) } returns updatedBooking

      val result = service.updateBooking(updateDetails)

      assertThatCasResult(result).isSuccess().with { assertThat(it).isEqualTo(updatedBooking) }
    }
  }

  @Nested
  inner class ShortenSpaceBooking {

    private val originalDepartureDate = LocalDate.now().plusDays(7)

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
    fun `should return validation error when new departure date is before today`() {
      every { cas1PremisesService.findPremisesById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.shortenBooking(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          departureDate = LocalDate.now().minusDays(1),
          updatedBy = user,
          updateType = UpdateType.SHORTENING,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is in the past.")
    }

    @Test
    fun `should return validation error if new departure date is after expected departure date`() {
      every { cas1PremisesService.findPremisesById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.shortenBooking(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          departureDate = LocalDate.now().plusDays(8),
          updatedBy = user,
          updateType = UpdateType.SHORTENING,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is after the current expected departure date.")
    }

    @Test
    fun `should return validation error if new departure date is equal to expected departure date`() {
      every { cas1PremisesService.findPremisesById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.shortenBooking(
        UpdateBookingDetails(
          bookingId = UUID.randomUUID(),
          premisesId = UUID.randomUUID(),
          departureDate = LocalDate.now().plusDays(7),
          updatedBy = user,
          updateType = UpdateType.SHORTENING,
        ),
      )

      assertThatCasResult(result)
        .isFieldValidationError()
        .hasMessage("$.departureDate", "The departure date is the same as the current expected departure date.")
    }

    @Test
    fun `should return validation error if common update validation fails`() {
      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremisesById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every {
        cas1SpaceBookingActionsService.determineActions(existingSpaceBooking)
          .unavailableReason(SpaceBookingAction.SHORTEN)
      } returns null

      val validationResult = CasResult.GeneralValidationError<Unit>("common validation failed")

      val updateDetails = UpdateBookingDetails(
        bookingId = existingSpaceBooking.id,
        premisesId = PREMISES_ID,
        departureDate = LocalDate.now().plusDays(1),
        updatedBy = user,
        updateType = UpdateType.SHORTENING,
      )

      every { cas1SpaceBookingUpdateService.validate(updateDetails) } returns validationResult

      val shortenBookingDetails = UpdateBookingDetails(
        bookingId = existingSpaceBooking.id,
        premisesId = PREMISES_ID,
        departureDate = LocalDate.now().plusDays(1),
        updatedBy = user,
        updateType = UpdateType.SHORTENING,
      )

      val result = service.shortenBooking(shortenBookingDetails)

      assertThat(result).isEqualTo(validationResult)
    }

    @Test
    fun `should return a general validation error when action not allowed for shorten`() {
      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { cas1PremisesService.findPremisesById(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every {
        cas1SpaceBookingActionsService.determineActions(existingSpaceBooking)
      } returns ActionsResult.forUnavailableAction(SpaceBookingAction.SHORTEN, "nope")

      val shortenBookingDetails = UpdateBookingDetails(
        bookingId = existingSpaceBooking.id,
        premisesId = PREMISES_ID,
        departureDate = LocalDate.now().plusDays(1),
        updatedBy = user,
        updateType = UpdateType.SHORTENING,
      )

      val result = service.shortenBooking(shortenBookingDetails)

      assertThatCasResult(result).isGeneralValidationError("nope")
    }

    @Test
    fun `should update departure date when status is hasArrival and send emails`() {
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every {
        cas1SpaceBookingActionsService.determineActions(existingSpaceBooking)
          .unavailableReason(SpaceBookingAction.SHORTEN)
      } returns null

      val updateDetails = UpdateBookingDetails(
        bookingId = existingSpaceBooking.id,
        premisesId = PREMISES_ID,
        departureDate = LocalDate.now().plusDays(1),
        updatedBy = user,
        updateType = UpdateType.SHORTENING,
      )

      every { cas1SpaceBookingUpdateService.validate(updateDetails) } returns CasResult.Success(Unit)
      every { cas1SpaceBookingUpdateService.update(updateDetails) } returns existingSpaceBooking

      val shortenBookingDetails = UpdateBookingDetails(
        bookingId = existingSpaceBooking.id,
        premisesId = PREMISES_ID,
        departureDate = LocalDate.now().plusDays(1),
        updatedBy = user,
        updateType = UpdateType.SHORTENING,
      )

      val result = service.shortenBooking(shortenBookingDetails)

      assertThatCasResult(result).isSuccess()
    }

    @Test
    fun `should update departure date when status is hasArrival and new departure date and actual arrival date are today`() {
      existingSpaceBooking.actualArrivalDate = LocalDate.now()

      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every {
        cas1SpaceBookingActionsService.determineActions(existingSpaceBooking)
          .unavailableReason(SpaceBookingAction.SHORTEN)
      } returns null

      val updateDetails = UpdateBookingDetails(
        bookingId = existingSpaceBooking.id,
        premisesId = PREMISES_ID,
        departureDate = LocalDate.now(),
        updatedBy = user,
        updateType = UpdateType.SHORTENING,
      )

      every { cas1SpaceBookingUpdateService.validate(updateDetails) } returns CasResult.Success(Unit)
      every { cas1SpaceBookingUpdateService.update(updateDetails) } returns existingSpaceBooking

      val shortenBookingDetails = UpdateBookingDetails(
        bookingId = existingSpaceBooking.id,
        premisesId = PREMISES_ID,
        departureDate = LocalDate.now(),
        updatedBy = user,
        updateType = UpdateType.SHORTENING,
      )

      val result = service.shortenBooking(shortenBookingDetails)

      assertThatCasResult(result).isSuccess()
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
      every { cas1PremisesService.findPremisesById(DESTINATION_PREMISES_ID) } returns destinationPremises
      every { cas1PremisesService.findPremisesById(PREMISES_ID) } returns currentPremises
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
    fun `should return a general validation error when action not allowed for transfer`() {
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every {
        cas1SpaceBookingActionsService.determineActions(existingSpaceBooking)
      } returns ActionsResult.forUnavailableAction(SpaceBookingAction.PLANNED_TRANSFER_REQUEST, "nope")
      every { cas1SpaceBookingUpdateService.validate(any()) } returns CasResult.Success(Unit)

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

    @Test
    fun `should return an error if raised by common create booking validation`() {
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every {
        cas1SpaceBookingActionsService.determineActions(existingSpaceBooking)
      } returns ActionsResult.forAllowedAction(SpaceBookingAction.PLANNED_TRANSFER_REQUEST)

      val transferDate = LocalDate.now().minusDays(1)
      val departureDate = LocalDate.now().plusMonths(2)

      val details = CreateBookingDetails(
        premisesId = DESTINATION_PREMISES_ID,
        placementRequestId = existingSpaceBooking.placementRequest!!.id,
        expectedArrivalDate = transferDate,
        expectedDepartureDate = departureDate,
        createdBy = user,
        characteristics = existingSpaceBooking.criteria,
        transferredFrom = TransferInfo(
          type = TransferType.EMERGENCY,
          booking = existingSpaceBooking,
          changeRequestId = null,
        ),
      )

      val commonCreateValidationError = CasResult.GeneralValidationError<ValidatedCreateBooking>("oh no create validation failed")
      every { cas1SpaceBookingCreateService.validate(details) } returns commonCreateValidationError

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

      assertThatCasResult(result).isEqualTo(commonCreateValidationError)
    }

    @Test
    fun `should return an error if raised by common update booking validation`() {
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every {
        cas1SpaceBookingActionsService.determineActions(existingSpaceBooking)
      } returns ActionsResult.forAllowedAction(SpaceBookingAction.PLANNED_TRANSFER_REQUEST)

      every { cas1SpaceBookingCreateService.validate(any()) } returns CasResult.Success(mockk())

      val transferDate = LocalDate.now().minusDays(1)
      val departureDate = LocalDate.now().plusMonths(2)

      val bookingToCreate = Cas1SpaceBookingEntityFactory().produce()
      every { cas1SpaceBookingCreateService.validate(any()) } returns CasResult.Success(
        ValidatedCreateBooking(
          bookingToCreate = bookingToCreate,
          transferredFrom = null,
        ),
      )

      val updateDetails = UpdateBookingDetails(
        bookingId = existingSpaceBooking.id,
        premisesId = currentPremises.id,
        departureDate = transferDate,
        updatedBy = user,
        updateType = UpdateType.TRANSFER,
        transferredTo = TransferInfo(
          type = TransferType.EMERGENCY,
          booking = bookingToCreate,
          changeRequestId = null,
        ),
      )

      val commonUpdateValidationError = CasResult.GeneralValidationError<Unit>("oh no create validation failed")
      every { cas1SpaceBookingUpdateService.validate(updateDetails) } returns commonUpdateValidationError

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

      assertThatCasResult(result).isEqualTo(commonUpdateValidationError)
    }

    @ParameterizedTest
    @CsvSource("0", "1", "2", "3", "4", "5", "6", "7")
    fun `create an emergency booking and update the existing booking if arrival date within last 7 days`(daysAgo: Long) {
      val transferDate = LocalDate.now().minusDays(daysAgo)
      val departureDate = LocalDate.now().plusMonths(2)

      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every {
        cas1SpaceBookingActionsService.determineActions(existingSpaceBooking)
      } returns ActionsResult.forAllowedAction(SpaceBookingAction.PLANNED_TRANSFER_REQUEST)

      val transferredFrom = TransferInfo(
        type = TransferType.EMERGENCY,
        booking = existingSpaceBooking,
        changeRequestId = null,
      )

      val createBookingDetails = CreateBookingDetails(
        premisesId = DESTINATION_PREMISES_ID,
        placementRequestId = existingSpaceBooking.placementRequest!!.id,
        expectedArrivalDate = transferDate,
        expectedDepartureDate = departureDate,
        createdBy = user,
        characteristics = existingSpaceBooking.criteria,
        transferredFrom = transferredFrom,
      )

      val bookingToCreate = Cas1SpaceBookingEntityFactory().produce()
      val validatedCreateBooking = ValidatedCreateBooking(
        bookingToCreate = bookingToCreate,
        transferredFrom = transferredFrom,
      )
      every { cas1SpaceBookingCreateService.validate(createBookingDetails) } returns CasResult.Success(validatedCreateBooking)
      val createdSpaceBooking = Cas1SpaceBookingEntityFactory().produce()
      every { cas1SpaceBookingCreateService.create(validatedCreateBooking) } returns createdSpaceBooking

      val updateDetails = UpdateBookingDetails(
        bookingId = existingSpaceBooking.id,
        premisesId = currentPremises.id,
        departureDate = transferDate,
        updatedBy = user,
        updateType = UpdateType.TRANSFER,
        transferredTo = TransferInfo(
          type = TransferType.EMERGENCY,
          booking = bookingToCreate,
          changeRequestId = null,
        ),
      )

      every { cas1SpaceBookingUpdateService.validate(updateDetails) } returns CasResult.Success(Unit)
      every { cas1SpaceBookingUpdateService.update(updateDetails) } returns existingSpaceBooking

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

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(createdSpaceBooking)
      }

      verify { cas1SpaceBookingCreateService.create(validatedCreateBooking) }
      verify { cas1SpaceBookingUpdateService.update(updateDetails) }
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

    @Test
    fun `should return validation error if arrivalDate is not in the future`() {
      every { cas1PremisesService.findPremisesById(any()) } returns destinationPremises
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
    fun `should return validation error if booking does not exist`() {
      every { cas1PremisesService.findPremisesById(any()) } returns destinationPremises
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
      every { cas1PremisesService.findPremisesById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns anotherChangeRequest

      every { characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises) } returns emptyList()

      every { spaceBookingRepository.saveAndFlush(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.approvedPlannedTransfer(any(), any(), any()) } returns Unit

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

      every { cas1PremisesService.findPremisesById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest

      every { characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises) } returns emptyList()

      every { spaceBookingRepository.saveAndFlush(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.approvedPlannedTransfer(any(), any(), any()) } returns Unit

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
    fun `should return an error if raised by common create booking validation`() {
      every { cas1PremisesService.findPremisesById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest
      every { characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises) } returns emptyList()

      val details = CreateBookingDetails(
        premisesId = DESTINATION_PREMISES_ID,
        placementRequestId = existingSpaceBooking.placementRequest!!.id,
        expectedArrivalDate = LocalDate.now().plusDays(2),
        expectedDepartureDate = LocalDate.now().plusMonths(1),
        createdBy = user,
        characteristics = emptyList(),
        transferredFrom = TransferInfo(
          type = TransferType.PLANNED,
          booking = existingSpaceBooking,
          changeRequestId = existingChangeRequest.id,
        ),
      )

      val commonCreateValidationError = CasResult.GeneralValidationError<ValidatedCreateBooking>("common create validation failed")
      every { cas1SpaceBookingCreateService.validate(details) } returns commonCreateValidationError

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

      assertThat(result).isEqualTo(commonCreateValidationError)
    }

    @Test
    fun `should return an error if raised by common update booking validation`() {
      every { cas1PremisesService.findPremisesById(any()) } returns destinationPremises
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest
      every { characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises) } returns emptyList()

      val transferInfo = TransferInfo(
        type = TransferType.PLANNED,
        booking = existingSpaceBooking,
        changeRequestId = existingChangeRequest.id,
      )
      val createDetails = CreateBookingDetails(
        premisesId = DESTINATION_PREMISES_ID,
        placementRequestId = existingSpaceBooking.placementRequest!!.id,
        expectedArrivalDate = LocalDate.now().plusDays(2),
        expectedDepartureDate = LocalDate.now().plusMonths(1),
        createdBy = user,
        characteristics = emptyList(),
        transferredFrom = transferInfo,
      )

      val bookingToCreate = Cas1SpaceBookingEntityFactory().produce()
      val validatedCreateBooking = ValidatedCreateBooking(bookingToCreate, transferInfo)
      every { cas1SpaceBookingCreateService.validate(createDetails) } returns CasResult.Success(validatedCreateBooking)

      val updateDetails = UpdateBookingDetails(
        bookingId = existingSpaceBooking.id,
        premisesId = currentPremises.id,
        departureDate = LocalDate.now().plusDays(2),
        updatedBy = user,
        updateType = UpdateType.TRANSFER,
        transferredTo = TransferInfo(
          booking = bookingToCreate,
          type = TransferType.PLANNED,
          changeRequestId = existingChangeRequest.id,
        ),
      )

      val commonUpdateValidationError = CasResult.GeneralValidationError<Unit>("common create validation failed")
      every { cas1SpaceBookingUpdateService.validate(updateDetails) } returns commonUpdateValidationError

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

      assertThat(result).isEqualTo(commonUpdateValidationError)
    }

    @Test
    fun `successfully create an transferred booking and update the existing booking`() {
      every { lockableCas1SpaceBookingRepository.acquirePessimisticLock(any()) } returns LockableCas1SpaceBookingEntity(
        existingSpaceBooking.id,
      )
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      every { cas1ChangeRequestService.findChangeRequest(any()) } returns existingChangeRequest

      every { characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises) } returns emptyList()

      every { cas1ChangeRequestService.approvedPlannedTransfer(any(), any(), any()) } returns Unit

      val transferInfo = TransferInfo(
        type = TransferType.PLANNED,
        booking = existingSpaceBooking,
        changeRequestId = existingChangeRequest.id,
      )
      val details = CreateBookingDetails(
        premisesId = DESTINATION_PREMISES_ID,
        placementRequestId = existingSpaceBooking.placementRequest!!.id,
        expectedArrivalDate = LocalDate.now().plusDays(2),
        expectedDepartureDate = LocalDate.now().plusMonths(1),
        createdBy = user,
        characteristics = emptyList(),
        transferredFrom = transferInfo,
      )

      val bookingToCreate = Cas1SpaceBookingEntityFactory().produce()
      val validatedCreateBooking = ValidatedCreateBooking(
        bookingToCreate = bookingToCreate,
        transferredFrom = transferInfo,
      )
      every { cas1SpaceBookingCreateService.validate(details) } returns CasResult.Success(validatedCreateBooking)
      val createdSpaceBooking = Cas1SpaceBookingEntityFactory().produce()
      every { cas1SpaceBookingCreateService.create(validatedCreateBooking) } returns createdSpaceBooking

      val updateDetails = UpdateBookingDetails(
        bookingId = existingSpaceBooking.id,
        premisesId = currentPremises.id,
        departureDate = LocalDate.now().plusDays(2),
        updatedBy = user,
        updateType = UpdateType.TRANSFER,
        transferredTo = TransferInfo(
          type = TransferType.PLANNED,
          booking = bookingToCreate,
          changeRequestId = existingChangeRequest.id,
        ),
      )

      every { cas1SpaceBookingUpdateService.validate(updateDetails) } returns CasResult.Success(Unit)
      every { cas1SpaceBookingUpdateService.update(updateDetails) } returns existingSpaceBooking

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

      assertThatCasResult(result).isSuccess()

      verify { cas1ChangeRequestService.approvedPlannedTransfer(existingChangeRequest, user, createdSpaceBooking) }
      verify { cas1SpaceBookingCreateService.create(validatedCreateBooking) }
      verify { cas1SpaceBookingUpdateService.update(updateDetails) }
    }
  }
}
