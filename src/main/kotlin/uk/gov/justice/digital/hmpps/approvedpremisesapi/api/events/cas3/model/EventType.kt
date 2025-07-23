package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
* The type of an event
* Values: bookingCancelled,bookingCancelledUpdated,bookingConfirmed,bookingProvisionallyMade,personArrived,personArrivedUpdated,personDeparted,referralSubmitted,personDepartureUpdated,assessmentUpdated,draftReferralDeleted
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class EventType(@get:JsonValue val value: String) {

  bedspaceArchived("accommodation.cas3.bedspace.archived"),
  bedspaceUnarchived("accommodation.cas3.bedspace.unarchived"),
  bookingCancelled("accommodation.cas3.booking.cancelled"),
  bookingCancelledUpdated("accommodation.cas3.booking.cancelled.updated"),
  bookingConfirmed("accommodation.cas3.booking.confirmed"),
  bookingProvisionallyMade("accommodation.cas3.booking.provisionally-made"),
  personArrived("accommodation.cas3.person.arrived"),
  personArrivedUpdated("accommodation.cas3.person.arrived.updated"),
  personDeparted("accommodation.cas3.person.departed"),
  referralSubmitted("accommodation.cas3.referral.submitted"),
  personDepartureUpdated("accommodation.cas3.person.departed.updated"),
  assessmentUpdated("accommodation.cas3.assessment.updated"),
  draftReferralDeleted("accommodation.cas3.draft.referral.deleted"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): EventType = values().first { it -> it.value == value }
  }
}
