package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingResidency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PageCriteriaFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1SpaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.SpaceAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingManagementDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1SpaceBookingServiceTest {
  @MockK
  private lateinit var premisesService: PremisesService

  @MockK
  private lateinit var placementRequestService: PlacementRequestService

  @MockK
  private lateinit var spaceBookingRepository: Cas1SpaceBookingRepository

  @MockK
  private lateinit var spaceSearchRepository: Cas1SpaceSearchRepository

  @MockK
  private lateinit var cas1BookingDomainEventService: Cas1BookingDomainEventService

  @MockK
  private lateinit var cas1BookingEmailService: Cas1BookingEmailService

  @MockK
  private lateinit var cas1SpaceBookingManagementDomainEventService: Cas1SpaceBookingManagementDomainEventService

  @MockK
  private lateinit var cas1ApplicationStatusService: Cas1ApplicationStatusService

  @InjectMockKs
  private lateinit var service: Cas1SpaceBookingService

  companion object CONSTANTS {
    val PREMISES_ID: UUID = UUID.randomUUID()
  }

  @Nested
  inner class CreateNewBooking {
    @Test
    fun `Returns validation error if no premises with the given ID exists`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .produce()

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      every { premisesService.getApprovedPremises(any()) } returns null
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest

      val result = service.createNewBooking(
        premisesId = UUID.randomUUID(),
        placementRequestId = placementRequest.id,
        arrivalDate = LocalDate.now(),
        departureDate = LocalDate.now().plusDays(1),
        createdBy = user,
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.premisesId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if no placement request with the given ID exists`() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      every { premisesService.getApprovedPremises(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(any()) } returns null

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = UUID.randomUUID(),
        arrivalDate = LocalDate.now(),
        departureDate = LocalDate.now().plusDays(1),
        createdBy = user,
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.placementRequestId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if the departure date is before the arrival date`() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .produce()

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      every { premisesService.getApprovedPremises(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        arrivalDate = LocalDate.now().plusDays(1),
        departureDate = LocalDate.now(),
        createdBy = user,
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.departureDate" && value == "shouldBeAfterArrivalDate"
      }
    }

    @Test
    fun `Returns conflict error if a booking already exists for the same premises and placement request`() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .produce()

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withPremises(premises)
        .withPlacementRequest(placementRequest)
        .produce()

      every { premisesService.getApprovedPremises(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest
      every { spaceBookingRepository.findByPremisesIdAndPlacementRequestId(premises.id, placementRequest.id) } returns existingSpaceBooking

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        arrivalDate = LocalDate.now(),
        departureDate = LocalDate.now().plusDays(1),
        createdBy = user,
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.conflictingEntityId).isEqualTo(existingSpaceBooking.id)
      assertThat(result.message).contains("A Space Booking already exists")
    }

    @Test
    fun `Creates new booking if all data is valid, updates application status, raises domain event and sends email`() {
      val premises = ApprovedPremisesEntityFactory().withDefaults().produce()
      val application = ApprovedPremisesApplicationEntityFactory().withDefaults().produce()
      val placementApplication = PlacementApplicationEntityFactory().withDefaults().produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withPlacementApplication(placementApplication)
        .produce()

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      val arrivalDate = LocalDate.now()
      val durationInDays = 1
      val departureDate = arrivalDate.plusDays(durationInDays.toLong())

      val spaceAvailability = SpaceAvailability(
        premisesId = premises.id,
      )

      every { premisesService.getApprovedPremises(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest
      every { spaceBookingRepository.findByPremisesIdAndPlacementRequestId(premises.id, placementRequest.id) } returns null

      every {
        spaceSearchRepository.getSpaceAvailabilityForCandidatePremises(listOf(premises.id), arrivalDate, durationInDays)
      } returns listOf(spaceAvailability)

      every { cas1ApplicationStatusService.spaceBookingMade(any()) } returns Unit

      every {
        cas1BookingDomainEventService.spaceBookingMade(
          application,
          any(),
          user,
          placementRequest,
        )
      } returns Unit

      every {
        cas1BookingEmailService.spaceBookingMade(
          any(),
          application,
          placementApplication,
        )
      } returns Unit

      val persistedBookingCaptor = slot<Cas1SpaceBookingEntity>()
      every { spaceBookingRepository.save(capture(persistedBookingCaptor)) } returnsArgument 0

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
        createdBy = user,
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
      assertThat(persistedBooking.actualArrivalDateTime).isNull()
      assertThat(persistedBooking.actualDepartureDateTime).isNull()
      assertThat(persistedBooking.canonicalArrivalDate).isEqualTo(arrivalDate)
      assertThat(persistedBooking.canonicalDepartureDate).isEqualTo(departureDate)
      assertThat(persistedBooking.crn).isEqualTo(application.crn)
      assertThat(persistedBooking.keyWorkerStaffCode).isNull()
      assertThat(persistedBooking.keyWorkerAssignedAt).isNull()

      verify { cas1ApplicationStatusService.spaceBookingMade(persistedBooking) }
    }
  }

  @Nested
  inner class Search {

    @Test
    fun `approved premises not found return error`() {
      every { premisesService.getApprovedPremises(PREMISES_ID) } returns null

      val result = service.search(
        PREMISES_ID,
        Cas1SpaceBookingService.SpaceBookingFilterCriteria(
          residency = null,
          crnOrName = null,
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
      every { premisesService.getApprovedPremises(PREMISES_ID) } returns ApprovedPremisesEntityFactory().withDefaults().produce()

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
          PREMISES_ID,
          capture(pageableCaptor),
        )
      } returns results

      val result = service.search(
        PREMISES_ID,
        Cas1SpaceBookingService.SpaceBookingFilterCriteria(
          residency = Cas1SpaceBookingResidency.current,
          crnOrName = "theCrnOrName",
        ),
        PageCriteriaFactory(inputSortField).produce(),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.first).hasSize(3)

      assertThat(pageableCaptor.captured.sort.toList()[0].property).isEqualTo(sqlSortField)
    }
  }

  @Nested
  inner class GetBooking {

    @Test
    fun `Returns not found error if premises with the given ID doesn't exist`() {
      every { premisesService.getApprovedPremises(any()) } returns null

      val result = service.getBooking(UUID.randomUUID(), UUID.randomUUID())

      assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
      assertThat((result as CasResult.NotFound).entityType).isEqualTo("premises")
    }

    @Test
    fun `Returns not found error if booking with the given ID doesn't exist`() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      every { premisesService.getApprovedPremises(premises.id) } returns premises
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

      every { premisesService.getApprovedPremises(premises.id) } returns premises
      every { spaceBookingRepository.findByIdOrNull(spaceBooking.id) } returns spaceBooking

      val result = service.getBooking(premises.id, spaceBooking.id)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      assertThat((result as CasResult.Success).value).isEqualTo(spaceBooking)
    }
  }

  @Nested
  inner class RecordArrival {

    private val originalArrivalDate = LocalDateTime.now().minusDays(10).toInstant(ZoneOffset.UTC)
    private val arrivalDateTime = LocalDateTime.now().toInstant(ZoneOffset.UTC)
    private val expectedDepartureDate = LocalDate.now().plusMonths(3)

    private val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .produce()

    @Test
    fun `Returns validation error if no premises with the given premisesId exists`() {
      every { premisesService.getApprovedPremises(any()) } returns null
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking

      val result = service.recordArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        cas1NewArrival = Cas1NewArrival(expectedDepartureDate, arrivalDateTime),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.premisesId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns validation error if no space booking with the given bookingId exists`() {
      every { premisesService.getApprovedPremises(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns null

      val result = service.recordArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        cas1NewArrival = Cas1NewArrival(LocalDate.of(2024, 12, 31), LocalDateTime.now().toInstant(ZoneOffset.UTC)),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.bookingId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Returns conflict error if the space booking record already has an arrival date recorded`() {
      val existingSpaceBookingWithArrivalDate = existingSpaceBooking.copy(actualArrivalDateTime = originalArrivalDate)

      every { premisesService.getApprovedPremises(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBookingWithArrivalDate

      val result = service.recordArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        cas1NewArrival = Cas1NewArrival(expectedDepartureDate, arrivalDateTime),
      )

      assertThat(result).isInstanceOf(CasResult.ConflictError::class.java)
      result as CasResult.ConflictError

      assertThat(result.message).isEqualTo("An arrival is already recorded for this Space Booking")
    }

    @Test
    fun `Updates existing space booking with arrival information and raises domain event`() {
      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()

      every { premisesService.getApprovedPremises(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1SpaceBookingManagementDomainEventService.arrivalRecorded(any(), any()) } returns Unit

      val result = service.recordArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        cas1NewArrival = Cas1NewArrival(expectedDepartureDate, arrivalDateTime),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(existingSpaceBooking.premises).isEqualTo(updatedSpaceBooking.premises)
      assertThat(existingSpaceBooking.placementRequest).isEqualTo(updatedSpaceBooking.placementRequest)
      assertThat(existingSpaceBooking.application).isEqualTo(updatedSpaceBooking.application)
      assertThat(existingSpaceBooking.createdAt).isEqualTo(updatedSpaceBooking.createdAt)
      assertThat(existingSpaceBooking.createdBy).isEqualTo(updatedSpaceBooking.createdBy)
      assertThat(existingSpaceBooking.actualDepartureDateTime).isEqualTo(updatedSpaceBooking.actualDepartureDateTime)
      assertThat(existingSpaceBooking.crn).isEqualTo(updatedSpaceBooking.crn)
      assertThat(existingSpaceBooking.keyWorkerStaffCode).isEqualTo(updatedSpaceBooking.keyWorkerStaffCode)
      assertThat(existingSpaceBooking.keyWorkerAssignedAt).isEqualTo(updatedSpaceBooking.keyWorkerAssignedAt)
      assertThat(existingSpaceBooking.expectedArrivalDate).isEqualTo(updatedSpaceBooking.expectedArrivalDate)

      assertThat(arrivalDateTime).isEqualTo(updatedSpaceBooking.actualArrivalDateTime)
      assertThat(arrivalDateTime.toLocalDate()).isEqualTo(updatedSpaceBooking.canonicalArrivalDate)
      assertThat(expectedDepartureDate).isEqualTo(updatedSpaceBooking.expectedDepartureDate)
      assertThat(expectedDepartureDate).isEqualTo(updatedSpaceBooking.canonicalDepartureDate)
    }

    @Test
    fun `Updates existing space booking with arrival information and recognises change in expected departure date`() {
      val updatedSpaceBookingCaptor = slot<Cas1SpaceBookingEntity>()
      val updatedExpectedDepartureDate = expectedDepartureDate.plusMonths(1)

      every { premisesService.getApprovedPremises(any()) } returns premises
      every { spaceBookingRepository.findByIdOrNull(any()) } returns existingSpaceBooking
      every { spaceBookingRepository.save(capture(updatedSpaceBookingCaptor)) } returnsArgument 0
      every { cas1SpaceBookingManagementDomainEventService.arrivalRecorded(any(), any()) } returns Unit

      val result = service.recordArrivalForBooking(
        premisesId = UUID.randomUUID(),
        bookingId = UUID.randomUUID(),
        cas1NewArrival = Cas1NewArrival(expectedDepartureDate.plusMonths(1), arrivalDateTime),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      val updatedSpaceBooking = updatedSpaceBookingCaptor.captured
      assertThat(existingSpaceBooking.premises).isEqualTo(updatedSpaceBooking.premises)
      assertThat(existingSpaceBooking.placementRequest).isEqualTo(updatedSpaceBooking.placementRequest)
      assertThat(existingSpaceBooking.application).isEqualTo(updatedSpaceBooking.application)
      assertThat(existingSpaceBooking.createdAt).isEqualTo(updatedSpaceBooking.createdAt)
      assertThat(existingSpaceBooking.createdBy).isEqualTo(updatedSpaceBooking.createdBy)
      assertThat(existingSpaceBooking.actualDepartureDateTime).isEqualTo(updatedSpaceBooking.actualDepartureDateTime)
      assertThat(existingSpaceBooking.crn).isEqualTo(updatedSpaceBooking.crn)
      assertThat(existingSpaceBooking.keyWorkerStaffCode).isEqualTo(updatedSpaceBooking.keyWorkerStaffCode)
      assertThat(existingSpaceBooking.keyWorkerAssignedAt).isEqualTo(updatedSpaceBooking.keyWorkerAssignedAt)
      assertThat(existingSpaceBooking.expectedArrivalDate).isEqualTo(updatedSpaceBooking.expectedArrivalDate)

      assertThat(arrivalDateTime).isEqualTo(updatedSpaceBooking.actualArrivalDateTime)
      assertThat(arrivalDateTime.toLocalDate()).isEqualTo(updatedSpaceBooking.canonicalArrivalDate)
      assertThat(updatedExpectedDepartureDate).isEqualTo(updatedSpaceBooking.expectedDepartureDate)
      assertThat(updatedExpectedDepartureDate).isEqualTo(updatedSpaceBooking.canonicalDepartureDate)
    }
  }

  @Nested
  inner class GetWithdrawalState {

    @Test
    fun `is withdrawable if no arrival and not cancelled`() {
      val result = service.getWithdrawableState(
        Cas1SpaceBookingEntityFactory()
          .withActualArrivalDateTime(null)
          .withCancellationOccurredAt(null)
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
          .withActualArrivalDateTime(Instant.now())
          .withCancellationOccurredAt(null)
          .produce(),
        UserEntityFactory().withDefaults().produce(),
      )

      assertThat(result.withdrawable).isEqualTo(false)
      assertThat(result.withdrawn).isEqualTo(false)
      assertThat(result.blockingReason).isEqualTo(BlockingReason.ArrivalRecordedInCas1)
    }

    @Test
    fun `is not withdrawable if already cancelled`() {
      val result = service.getWithdrawableState(
        Cas1SpaceBookingEntityFactory()
          .withActualArrivalDateTime(null)
          .withCancellationOccurredAt(LocalDate.now())
          .produce(),
        UserEntityFactory().withDefaults().produce(),
      )

      assertThat(result.withdrawable).isEqualTo(false)
      assertThat(result.withdrawn).isEqualTo(true)
      assertThat(result.blockingReason).isNull()
    }

    @Test
    fun `user without CAS1_BOOKING_WITHDRAW permission cannot directly withdraw`() {
      val result = service.getWithdrawableState(
        Cas1SpaceBookingEntityFactory().produce(),
        UserEntityFactory.mockUserWithoutPermission(UserPermission.CAS1_BOOKING_WITHDRAW),
      )

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(false)
    }

    @Test
    fun `user with CAS1_BOOKING_WITHDRAW can directly withdraw`() {
      val result = service.getWithdrawableState(
        Cas1SpaceBookingEntityFactory().produce(),
        UserEntityFactory.mockUserWithPermission(UserPermission.CAS1_BOOKING_WITHDRAW),
      )

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(true)
    }
  }
}
