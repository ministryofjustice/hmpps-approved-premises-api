package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventAssociatedUrl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventUrlType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventDescriber
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Component
class Cas1ApplicationTimelineTransformer(
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.assessment}") private val assessmentUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.cas1.space-booking}") private val cas1SpaceBookingUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-appeal}") private val appealUrlTemplate: UrlTemplate,
  private val cas1DomainEventDescriber: Cas1DomainEventDescriber,
  private val userTransformer: UserTransformer,
) {

  fun transformDomainEventSummaryToTimelineEvent(domainEventSummary: DomainEventSummary): Cas1TimelineEvent {
    val associatedUrls = generateUrlsForTimelineEventType(domainEventSummary)
    val descriptionAndPayload = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    return Cas1TimelineEvent(
      id = domainEventSummary.id,
      type = domainEventSummary.type.cas1Info?.timelineEventType ?: throw IllegalArgumentException("Cannot map ${domainEventSummary.type}, only CAS1 is currently supported"),
      schemaVersion = domainEventSummary.schemaVersion,
      occurredAt = domainEventSummary.occurredAt.toInstant(),
      associatedUrls = associatedUrls,
      content = descriptionAndPayload.description,
      payload = descriptionAndPayload.payload,
      createdBySummary = domainEventSummary.triggeredByUser?.let { userTransformer.transformJpaToSummaryApi(it) },
      triggerSource = when (domainEventSummary.triggerSource) {
        uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType.USER -> Cas1TriggerSourceType.user
        uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType.SYSTEM -> Cas1TriggerSourceType.system
        null -> null
      },
    )
  }

  private fun appealUrlOrNull(domainEventSummary: DomainEventSummary): Cas1TimelineEventAssociatedUrl? = if (
    domainEventSummary.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED &&
    domainEventSummary.appealId !== null
  ) {
    Cas1TimelineEventAssociatedUrl(
      Cas1TimelineEventUrlType.assessmentAppeal,
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

  private fun applicationUrlOrNull(domainEventSummary: DomainEventSummary) = domainEventSummary.applicationId?.let {
    Cas1TimelineEventAssociatedUrl(
      Cas1TimelineEventUrlType.application,
      applicationUrlTemplate.resolve(mapOf("id" to domainEventSummary.applicationId.toString())),
    )
  }

  private fun assessmentUrlOrNull(domainEventSummary: DomainEventSummary) = domainEventSummary.assessmentId?.let {
    Cas1TimelineEventAssociatedUrl(
      Cas1TimelineEventUrlType.assessment,
      assessmentUrlTemplate.resolve(mapOf("id" to domainEventSummary.assessmentId.toString())),
    )
  }

  private fun cas1SpaceBookingUrlOrNull(domainEventSummary: DomainEventSummary) = domainEventSummary.cas1SpaceBookingId?.let {
    domainEventSummary.premisesId?.let {
      Cas1TimelineEventAssociatedUrl(
        Cas1TimelineEventUrlType.spaceBooking,
        cas1SpaceBookingUrlTemplate.resolve(
          mapOf(
            "premisesId" to domainEventSummary.premisesId.toString(),
            "bookingId" to domainEventSummary.cas1SpaceBookingId.toString(),
          ),
        ),
      )
    }
  }

  fun generateUrlsForTimelineEventType(domainEventSummary: DomainEventSummary): List<Cas1TimelineEventAssociatedUrl> = when (domainEventSummary.type) {
    DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED -> listOfNotNull(
      appealUrlOrNull(domainEventSummary),
    )
    DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED,
    DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
    -> listOfNotNull(
      applicationUrlOrNull(domainEventSummary),
    )
    else -> listOfNotNull(
      applicationUrlOrNull(domainEventSummary),
      assessmentUrlOrNull(domainEventSummary),
      cas1SpaceBookingUrlOrNull(domainEventSummary),
    )
  }
}
