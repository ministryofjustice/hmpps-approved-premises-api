package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission

enum class UserPermission(val cas1ApiValue: ApprovedPremisesUserPermission?) {
  @Deprecated("This should no longer be used on the UI or API")
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

  @Deprecated("Legacy bookings have been replaced by space bookings")
  CAS1_BOOKING_CREATE(ApprovedPremisesUserPermission.bookingCreate),
  CAS1_BOOKING_WITHDRAW(ApprovedPremisesUserPermission.bookingWithdraw),
  CAS1_BOOKING_CHANGE_DATES(ApprovedPremisesUserPermission.bookingChangeDates),

  CAS1_CHANGE_REQUEST_LIST(ApprovedPremisesUserPermission.changeRequestList),
  CAS1_CHANGE_REQUEST_VIEW(ApprovedPremisesUserPermission.changeRequestView),

  CAS1_OFFLINE_APPLICATION_VIEW(ApprovedPremisesUserPermission.offlineApplicationView),

  CAS1_OUT_OF_SERVICE_BED_CREATE(ApprovedPremisesUserPermission.outOfServiceBedCreate),
  CAS1_OUT_OF_SERVICE_BED_CREATE_BED_ON_HOLD(ApprovedPremisesUserPermission.outOfServiceBedCreateBedOnHold),
  CAS1_OUT_OF_SERVICE_BED_CANCEL(ApprovedPremisesUserPermission.outOfServiceBedCancel),

  CAS1_PLACEMENT_REQUEST_RECORD_UNABLE_TO_MATCH(ApprovedPremisesUserPermission.placementRequestRecordUnableToMatch),

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

  /**
   * Also allows for space booking modification
   */
  CAS1_SPACE_BOOKING_CREATE(ApprovedPremisesUserPermission.spaceBookingCreate),
  CAS1_SPACE_BOOKING_LIST(ApprovedPremisesUserPermission.spaceBookingList),
  CAS1_SPACE_BOOKING_RECORD_ARRIVAL(ApprovedPremisesUserPermission.spaceBookingRecordArrival),
  CAS1_SPACE_BOOKING_RECORD_DEPARTURE(ApprovedPremisesUserPermission.spaceBookingRecordDeparture),
  CAS1_SPACE_BOOKING_RECORD_NON_ARRIVAL(ApprovedPremisesUserPermission.spaceBookingRecordNonArrival),
  CAS1_SPACE_BOOKING_RECORD_KEYWORKER(ApprovedPremisesUserPermission.spaceBookingRecordKeyworker),
  CAS1_SPACE_BOOKING_VIEW(ApprovedPremisesUserPermission.spaceBookingView),
  CAS1_SPACE_BOOKING_WITHDRAW(ApprovedPremisesUserPermission.spaceBookingWithdraw),
  CAS1_SPACE_BOOKING_SHORTEN(ApprovedPremisesUserPermission.spaceBookingShorten),

  CAS1_NATIONAL_OCCUPANCY_VIEW(ApprovedPremisesUserPermission.nationalOccupancyView),

  CAS1_PREMISES_CAPACITY_REPORT_VIEW(ApprovedPremisesUserPermission.premisesCapacityReportView),
  CAS1_PREMISES_VIEW(ApprovedPremisesUserPermission.premisesView),
  CAS1_PREMISES_MANAGE(ApprovedPremisesUserPermission.premisesManage),

  CAS1_TASKS_LIST(ApprovedPremisesUserPermission.tasksList),
  CAS1_TASK_ALLOCATE(ApprovedPremisesUserPermission.taskAllocate),

  CAS1_APPLICATION_WITHDRAW_OTHERS(ApprovedPremisesUserPermission.applicationWithdrawOthers),

  /**
   * View reports, excluding those containing PII
   */
  CAS1_REPORTS_VIEW(ApprovedPremisesUserPermission.reportsView),
  CAS1_REPORTS_VIEW_WITH_PII(ApprovedPremisesUserPermission.reportsViewWithPii),
  CAS1_REQUEST_FOR_PLACEMENT_WITHDRAW_OTHERS(ApprovedPremisesUserPermission.requestForPlacementWithdrawOthers),

  CAS1_TRANSFER_ASSESS(ApprovedPremisesUserPermission.transferAssess),

  /**
   * Create a planned transfer request or immediately action an emergency transfer
   */
  CAS1_TRANSFER_CREATE(ApprovedPremisesUserPermission.transferCreate),

  CAS1_PLACEMENT_APPEAL_CREATE(ApprovedPremisesUserPermission.placementAppealCreate),
  CAS1_PLACEMENT_APPEAL_ASSESS(ApprovedPremisesUserPermission.placementAppealAssess),
}
