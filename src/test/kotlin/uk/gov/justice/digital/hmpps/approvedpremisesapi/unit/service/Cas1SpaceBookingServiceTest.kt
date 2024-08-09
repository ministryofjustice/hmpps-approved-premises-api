package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1SpaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.SpaceAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
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

  @InjectMockKs
  private lateinit var service: Cas1SpaceBookingService

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

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      result as ValidatableActionResult.FieldValidationError

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

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      result as ValidatableActionResult.FieldValidationError

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

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      result as ValidatableActionResult.FieldValidationError

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

      assertThat(result).isInstanceOf(ValidatableActionResult.ConflictError::class.java)
      result as ValidatableActionResult.ConflictError

      assertThat(result.conflictingEntityId).isEqualTo(existingSpaceBooking.id)
      assertThat(result.message).contains("A Space Booking already exists")
    }

    @Test
    fun `Returns new booking if all data is valid`() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .produce()

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      val startDate = LocalDate.now()
      val durationInDays = 1
      val departureDate = startDate.plusDays(durationInDays.toLong())

      val spaceAvailability = SpaceAvailability(
        premisesId = premises.id,
      )

      every { premisesService.getPremises(premises.id) } returns premises
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest
      every { spaceBookingRepository.findByPremisesIdAndPlacementRequestId(premises.id, placementRequest.id) } returns null

      every {
        spaceSearchRepository.getSpaceAvailabilityForCandidatePremises(listOf(premises.id), startDate, durationInDays)
      } returns listOf(spaceAvailability)

      every { spaceBookingRepository.save(any()) } returnsArgument 0

      val result = service.createNewBooking(
        premisesId = premises.id,
        placementRequestId = placementRequest.id,
        arrivalDate = startDate,
        departureDate = departureDate,
        createdBy = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success

      assertThat(result.entity.premises).isEqualTo(premises)
      assertThat(result.entity.placementRequest).isEqualTo(placementRequest)
      assertThat(result.entity.arrivalDate).isEqualTo(startDate)
      assertThat(result.entity.departureDate).isEqualTo(departureDate)
    }
  }
}
