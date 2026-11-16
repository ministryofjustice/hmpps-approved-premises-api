package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TransferReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1NewSpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingResidency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingSummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PageCriteriaFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.ActionsResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingActionsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingCreateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingCreateService.CreateBookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingCreateService.ValidatedCreateBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.UpdateType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingUpdateService.UpdateBookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCancelledEvent
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
  private val cas1BookingDomainEventService = mockk<Cas1SpaceBookingDomainEventService>()
  private val cas1BookingEmailService = mockk<Cas1BookingEmailService>()
  private val cas1ApplicationStatusService = mockk<Cas1ApplicationStatusService>()
  private val cancellationReasonRepository = mockk<CancellationReasonRepository>()
  private val lockablePlacementRequestRepository = mockk<LockablePlacementRequestRepository>()
  private val characteristicService = mockk<CharacteristicService>()
  private val cas1SpaceBookingActionsService = mockk<Cas1SpaceBookingActionsService>()
  private val cas1SpaceBookingCreateService = mockk<Cas1SpaceBookingCreateService>()
  private val cas1SpaceBookingUpdateService = mockk<Cas1SpaceBookingUpdateService>()
  private val offenderService = mockk<OffenderService>()
  private val cas1SpaceBookingTransformer = mockk<Cas1SpaceBookingTransformer>()
  private val featureFlagService = mockk<FeatureFlagService>()

  private val service = Cas1SpaceBookingService(
    cas1PremisesService,
    spaceBookingRepository,
    cas1BookingDomainEventService,
    cas1BookingEmailService,
    cas1ApplicationStatusService,
    cancellationReasonRepository,
    lockablePlacementRequestRepository,
    characteristicService,
    cas1SpaceBookingActionsService,
    cas1SpaceBookingCreateService,
    cas1SpaceBookingUpdateService,
    offenderService,
    cas1SpaceBookingTransformer,
    featureFlagService,
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
        placementRequestId = placementRequest.id,
        newSpaceBooking = Cas1NewSpaceBooking(
          premisesId = premises.id,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusDays(1),
        ),
        createdBy = user,
        characteristics = emptyList(),
      )

      assertThat(result).isEqualTo(commonValidationError)
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
        Cas1CharacteristicEntityFactory().withName("c1").produce(),
        Cas1CharacteristicEntityFactory().withName("c2").produce(),
      )

      val details = CreateBookingDetails(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        expectedArrivalDate = LocalDate.now(),
        expectedDepartureDate = LocalDate.now().plusDays(1),
        createdBy = user,
        characteristics = characteristics,
        additionalInformation = "Transfer to another AP",
        transferReason = null,
      )

      val validatedCreateBooking = mockk<ValidatedCreateBooking>()
      every { cas1SpaceBookingCreateService.validate(details) } returns CasResult.Success(validatedCreateBooking)
      val createdSpaceBooking = Cas1SpaceBookingEntityFactory().produce()
      every { cas1SpaceBookingCreateService.create(validatedCreateBooking) } returns createdSpaceBooking

      val result = service.createNewBooking(
        placementRequestId = placementRequest.id,
        newSpaceBooking = Cas1NewSpaceBooking(
          premisesId = premises.id,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusDays(1),
          additionalInformation = "Transfer to another AP",
        ),
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
        Cas1CharacteristicEntityFactory().withName("c1").produce(),
        Cas1CharacteristicEntityFactory().withName("c2").produce(),
      )

      val details = CreateBookingDetails(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        expectedArrivalDate = LocalDate.now(),
        expectedDepartureDate = LocalDate.now().plusDays(1),
        createdBy = user,
        characteristics = characteristics,
        additionalInformation = null,
        transferReason = null,
      )

      val validatedCreateBooking = mockk<ValidatedCreateBooking>()
      every { cas1SpaceBookingCreateService.validate(details) } returns CasResult.Success(validatedCreateBooking)
      val createdSpaceBooking = Cas1SpaceBookingEntityFactory().produce()
      every { cas1SpaceBookingCreateService.create(validatedCreateBooking) } returns createdSpaceBooking

      val result = service.createNewBooking(
        placementRequestId = placementRequest.id,
        newSpaceBooking = Cas1NewSpaceBooking(
          premisesId = premises.id,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusDays(1),
        ),
        createdBy = user,
        characteristics = characteristics,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(createdSpaceBooking)
      }
    }

    @Test
    fun `Creates a transfer booking with transfer reason`() {
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
        Cas1CharacteristicEntityFactory().withName("c1").produce(),
        Cas1CharacteristicEntityFactory().withName("c2").produce(),
      )

      val details = CreateBookingDetails(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        expectedArrivalDate = LocalDate.now(),
        expectedDepartureDate = LocalDate.now().plusDays(1),
        createdBy = user,
        characteristics = characteristics,
        additionalInformation = "Transfer to another AP",
        transferReason = TransferReason.riskToResident,
      )

      val validatedCreateBooking = mockk<ValidatedCreateBooking>()
      every { cas1SpaceBookingCreateService.validate(details) } returns CasResult.Success(validatedCreateBooking)
      val createdSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withTransferReason(TransferReason.riskToResident)
        .withAdditionalInformation("Transfer to another AP")
        .produce()
      every { cas1SpaceBookingCreateService.create(validatedCreateBooking) } returns createdSpaceBooking

      val result = service.createNewBooking(
        placementRequestId = placementRequest.id,
        newSpaceBooking = Cas1NewSpaceBooking(
          premisesId = premises.id,
          arrivalDate = LocalDate.now(),
          departureDate = LocalDate.now().plusDays(1),
          transferReason = TransferReason.riskToResident,
          additionalInformation = "Transfer to another AP",
        ),
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
      every { featureFlagService.getBooleanFlag("use-tier-v3") } returns false

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
      "personName,personName,false",
      "canonicalArrivalDate,canonicalArrivalDate,false",
      "canonicalDepartureDate,canonicalDepartureDate,false",
      "keyWorkerName,keyWorkerName,false",
      "tier,tierOnApplicationCreation,false",
      "personTier,personTierV2Score,false",
      "personTier,personTierV3Score,true",
    )
    fun `delegate to repository, defining correct sort column`(
      inputSortField: Cas1SpaceBookingSummarySortField,
      sqlSortField: String,
      useTierV3FeatureFlag: Boolean,
    ) {
      every { featureFlagService.getBooleanFlag("use-tier-v3") } returns useTierV3FeatureFlag

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

      verify { cas1BookingDomainEventService.spaceBookingCancelled(Cas1BookingCancelledEvent(spaceBooking, user, reason)) }
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
  inner class FindAllSpaceBookingsForCrn {
    @Test
    fun `successfully return bookings`() {
      val includeCancelled = false
      val crn = "X12345"
      val booking1 = Cas1SpaceBookingEntityFactory().withCancellationOccurredAt(null).produce()
      val booking2 = Cas1SpaceBookingEntityFactory().withCancellationOccurredAt(null).produce()
      val expected = listOf(booking1, booking2)

      every { spaceBookingRepository.findAllSpaceBookingsForCrn(crn, includeCancelled) } returns expected

      val result = service.findAllBookingsForCrn(crn, includeCancelled)

      assertThat(result).isEqualTo(expected)
      verify(exactly = 1) { spaceBookingRepository.findAllSpaceBookingsForCrn(crn, includeCancelled) }
    }

    @Test
    fun `returns empty list when no bookings`() {
      val includeCancelled = false
      val crn = "X99999"
      every { spaceBookingRepository.findAllSpaceBookingsForCrn(crn, includeCancelled) } returns emptyList()

      val result = service.findAllBookingsForCrn(crn, includeCancelled)

      assertThat(result).isEmpty()
      verify(exactly = 1) { spaceBookingRepository.findAllSpaceBookingsForCrn(crn, includeCancelled) }
    }
  }

  @Nested
  inner class FindLatestPlacement {
    private val user = UserEntityFactory()
      .withDefaults()
      .produce()
    private val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
      .produce()

    @Test
    fun `successfully return latest placement`() {
      val booking1 = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(null)
        .withCreatedAt(OffsetDateTime.now().minusDays(2))
        .withApplication(application)
        .produce()
      val latestBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(LocalDate.now())
        .withCreatedAt(OffsetDateTime.now())
        .withApplication(application)
        .produce()
      val booking2 = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(null)
        .withCreatedAt(OffsetDateTime.now().minusDays(1))
        .withApplication(application)
        .produce()
      val bookings = listOf(booking1, latestBooking, booking2)

      every { spaceBookingRepository.findAllSpaceBookingsForApplicationId(application.id) } returns bookings

      val result = service.getLatestPlacement(application.id)

      assertThat(result).isEqualTo(latestBooking)
      verify(exactly = 1) { spaceBookingRepository.findAllSpaceBookingsForApplicationId(application.id) }
    }

    @Test
    fun `returns null when no placements`() {
      every { spaceBookingRepository.findAllSpaceBookingsForApplicationId(application.id) } returns emptyList()

      val result = service.getLatestPlacement(application.id)

      assertThat(result).isNull()
      verify(exactly = 1) { spaceBookingRepository.findAllSpaceBookingsForApplicationId(application.id) }
    }
  }
}
