package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: approvedPremises,approvedPremisesRooms,temporaryAccommodationPremises,temporaryAccommodationBedspace,user,nomisUsers,externalUsers,cas2Applications,temporaryAccommodationUsers,approvedPremisesUsers,characteristics,updateNomsNumber,updateUsersFromApi,approvedPremisesApStaffUsers,approvedPremisesCancelBookings,approvedPremisesAssessmentMoreInfoBugFix,approvedPremisesRedactAssessmentDetails,approvedPremisesBookingToSpaceBooking,approvedPremisesWithdrawPlacementRequest,approvedPremisesReplayDomainEvents,approvedPremisesDuplicateApplication,approvedPremisesUpdateEventNumber,approvedPremisesLinkBookingToPlacementRequest,approvedPremisesOutOfServiceBeds,approvedPremisesCruManagementAreas,approvedPremisesSpacePlanningDryRun,approvedPremisesImportDeliusBookingManagementData,approvedPremisesUpdateSpaceBooking
*/
enum class SeedFileType(val value: kotlin.String) {

    @JsonProperty("approved_premises") approvedPremises("approved_premises"),
    @JsonProperty("approved_premises_rooms") approvedPremisesRooms("approved_premises_rooms"),
    @JsonProperty("temporary_accommodation_premises") temporaryAccommodationPremises("temporary_accommodation_premises"),
    @JsonProperty("temporary_accommodation_bedspace") temporaryAccommodationBedspace("temporary_accommodation_bedspace"),
    @JsonProperty("user") user("user"),
    @JsonProperty("nomis_users") nomisUsers("nomis_users"),
    @JsonProperty("external_users") externalUsers("external_users"),
    @JsonProperty("cas2_applications") cas2Applications("cas2_applications"),
    @JsonProperty("temporary_accommodation_users") temporaryAccommodationUsers("temporary_accommodation_users"),
    @JsonProperty("approved_premises_users") approvedPremisesUsers("approved_premises_users"),
    @JsonProperty("characteristics") characteristics("characteristics"),
    @JsonProperty("update_noms_number") updateNomsNumber("update_noms_number"),
    @JsonProperty("update_users_from_api") updateUsersFromApi("update_users_from_api"),
    @JsonProperty("approved_premises_ap_staff_users") approvedPremisesApStaffUsers("approved_premises_ap_staff_users"),
    @JsonProperty("approved_premises_cancel_bookings") approvedPremisesCancelBookings("approved_premises_cancel_bookings"),
    @JsonProperty("approved_premises_assessment_more_info_bug_fix") approvedPremisesAssessmentMoreInfoBugFix("approved_premises_assessment_more_info_bug_fix"),
    @JsonProperty("approved_premises_redact_assessment_details") approvedPremisesRedactAssessmentDetails("approved_premises_redact_assessment_details"),
    @JsonProperty("approved_premises_booking_to_space_booking") approvedPremisesBookingToSpaceBooking("approved_premises_booking_to_space_booking"),
    @JsonProperty("approved_premises_withdraw_placement_request") approvedPremisesWithdrawPlacementRequest("approved_premises_withdraw_placement_request"),
    @JsonProperty("approved_premises_replay_domain_events") approvedPremisesReplayDomainEvents("approved_premises_replay_domain_events"),
    @JsonProperty("approved_premises_duplicate_application") approvedPremisesDuplicateApplication("approved_premises_duplicate_application"),
    @JsonProperty("approved_premises_update_event_number") approvedPremisesUpdateEventNumber("approved_premises_update_event_number"),
    @JsonProperty("approved_premises_link_booking_to_placement_request") approvedPremisesLinkBookingToPlacementRequest("approved_premises_link_booking_to_placement_request"),
    @JsonProperty("approved_premises_out_of_service_beds") approvedPremisesOutOfServiceBeds("approved_premises_out_of_service_beds"),
    @JsonProperty("approved_premises_cru_management_areas") approvedPremisesCruManagementAreas("approved_premises_cru_management_areas"),
    @JsonProperty("approved_premises_space_planning_dry_run") approvedPremisesSpacePlanningDryRun("approved_premises_space_planning_dry_run"),
    @JsonProperty("approved_premises_import_delius_booking_management_data") approvedPremisesImportDeliusBookingManagementData("approved_premises_import_delius_booking_management_data"),
    @JsonProperty("approved_premises_update_space_booking") approvedPremisesUpdateSpaceBooking("approved_premises_update_space_booking")
}

