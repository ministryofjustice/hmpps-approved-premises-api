package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: submittedMinusApplications,applicationMinusStatusMinusUpdates,unsubmittedMinusApplications
*/
enum class Cas2ReportName(val value: kotlin.String) {

  @JsonProperty("submitted-applications")
  submittedMinusApplications("submitted-applications"),

  @JsonProperty("application-status-updates")
  applicationMinusStatusMinusUpdates("application-status-updates"),

  @JsonProperty("unsubmitted-applications")
  unsubmittedMinusApplications("unsubmitted-applications"),
}
