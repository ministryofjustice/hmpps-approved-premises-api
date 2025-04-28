package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventCodedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealAcceptedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealRejectedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.DomainEventUtils.mapApprovedPremisesEntityToPremises
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

    cas1DomainEventService.savePlacementAppealCreated(
      domainEvent = SaveCas1DomainEvent(
        id = domainEventId,
        applicationId = changeRequest.placementRequest.application.id,
        crn = changeRequest.placementRequest.application.crn,
        nomsNumber = changeRequest.placementRequest.application.nomsNumber,
        occurredAt = OffsetDateTime.now().toInstant(),
        cas1SpaceBookingId = spaceBooking.id,
        bookingId = null,
        schemaVersion = null,
        data = PlacementAppealCreatedEnvelope(
          id = domainEventId,
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.placementAppealCreated,
          eventDetails = PlacementAppealCreated(
            bookingId = spaceBooking.id,
            premises = mapApprovedPremisesEntityToPremises(spaceBooking.premises),
            arrivalOn = spaceBooking.expectedArrivalDate,
            departureOn = spaceBooking.expectedDepartureDate,
            requestedBy = getStaffDetailsByUsername(requestingUser.deliusUsername).toStaffMember(),
            appealReason = Cas1DomainEventCodedId(
              id = reason.id,
              code = reason.code,
            ),
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

    cas1DomainEventService.savePlacementAppealRejected(
      domainEvent = SaveCas1DomainEvent(
        id = domainEventId,
        applicationId = changeRequest.placementRequest.application.id,
        crn = changeRequest.placementRequest.application.crn,
        nomsNumber = changeRequest.placementRequest.application.nomsNumber,
        occurredAt = OffsetDateTime.now().toInstant(),
        cas1SpaceBookingId = spaceBooking.id,
        bookingId = null,
        schemaVersion = null,
        data = PlacementAppealRejectedEnvelope(
          id = domainEventId,
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.placementAppealRejected,
          eventDetails = PlacementAppealRejected(
            bookingId = spaceBooking.id,
            premises = mapApprovedPremisesEntityToPremises(spaceBooking.premises),
            arrivalOn = spaceBooking.expectedArrivalDate,
            departureOn = spaceBooking.expectedDepartureDate,
            rejectedBy = getStaffDetailsByUsername(rejectingUser.deliusUsername).toStaffMember(),
            rejectionReason = Cas1DomainEventCodedId(
              id = reason.id,
              code = reason.code,
            ),
          ),
        ),
      ),
    )
  }

  private fun getStaffDetailsByUsername(deliusUsername: String) = when (val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(deliusUsername)) {
    is ClientResult.Success -> staffDetailsResult.body
    is ClientResult.Failure -> staffDetailsResult.throwException()
  }
}
