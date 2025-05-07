package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventCodedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.toEventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestRejected
import java.time.OffsetDateTime

@Service
class Cas1ChangeRequestDomainEventService(
  private val cas1DomainEventService: Cas1DomainEventService,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
) {
  @EventListener
  fun placementAppealAccepted(placementAppealAccepted: PlacementAppealAccepted) = saveChangeRequestAcceptedDomainEvent(placementAppealAccepted.changeRequest)

  @EventListener
  fun placementAppealCreated(placementAppealCreated: PlacementAppealCreated) = saveChangeRequestCreated(placementAppealCreated.changeRequest, placementAppealCreated.requestingUser)

  @EventListener
  fun placementAppealRejected(placementAppealRejected: PlacementAppealRejected) = saveChangeRequestRejected(placementAppealRejected.changeRequest, placementAppealRejected.rejectingUser)

  @EventListener
  fun plannedTransferRequestCreated(plannedTransferRequestCreated: PlannedTransferRequestCreated) = saveChangeRequestCreated(plannedTransferRequestCreated.changeRequest, plannedTransferRequestCreated.requestingUser)

  @EventListener
  fun plannedTransferRequestRejected(plannedTransferRequestRejected: PlannedTransferRequestRejected) = saveChangeRequestRejected(plannedTransferRequestRejected.changeRequest, plannedTransferRequestRejected.rejectingUser)

  @EventListener
  fun plannedTransferRequestAccepted(plannedTransferRequestAccepted: PlannedTransferRequestAccepted) = saveChangeRequestAcceptedDomainEvent(plannedTransferRequestAccepted.changeRequest)

  fun saveChangeRequestCreated(
    changeRequest: Cas1ChangeRequestEntity,
    requestingUser: UserEntity,
  ) {
    val reason = changeRequest.requestReason

    save(
      DomainEventType.APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_CREATED,
      changeRequest,
      PlacementChangeRequestCreated(
        changeRequestId = changeRequest.id,
        changeRequestType = changeRequest.domainEventType(),
        booking = changeRequest.spaceBooking.toEventBookingSummary(),
        requestedBy = getStaffDetailsByUsername(requestingUser.deliusUsername).toStaffMember(),
        reason = Cas1DomainEventCodedId(
          id = reason.id,
          code = reason.code,
        ),
      ),
    )
  }

  fun saveChangeRequestRejected(
    changeRequest: Cas1ChangeRequestEntity,
    rejectingUser: UserEntity,
  ) {
    val reason = changeRequest.rejectionReason!!

    save(
      DomainEventType.APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_REJECTED,
      changeRequest,
      PlacementChangeRequestRejected(
        changeRequestId = changeRequest.id,
        changeRequestType = changeRequest.domainEventType(),
        booking = changeRequest.spaceBooking.toEventBookingSummary(),
        rejectedBy = getStaffDetailsByUsername(rejectingUser.deliusUsername).toStaffMember(),
        reason = Cas1DomainEventCodedId(
          id = reason.id,
          code = reason.code,
        ),
      ),
    )
  }

  private fun saveChangeRequestAcceptedDomainEvent(
    changeRequest: Cas1ChangeRequestEntity,
  ) = save(
    DomainEventType.APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_ACCEPTED,
    changeRequest,
    PlacementChangeRequestAccepted(
      changeRequestId = changeRequest.id,
      changeRequestType = changeRequest.domainEventType(),
      booking = changeRequest.spaceBooking.toEventBookingSummary(),
      acceptedBy = getStaffDetailsByUsername(changeRequest.decisionMadeByUser!!.deliusUsername).toStaffMember(),
    ),
  )

  private fun Cas1ChangeRequestEntity.domainEventType() = when (this.type) {
    ChangeRequestType.PLACEMENT_APPEAL -> EventChangeRequestType.PLACEMENT_APPEAL
    ChangeRequestType.PLACEMENT_EXTENSION -> EventChangeRequestType.PLACEMENT_EXTENSION
    ChangeRequestType.PLANNED_TRANSFER -> EventChangeRequestType.PLANNED_TRANSFER
  }

  private fun save(
    type: DomainEventType,
    changeRequest: Cas1ChangeRequestEntity,
    payload: Cas1DomainEventPayload,
  ) {
    val spaceBooking = changeRequest.spaceBooking

    cas1DomainEventService.save(
      SaveCas1DomainEventWithPayload(
        type = type,
        applicationId = changeRequest.placementRequest.application.id,
        crn = changeRequest.placementRequest.application.crn,
        nomsNumber = changeRequest.placementRequest.application.nomsNumber,
        occurredAt = OffsetDateTime.now().toInstant(),
        cas1SpaceBookingId = spaceBooking.id,
        schemaVersion = null,
        data = payload,
      ),
    )
  }

  private fun getStaffDetailsByUsername(deliusUsername: String) = when (val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(deliusUsername)) {
    is ClientResult.Success -> staffDetailsResult.body
    is ClientResult.Failure -> staffDetailsResult.throwException()
  }
}
