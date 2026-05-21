package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

@Suppress("LongParameterList")
@JsonIgnoreProperties(ignoreUnknown = true)
class HealthDetails(
  assessmentId: Long,
  assessmentType: String,
  dateCompleted: OffsetDateTime?,
  assessorSignedDate: OffsetDateTime?,
  initiationDate: OffsetDateTime,
  assessmentStatus: String,
  superStatus: String?,
  limitedAccessOffender: Boolean,
  laterWIPAssessmentExists: Boolean?,
  lastUpdatedDate: OffsetDateTime?,
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
  laterWIPAssessmentExists,
  lastUpdatedDate,
)

@Suppress("LongParameterList")
@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthDetailsInner(
  val generalHealth: Boolean?,
  val generalHealthSpecify: String?,
  val electronicMonitoringSpecify: String?,
  val electronicMonitoringElectricity: Boolean?,
  val electronicMonitoring: Boolean?,
  val generalHeathSpecify: String?,
  val healthIssues: HealthIssue?,
  val drugsMisuse: HealthIssue?,
  val chaoticLifestyle: HealthIssue?,
  val religiousOrCulturalRequirements: HealthIssue?,
  val transportDifficulties: HealthIssue?,
  val employmentCommitments: HealthIssue?,
  val educationCommitments: HealthIssue?,
  val childCareAndCarers: HealthIssue?,
  val disability: HealthIssue?,
  val psychiatricPsychologicalRequirements: HealthIssue?,
  val levelOfMotivation: HealthIssue?,
  val learningDifficulties: HealthIssue?,
  val literacyProblems: HealthIssue?,
  val poorCommunicationSkills: HealthIssue?,
  val needForInterpreter: HealthIssue?,
  val alcoholMisuse: HealthIssue?,
)

@Suppress("LongParameterList")
@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthIssue(
  val community: String?,
  val electronicMonitoring: String?,
  val programme: String?,
)
