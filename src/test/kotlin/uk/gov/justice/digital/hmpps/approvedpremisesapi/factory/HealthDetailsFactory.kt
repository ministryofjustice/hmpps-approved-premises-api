package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.HealthDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.HealthDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.HealthDetailsInner

class HealthDetailsFactory : AssessmentInfoFactory<HealthDetails>() {
  private var generalHealth: Yielded<Boolean> = { false }
  private var generalHealthSpecify: Yielded<String?> = { null }
  private var electronicMonitoring: Yielded<Boolean> = { false }
  private var electronicMonitoringSpecify: Yielded<String?> = { null }
  private var electronicMonitoringElectricity: Yielded<Boolean?> = { null }
  private var healthIssues: Yielded<HealthDetail?> = { null }
  private var drugsMisuse: Yielded<HealthDetail?> = { null }
  private var chaoticLifestyle: Yielded<HealthDetail?> = { null }
  private var religiousOrCulturalRequirements: Yielded<HealthDetail?> = { null }
  private var transportDifficulties: Yielded<HealthDetail?> = { null }
  private var employmentCommitments: Yielded<HealthDetail?> = { null }
  private var educationCommitments: Yielded<HealthDetail?> = { null }
  private var childCareAndCarers: Yielded<HealthDetail?> = { null }
  private var disability: Yielded<HealthDetail?> = { null }
  private var psychiatricPsychologicalRequirements: Yielded<HealthDetail?> = { null }
  private var levelOfMotivation: Yielded<HealthDetail?> = { null }
  private var learningDifficulties: Yielded<HealthDetail?> = { null }
  private var literacyProblems: Yielded<HealthDetail?> = { null }
  private var poorCommunicationSkills: Yielded<HealthDetail?> = { null }
  private var needForInterpreter: Yielded<HealthDetail?> = { null }
  private var alcoholMisuse: Yielded<HealthDetail?> = { null }

  fun withGeneralHealth(provided: Boolean, value: String) = apply {
    this.generalHealth = { provided }
    this.generalHealthSpecify = { value }
  }

  fun withElectronicMonitoring(provided: Boolean, value: String) = apply {
    this.electronicMonitoring = { provided }
    this.electronicMonitoringSpecify = { value }
  }

  fun electronicMonitoringElectricity(electronicMonitoringElectricity: Boolean?) = apply {
    this.electronicMonitoringElectricity = { electronicMonitoringElectricity }
  }

  fun withHealthIssues(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.healthIssues = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withDrugsMisuse(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.drugsMisuse = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withChaoticLifestyle(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.chaoticLifestyle = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withReligiousOrCulturalRequirements(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.religiousOrCulturalRequirements = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withTransportDifficulties(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.transportDifficulties = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withEmploymentCommitments(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.employmentCommitments = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withEducationCommitments(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.educationCommitments = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withChildCareAndCarers(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.childCareAndCarers = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withDisability(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.disability = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withPsychiatricPsychologicalRequirements(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.psychiatricPsychologicalRequirements = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withLevelOfMotivation(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.levelOfMotivation = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withLearningDifficulties(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.learningDifficulties = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withLiteracyProblems(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.literacyProblems = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withPoorCommunicationSkills(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.poorCommunicationSkills = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withNeedForInterpreter(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.needForInterpreter = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
  }

  fun withAlcoholMisuse(community: String?, electronicMonitoring: String?, programme: String?) = apply {
    this.alcoholMisuse = {
      HealthDetail(
        community = community,
        electronicMonitoring = electronicMonitoring,
        programme = programme,
      )
    }
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
    health = HealthDetailsInner(
      generalHealth = this.generalHealth(),
      generalHealthSpecify = this.generalHealthSpecify(),
      electronicMonitoring = this.electronicMonitoring(),
      electronicMonitoringSpecify = this.electronicMonitoringSpecify(),
      electronicMonitoringElectricity = this.electronicMonitoringElectricity(),
      healthIssues = this.healthIssues(),
      drugsMisuse = this.drugsMisuse(),
      chaoticLifestyle = this.chaoticLifestyle(),
      religiousOrCulturalRequirements = this.religiousOrCulturalRequirements(),
      transportDifficulties = this.transportDifficulties(),
      employmentCommitments = this.employmentCommitments(),
      educationCommitments = this.educationCommitments(),
      childCareAndCarers = this.childCareAndCarers(),
      disability = this.disability(),
      psychiatricPsychologicalRequirements = this.psychiatricPsychologicalRequirements(),
      levelOfMotivation = this.levelOfMotivation(),
      learningDifficulties = this.learningDifficulties(),
      literacyProblems = this.literacyProblems(),
      poorCommunicationSkills = this.poorCommunicationSkills(),
      needForInterpreter = this.needForInterpreter(),
      alcoholMisuse = this.alcoholMisuse(),
    ),
  )
}
