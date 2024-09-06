package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingResidency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PageCriteriaFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1SpaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.SpaceAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.LocalDate
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

  @InjectMockKs
  private lateinit var service: Cas1SpaceBookingService

  companion object CONSTANTS {
    val PREMISES_ID = UUID.randomUUID()
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

      every { premisesService.getPremises(any()) } returns null
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

      every { premisesService.getPremises(premises.id) } returns premises
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

      every { premisesService.getPremises(premises.id) } returns premises
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

      every { premisesService.getPremises(premises.id) } returns premises
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
    fun `Creates new booking if all data is valid and raises domain event`() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withApplication(application)
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

      every { premisesService.getPremises(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest
      every { spaceBookingRepository.findByPremisesIdAndPlacementRequestId(premises.id, placementRequest.id) } returns null

      every {
        spaceSearchRepository.getSpaceAvailabilityForCandidatePremises(listOf(premises.id), arrivalDate, durationInDays)
      } returns listOf(spaceAvailability)

      every {
        cas1BookingDomainEventService.spaceBookingMade(
          application,
          any(),
          user,
          placementRequest,
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
    }
  }

  @Nested
  inner class Search {

    @Test
    fun `premises not found return error`() {
      every { premisesService.getPremises(PREMISES_ID) } returns null

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

    @Test
    fun `premises not approved premises return error`() {
      every { premisesService.getPremises(PREMISES_ID) } returns TemporaryAccommodationPremisesEntityFactory()
        .withDefaults()
        .produce()

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
      every { premisesService.getPremises(PREMISES_ID) } returns ApprovedPremisesEntityFactory().withDefaults().produce()

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
}
