package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext

import java.time.OffsetDateTime

class RiskManagementPlan(
  assessmentId: Long,
  assessmentType: String,
  dateCompleted: OffsetDateTime?,
  assessorSignedDate: OffsetDateTime?,
  initiationDate: OffsetDateTime,
  assessmentStatus: String,
  superStatus: String?,
  limitedAccessOffender: Boolean,
  val riskManagementPlan: RiskManagementPlanInner?,
) : AssessmentInfo(
  assessmentId,
  assessmentType,
  dateCompleted,
  assessorSignedDate,
  initiationDate,
  assessmentStatus,
  superStatus,
  limitedAccessOffender,
)

data class RiskManagementPlanInner(
  // RM28
  val furtherConsiderations: String?,
  // RM35
  val additionalComments: String?,
  // RM35
  val contingencyPlans: String?,
  // RM33
  val victimSafetyPlanning: String?,
  // RM32
  val interventionsAndTreatment: String?,
  // RM31
  val monitoringAndControl: String?,
  // RM30
  val supervision: String?,
  // RM28.1
  val keyInformationAboutCurrentSituation: String?,
)
