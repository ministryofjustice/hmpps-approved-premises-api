package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas2HdcReportName(@get:JsonValue val value: String) {

  submittedMinusApplications("submitted-applications"),
  applicationMinusStatusMinusUpdates("application-status-updates"),
  unsubmittedMinusApplications("unsubmitted-applications"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas2HdcReportName = entries.first { it.value == value }
  }
}
