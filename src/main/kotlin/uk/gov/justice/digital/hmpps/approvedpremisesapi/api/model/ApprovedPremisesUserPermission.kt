package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: adhocBookingCreate,applicationWithdrawOthers,assessAppealedApplication,assessApplication,assessPlacementApplication,assessPlacementRequest,bookingCreate,bookingChangeDates,bookingWithdraw,outOfServiceBedCreate,processAnAppeal,userList,userManagement,viewAssignedAssessments,viewCruDashboard,viewManageTasks,viewOutOfServiceBeds,requestForPlacementWithdrawOthers,spaceBookingCreate,spaceBookingList,spaceBookingRecordArrival,spaceBookingRecordDeparture,spaceBookingRecordNonArrival,spaceBookingRecordKeyworker,spaceBookingView,spaceBookingWithdraw,premisesViewCapacity,premisesViewSummary,reportsView
*/
enum class ApprovedPremisesUserPermission(val value: kotlin.String) {

  @JsonProperty("cas1_adhoc_booking_create")
  adhocBookingCreate("cas1_adhoc_booking_create"),

  @JsonProperty("cas1_application_withdraw_others")
  applicationWithdrawOthers("cas1_application_withdraw_others"),

  @JsonProperty("cas1_assess_appealed_application")
  assessAppealedApplication("cas1_assess_appealed_application"),

  @JsonProperty("cas1_assess_application")
  assessApplication("cas1_assess_application"),

  @JsonProperty("cas1_assess_placement_application")
  assessPlacementApplication("cas1_assess_placement_application"),

  @JsonProperty("cas1_assess_placement_request")
  assessPlacementRequest("cas1_assess_placement_request"),

  @JsonProperty("cas1_booking_create")
  bookingCreate("cas1_booking_create"),

  @JsonProperty("cas1_booking_change_dates")
  bookingChangeDates("cas1_booking_change_dates"),

  @JsonProperty("cas1_booking_withdraw")
  bookingWithdraw("cas1_booking_withdraw"),

  @JsonProperty("cas1_out_of_service_bed_create")
  outOfServiceBedCreate("cas1_out_of_service_bed_create"),

  @JsonProperty("cas1_process_an_appeal")
  processAnAppeal("cas1_process_an_appeal"),

  @JsonProperty("cas1_user_list")
  userList("cas1_user_list"),

  @JsonProperty("cas1_user_management")
  userManagement("cas1_user_management"),

  @JsonProperty("cas1_view_assigned_assessments")
  viewAssignedAssessments("cas1_view_assigned_assessments"),

  @JsonProperty("cas1_view_cru_dashboard")
  viewCruDashboard("cas1_view_cru_dashboard"),

  @JsonProperty("cas1_view_manage_tasks")
  viewManageTasks("cas1_view_manage_tasks"),

  @JsonProperty("cas1_view_out_of_service_beds")
  viewOutOfServiceBeds("cas1_view_out_of_service_beds"),

  @JsonProperty("cas1_request_for_placement_withdraw_others")
  requestForPlacementWithdrawOthers("cas1_request_for_placement_withdraw_others"),

  @JsonProperty("cas1_space_booking_create")
  spaceBookingCreate("cas1_space_booking_create"),

  @JsonProperty("cas1_space_booking_list")
  spaceBookingList("cas1_space_booking_list"),

  @JsonProperty("cas1_space_booking_record_arrival")
  spaceBookingRecordArrival("cas1_space_booking_record_arrival"),

  @JsonProperty("cas1_space_booking_record_departure")
  spaceBookingRecordDeparture("cas1_space_booking_record_departure"),

  @JsonProperty("cas1_space_booking_record_non_arrival")
  spaceBookingRecordNonArrival("cas1_space_booking_record_non_arrival"),

  @JsonProperty("cas1_space_booking_record_keyworker")
  spaceBookingRecordKeyworker("cas1_space_booking_record_keyworker"),

  @JsonProperty("cas1_space_booking_view")
  spaceBookingView("cas1_space_booking_view"),

  @JsonProperty("cas1_space_booking_withdraw")
  spaceBookingWithdraw("cas1_space_booking_withdraw"),

  @JsonProperty("cas1_premises_view_capacity")
  premisesViewCapacity("cas1_premises_view_capacity"),

  @JsonProperty("cas1_premises_view_summary")
  premisesViewSummary("cas1_premises_view_summary"),

  @JsonProperty("cas1_reports_view")
  reportsView("cas1_reports_view"),
}
