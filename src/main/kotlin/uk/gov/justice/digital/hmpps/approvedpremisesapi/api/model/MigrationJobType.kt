package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class MigrationJobType(@get:JsonValue val value: String) {
  updateAllUsersFromCommunityApi("update_all_users_from_community_api"),
  updateSentenceTypeAndSituation("update_sentence_type_and_situation"),
  updateBookingStatus("update_booking_status"),
  updateTaskDueDates("update_task_due_dates"),
  updateUsersPduByApi("update_users_pdu_by_api"),
  updateCas2ApplicationsWithAssessments("update_cas2_applications_with_assessments"),
  updateCas2StatusUpdatesWithAssessments("update_cas2_status_updates_with_assessments"),
  updateCas2NotesWithAssessments("update_cas2_notes_with_assessments"),
  updateCas1BackfillUserApArea("update_cas1_backfill_user_ap_area"),
  updateCas3ApplicationOffenderName("update_cas3_application_offender_name"),
  updateCas3BookingOffenderName("update_cas3_booking_offender_name"),
  updateCas3DomainEventTypeForPersonDepartedUpdated("update_cas3_domain_event_type_for_person_departed_updated"),
  updateCas1ApprovedPremisesAssessmentReportProperties("update_cas1_approved_premises_assessment_report_properties"),
  cas1UpdateRoomCodes("cas1_update_room_codes"),
  updateCas1ApplicationsWithOffender("update_cas1_applications_with_offender"),
  updateCas3BedspaceModelData("update_cas3_bedspace_model_data"),
  updateCas3VoidBedspaceData("update_cas3_void_bedspace_data"),
  cas1BackfillApplicationDuration("cas1_backfill_application_duration"),
  cas1BackfillAutomaticPlacementApplications("cas1_backfill_automatic_placement_applications"),
  cas1BackfillKeyWorkerUserAssignments("cas1_backfill_key_worker_user_assignments"),
  cas1CapacityPerformanceTest("cas1_capacity_performance_test"),
  migrateDataToCas2Tables("migrate_data_to_cas2_tables"),
  updateCas3DomainEventArchiveUnarchiveTransaction("update_cas3_domain_event_archive_unarchive_transaction"),
}
