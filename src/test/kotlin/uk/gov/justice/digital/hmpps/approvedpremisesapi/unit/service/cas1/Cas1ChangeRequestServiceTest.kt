package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1RejectChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestRejectionReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1NewChangeRequestFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.LockableCas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.LockableCas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.ActionsResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingActionsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1ChangeRequestServiceTest {
  @MockK
  lateinit var placementRequestRepository: PlacementRequestRepository

  @MockK
  lateinit var cas1ChangeRequestReasonRepository: Cas1ChangeRequestReasonRepository

  @MockK
  lateinit var cas1SpaceBookingRepository: Cas1SpaceBookingRepository

  @MockK
  lateinit var objectMapper: ObjectMapper

  @MockK
  lateinit var cas1ChangeRequestRepository: Cas1ChangeRequestRepository

  @MockK
  lateinit var cas1ChangeRequestRejectionReasonRepository: Cas1ChangeRequestRejectionReasonRepository

  @MockK
  lateinit var lockableCas1ChangeRequestEntityRepository: LockableCas1ChangeRequestRepository

  @MockK
  lateinit var cas1ChangeRequestEmailService: Cas1ChangeRequestEmailService

  @MockK
  lateinit var cas1ChangeRequestDomainEventService: Cas1ChangeRequestDomainEventService

  @MockK
  lateinit var userService: UserService

  @MockK
  lateinit var cas1SpaceBookingActionsService: Cas1SpaceBookingActionsService

  @InjectMockKs
  lateinit var service: Cas1ChangeRequestService

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
    fun `returns success for a valid placement appeal change request`() {
      val cas1SpaceBooking = Cas1SpaceBookingEntityFactory().produce()
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withSpaceBookings(mutableListOf(cas1SpaceBooking))
        .produce()
      val cas1ChangeRequestReason = Cas1ChangeRequestReasonEntityFactory().produce()

      val cas1NewChangeRequest = Cas1NewChangeRequestFactory()
        .withType(Cas1ChangeRequestType.PLACEMENT_APPEAL)
        .withReasonId(cas1ChangeRequestReason.id)
        .withSpaceBookingId(cas1SpaceBooking.id)
        .produce()

      every { userService.getUserForRequest() } returns UserEntityFactory().withDefaults().produce()
      every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { cas1ChangeRequestReasonRepository.findByIdOrNull(cas1ChangeRequestReason.id) } returns cas1ChangeRequestReason
      every { cas1SpaceBookingRepository.findByIdOrNull(cas1SpaceBooking.id) } returns cas1SpaceBooking
      every { cas1SpaceBookingActionsService.determineActions(cas1SpaceBooking) } returns ActionsResult.forAllowedAction(SpaceBookingAction.APPEAL_CREATE)
      every { objectMapper.writeValueAsString(cas1NewChangeRequest.requestJson) } returns "{test: 1}"
      every { cas1ChangeRequestRepository.save(any()) } returnsArgument 0
      every { cas1ChangeRequestEmailService.placementAppealCreated(any()) } returns Unit
      every { cas1ChangeRequestDomainEventService.placementAppealCreated(any(), any()) } returns Unit

      val result = service.createChangeRequest(placementRequest.id, cas1NewChangeRequest)

      assertThatCasResult(result).isSuccess()

      val savedChangeRequest = getSavedChangeRequest()
      assertThat(savedChangeRequest.type).isEqualTo(ChangeRequestType.PLACEMENT_APPEAL)
      assertThat(savedChangeRequest.resolved).isEqualTo(false)

      verify {
        cas1ChangeRequestEmailService.placementAppealCreated(
          changeRequest = savedChangeRequest,
        )
      }

      verify {
        cas1ChangeRequestDomainEventService.placementAppealCreated(
          changeRequest = savedChangeRequest,
          requestingUser = any(),
        )
      }
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

      every { userService.getUserForRequest() } returns UserEntityFactory().withDefaults().produce()
      every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { cas1ChangeRequestReasonRepository.findByIdOrNull(cas1ChangeRequestReason.id) } returns cas1ChangeRequestReason
      every { cas1SpaceBookingRepository.findByIdOrNull(cas1SpaceBooking.id) } returns cas1SpaceBooking
      every { cas1SpaceBookingActionsService.determineActions(cas1SpaceBooking) } returns ActionsResult.forAllowedAction(SpaceBookingAction.PLANNED_TRANSFER_REQUEST)
      every { objectMapper.writeValueAsString(cas1NewChangeRequest.requestJson) } returns "{test: 1}"
      every { cas1ChangeRequestRepository.save(any()) } returnsArgument 0
      every { cas1ChangeRequestDomainEventService.plannedTransferRequestCreated(any(), any()) } returns Unit

      val result = service.createChangeRequest(placementRequest.id, cas1NewChangeRequest)

      assertThatCasResult(result).isSuccess()

      val savedChangeRequest = getSavedChangeRequest()
      assertThat(savedChangeRequest.type).isEqualTo(ChangeRequestType.PLANNED_TRANSFER)
      assertThat(savedChangeRequest.resolved).isEqualTo(false)

      verify {
        cas1ChangeRequestDomainEventService.plannedTransferRequestCreated(
          changeRequest = savedChangeRequest,
          requestingUser = any(),
        )
      }
    }

    @Test
    fun `throws general validation error when action not allowed for placement appeal`() {
      val cas1SpaceBooking = Cas1SpaceBookingEntityFactory()
        .withActualArrivalDate(LocalDate.now())
        .produce()
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withSpaceBookings(mutableListOf(cas1SpaceBooking))
        .produce()
      val cas1ChangeRequestReason = Cas1ChangeRequestReasonEntityFactory().produce()

      val cas1NewChangeRequest = Cas1NewChangeRequestFactory()
        .withType(Cas1ChangeRequestType.PLACEMENT_APPEAL)
        .withReasonId(cas1ChangeRequestReason.id)
        .withSpaceBookingId(cas1SpaceBooking.id)
        .produce()

      every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { cas1ChangeRequestReasonRepository.findByIdOrNull(cas1ChangeRequestReason.id) } returns cas1ChangeRequestReason
      every { cas1SpaceBookingRepository.findByIdOrNull(cas1SpaceBooking.id) } returns cas1SpaceBooking
      every {
        cas1SpaceBookingActionsService.determineActions(cas1SpaceBooking)
      } returns ActionsResult.forUnavailableAction(SpaceBookingAction.APPEAL_CREATE, "appeal create not allowed!")
      every { objectMapper.writeValueAsString(cas1NewChangeRequest.requestJson) } returns "{test: 1}"
      every { cas1ChangeRequestRepository.save(any()) } returns null

      val result = service.createChangeRequest(placementRequest.id, cas1NewChangeRequest)

      assertThatCasResult(result).isGeneralValidationError("appeal create not allowed!")
    }

    @Test
    fun `throws general validation error when action not allowed for planned transfer`() {
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
      every { cas1SpaceBookingActionsService.determineActions(cas1SpaceBooking) } returns ActionsResult.forUnavailableAction(SpaceBookingAction.PLANNED_TRANSFER_REQUEST, "transfer not allowed")
      every { objectMapper.writeValueAsString(cas1NewChangeRequest.requestJson) } returns "{test: 1}"
      every { cas1ChangeRequestRepository.save(any()) } returns null

      val result = service.createChangeRequest(placementRequest.id, cas1NewChangeRequest)

      assertThatCasResult(result).isGeneralValidationError("transfer not allowed")
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
    fun `return success for a placement appeal`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .produce()
      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withType(ChangeRequestType.PLACEMENT_APPEAL)
        .withPlacementRequest(placementRequest)
        .withDecision(null)
        .produce()

      val cas1RejectChangeRequest = Cas1RejectChangeRequest(rejectionReasonId = UUID.randomUUID(), decisionJson = emptyMap())

      every { lockableCas1ChangeRequestEntityRepository.acquirePessimisticLock(changeRequest.id) } returns LockableCas1ChangeRequestEntity(id = changeRequest.id)
      every { cas1ChangeRequestRepository.findByIdOrNull(any()) } returns changeRequest

      val rejectionReason = Cas1ChangeRequestRejectionReasonEntityFactory().produce()
      every { cas1ChangeRequestRejectionReasonRepository.findByIdAndArchivedIsFalse(any()) } returns rejectionReason
      every { cas1ChangeRequestRepository.save(any()) } returns changeRequest
      every { cas1ChangeRequestEmailService.placementAppealRejected(any()) } returns Unit

      every { userService.getUserForRequest() } returns UserEntityFactory().withDefaults().produce()
      every { cas1ChangeRequestDomainEventService.placementAppealRejected(any(), any()) } returns Unit

      val result = service.rejectChangeRequest(placementRequest.id, changeRequest.id, cas1RejectChangeRequest)

      assertThatCasResult(result).isSuccess()

      val savedChangeRequest = getSavedChangeRequest()
      assertThat(savedChangeRequest.decision).isEqualTo(ChangeRequestDecision.REJECTED)
      assertThat(savedChangeRequest.rejectionReason).isEqualTo(rejectionReason)
      assertThat(savedChangeRequest.resolved).isTrue()
      assertThat(savedChangeRequest.resolvedAt).isNotNull()

      verify { cas1ChangeRequestEmailService.placementAppealRejected(savedChangeRequest) }

      verify {
        cas1ChangeRequestDomainEventService.placementAppealRejected(
          changeRequest = savedChangeRequest,
          rejectingUser = any(),
        )
      }
    }

    @Test
    fun `return success for a planned transfer`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .produce()
      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withType(ChangeRequestType.PLANNED_TRANSFER)
        .withPlacementRequest(placementRequest)
        .withDecision(null)
        .produce()

      val cas1RejectChangeRequest = Cas1RejectChangeRequest(rejectionReasonId = UUID.randomUUID(), decisionJson = emptyMap())

      every { lockableCas1ChangeRequestEntityRepository.acquirePessimisticLock(changeRequest.id) } returns LockableCas1ChangeRequestEntity(id = changeRequest.id)
      every { cas1ChangeRequestRepository.findByIdOrNull(any()) } returns changeRequest

      val rejectionReason = Cas1ChangeRequestRejectionReasonEntityFactory().produce()
      every { cas1ChangeRequestRejectionReasonRepository.findByIdAndArchivedIsFalse(any()) } returns rejectionReason
      every { cas1ChangeRequestRepository.save(any()) } returns changeRequest

      every { userService.getUserForRequest() } returns UserEntityFactory().withDefaults().produce()
      every { cas1ChangeRequestDomainEventService.plannedTransferRequestRejected(any(), any()) } returns Unit

      val result = service.rejectChangeRequest(placementRequest.id, changeRequest.id, cas1RejectChangeRequest)

      assertThatCasResult(result).isSuccess()

      val savedChangeRequest = getSavedChangeRequest()
      assertThat(savedChangeRequest.decision).isEqualTo(ChangeRequestDecision.REJECTED)
      assertThat(savedChangeRequest.rejectionReason).isEqualTo(rejectionReason)
      assertThat(savedChangeRequest.resolved).isTrue()
      assertThat(savedChangeRequest.resolvedAt).isNotNull()

      verify {
        cas1ChangeRequestDomainEventService.plannedTransferRequestRejected(
          changeRequest = savedChangeRequest,
          rejectingUser = any(),
        )
      }
    }
  }

  @Nested
  inner class ApprovePlacementAppeal {

    @Test
    fun `throw validation error when change request is not found`() {
      val user = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      val changeRequestId = UUID.randomUUID()

      every { cas1ChangeRequestRepository.findByIdOrNull(changeRequestId) } returns null

      val result = service.approvePlacementAppeal(changeRequestId, user)

      assertThatCasResult(result).isNotFound("change request", changeRequestId)
    }

    @Test
    fun `throw validation error when change request is already resolved`() {
      val user = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withDecision(ChangeRequestDecision.APPROVED)
        .withResolved(true)
        .produce()

      every { cas1ChangeRequestRepository.findByIdOrNull(changeRequest.id) } returns changeRequest

      val result = service.approvePlacementAppeal(changeRequest.id, user)

      assertThatCasResult(result).isGeneralValidationError("This change request is already resolved")
    }

    @Test
    fun `return success for valid approval`() {
      val user = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      val changeRequest = Cas1ChangeRequestEntityFactory()
        .withDecision(ChangeRequestDecision.REJECTED)
        .produce()

      every { cas1ChangeRequestRepository.findByIdOrNull(changeRequest.id) } returns changeRequest
      every { cas1ChangeRequestRepository.saveAndFlush(any()) } returns changeRequest
      every { cas1ChangeRequestDomainEventService.placementAppealAccepted(any()) } returns Unit
      every { cas1ChangeRequestEmailService.placementAppealAccepted(any()) } returns Unit

      val result = service.approvePlacementAppeal(changeRequest.id, user)

      assertThatCasResult(result).isSuccess()

      verify { cas1ChangeRequestEmailService.placementAppealAccepted(changeRequest) }
    }
  }

  @Nested
  inner class GetChangeRequest {

    @Test
    fun `throw not found error if change request with the given ID doesn't exist`() {
      val placementRequest = PlacementRequestEntityFactory().withDefaults().produce()
      every { cas1ChangeRequestRepository.findByIdOrNull(any()) } returns null

      val result = service.getChangeRequest(placementRequest.id, UUID.randomUUID())

      assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
      assertThat((result as CasResult.NotFound).entityType).isEqualTo("Change Request")
    }

    @Test
    fun `throw validation error when change request associated with different placement request`() {
      val placementRequest = PlacementRequestEntityFactory().withDefaults().produce()
      val cas1ChangeRequest = Cas1ChangeRequestEntityFactory().withPlacementRequest(placementRequest).produce()

      every { cas1ChangeRequestRepository.findByIdOrNull(any()) } returns cas1ChangeRequest

      val result = service.getChangeRequest(UUID.randomUUID(), cas1ChangeRequest.id)

      assertThat(result).isInstanceOf(CasResult.GeneralValidationError::class.java)
      assertThatCasResult(result).isGeneralValidationError("The change request does not belong to the specified placement request")
    }

    @Test
    fun `returns success for a valid change request id`() {
      val placementRequest = PlacementRequestEntityFactory().withDefaults().produce()
      val cas1ChangeRequest = Cas1ChangeRequestEntityFactory().withPlacementRequest(placementRequest).produce()

      every { cas1ChangeRequestRepository.findByIdOrNull(any()) } returns cas1ChangeRequest

      val result = service.getChangeRequest(placementRequest.id, cas1ChangeRequest.id)

      assertThatCasResult(result).isSuccess()

      assertThat((result as CasResult.Success).value).isEqualTo(cas1ChangeRequest)
    }
  }

  private fun getSavedChangeRequest(): Cas1ChangeRequestEntity {
    val savedChangeRequestCaptor = slot<Cas1ChangeRequestEntity>()
    verify {
      cas1ChangeRequestRepository.save(
        capture(savedChangeRequestCaptor),
      )
    }
    return savedChangeRequestCaptor.captured
  }
}
