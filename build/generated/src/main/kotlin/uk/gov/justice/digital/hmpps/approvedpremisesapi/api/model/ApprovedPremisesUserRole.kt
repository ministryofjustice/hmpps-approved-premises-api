package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: assessor,matcher,futureManager,workflowManager,cruMember,cruMemberFindAndBookBeta,applicant,roleAdmin,reportViewer,excludedFromAssessAllocation,excludedFromMatchAllocation,excludedFromPlacementApplicationAllocation,appealsManager,janitor,userManager
*/
enum class ApprovedPremisesUserRole(val value: kotlin.String) {

    @JsonProperty("assessor") assessor("assessor"),
    @JsonProperty("matcher") matcher("matcher"),
    @JsonProperty("future_manager") futureManager("future_manager"),
    @JsonProperty("workflow_manager") workflowManager("workflow_manager"),
    @JsonProperty("cru_member") cruMember("cru_member"),
    @JsonProperty("cru_member_find_and_book_beta") cruMemberFindAndBookBeta("cru_member_find_and_book_beta"),
    @JsonProperty("applicant") applicant("applicant"),
    @JsonProperty("role_admin") roleAdmin("role_admin"),
    @JsonProperty("report_viewer") reportViewer("report_viewer"),
    @JsonProperty("excluded_from_assess_allocation") excludedFromAssessAllocation("excluded_from_assess_allocation"),
    @JsonProperty("excluded_from_match_allocation") excludedFromMatchAllocation("excluded_from_match_allocation"),
    @JsonProperty("excluded_from_placement_application_allocation") excludedFromPlacementApplicationAllocation("excluded_from_placement_application_allocation"),
    @JsonProperty("appeals_manager") appealsManager("appeals_manager"),
    @JsonProperty("janitor") janitor("janitor"),
    @JsonProperty("user_manager") userManager("user_manager")
}

