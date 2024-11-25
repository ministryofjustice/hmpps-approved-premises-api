package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: allUsersFromCommunityApi,sentenceTypeAndSituation,bookingStatus,taskDueDates,usersPduByApi,cas2ApplicationsWithAssessments,cas2StatusUpdatesWithAssessments,cas2NotesWithAssessments,cas1FixPlacementAppLinks,cas1NoticeTypes,cas1BackfillUserApArea,cas3ApplicationOffenderName,cas3DomainEventTypeForPersonDepartedUpdated
*/
enum class MigrationJobType(val value: kotlin.String) {

  @JsonProperty("update_all_users_from_community_api")
  allUsersFromCommunityApi("update_all_users_from_community_api"),

  @JsonProperty("update_sentence_type_and_situation")
  sentenceTypeAndSituation("update_sentence_type_and_situation"),

  @JsonProperty("update_booking_status")
  bookingStatus("update_booking_status"),

  @JsonProperty("update_task_due_dates")
  taskDueDates("update_task_due_dates"),

  @JsonProperty("update_users_pdu_by_api")
  usersPduByApi("update_users_pdu_by_api"),

  @JsonProperty("update_cas2_applications_with_assessments")
  cas2ApplicationsWithAssessments("update_cas2_applications_with_assessments"),

  @JsonProperty("update_cas2_status_updates_with_assessments")
  cas2StatusUpdatesWithAssessments("update_cas2_status_updates_with_assessments"),

  @JsonProperty("update_cas2_notes_with_assessments")
  cas2NotesWithAssessments("update_cas2_notes_with_assessments"),

  @JsonProperty("update_cas1_fix_placement_app_links")
  cas1FixPlacementAppLinks("update_cas1_fix_placement_app_links"),

  @JsonProperty("update_cas1_notice_types")
  cas1NoticeTypes("update_cas1_notice_types"),

  @JsonProperty("update_cas1_backfill_user_ap_area")
  cas1BackfillUserApArea("update_cas1_backfill_user_ap_area"),

  @JsonProperty("update_cas3_application_offender_name")
  cas3ApplicationOffenderName("update_cas3_application_offender_name"),

  @JsonProperty("update_cas3_domain_event_type_for_person_departed_updated")
  cas3DomainEventTypeForPersonDepartedUpdated("update_cas3_domain_event_type_for_person_departed_updated"),
}
