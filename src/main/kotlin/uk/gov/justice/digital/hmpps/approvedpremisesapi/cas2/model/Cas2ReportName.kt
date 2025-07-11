package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas2ReportName(@get:JsonValue val value: String) {

  submittedMinusApplications("submitted-applications"),
  applicationMinusStatusMinusUpdates("application-status-updates"),
  unsubmittedMinusApplications("unsubmitted-applications"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas2ReportName = values().first { it -> it.value == value }
  }
}
