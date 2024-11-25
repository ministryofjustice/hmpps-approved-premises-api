package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
* The type of an event
* Values: bookingCancelled,bookingCancelledUpdated,bookingConfirmed,bookingProvisionallyMade,personArrived,personArrivedUpdated,personDeparted,referralSubmitted,personDepartureUpdated,assessmentUpdated
*/
enum class EventType(val value: kotlin.String) {

  @JsonProperty("accommodation.cas3.booking.cancelled")
  bookingCancelled("accommodation.cas3.booking.cancelled"),

  @JsonProperty("accommodation.cas3.booking.cancelled.updated")
  bookingCancelledUpdated("accommodation.cas3.booking.cancelled.updated"),

  @JsonProperty("accommodation.cas3.booking.confirmed")
  bookingConfirmed("accommodation.cas3.booking.confirmed"),

  @JsonProperty("accommodation.cas3.booking.provisionally-made")
  bookingProvisionallyMade("accommodation.cas3.booking.provisionally-made"),

  @JsonProperty("accommodation.cas3.person.arrived")
  personArrived("accommodation.cas3.person.arrived"),

  @JsonProperty("accommodation.cas3.person.arrived.updated")
  personArrivedUpdated("accommodation.cas3.person.arrived.updated"),

  @JsonProperty("accommodation.cas3.person.departed")
  personDeparted("accommodation.cas3.person.departed"),

  @JsonProperty("accommodation.cas3.referral.submitted")
  referralSubmitted("accommodation.cas3.referral.submitted"),

  @JsonProperty("accommodation.cas3.person.departed.updated")
  personDepartureUpdated("accommodation.cas3.person.departed.updated"),

  @JsonProperty("accommodation.cas3.assessment.updated")
  assessmentUpdated("accommodation.cas3.assessment.updated"),
}
