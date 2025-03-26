package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1RejectChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestRejectionReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1NewChangeRequestFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.LockableCas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.LockableCas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.LocalDate
import java.util.UUID

class Cas1ChangeRequestServiceTest {
  private val placementRequestRepository = mockk<PlacementRequestRepository>()
  private val cas1ChangeRequestReasonRepository = mockk<Cas1ChangeRequestReasonRepository>()
  private val cas1SpaceBookingRepository = mockk<Cas1SpaceBookingRepository>()
  private val objectMapper = mockk<ObjectMapper>()
  private val cas1ChangeRequestRepository = mockk<Cas1ChangeRequestRepository>()
  private val cas1ChangeRequestRejectionReasonRepository = mockk<Cas1ChangeRequestRejectionReasonRepository>()
  private val lockableCas1ChangeRequestEntityRepository = mockk<LockableCas1ChangeRequestRepository>()
  private val service = Cas1ChangeRequestService(
    cas1ChangeRequestRepository,
    placementRequestRepository,
    cas1ChangeRequestReasonRepository,
    objectMapper,
    cas1SpaceBookingRepository,
    lockableCas1ChangeRequestEntityRepository,
    cas1ChangeRequestRejectionReasonRepository,
  )

  @Nested
  inner class CreateChangeRequest {
    @Test
    fun `throws not found error when placement request is not found`() {
      val placementRequest = PlacementRequestEntityFactory().withDefaults().produce()
      val cas1NewChangeRequest = Cas1NewChangeRequestFactory().produce()

      every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns null

      val result = service.createChangeRequest(placementRequest.id, cas1NewChangeRequest)

      assertThatCasResult(result).isNotFound("Placement Request", placementRequest.id)
    }

    @Test
    fun `throws not found error when change request reason is not found`() {
      val placementRequest = PlacementRequestEntityFactory().withDefaults().produce()
      val cas1NewChangeRequest = Cas1NewChangeRequestFactory().produce()

      every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { cas1ChangeRequestReasonRepository.findByIdOrNull(cas1NewChangeRequest.reasonId) } returns null

      val result = service.createChangeRequest(placementRequest.id, cas1NewChangeRequest)

      assertThatCasResult(result).isNotFound("Change Request Reason", cas1NewChangeRequest.reasonId)
    }

    @Test
    fun `throws not found error when space booking is not found`() {
      val placementRequest = PlacementRequestEntityFactory().withDefaults().produce()
      val cas1ChangeRequestReason = Cas1ChangeRequestReasonEntityFactory().produce()
      val cas1SpaceBooking = Cas1SpaceBookingEntityFactory().produce()
      val cas1NewChangeRequest = Cas1NewChangeRequestFactory()
        .withReasonId(cas1ChangeRequestReason.id)
        .withSpaceBookingId(cas1SpaceBooking.id)
        .produce()

      every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { cas1ChangeRequestReasonRepository.findByIdOrNull(cas1ChangeRequestReason.id) } returns cas1ChangeRequestReason
      every { cas1SpaceBookingRepository.findByIdOrNull(cas1SpaceBooking.id) } returns null

      val result = service.createChangeRequest(placementRequest.id, cas1NewChangeRequest)

      assertThatCasResult(result).isNotFound("Space Booking", cas1SpaceBooking.id)
    }

    @Test
    fun `throws not found error when space booking is not associated with placement request`() {
      val placementRequest = PlacementRequestEntityFactory().withDefaults().produce()
      val cas1ChangeRequestReason = Cas1ChangeRequestReasonEntityFactory().produce()
      val cas1SpaceBooking = Cas1SpaceBookingEntityFactory().produce()
      val cas1NewChangeRequest = Cas1NewChangeRequestFactory()
        .withReasonId(cas1ChangeRequestReason.id)
        .withSpaceBookingId(cas1SpaceBooking.id)
        .produce()

      every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { cas1ChangeRequestReasonRepository.findByIdOrNull(cas1ChangeRequestReason.id) } returns cas1ChangeRequestReason
      every { cas1SpaceBookingRepository.findByIdOrNull(cas1SpaceBooking.id) } returns cas1SpaceBooking

      val result = service.createChangeRequest(placementRequest.id, cas1NewChangeRequest)

      assertThatCasResult(result).isNotFound("Placement Request with Space Booking", cas1SpaceBooking.id)
    }

    @Test
    fun `returns success for a valid appeal change request`() {
      val cas1SpaceBooking = Cas1SpaceBookingEntityFactory().produce()
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withSpaceBookings(mutableListOf(cas1SpaceBooking))
        .produce()
      val cas1ChangeRequestReason = Cas1ChangeRequestReasonEntityFactory().produce()

      val cas1NewChangeRequest = Cas1NewChangeRequestFactory()
        .withType(Cas1ChangeRequestType.APPEAL)
        .withReasonId(cas1ChangeRequestReason.id)
        .withSpaceBookingId(cas1SpaceBooking.id)
        .produce()

      every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { cas1ChangeRequestReasonRepository.findByIdOrNull(cas1ChangeRequestReason.id) } returns cas1ChangeRequestReason
      every { cas1SpaceBookingRepository.findByIdOrNull(cas1SpaceBooking.id) } returns cas1SpaceBooking
      every { objectMapper.writeValueAsString(cas1NewChangeRequest.requestJson) } returns "{test: 1}"
      every { cas1ChangeRequestRepository.save(any()) } returns null

      val result = service.createChangeRequest(placementRequest.id, cas1NewChangeRequest)

      assertThatCasResult(result).isSuccess()
    }

    @Test
    fun `returns success for a valid planned transfer change request`() {
      val cas1SpaceBooking = Cas1SpaceBookingEntityFactory()
        .withActualArrivalDate(LocalDate.now())
        .produce()
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withSpaceBookings(mutableListOf(cas1SpaceBooking))
        .produce()
      val cas1ChangeRequestReason = Cas1ChangeRequestReasonEntityFactory().produce()

      val cas1NewChangeRequest = Cas1NewChangeRequestFactory()
        .withType(Cas1ChangeRequestType.PLANNED_TRANSFER)
        .withReasonId(cas1ChangeRequestReason.id)
        .withSpaceBookingId(cas1SpaceBooking.id)
        .produce()

      every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { cas1ChangeRequestReasonRepository.findByIdOrNull(cas1ChangeRequestReason.id) } returns cas1ChangeRequestReason
      every { cas1SpaceBookingRepository.findByIdOrNull(cas1SpaceBooking.id) } returns cas1SpaceBooking
      every { objectMapper.writeValueAsString(cas1NewChangeRequest.requestJson) } returns "{test: 1}"
      every { cas1ChangeRequestRepository.save(any()) } returns null

      val result = service.createChangeRequest(placementRequest.id, cas1NewChangeRequest)

      assertThatCasResult(result).isSuccess()
    }

    @Test
    fun `throws general validation error when space booking has null actual arrival date`() {
      val cas1SpaceBooking = Cas1SpaceBookingEntityFactory().produce()
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withSpaceBookings(mutableListOf(cas1SpaceBooking))
        .produce()
      val cas1ChangeRequestReason = Cas1ChangeRequestReasonEntityFactory().produce()

      val cas1NewChangeRequest = Cas1NewChangeRequestFactory()
        .withType(Cas1ChangeRequestType.PLANNED_TRANSFER)
        .withReasonId(cas1ChangeRequestReason.id)
        .withSpaceBookingId(cas1SpaceBooking.id)
        .produce()

      every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { cas1ChangeRequestReasonRepository.findByIdOrNull(cas1ChangeRequestReason.id) } returns cas1ChangeRequestReason
      every { cas1SpaceBookingRepository.findByIdOrNull(cas1SpaceBooking.id) } returns cas1SpaceBooking
      every { objectMapper.writeValueAsString(cas1NewChangeRequest.requestJson) } returns "{test: 1}"
      every { cas1ChangeRequestRepository.save(any()) } returns null

      val result = service.createChangeRequest(placementRequest.id, cas1NewChangeRequest)

      assertThatCasResult(result).isGeneralValidationError("Associated space booking does not have an actual arrival date")
    }
  }

