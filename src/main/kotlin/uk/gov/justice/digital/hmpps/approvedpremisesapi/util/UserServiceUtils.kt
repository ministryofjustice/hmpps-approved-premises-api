package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification

fun transformQualifications(qualification: ApiUserQualification): UserQualification = when (qualification) {
  ApiUserQualification.emergency -> UserQualification.EMERGENCY
  ApiUserQualification.esap -> UserQualification.ESAP
  ApiUserQualification.lao -> UserQualification.LAO
  ApiUserQualification.pipe -> UserQualification.PIPE
  ApiUserQualification.mentalHealthSpecialist -> UserQualification.MENTAL_HEALTH_SPECIALIST
  ApiUserQualification.recoveryFocused -> UserQualification.RECOVERY_FOCUSED
}
