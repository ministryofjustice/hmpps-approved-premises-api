package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class SeedFileType(@get:JsonValue val value: String) {

  approvedPremises("approved_premises"),
  approvedPremisesRooms("approved_premises_rooms"),
  user("user"),
  nomisUsers("nomis_users"),
  externalUsers("external_users"),
  cas2Applications("cas2_applications"),
  cas2v2Applications("cas2v2_applications"),
  cas2v2Users("cas2v2_users"),
  cas2Users("cas2_users"),
  temporaryAccommodationUsers("temporary_accommodation_users"),
  approvedPremisesUsers("approved_premises_users"),
  characteristics("characteristics"),
  updateNomsNumber("update_noms_number"),
  updateUsersFromApi("update_users_from_api"),
  usersBasic("users_basic"),
  approvedPremisesAssessmentMoreInfoBugFix("approved_premises_assessment_more_info_bug_fix"),
  approvedPremisesRedactAssessmentDetails("approved_premises_redact_assessment_details"),
  approvedPremisesWithdrawPlacementRequest("approved_premises_withdraw_placement_request"),
  approvedPremisesReplayDomainEvents("approved_premises_replay_domain_events"),
  approvedPremisesDuplicateApplication("approved_premises_duplicate_application"),
  approvedPremisesUpdateEventNumber("approved_premises_update_event_number"),
  approvedPremisesOutOfServiceBeds("approved_premises_out_of_service_beds"),
  approvedPremisesCruManagementAreas("approved_premises_cru_management_areas"),
  approvedPremisesUpdateSpaceBooking("approved_premises_update_space_booking"),
  approvedPremisesCreateTestApplications("approved_premises_create_test_applications"),
  approvedPremisesDeleteApplicationTimelineNotes("approved_premises_delete_application_timeline_notes"),
  approvedPremisesUpdateActualArrivalDate("approved_premises_update_actual_arrival_date"),
  approvedPremisesUpdateApplicationContactDetails("approved_premises_update_application_contact_details"),
  approvedPremisesUpdatePremisesStatus("approved_premises_update_premises_status"),
  temporaryAccommodationReferralRejection("temporary_accommodation_referral_rejection"),
  approvedPremisesRemapBedCodes("approved_premises_remap_bed_codes"),
  shortTermAccommodationCreateOmus("short_term_accommodation_create_omus"),
  temporaryAccommodationAssignApplicationToPdu("temporary_accommodation_assign_application_to_pdu"),
  Cas2UpdateNomisUserEmailAddress("cas2_update_nomis_user_email_address"),
  cas2UpdateAssessmentStatus("cas2_update_assessment_status"),
}
