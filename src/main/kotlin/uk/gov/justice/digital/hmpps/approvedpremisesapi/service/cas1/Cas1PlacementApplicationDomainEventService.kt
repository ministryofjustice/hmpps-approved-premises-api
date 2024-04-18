package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.Instant
import java.util.UUID

@Component
class Cas1PlacementApplicationDomainEventService(
  private val domainEventService: DomainEventService,
  private val domainEventTransformer: DomainEventTransformer,
  private val communityApiClient: CommunityApiClient,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
) {

  fun placementApplicationSubmitted(
    placementApplication: PlacementApplicationEntity,
    username: String,
  ) {
    checkNotNull(placementApplication.placementType)
    require(placementApplication.placementDates.size == 1)

    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now()
    val application = placementApplication.application
    val dates = placementApplication.placementDates[0]

    val placementType = when (placementApplication.placementType!!) {
      PlacementType.ROTL -> RequestForPlacementType.rotl
      PlacementType.RELEASE_FOLLOWING_DECISION -> RequestForPlacementType.releaseFollowingDecisions
      PlacementType.ADDITIONAL_PLACEMENT -> RequestForPlacementType.additionalPlacement
    }

    val staffDetails = when (val staffDetailsResult = communityApiClient.getStaffUserDetails(username)) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    val eventDetails = RequestForPlacementCreated(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
      requestForPlacementId = placementApplication.id,
      personReference = PersonReference(
        crn = application.crn,
        noms = application.nomsNumber ?: "Unknown NOMS Number",
      ),
      deliusEventNumber = application.eventNumber,
      createdAt = eventOccurredAt,
      createdBy = domainEventTransformer.toStaffMember(staffDetails),
      expectedArrival = dates.expectedArrival,
      duration = dates.duration,
      requestForPlacementType = placementType,
    )

    domainEventService.saveRequestForPlacementCreatedEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        occurredAt = eventOccurredAt,
        data = RequestForPlacementCreatedEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = "approved-premises.request-for-placement.created",
          eventDetails = eventDetails,
        ),
      ),
      emit = false,
    )
  }

  fun placementApplicationWithdrawn(placementApplication: PlacementApplicationEntity, withdrawalContext: WithdrawalContext) {
    val user = requireNotNull(withdrawalContext.triggeringUser)

    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now()
    val application = placementApplication.application

    val eventDetails = PlacementApplicationWithdrawn(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
      placementApplicationId = placementApplication.id,
      personReference = PersonReference(
        crn = application.crn,
        noms = application.nomsNumber ?: "Unknown NOMS Number",
      ),
      deliusEventNumber = application.eventNumber,
      withdrawnAt = eventOccurredAt,
      withdrawnBy = domainEventTransformer.toWithdrawnBy(user),
      withdrawalReason = placementApplication.withdrawalReason!!.name,
      placementDates = placementApplication.placementDates.map {
        DatePeriod(
          it.expectedArrival,
          it.expectedDeparture(),
        )
      },
    )

    domainEventService.savePlacementApplicationWithdrawnEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        occurredAt = eventOccurredAt,
        data = PlacementApplicationWithdrawnEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = "approved-premises.placement-application.withdrawn",
          eventDetails = eventDetails,
        ),
      ),
    )
  }

  fun placementApplicationAllocated(placementApplication: PlacementApplicationEntity, allocatedByUser: UserEntity?) {
    val allocatedAt = requireNotNull(placementApplication.allocatedAt)
    val allocatedToUser = requireNotNull(placementApplication.allocatedToUser)

    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now()
    val application = placementApplication.application

    val placementApplicationAllocated = PlacementApplicationAllocated(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
      placementApplicationId = placementApplication.id,
      personReference = PersonReference(
        crn = application.crn,
        noms = application.nomsNumber ?: "Unknown NOMS Number",
      ),
      allocatedAt = allocatedAt.toInstant(),
      allocatedTo = domainEventTransformer.toStaffMember(allocatedToUser),
      allocatedBy = allocatedByUser?.let { domainEventTransformer.toStaffMember(it) },
      placementDates = placementApplication.placementDates.map {
        DatePeriod(
          it.expectedArrival,
          it.expectedDeparture(),
        )
      },
    )

    domainEventService.savePlacementApplicationAllocatedEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        occurredAt = eventOccurredAt,
        data = PlacementApplicationAllocatedEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = "approved-premises.placement-application.allocated",
          eventDetails = placementApplicationAllocated,
        ),
      ),
    )
  }
}
