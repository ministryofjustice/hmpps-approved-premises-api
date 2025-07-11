package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: applicationsV2,applicationsV2WithPii,dailyMetrics,outOfServiceBeds,outOfServiceBedsWithPii,placementMatchingOutcomesV2,placementMatchingOutcomesV2WithPii,requestsForPlacement,requestsForPlacementWithPii,placements,placementsWithPii,overduePlacements
*/
enum class Cas1ReportName(@get:JsonValue val value: kotlin.String) {

  applicationsV2("applicationsV2"),
  applicationsV2WithPii("applicationsV2WithPii"),
  dailyMetrics("dailyMetrics"),
  outOfServiceBeds("outOfServiceBeds"),
  outOfServiceBedsWithPii("outOfServiceBedsWithPii"),
  placementMatchingOutcomesV2("placementMatchingOutcomesV2"),
  placementMatchingOutcomesV2WithPii("placementMatchingOutcomesV2WithPii"),
  requestsForPlacement("requestsForPlacement"),
  requestsForPlacementWithPii("requestsForPlacementWithPii"),
  placements("placements"),
  placementsWithPii("placementsWithPii"),
  overduePlacements("overduePlacements"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1ReportName = entries.first { it.value == value }
  }
}
