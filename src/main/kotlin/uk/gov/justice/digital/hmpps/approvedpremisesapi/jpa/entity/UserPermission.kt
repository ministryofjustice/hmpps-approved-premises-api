package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

enum class UserPermission {
  CAS1_ADHOC_BOOKING_CREATE,

  /**
   * If the user can be allocated and assess an appealed application
   * If given this role the user must also have CAS1_ASSESS_APPLICATION
   */
  CAS1_ASSESS_APPEALED_APPLICATION,

  /**
   * If the user can be allocated and assess an application
   */
  CAS1_ASSESS_APPLICATION,

  /**
   * If the user can be allocated and assess placement applications
   */
  CAS1_ASSESS_PLACEMENT_APPLICATION,

  CAS1_BOOKING_CREATE,
  CAS1_BOOKING_WITHDRAW,
  CAS1_BOOKING_CHANGE_DATES,
  CAS1_OUT_OF_SERVICE_BED_CREATE,
  CAS1_OUT_OF_SERVICE_BED_CANCEL,

  /**
   * If the user can record an appeal against a rejected application
   */
  CAS1_PROCESS_AN_APPEAL,

  /**
   * Used for both listing user summaries (e.g. for drop-downs) and listing complete
   * user information (e.g. for user management). Ideally this would be split
   */
  CAS1_USER_LIST,
  CAS1_USER_MANAGEMENT,

  /**
   * If the user has general access to the 'assess' tile/menu option
   */
  CAS1_VIEW_ASSIGNED_ASSESSMENTS,
  CAS1_VIEW_CRU_DASHBOARD,
  CAS1_VIEW_MANAGE_TASKS,
  CAS1_VIEW_OUT_OF_SERVICE_BEDS,
  CAS1_SPACE_BOOKING_CREATE,
  CAS1_SPACE_BOOKING_LIST,
  CAS1_SPACE_BOOKING_RECORD_ARRIVAL,
  CAS1_SPACE_BOOKING_RECORD_DEPARTURE,
  CAS1_SPACE_BOOKING_RECORD_NON_ARRIVAL,
  CAS1_SPACE_BOOKING_RECORD_KEYWORKER,
  CAS1_SPACE_BOOKING_VIEW,
  CAS1_SPACE_BOOKING_WITHDRAW,
  CAS1_PREMISES_VIEW,
  CAS1_PREMISES_MANAGE,
  CAS1_APPLICATION_WITHDRAW_OTHERS,

  /**
   * View reports, excluding those containing PII
   */
  CAS1_REPORTS_VIEW,
  CAS1_REPORTS_VIEW_WITH_PII,
  CAS1_REQUEST_FOR_PLACEMENT_WITHDRAW_OTHERS,
}
