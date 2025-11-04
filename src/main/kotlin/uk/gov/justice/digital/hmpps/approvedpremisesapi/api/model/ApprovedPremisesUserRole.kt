package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class ApprovedPremisesUserRole(@get:JsonValue val value: kotlin.String) {

  assessor("assessor"),
  futureManager("future_manager"),
  changeRequestDev("change_request_dev"),
  cruMember("cru_member"),
  applicant("applicant"),
  reportViewer("report_viewer"),
  reportViewerWithPii("report_viewer_with_pii"),
  excludedFromAssessAllocation("excluded_from_assess_allocation"),
  excludedFromMatchAllocation("excluded_from_match_allocation"),
  excludedFromPlacementApplicationAllocation("excluded_from_placement_application_allocation"),
  appealsManager("appeals_manager"),
  janitor("janitor"),
  userManager("user_manager"),
  apAreaManager("ap_area_manager"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): ApprovedPremisesUserRole = values().first { it -> it.value == value }
  }
}
