package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: pipe,lao,emergency,esap,recoveryFocused,mentalHealthSpecialist
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class UserQualification(@get:JsonValue val value: kotlin.String) {

  pipe("pipe"),
  lao("lao"),
  emergency("emergency"),
  esap("esap"),
  recoveryFocused("recovery_focused"),
  mentalHealthSpecialist("mental_health_specialist"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): UserQualification = values().first { it -> it.value == value }
  }
}
