package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission

enum class UserPermission(val cas1ApiValue: ApprovedPremisesUserPermission?) {
  CAS1_ADHOC_BOOKING_CREATE(ApprovedPremisesUserPermission.adhocBookingCreate),

  /**
   * If the user can be allocated and assess an appealed application
   * If given this role the user must also have CAS1_ASSESS_APPLICATION
   */
  CAS1_ASSESS_APPEALED_APPLICATION(ApprovedPremisesUserPermission.assessAppealedApplication),

  /**
   * If the user can be allocated and assess an application
   */
  CAS1_ASSESS_APPLICATION(ApprovedPremisesUserPermission.assessApplication),

  /**
   * If the user can be allocated and assess placement applications
   */
  CAS1_ASSESS_PLACEMENT_APPLICATION(ApprovedPremisesUserPermission.assessPlacementApplication),

  CAS1_BOOKING_CREATE(ApprovedPremisesUserPermission.bookingCreate),
  CAS1_BOOKING_WITHDRAW(ApprovedPremisesUserPermission.bookingWithdraw),
  CAS1_BOOKING_CHANGE_DATES(ApprovedPremisesUserPermission.bookingChangeDates),
  CAS1_OUT_OF_SERVICE_BED_CREATE(ApprovedPremisesUserPermission.outOfServiceBedCreate),
  CAS1_OUT_OF_SERVICE_BED_CANCEL(ApprovedPremisesUserPermission.outOfServiceBedCancel),

  /**
   * If the user can record an appeal against a rejected application
   */
  CAS1_PROCESS_AN_APPEAL(ApprovedPremisesUserPermission.processAnAppeal),

  /**
   * Used for both listing user summaries (e.g. for drop-downs) and listing complete
   * user information (e.g. for user management). Ideally this would be split
   */
  CAS1_USER_LIST(ApprovedPremisesUserPermission.userList),
  CAS1_USER_MANAGEMENT(ApprovedPremisesUserPermission.userManagement),

  /**
   * If the user has general access to the 'assess' tile/menu option
   */
  CAS1_VIEW_ASSIGNED_ASSESSMENTS(ApprovedPremisesUserPermission.viewAssignedAssessments),
  CAS1_VIEW_CRU_DASHBOARD(ApprovedPremisesUserPermission.viewCruDashboard),
  CAS1_VIEW_MANAGE_TASKS(ApprovedPremisesUserPermission.viewManageTasks),
  CAS1_VIEW_OUT_OF_SERVICE_BEDS(ApprovedPremisesUserPermission.viewOutOfServiceBeds),
  CAS1_SPACE_BOOKING_CREATE(ApprovedPremisesUserPermission.spaceBookingCreate),
  CAS1_SPACE_BOOKING_LIST(ApprovedPremisesUserPermission.spaceBookingList),
  CAS1_SPACE_BOOKING_RECORD_ARRIVAL(ApprovedPremisesUserPermission.spaceBookingRecordArrival),
  CAS1_SPACE_BOOKING_RECORD_DEPARTURE(ApprovedPremisesUserPermission.spaceBookingRecordDeparture),
  CAS1_SPACE_BOOKING_RECORD_NON_ARRIVAL(ApprovedPremisesUserPermission.spaceBookingRecordNonArrival),
  CAS1_SPACE_BOOKING_RECORD_KEYWORKER(ApprovedPremisesUserPermission.spaceBookingRecordKeyworker),
  CAS1_SPACE_BOOKING_VIEW(ApprovedPremisesUserPermission.spaceBookingView),
  CAS1_SPACE_BOOKING_WITHDRAW(ApprovedPremisesUserPermission.spaceBookingWithdraw),
  CAS1_PREMISES_VIEW(ApprovedPremisesUserPermission.premisesView),
  CAS1_PREMISES_MANAGE(ApprovedPremisesUserPermission.premisesManage),
  CAS1_APPLICATION_WITHDRAW_OTHERS(ApprovedPremisesUserPermission.applicationWithdrawOthers),

  /**
   * View reports, excluding those containing PII
   */
  CAS1_REPORTS_VIEW(ApprovedPremisesUserPermission.reportsView),
  CAS1_REPORTS_VIEW_WITH_PII(ApprovedPremisesUserPermission.reportsViewWithPii),
  CAS1_REQUEST_FOR_PLACEMENT_WITHDRAW_OTHERS(ApprovedPremisesUserPermission.requestForPlacementWithdrawOthers),
}
