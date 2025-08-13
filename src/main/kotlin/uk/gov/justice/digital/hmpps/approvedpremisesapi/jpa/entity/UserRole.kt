package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName

enum class UserRole(val service: ServiceName, val cas1ApiValue: ApprovedPremisesUserRole?, val permissions: List<UserPermission> = emptyList()) {

  CAS1_ASSESSOR(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.assessor,
    listOf(
      UserPermission.CAS1_ASSESS_APPEALED_APPLICATION,
      UserPermission.CAS1_ASSESS_APPLICATION,
      UserPermission.CAS1_ASSESS_PLACEMENT_APPLICATION,
      UserPermission.CAS1_OFFLINE_APPLICATION_VIEW,
      UserPermission.CAS1_VIEW_ASSIGNED_ASSESSMENTS,
    ),
  ),

  CAS1_FUTURE_MANAGER(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.futureManager,
    listOf(
      UserPermission.CAS1_KEYWORKER_ASSIGNABLE_AS,
      UserPermission.CAS1_OFFLINE_APPLICATION_VIEW,
      UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE,
      UserPermission.CAS1_PREMISES_VIEW,
      UserPermission.CAS1_PREMISES_MANAGE,
      UserPermission.CAS1_SPACE_BOOKING_LIST,
      UserPermission.CAS1_SPACE_BOOKING_RECORD_ARRIVAL,
      UserPermission.CAS1_SPACE_BOOKING_RECORD_DEPARTURE,
      UserPermission.CAS1_SPACE_BOOKING_RECORD_NON_ARRIVAL,
      UserPermission.CAS1_SPACE_BOOKING_RECORD_KEYWORKER,
      UserPermission.CAS1_SPACE_BOOKING_VIEW,
      UserPermission.CAS1_USER_SUMMARY_LIST,
      UserPermission.CAS1_VIEW_OUT_OF_SERVICE_BEDS,
    ),
  ),

  CAS1_CRU_MEMBER(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.cruMember,
    permissions = listOf(
      UserPermission.CAS1_APPLICATION_WITHDRAW_OTHERS,
      UserPermission.CAS1_BOOKING_CHANGE_DATES,
      UserPermission.CAS1_BOOKING_WITHDRAW,
      UserPermission.CAS1_NATIONAL_OCCUPANCY_VIEW,
      UserPermission.CAS1_OFFLINE_APPLICATION_VIEW,
      UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE,
      UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE_BED_ON_HOLD,
      UserPermission.CAS1_OUT_OF_SERVICE_BED_CANCEL,
      UserPermission.CAS1_PLACEMENT_REQUEST_RECORD_UNABLE_TO_MATCH,
      UserPermission.CAS1_PREMISES_CAPACITY_REPORT_VIEW,
      UserPermission.CAS1_PREMISES_LOCAL_RESTRICTIONS_MANAGE,
      UserPermission.CAS1_PREMISES_VIEW,
      UserPermission.CAS1_REQUEST_FOR_PLACEMENT_WITHDRAW_OTHERS,
      UserPermission.CAS1_SPACE_BOOKING_CREATE,
      UserPermission.CAS1_SPACE_BOOKING_LIST,
      UserPermission.CAS1_SPACE_BOOKING_VIEW,
      UserPermission.CAS1_SPACE_BOOKING_WITHDRAW,
      UserPermission.CAS1_TASK_ALLOCATE,
      UserPermission.CAS1_TASKS_LIST,
      UserPermission.CAS1_USER_LIST,
      UserPermission.CAS1_USER_SUMMARY_LIST,
      UserPermission.CAS1_VIEW_CRU_DASHBOARD,
      UserPermission.CAS1_VIEW_MANAGE_TASKS,
      UserPermission.CAS1_VIEW_OUT_OF_SERVICE_BEDS,
    ),
  ),

  /**
   * A temporary role used while rolling out change request functionality
   */
  CAS1_CHANGE_REQUEST_DEV(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.changeRequestDev,
    permissions = listOf(
      // The following will be assigned to CRU MEMBER
      UserPermission.CAS1_CHANGE_REQUEST_LIST,
      UserPermission.CAS1_CHANGE_REQUEST_VIEW,
      UserPermission.CAS1_PLACEMENT_APPEAL_ASSESS,
      UserPermission.CAS1_TRANSFER_ASSESS,
      // The following will be assigned to FUTURE_MANAGER
      UserPermission.CAS1_PLACEMENT_APPEAL_CREATE,
      UserPermission.CAS1_TRANSFER_CREATE,
      UserPermission.CAS1_SPACE_BOOKING_SHORTEN,
    ),
  ),

  CAS1_APPLICANT(ServiceName.approvedPremises, ApprovedPremisesUserRole.applicant),

  CAS1_REPORT_VIEWER(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.reportViewer,
    listOf(
      UserPermission.CAS1_REPORTS_VIEW,
    ),
  ),

  CAS1_REPORT_VIEWER_WITH_PII(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.reportViewerWithPii,
    listOf(
      UserPermission.CAS1_REPORTS_VIEW,
      UserPermission.CAS1_REPORTS_VIEW_WITH_PII,
    ),
  ),

  CAS1_EXCLUDED_FROM_ASSESS_ALLOCATION(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.excludedFromAssessAllocation,
  ),
  CAS1_EXCLUDED_FROM_MATCH_ALLOCATION(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.excludedFromMatchAllocation,
  ),
  CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.excludedFromPlacementApplicationAllocation,
  ),
  CAS1_APPEALS_MANAGER(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.appealsManager,
    listOf(
      UserPermission.CAS1_ASSESS_APPEALED_APPLICATION,
      UserPermission.CAS1_ASSESS_APPLICATION,
      UserPermission.CAS1_PROCESS_AN_APPEAL,
      UserPermission.CAS1_VIEW_ASSIGNED_ASSESSMENTS,
    ),
  ),
  CAS1_JANITOR(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.janitor,
    UserPermission.entries.toList(),
  ),
  CAS1_USER_MANAGER(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.userManager,
    listOf(
      UserPermission.CAS1_USER_LIST,
      UserPermission.CAS1_USER_SUMMARY_LIST,
      UserPermission.CAS1_USER_MANAGEMENT,
    ),
  ),
  CAS1_AP_AREA_MANAGER(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.apAreaManager,
    listOf(
      UserPermission.CAS1_VIEW_MANAGE_TASKS,
      UserPermission.CAS1_TASKS_LIST,
      UserPermission.CAS1_USER_LIST,
      UserPermission.CAS1_USER_SUMMARY_LIST,
    ),
  ),

  CAS3_ASSESSOR(ServiceName.temporaryAccommodation, null),
  CAS3_REFERRER(ServiceName.temporaryAccommodation, null),
  CAS3_REPORTER(ServiceName.temporaryAccommodation, null),
  ;

  fun hasPermission(permission: UserPermission) = permissions.contains(permission)

  companion object {
    fun getAllRolesForService(service: ServiceName) = entries.filter { it.service == service }

    fun valueOf(apiValue: ApprovedPremisesUserRole) = UserRole.entries.first { it.cas1ApiValue == apiValue }

    fun getAllRolesForPermission(permission: UserPermission) = entries.filter { it.permissions.contains(permission) }

    fun getAllRolesExcept(vararg rolesToExclude: UserRole) = UserRole.entries.filter { !rolesToExclude.contains(it) }
  }
}
