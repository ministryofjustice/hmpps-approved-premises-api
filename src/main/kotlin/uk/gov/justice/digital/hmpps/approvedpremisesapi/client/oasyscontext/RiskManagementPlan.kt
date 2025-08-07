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
  val furtherConsiderations: String?,
  val additionalComments: String?,
  val contingencyPlans: String?,
  val victimSafetyPlanning: String?,
  val interventionsAndTreatment: String?,
  val monitoringAndControl: String?,
  val supervision: String?,
  val keyInformationAboutCurrentSituation: String?,
)