  @Nested
  inner class RejectChangeRequest {
    @Test
    fun `throw validation error when change request associated with different placement request`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .produce()
      val changeRequest = Cas1ChangeRequestEntityFactory().produce()

      val cas1RejectChangeRequest = Cas1RejectChangeRequest(rejectionReasonId = UUID.randomUUID(), decisionJson = emptyMap())

      every { lockableCas1ChangeRequestEntityRepository.acquirePessimisticLock(changeRequest.id) } returns LockableCas1ChangeRequestEntity(id = changeRequest.id)
      every { cas1ChangeRequestRepository.findByIdOrNull(any()) } returns changeRequest

      val result = service.rejectChangeRequest(placementRequest.id, changeRequest.id, cas1RejectChangeRequest)

      assertThatCasResult(result).isGeneralValidationError("The change request does not belong to the specified placement request")
    }

    @Test
    fun `return success when change request was already rejected`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .produce()
      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withPlacementRequest(placementRequest)
        .withDecision(ChangeRequestDecision.REJECTED)
        .produce()

      val cas1RejectChangeRequest = Cas1RejectChangeRequest(rejectionReasonId = UUID.randomUUID(), decisionJson = emptyMap())

      every { lockableCas1ChangeRequestEntityRepository.acquirePessimisticLock(changeRequest.id) } returns LockableCas1ChangeRequestEntity(id = changeRequest.id)
      every { cas1ChangeRequestRepository.findByIdOrNull(any()) } returns changeRequest
      every { cas1ChangeRequestRepository.saveAndFlush(any()) } returns changeRequest

