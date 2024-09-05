package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.util.UUID

@Configuration
@ConfigurationProperties(prefix = "url-templates.api")
class DomainEventUrlConfig {
  lateinit var cas1: Map<String, UrlTemplate>
  lateinit var cas2: Map<String, UrlTemplate>
  lateinit var cas3: Map<String, UrlTemplate>

  fun getUrlForDomainEventId(domainEventType: DomainEventType, eventId: UUID): String {
    val template = when (domainEventType) {
      DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED -> cas1["application-submitted-event-detail"]
      DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED -> cas1["application-assessed-event-detail"]
      DomainEventType.APPROVED_PREMISES_BOOKING_MADE -> cas1["booking-made-event-detail"]
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> cas1["person-arrived-event-detail"]
      DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED -> cas1["person-not-arrived-event-detail"]
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> cas1["person-departed-event-detail"]
      DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE -> cas1["booking-not-made-event-detail"]
      DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED -> cas1["booking-cancelled-event-detail"]
      DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED -> cas1["booking-changed-event-detail"]
      DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN -> cas1["application-withdrawn-event-detail"]
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED -> cas1["assessment-appealed-event-detail"]
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED -> cas1["assessment-allocated-event-detail"]
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN -> cas1["placement-application-withdrawn-event-detail"]
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED -> cas1["placement-application-allocated-event-detail"]
      DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN -> cas1["match-request-withdrawn-event-detail"]
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED -> cas1["request-for-placement-created-event-detail"]
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED -> cas1["request-for-placement-assessed-event-detail"]
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED -> cas1["assessment-info-requested-detail"]
      DomainEventType.CAS2_APPLICATION_SUBMITTED -> cas2["application-submitted-event-detail"]
      DomainEventType.CAS2_APPLICATION_STATUS_UPDATED -> cas2["application-status-updated-event-detail"]
      DomainEventType.CAS3_BOOKING_CANCELLED -> cas3["booking-cancelled-event-detail"]
      DomainEventType.CAS3_BOOKING_CONFIRMED -> cas3["booking-confirmed-event-detail"]
      DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE -> cas3["booking-provisionally-made-event-detail"]
      DomainEventType.CAS3_PERSON_ARRIVED -> cas3["person-arrived-event-detail"]
      DomainEventType.CAS3_PERSON_ARRIVED_UPDATED -> cas3["person-arrived-updated-event-detail"]
      DomainEventType.CAS3_PERSON_DEPARTED -> cas3["person-departed-event-detail"]
      DomainEventType.CAS3_REFERRAL_SUBMITTED -> cas3["referral-submitted-event-detail"]
      DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED -> cas3["person-departure-updated-event-detail"]
      DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED -> cas3["booking-cancelled-updated-event-detail"]
      DomainEventType.CAS3_ASSESSMENT_UPDATED -> throw IllegalArgumentException("Don't emit for $domainEventType")
    } ?: throw IllegalStateException("Missing URL for $domainEventType")

    return template.resolve("eventId", eventId.toString())
  }
}
