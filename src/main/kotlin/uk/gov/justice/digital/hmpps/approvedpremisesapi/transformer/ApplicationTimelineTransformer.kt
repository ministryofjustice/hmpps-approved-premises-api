package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventAssociatedUrl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventUrlType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.domainevents.DomainEventDescriber
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Component
class ApplicationTimelineTransformer(
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.assessment}") private val assessmentUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.booking}") private val bookingUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-appeal}") private val appealUrlTemplate: UrlTemplate,
  private val domainEventDescriber: DomainEventDescriber,
  private val userTransformer: UserTransformer,
) {

  fun transformDomainEventSummaryToTimelineEvent(domainEventSummary: DomainEventSummary): TimelineEvent {
    val associatedUrls = generateUrlsForTimelineEventType(domainEventSummary)

    return TimelineEvent(
      id = domainEventSummary.id,
      type = transformDomainEventTypeToTimelineEventType(domainEventSummary.type),
      occurredAt = domainEventSummary.occurredAt.toInstant(),
      associatedUrls = associatedUrls,
      content = domainEventDescriber.getDescription(domainEventSummary),
      createdBy = domainEventSummary.triggeredByUser?.let { userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) },
    )
  }

  private fun appealUrlOrNull(domainEventSummary: DomainEventSummary): TimelineEventAssociatedUrl? {
    return if (domainEventSummary.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED && domainEventSummary.appealId !== null) {
      TimelineEventAssociatedUrl(
        TimelineEventUrlType.assessmentAppeal,
        appealUrlTemplate.resolve(
          mapOf(
            "applicationId" to domainEventSummary.applicationId.toString(),
            "appealId" to domainEventSummary.appealId.toString(),
          ),
        ),
      )
    } else {
      null
    }
  }

  private fun applicationUrlOrNull(domainEventSummary: DomainEventSummary) = domainEventSummary.applicationId?.let {
    TimelineEventAssociatedUrl(
      TimelineEventUrlType.application,
      applicationUrlTemplate.resolve(mapOf("id" to domainEventSummary.applicationId.toString())),
    )
  }

  private fun assessmentUrlOrNull(domainEventSummary: DomainEventSummary) = domainEventSummary.assessmentId?.let {
    TimelineEventAssociatedUrl(
      TimelineEventUrlType.assessment,
      assessmentUrlTemplate.resolve(mapOf("id" to domainEventSummary.assessmentId.toString())),
    )
  }

  private fun bookingUrlOrNull(domainEventSummary: DomainEventSummary) = domainEventSummary.bookingId?.let {
    domainEventSummary.premisesId?.let {
      TimelineEventAssociatedUrl(
        TimelineEventUrlType.booking,
        bookingUrlTemplate.resolve(
          mapOf(
            "premisesId" to domainEventSummary.premisesId.toString(),
            "bookingId" to domainEventSummary.bookingId.toString(),
          ),
        ),
      )
    }
  }

  fun generateUrlsForTimelineEventType(domainEventSummary: DomainEventSummary): List<TimelineEventAssociatedUrl> {
    return if (domainEventSummary.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED) {
      listOfNotNull(
        appealUrlOrNull(domainEventSummary),
      )
    } else {
      listOfNotNull(
        applicationUrlOrNull(domainEventSummary),
        assessmentUrlOrNull(domainEventSummary),
        bookingUrlOrNull(domainEventSummary),
      )
    }
  }

  @SuppressWarnings("CyclomaticComplexMethod")
  fun transformDomainEventTypeToTimelineEventType(domainEventType: DomainEventType): TimelineEventType {
    return when (domainEventType) {
      DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED -> TimelineEventType.approvedPremisesApplicationSubmitted
      DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED -> TimelineEventType.approvedPremisesApplicationAssessed
      DomainEventType.APPROVED_PREMISES_BOOKING_MADE -> TimelineEventType.approvedPremisesBookingMade
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> TimelineEventType.approvedPremisesPersonArrived
      DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED -> TimelineEventType.approvedPremisesPersonNotArrived
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> TimelineEventType.approvedPremisesPersonDeparted
      DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE -> TimelineEventType.approvedPremisesBookingNotMade
      DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED -> TimelineEventType.approvedPremisesBookingCancelled
      DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED -> TimelineEventType.approvedPremisesBookingChanged
      DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN -> TimelineEventType.approvedPremisesApplicationWithdrawn
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED -> TimelineEventType.approvedPremisesAssessmentAppealed
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED -> TimelineEventType.approvedPremisesAssessmentAllocated
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN -> TimelineEventType.approvedPremisesPlacementApplicationWithdrawn
      DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN -> TimelineEventType.approvedPremisesMatchRequestWithdrawn
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED -> TimelineEventType.approvedPremisesRequestForPlacementCreated
      else -> throw IllegalArgumentException("Cannot map $domainEventType, only CAS1 is currently supported")
    }
  }
}
