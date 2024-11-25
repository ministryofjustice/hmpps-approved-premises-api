package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: application,booking,assessment,assessmentAppeal,cas1SpaceBooking
*/
enum class TimelineEventUrlType(val value: kotlin.String) {

  @JsonProperty("application")
  application("application"),

  @JsonProperty("booking")
  booking("booking"),

  @JsonProperty("assessment")
  assessment("assessment"),

  @JsonProperty("assessmentAppeal")
  assessmentAppeal("assessmentAppeal"),

  @JsonProperty("cas1SpaceBooking")
  cas1SpaceBooking("cas1SpaceBooking"),
}