      val result = service.rejectChangeRequest(placementRequest.id, changeRequest.id, cas1RejectChangeRequest)

      assertThatCasResult(result).isSuccess()
    }

    @Test
    fun `throw validation error when change request has a decision that is not rejected`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .produce()
      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withPlacementRequest(placementRequest)
        .withDecision(ChangeRequestDecision.APPROVED)
        .produce()

      val cas1RejectChangeRequest = Cas1RejectChangeRequest(rejectionReasonId = UUID.randomUUID(), decisionJson = emptyMap())

      every { lockableCas1ChangeRequestEntityRepository.acquirePessimisticLock(changeRequest.id) } returns LockableCas1ChangeRequestEntity(id = changeRequest.id)
      every { cas1ChangeRequestRepository.findByIdOrNull(any()) } returns changeRequest
      every { cas1ChangeRequestRepository.saveAndFlush(any()) } returns changeRequest

      val result = service.rejectChangeRequest(placementRequest.id, changeRequest.id, cas1RejectChangeRequest)

      assertThatCasResult(result).isGeneralValidationError("A decision has already been made for the change request")
    }

    @Test
    fun `return success for a valid change request`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .produce()
      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withPlacementRequest(placementRequest)
        .withDecision(null)
        .produce()

      val cas1RejectChangeRequest = Cas1RejectChangeRequest(rejectionReasonId = UUID.randomUUID(), decisionJson = emptyMap())

      every { lockableCas1ChangeRequestEntityRepository.acquirePessimisticLock(changeRequest.id) } returns LockableCas1ChangeRequestEntity(id = changeRequest.id)
      every { cas1ChangeRequestRepository.findByIdOrNull(any()) } returns changeRequest
      every { cas1ChangeRequestRejectionReasonRepository.findByIdAndArchivedIsFalse(any()) } returns Cas1ChangeRequestRejectionReasonEntityFactory().produce()
      every { cas1ChangeRequestRepository.saveAndFlush(any()) } returns changeRequest

      val result = service.rejectChangeRequest(placementRequest.id, changeRequest.id, cas1RejectChangeRequest)

      assertThatCasResult(result).isSuccess()
    }
  }
}
