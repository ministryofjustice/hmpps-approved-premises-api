package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: applications,applicationsV2,dailyMetrics,lostBeds,outOfServiceBeds,placementApplications,placementMatchingOutcomes,placementMatchingOutcomesV2,requestsForPlacement
*/
enum class Cas1ReportName(val value: kotlin.String) {

  @JsonProperty("applications")
  applications("applications"),

  @JsonProperty("applicationsV2")
  applicationsV2("applicationsV2"),

  @JsonProperty("dailyMetrics")
  dailyMetrics("dailyMetrics"),

  @JsonProperty("lostBeds")
  lostBeds("lostBeds"),

  @JsonProperty("outOfServiceBeds")
  outOfServiceBeds("outOfServiceBeds"),

  @JsonProperty("placementApplications")
  placementApplications("placementApplications"),

  @JsonProperty("placementMatchingOutcomes")
  placementMatchingOutcomes("placementMatchingOutcomes"),

  @JsonProperty("placementMatchingOutcomesV2")
  placementMatchingOutcomesV2("placementMatchingOutcomesV2"),

  @JsonProperty("requestsForPlacement")
  requestsForPlacement("requestsForPlacement"),
}
