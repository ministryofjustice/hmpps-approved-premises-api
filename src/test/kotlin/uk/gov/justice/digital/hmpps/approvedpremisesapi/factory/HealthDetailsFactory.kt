package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.HealthDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.HealthDetailsInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.HealthIssue

class HealthDetailsFactory : AssessmentInfoFactory<HealthDetails>() {
  private var generalHealth: Yielded<Boolean?> = { false }
  private var generalHealthSpecify: Yielded<String?> = { null }
  private var drugsMisuse: Yielded<HealthIssue?> = { null }
  private var alcoholMisuse: Yielded<HealthIssue?> = { null }

  fun withGeneralHealth(generalHealth: Boolean?, generalHealthSpecify: String?) = apply {
    this.generalHealth = { generalHealth }
    this.generalHealthSpecify = { generalHealthSpecify }
  }

  fun withDrugsMisuse(drugsMisuse: HealthIssue?) = apply {
    this.drugsMisuse = { drugsMisuse }
  }

  fun withAlcoholMisuse(alcoholMisuse: HealthIssue?) = apply {
    this.alcoholMisuse = { alcoholMisuse }
  }

  override fun produce() = HealthDetails(
    assessmentId = this.assessmentId(),
    assessmentType = this.assessmentType(),
    dateCompleted = this.dateCompleted(),
    assessorSignedDate = this.assessorSignedDate(),
    initiationDate = this.initiationDate(),
    assessmentStatus = this.assessmentStatus(),
    superStatus = this.superStatus(),
    limitedAccessOffender = this.limitedAccessOffender(),
    laterWIPAssessmentExists = this.laterWIPAssessmentExists(),
    lastUpdatedDate = this.lastUpdatedDate(),
    health = HealthDetailsInner(
      generalHealth = this.generalHealth(),
      generalHealthSpecify = this.generalHealthSpecify(),
      electronicMonitoringSpecify = null,
      electronicMonitoringElectricity = null,
      electronicMonitoring = null,
      generalHeathSpecify = null,
      healthIssues = null,
      drugsMisuse = this.drugsMisuse(),
      chaoticLifestyle = null,
      religiousOrCulturalRequirements = null,
      transportDifficulties = null,
      employmentCommitments = null,
      educationCommitments = null,
      childCareAndCarers = null,
      disability = null,
      psychiatricPsychologicalRequirements = null,
      levelOfMotivation = null,
      learningDifficulties = null,
      literacyProblems = null,
      poorCommunicationSkills = null,
      needForInterpreter = null,
      alcoholMisuse = this.alcoholMisuse(),
    ),
  )
}
