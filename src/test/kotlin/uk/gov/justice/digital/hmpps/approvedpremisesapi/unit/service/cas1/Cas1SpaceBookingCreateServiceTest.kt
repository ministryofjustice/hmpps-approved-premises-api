package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks.ClockConfiguration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingCreateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCreatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.LocalDate
import java.util.UUID

class Cas1SpaceBookingCreateServiceTest {
  private val cas1PremisesService = mockk<Cas1PremisesService>()
  private val placementRequestService = mockk<PlacementRequestService>()
  private val spaceBookingRepository = mockk<Cas1SpaceBookingRepository>()
  private val cas1BookingDomainEventService = mockk<Cas1BookingDomainEventService>()
  private val cas1BookingEmailService = mockk<Cas1BookingEmailService>()
  private val cas1ApplicationStatusService = mockk<Cas1ApplicationStatusService>()
  private val clock = ClockConfiguration.FixedClock()

  private val service = Cas1SpaceBookingCreateService(
    placementRequestService,
    cas1PremisesService,
    spaceBookingRepository,
    cas1ApplicationStatusService,
    cas1BookingDomainEventService,
    cas1BookingEmailService,
    clock,
  )

  @Nested
  inner class Validate {
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
    fun `Error if no premises with the given ID exists`() {
      every { cas1PremisesService.findPremiseById(any()) } returns null
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest
      LockablePlacementRequestEntity(placementRequest.id)

      val result = service.validate(
        Cas1SpaceBookingCreateService.CreateBookingDetails(
          premisesId = UUID.randomUUID(),
          placementRequestId = placementRequest.id,
          expectedArrivalDate = LocalDate.now(),
          expectedDepartureDate = LocalDate.now().plusDays(1),
          createdBy = user,
          characteristics = emptyList(),
          transferType = null,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.premisesId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Error if premises supplied does not support space bookings`() {
      val premisesDoesntSupportSpaceBookings = ApprovedPremisesEntityFactory()
        .withSupportsSpaceBookings(false)
        .withDefaults()
        .produce()

      every { cas1PremisesService.findPremiseById(any()) } returns premisesDoesntSupportSpaceBookings
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest
      LockablePlacementRequestEntity(placementRequest.id)

      val result = service.validate(
        Cas1SpaceBookingCreateService.CreateBookingDetails(
          premisesId = premisesDoesntSupportSpaceBookings.id,
          placementRequestId = placementRequest.id,
          expectedArrivalDate = LocalDate.now(),
          expectedDepartureDate = LocalDate.now().plusDays(1),
          createdBy = user,
          characteristics = emptyList(),
          transferType = null,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.premisesId" && value == "doesNotSupportSpaceBookings"
      }
    }

    @Test
    fun `Error if no placement request with the given ID exists`() {
      every { cas1PremisesService.findPremiseById(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(any()) } returns null
      LockablePlacementRequestEntity(placementRequest.id)

      val result = service.validate(
        Cas1SpaceBookingCreateService.CreateBookingDetails(
          premisesId = premises.id,
          placementRequestId = UUID.randomUUID(),
          expectedArrivalDate = LocalDate.now(),
          expectedDepartureDate = LocalDate.now().plusDays(1),
          createdBy = user,
          characteristics = emptyList(),
          transferType = null,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.placementRequestId" && value == "doesNotExist"
      }
    }

    @Test
    fun `Error if the departure date is before the arrival date`() {
      every { cas1PremisesService.findPremiseById(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest
      LockablePlacementRequestEntity(placementRequest.id)

      val result = service.validate(
        Cas1SpaceBookingCreateService.CreateBookingDetails(
          premisesId = premises.id,
          placementRequestId = placementRequest.id,
          expectedArrivalDate = LocalDate.now().plusDays(1),
          expectedDepartureDate = LocalDate.now(),
          createdBy = user,
          characteristics = emptyList(),
          transferType = null,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      result as CasResult.FieldValidationError

      assertThat(result.validationMessages).anySatisfy { key, value ->
        key == "$.departureDate" && value == "shouldBeAfterArrivalDate"
      }
    }

    @Test
    fun valid() {
      every { cas1PremisesService.findPremiseById(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest

      val result = service.validate(
        Cas1SpaceBookingCreateService.CreateBookingDetails(
          premisesId = premises.id,
          placementRequestId = placementRequest.id,
          expectedArrivalDate = LocalDate.now(),
          expectedDepartureDate = LocalDate.now().plusDays(2),
          createdBy = user,
          characteristics = listOf(
            CharacteristicEntityFactory().withName("c1").produce(),
            CharacteristicEntityFactory().withName("c2").produce(),
          ),
          transferType = null,
        ),
      )

      assertThatCasResult(result).isSuccess()
    }
  }

  @Nested
  inner class Create {
    @Test
    fun `Creates a new booking if data is valid and legacy and space bookings are cancelled`() {
      val user = UserEntityFactory()
        .withDefaults()
        .produce()

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
      every { cas1ApplicationStatusService.spaceBookingMade(any()) } returns Unit
      every { cas1BookingDomainEventService.spaceBookingMade(any()) } returns Unit
      every { cas1BookingEmailService.spaceBookingMade(any(), any()) } returns Unit

      val persistedBookingCaptor = slot<Cas1SpaceBookingEntity>()
      every { spaceBookingRepository.save(capture(persistedBookingCaptor)) } returnsArgument 0

      val result = service.create(
        Cas1SpaceBookingCreateService.CreateBookingDetails(
          premisesId = premises.id,
          placementRequestId = placementRequest.id,
          expectedArrivalDate = arrivalDate,
          expectedDepartureDate = departureDate,
          createdBy = user,
          characteristics = listOf(
            CharacteristicEntityFactory().withName("c1").produce(),
            CharacteristicEntityFactory().withName("c2").produce(),
          ),
          transferType = TransferType.PLANNED,
        ),
      )

      val persistedBooking = persistedBookingCaptor.captured
      assertThat(result).isEqualTo(persistedBooking)

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
      assertThat(persistedBooking.transferType).isEqualTo(TransferType.PLANNED)

      verify { cas1ApplicationStatusService.spaceBookingMade(persistedBooking) }
      verify { cas1BookingDomainEventService.spaceBookingMade(Cas1BookingCreatedEvent(persistedBooking, user)) }
      verify { cas1BookingEmailService.spaceBookingMade(persistedBooking, application) }
    }
  }
}
