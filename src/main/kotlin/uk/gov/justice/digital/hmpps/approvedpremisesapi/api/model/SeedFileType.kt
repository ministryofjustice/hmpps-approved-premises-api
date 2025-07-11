package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: approvedPremises,approvedPremisesRooms,temporaryAccommodationPremises,temporaryAccommodationBedspace,user,nomisUsers,externalUsers,cas2Applications,cas2v2Applications,cas2v2Users,temporaryAccommodationUsers,approvedPremisesUsers,characteristics,updateNomsNumber,updateUsersFromApi,usersBasic,approvedPremisesAssessmentMoreInfoBugFix,approvedPremisesRedactAssessmentDetails,approvedPremisesBookingToSpaceBooking,approvedPremisesWithdrawPlacementRequest,approvedPremisesReplayDomainEvents,approvedPremisesDuplicateApplication,approvedPremisesUpdateEventNumber,approvedPremisesLinkBookingToPlacementRequest,approvedPremisesOutOfServiceBeds,approvedPremisesCruManagementAreas,approvedPremisesImportDeliusReferrals,approvedPremisesUpdateSpaceBooking,approvedPremisesBackfillActiveSpaceBookingsCreatedInDelius,approvedPremisesCreateTestApplications,approvedPremisesDeleteApplicationTimelineNotes,approvedPremisesUpdateActualArrivalDate,approvedPremisesUpdatePremisesStatus,temporaryAccommodationReferralRejection,approvedPremisesRemapBedCodes,shortTermAccommodationCreateOmus,temporaryAccommodationAssignApplicationToPdu
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class SeedFileType(@get:JsonValue val value: kotlin.String) {

  approvedPremises("approved_premises"),
  approvedPremisesRooms("approved_premises_rooms"),
  temporaryAccommodationPremises("temporary_accommodation_premises"),
  temporaryAccommodationBedspace("temporary_accommodation_bedspace"),
  user("user"),
  nomisUsers("nomis_users"),
  externalUsers("external_users"),
  cas2Applications("cas2_applications"),
  cas2v2Applications("cas2v2_applications"),
  cas2v2Users("cas2v2_users"),
  temporaryAccommodationUsers("temporary_accommodation_users"),
  approvedPremisesUsers("approved_premises_users"),
  characteristics("characteristics"),
  updateNomsNumber("update_noms_number"),
  updateUsersFromApi("update_users_from_api"),
  usersBasic("users_basic"),
  approvedPremisesAssessmentMoreInfoBugFix("approved_premises_assessment_more_info_bug_fix"),
  approvedPremisesRedactAssessmentDetails("approved_premises_redact_assessment_details"),
  approvedPremisesBookingToSpaceBooking("approved_premises_booking_to_space_booking"),
  approvedPremisesWithdrawPlacementRequest("approved_premises_withdraw_placement_request"),
  approvedPremisesReplayDomainEvents("approved_premises_replay_domain_events"),
  approvedPremisesDuplicateApplication("approved_premises_duplicate_application"),
  approvedPremisesUpdateEventNumber("approved_premises_update_event_number"),
  approvedPremisesLinkBookingToPlacementRequest("approved_premises_link_booking_to_placement_request"),
  approvedPremisesOutOfServiceBeds("approved_premises_out_of_service_beds"),
  approvedPremisesCruManagementAreas("approved_premises_cru_management_areas"),
  approvedPremisesImportDeliusReferrals("approved_premises_import_delius_referrals"),
  approvedPremisesUpdateSpaceBooking("approved_premises_update_space_booking"),
  approvedPremisesBackfillActiveSpaceBookingsCreatedInDelius("approved_premises_backfill_active_space_bookings_created_in_delius"),
  approvedPremisesCreateTestApplications("approved_premises_create_test_applications"),
  approvedPremisesDeleteApplicationTimelineNotes("approved_premises_delete_application_timeline_notes"),
  approvedPremisesUpdateActualArrivalDate("approved_premises_update_actual_arrival_date"),
  approvedPremisesUpdatePremisesStatus("approved_premises_update_premises_status"),
  temporaryAccommodationReferralRejection("temporary_accommodation_referral_rejection"),
  approvedPremisesRemapBedCodes("approved_premises_remap_bed_codes"),
  shortTermAccommodationCreateOmus("short_term_accommodation_create_omus"),
  temporaryAccommodationAssignApplicationToPdu("temporary_accommodation_assign_application_to_pdu"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): SeedFileType = values().first { it -> it.value == value }
  }
}
