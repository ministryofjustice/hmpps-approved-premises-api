package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: submittedMinusApplications,applicationMinusStatusMinusUpdates,unsubmittedMinusApplications
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas2v2ReportName(@get:JsonValue val value: String) {

  submittedMinusApplications("submitted-applications"),
  applicationMinusStatusMinusUpdates("application-status-updates"),
  unsubmittedMinusApplications("unsubmitted-applications"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas2v2ReportName = values().first { it -> it.value == value }
  }
}
