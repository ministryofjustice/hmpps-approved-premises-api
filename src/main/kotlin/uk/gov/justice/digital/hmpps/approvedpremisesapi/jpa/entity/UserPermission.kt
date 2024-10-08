package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

enum class UserPermission {
  CAS1_ADHOC_BOOKING_CREATE,
  CAS1_ASSESS_APPEALED_APPLICATION,
  CAS1_ASSESS_APPLICATION,
  CAS1_ASSESS_PLACEMENT_APPLICATION,
  CAS1_ASSESS_PLACEMENT_REQUEST,
  CAS1_BOOKING_CREATE,
  CAS1_BOOKING_WITHDRAW,
  CAS1_BOOKING_CHANGE_DATES,
  CAS1_OUT_OF_SERVICE_BED_CREATE,
  CAS1_PROCESS_AN_APPEAL,
  CAS1_VIEW_ASSIGNED_ASSESSMENTS,
  CAS1_VIEW_CRU_DASHBOARD,
  CAS1_VIEW_MANAGE_TASKS,
  CAS1_VIEW_OUT_OF_SERVICE_BEDS,
  CAS1_SPACE_BOOKING_LIST,
  CAS1_SPACE_BOOKING_RECORD_ARRIVAL,
  CAS1_SPACE_BOOKING_RECORD_DEPARTURE,
  CAS1_SPACE_BOOKING_RECORD_KEYWORKER,
  CAS1_SPACE_BOOKING_VIEW,
  CAS1_SPACE_BOOKING_WITHDRAW,
  CAS1_PREMISES_VIEW_SUMMARY,
  CAS1_APPLICATION_WITHDRAW_OTHERS,
  CAS1_REPORTS_VIEW,
  CAS1_REQUEST_FOR_PLACEMENT_WITHDRAW_OTHERS,
}
