package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext

import java.time.OffsetDateTime

class HealthDetails(
  assessmentId: Long,
  assessmentType: String,
  dateCompleted: OffsetDateTime?,
  assessorSignedDate: OffsetDateTime?,
  initiationDate: OffsetDateTime,
  assessmentStatus: String,
  superStatus: String?,
  limitedAccessOffender: Boolean,
  val health: HealthDetailsInner,
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

data class HealthDetailsInner(
  val generalHealth: Boolean?,
  val generalHealthSpecify: String?,
  val electronicMonitoringSpecify: String?,
  val electronicMonitoringElectricity: Boolean?,
  val electronicMonitoring: Boolean?,
  val healthIssues: HealthDetail?,
  val drugsMisuse: HealthDetail?,
  val chaoticLifestyle: HealthDetail?,
  val religiousOrCulturalRequirements: HealthDetail?,
  val transportDifficulties: HealthDetail?,
  val employmentCommitments: HealthDetail?,
  val educationCommitments: HealthDetail?,
  val childCareAndCarers: HealthDetail?,
  val disability: HealthDetail?,
  val psychiatricPsychologicalRequirements: HealthDetail?,
  val levelOfMotivation: HealthDetail?,
  val learningDifficulties: HealthDetail?,
  val literacyProblems: HealthDetail?,
  val poorCommunicationSkills: HealthDetail?,
  val needForInterpreter: HealthDetail?,
  val alcoholMisuse: HealthDetail?,
)

data class HealthDetail(
  val community: String?,
  val electronicMonitoring: String?,
  val programme: String?,
)
