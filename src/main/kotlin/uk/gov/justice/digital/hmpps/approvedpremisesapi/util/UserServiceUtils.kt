package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification

fun transformQualifications(qualification: ApiUserQualification): UserQualification = when (qualification) {
  ApiUserQualification.emergency -> UserQualification.EMERGENCY
  ApiUserQualification.esap -> UserQualification.ESAP
  ApiUserQualification.lao -> UserQualification.LAO
  ApiUserQualification.pipe -> UserQualification.PIPE
  ApiUserQualification.womens -> UserQualification.WOMENS
  ApiUserQualification.mentalHealthSpecialist -> UserQualification.MENTAL_HEALTH_SPECIALIST
  ApiUserQualification.recoveryFocused -> UserQualification.RECOVERY_FOCUSED
}

fun transformUserRoles(approvedPremisesUserRole: ApprovedPremisesUserRole): UserRole = when (approvedPremisesUserRole) {
  ApprovedPremisesUserRole.assessor -> UserRole.CAS1_ASSESSOR
  ApprovedPremisesUserRole.matcher -> UserRole.CAS1_MATCHER
  ApprovedPremisesUserRole.manager -> UserRole.CAS1_MANAGER
  ApprovedPremisesUserRole.legacyManager -> UserRole.CAS1_LEGACY_MANAGER
  ApprovedPremisesUserRole.futureManager -> UserRole.CAS1_FUTURE_MANAGER
  ApprovedPremisesUserRole.workflowManager -> UserRole.CAS1_WORKFLOW_MANAGER
  ApprovedPremisesUserRole.applicant -> UserRole.CAS1_APPLICANT
  ApprovedPremisesUserRole.roleAdmin -> UserRole.CAS1_ADMIN
  ApprovedPremisesUserRole.reportViewer -> UserRole.CAS1_REPORT_VIEWER
  ApprovedPremisesUserRole.excludedFromAssessAllocation -> UserRole.CAS1_EXCLUDED_FROM_ASSESS_ALLOCATION
  ApprovedPremisesUserRole.excludedFromMatchAllocation -> UserRole.CAS1_EXCLUDED_FROM_MATCH_ALLOCATION
  ApprovedPremisesUserRole.excludedFromPlacementApplicationAllocation -> UserRole.CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION
  ApprovedPremisesUserRole.appealsManager -> UserRole.CAS1_APPEALS_MANAGER
}
