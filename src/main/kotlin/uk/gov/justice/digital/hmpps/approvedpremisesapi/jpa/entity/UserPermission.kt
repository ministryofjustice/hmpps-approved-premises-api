package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService

enum class UserPermission(val cas1ApiValue: ApprovedPremisesUserPermission?, val experimental: Boolean = false) {
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

  CAS1_BOOKING_WITHDRAW(ApprovedPremisesUserPermission.bookingWithdraw),
  CAS1_BOOKING_CHANGE_DATES(ApprovedPremisesUserPermission.bookingChangeDates),

  CAS1_CHANGE_REQUEST_LIST(ApprovedPremisesUserPermission.changeRequestList, experimental = true),
  CAS1_CHANGE_REQUEST_VIEW(ApprovedPremisesUserPermission.changeRequestView, experimental = true),

  CAS1_EXPERIMENTAL_NEW_ASSIGN_KEYWORKER_FLOW(ApprovedPremisesUserPermission.experimentalNewAssignKeyworkerFlow, experimental = true),

  CAS1_EXPERIMENTAL_NEW_REQUEST_FOR_PLACEMENT_FLOW(ApprovedPremisesUserPermission.experimentalNewRequestForPlacementFlow, experimental = true),

  CAS1_KEYWORKER_ASSIGNABLE_AS(ApprovedPremisesUserPermission.keyworkerAssignableAs),

  CAS1_OFFLINE_APPLICATION_VIEW(ApprovedPremisesUserPermission.offlineApplicationView),

  CAS1_OUT_OF_SERVICE_BED_CREATE(ApprovedPremisesUserPermission.outOfServiceBedCreate),
  CAS1_OUT_OF_SERVICE_BED_CREATE_BED_ON_HOLD(ApprovedPremisesUserPermission.outOfServiceBedCreateBedOnHold),
  CAS1_OUT_OF_SERVICE_BED_CANCEL(ApprovedPremisesUserPermission.outOfServiceBedCancel),
  CAS1_OUT_OF_SERVICE_BED_NO_DATE_LIMIT(ApprovedPremisesUserPermission.outOfServiceNoDateLimit),

  CAS1_PLACEMENT_REQUEST_RECORD_UNABLE_TO_MATCH(ApprovedPremisesUserPermission.placementRequestRecordUnableToMatch),

  /**
   * If the user can record an appeal against a rejected application
   */
  CAS1_PROCESS_AN_APPEAL(ApprovedPremisesUserPermission.processAnAppeal),

  CAS1_USER_SUMMARY_LIST(ApprovedPremisesUserPermission.userSummaryList),

  /**
   * Used for both listing complete user information (e.g. for user management)
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
  CAS1_SPACE_BOOKING_RECORD_ARRIVAL_NO_DATE_LIMIT(ApprovedPremisesUserPermission.spaceBookingRecordArrivalNoDateLimit),
  CAS1_SPACE_BOOKING_RECORD_DEPARTURE(ApprovedPremisesUserPermission.spaceBookingRecordDeparture),
  CAS1_SPACE_BOOKING_RECORD_DEPARTURE_NO_DATE_LIMIT(ApprovedPremisesUserPermission.spaceBookingRecordDepartureNoDateLimit),
  CAS1_SPACE_BOOKING_RECORD_NON_ARRIVAL(ApprovedPremisesUserPermission.spaceBookingRecordNonArrival),
  CAS1_SPACE_BOOKING_RECORD_NON_ARRIVAL_NO_DATE_LIMIT(ApprovedPremisesUserPermission.spaceBookingRecordNonArrivalNoDateLimit),
  CAS1_SPACE_BOOKING_RECORD_KEYWORKER(ApprovedPremisesUserPermission.spaceBookingRecordKeyworker),

  @Deprecated("This is no longer used and should be removed once usage removed from the UI")
  CAS1_SPACE_BOOKING_VIEW(ApprovedPremisesUserPermission.spaceBookingView),
  CAS1_SPACE_BOOKING_WITHDRAW(ApprovedPremisesUserPermission.spaceBookingWithdraw),

  CAS1_SPACE_BOOKING_SHORTEN(ApprovedPremisesUserPermission.spaceBookingShorten, experimental = true),

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

  CAS1_TRANSFER_ASSESS(ApprovedPremisesUserPermission.transferAssess, experimental = true),

  /**
   * Create a planned transfer request or immediately action an emergency transfer
   */
  CAS1_TRANSFER_CREATE(ApprovedPremisesUserPermission.transferCreate, experimental = true),

  CAS1_PLACEMENT_APPEAL_CREATE(ApprovedPremisesUserPermission.placementAppealCreate, experimental = true),
  CAS1_PLACEMENT_APPEAL_ASSESS(ApprovedPremisesUserPermission.placementAppealAssess, experimental = true),

  CAS1_PREMISES_LOCAL_RESTRICTIONS_MANAGE(ApprovedPremisesUserPermission.premisesLocalRestrictionsManage),

  CAS1_TEST_EXPERIMENTAL_PERMISSION(ApprovedPremisesUserPermission.cas1TestExperimentalPermission, experimental = true),
  CAS1_SPACE_BOOKING_CREATE_ADDITIONAL(ApprovedPremisesUserPermission.cas1SpaceBookingCreateAdditional, experimental = true),
  ;

  fun isAvailable(environmentService: EnvironmentService): Boolean = !experimental || environmentService.isNotProd()

  companion object {
    fun forApiPermission(apiPermission: ApprovedPremisesUserPermission) = entries.first { it.cas1ApiValue == apiPermission }
  }
}
