package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: updateAllUsersFromCommunityApi,updateSentenceTypeAndSituation,updateBookingStatus,updateTaskDueDates,updateUsersPduByApi,updateCas2ApplicationsWithAssessments,updateCas2StatusUpdatesWithAssessments,updateCas2NotesWithAssessments,updateCas1BackfillUserApArea,updateCas3ApplicationOffenderName,updateCas3BookingOffenderName,updateCas3BedspaceStartDate,updateCas3PremisesStartDate,updateCas3DomainEventTypeForPersonDepartedUpdated,updateCas1ApplicationsLicenceExpiryDate,updateCas1BackfillOfflineApplicationName,updateCas1ArsonSuitableToArsonOffences,updateCas1BackfillArsonSuitable,updateCas1ApprovedPremisesAssessmentReportProperties,cas1UpdateRoomCodes,updateCas1ApplicationsWithOffender,updateCas3BedspaceModelData,updateCas3VoidBedspaceCancellationData,cas1CapacityPerformanceTest
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class MigrationJobType(@get:JsonValue val value: kotlin.String) {

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
  updateCas3BedspaceStartDate("update_cas3_bedspace_start_date"),
  updateCas3PremisesStartDate("update_cas3_premises_start_date"),
  updateCas3DomainEventTypeForPersonDepartedUpdated("update_cas3_domain_event_type_for_person_departed_updated"),
  updateCas1ApplicationsLicenceExpiryDate("update_cas1_applications_licence_expiry_date"),
  updateCas1BackfillOfflineApplicationName("update_cas1_backfill_offline_application_name"),
  updateCas1ArsonSuitableToArsonOffences("update_cas1_arson_suitable_to_arson_offences"),
  updateCas1BackfillArsonSuitable("update_cas1_backfill_arson_suitable"),
  updateCas1ApprovedPremisesAssessmentReportProperties("update_cas1_approved_premises_assessment_report_properties"),
  cas1UpdateRoomCodes("cas1_update_room_codes"),
  updateCas1ApplicationsWithOffender("update_cas1_applications_with_offender"),
  updateCas3BedspaceModelData("update_cas3_bedspace_model_data"),
  updateCas3VoidBedspaceData("update_cas3_void_bedspace_data"),
  cas1CapacityPerformanceTest("cas1_capacity_performance_test"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): MigrationJobType = values().first { it -> it.value == value }
  }
}
