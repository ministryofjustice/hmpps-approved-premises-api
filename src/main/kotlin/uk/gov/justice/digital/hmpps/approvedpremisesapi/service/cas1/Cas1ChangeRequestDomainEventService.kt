package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventCodedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealAcceptedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.DomainEventUtils.mapApprovedPremisesEntityToPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.toEventBookingSummary
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1ChangeRequestDomainEventService(
  private val cas1DomainEventService: Cas1DomainEventService,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
) {

  fun placementAppealAccepted(
    changeRequest: Cas1ChangeRequestEntity,
  ) {
    val domainEventId = UUID.randomUUID()

    val spaceBooking = changeRequest.spaceBooking

    cas1DomainEventService.savePlacementAppealAccepted(
      domainEvent = SaveCas1DomainEvent(
        id = domainEventId,
        applicationId = changeRequest.placementRequest.application.id,
        crn = changeRequest.placementRequest.application.crn,
        nomsNumber = changeRequest.placementRequest.application.nomsNumber,
        occurredAt = OffsetDateTime.now().toInstant(),
        cas1SpaceBookingId = spaceBooking.id,
        bookingId = null,
        schemaVersion = null,
        data = PlacementAppealAcceptedEnvelope(
          id = domainEventId,
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.placementAppealAccepted,
          eventDetails = PlacementAppealAccepted(
            bookingId = spaceBooking.id,
            premises = mapApprovedPremisesEntityToPremises(spaceBooking.premises),
            arrivalOn = spaceBooking.expectedArrivalDate,
            departureOn = spaceBooking.expectedDepartureDate,
            acceptedBy = getStaffDetailsByUsername(changeRequest.decisionMadeByUser!!.deliusUsername).toStaffMember(),
          ),
        ),
      ),
    )
  }

  fun placementAppealCreated(
    changeRequest: Cas1ChangeRequestEntity,
    requestingUser: UserEntity,
  ) {
    val domainEventId = UUID.randomUUID()

    val spaceBooking = changeRequest.spaceBooking
    val reason = changeRequest.requestReason

    cas1DomainEventService.save(
      SaveCas1DomainEventWithPayload(
        id = domainEventId,
        type = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPEAL_CREATED,
        applicationId = changeRequest.placementRequest.application.id,
        crn = changeRequest.placementRequest.application.crn,
        nomsNumber = changeRequest.placementRequest.application.nomsNumber,
        occurredAt = OffsetDateTime.now().toInstant(),
        cas1SpaceBookingId = spaceBooking.id,
        bookingId = null,
        schemaVersion = null,
        data = PlacementAppealCreated(
          booking = spaceBooking.toEventBookingSummary(),
          requestedBy = getStaffDetailsByUsername(requestingUser.deliusUsername).toStaffMember(),
          reason = Cas1DomainEventCodedId(
            id = reason.id,
            code = reason.code,
          ),
        ),
      ),
    )
  }

  fun placementAppealRejected(
    changeRequest: Cas1ChangeRequestEntity,
    rejectingUser: UserEntity,
  ) {
    val domainEventId = UUID.randomUUID()

    val spaceBooking = changeRequest.spaceBooking
    val reason = changeRequest.rejectionReason!!

    cas1DomainEventService.save(
      SaveCas1DomainEventWithPayload(
        id = domainEventId,
        type = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPEAL_REJECTED,
        applicationId = changeRequest.placementRequest.application.id,
        crn = changeRequest.placementRequest.application.crn,
        nomsNumber = changeRequest.placementRequest.application.nomsNumber,
        occurredAt = OffsetDateTime.now().toInstant(),
        cas1SpaceBookingId = spaceBooking.id,
        bookingId = null,
        schemaVersion = null,
        data = PlacementAppealRejected(
          booking = spaceBooking.toEventBookingSummary(),
          rejectedBy = getStaffDetailsByUsername(rejectingUser.deliusUsername).toStaffMember(),
          reason = Cas1DomainEventCodedId(
            id = reason.id,
            code = reason.code,
          ),
        ),
      ),
    )
  }

  fun plannedTransferRequestCreated(
    changeRequest: Cas1ChangeRequestEntity,
    requestingUser: UserEntity,
  ) {
    val spaceBooking = changeRequest.spaceBooking
    val reason = changeRequest.requestReason

    save(
      DomainEventType.APPROVED_PREMISES_PLANNED_TRANSFER_REQUEST_CREATED,
      changeRequest,
      PlannedTransferRequestCreated(
        changeRequestId = changeRequest.id,
        booking = spaceBooking.toEventBookingSummary(),
        requestedBy = getStaffDetailsByUsername(requestingUser.deliusUsername).toStaffMember(),
        reason = Cas1DomainEventCodedId(
          id = reason.id,
          code = reason.code,
        ),
      ),
    )
  }

  fun plannedTransferRequestRejected(
    changeRequest: Cas1ChangeRequestEntity,
    rejectingUser: UserEntity,
  ) {
    val spaceBooking = changeRequest.spaceBooking
    val reason = changeRequest.rejectionReason!!

    save(
      DomainEventType.APPROVED_PREMISES_PLANNED_TRANSFER_REQUEST_REJECTED,
      changeRequest,
      PlannedTransferRequestRejected(
        changeRequestId = changeRequest.id,
        booking = spaceBooking.toEventBookingSummary(),
        rejectedBy = getStaffDetailsByUsername(rejectingUser.deliusUsername).toStaffMember(),
        reason = Cas1DomainEventCodedId(
          id = reason.id,
          code = reason.code,
        ),
      ),
    )
  }

  fun plannedTransferRequestAccepted(
    changeRequest: Cas1ChangeRequestEntity,
    acceptingUser: UserEntity,
    from: Cas1SpaceBookingEntity,
    to: Cas1SpaceBookingEntity,
  ) {
    save(
      DomainEventType.APPROVED_PREMISES_PLANNED_TRANSFER_REQUEST_ACCEPTED,
      changeRequest,
      PlannedTransferRequestAccepted(
        changeRequestId = changeRequest.id,
        acceptedBy = getStaffDetailsByUsername(acceptingUser.deliusUsername).toStaffMember(),
        from = from.toEventBookingSummary(),
        to = to.toEventBookingSummary(),
      ),
    )
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
