package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: pipe,lao,emergency,esap,recoveryFocused,mentalHealthSpecialist
*/
enum class UserQualification(val value: kotlin.String) {

  @JsonProperty("pipe")
  pipe("pipe"),

  @JsonProperty("lao")
  lao("lao"),

  @JsonProperty("emergency")
  emergency("emergency"),

  @JsonProperty("esap")
  esap("esap"),

  @JsonProperty("recovery_focused")
  recoveryFocused("recovery_focused"),

  @JsonProperty("mental_health_specialist")
  mentalHealthSpecialist("mental_health_specialist"),
}
