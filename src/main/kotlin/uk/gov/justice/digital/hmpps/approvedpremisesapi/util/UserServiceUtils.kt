package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification

fun transformQualifications(qualification: ApiUserQualification): UserQualification = when (qualification) {
  ApiUserQualification.EMERGENCY -> UserQualification.EMERGENCY
  ApiUserQualification.ESAP -> UserQualification.ESAP
  ApiUserQualification.LAO -> UserQualification.LAO
  ApiUserQualification.PIPE -> UserQualification.PIPE
  ApiUserQualification.MENTAL_HEALTH_SPECIALIST -> UserQualification.MENTAL_HEALTH_SPECIALIST
  ApiUserQualification.RECOVERY_FOCUSED -> UserQualification.RECOVERY_FOCUSED
}
