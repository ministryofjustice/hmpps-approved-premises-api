package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RiskManagementPlanInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

class RiskManagementPlanFactory : AssessmentInfoFactory<RiskManagementPlan>() {
  private var furtherConsiderations: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var additionalComments: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var contingencyPlans: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var victimSafetyPlanning: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var interventionsAndTreatment: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var monitoringAndControl: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var supervision: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var keyInformationAboutCurrentSituation: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }

  fun withFurtherConsiderations(furtherConsiderations: String) = apply {
    this.furtherConsiderations = { furtherConsiderations }
  }

  fun withAdditionalComments(additionalComments: String) = apply {
    this.additionalComments = { additionalComments }
  }

  fun withContingencyPlans(contingencyPlans: String) = apply {
    this.contingencyPlans = { contingencyPlans }
  }

  fun withVictimSafetyPlanning(victimSafetyPlanning: String) = apply {
    this.victimSafetyPlanning = { victimSafetyPlanning }
  }

  fun withInterventionsAndTreatment(interventionsAndTreatment: String) = apply {
    this.interventionsAndTreatment = { interventionsAndTreatment }
  }

  fun withMonitoringAndControl(monitoringAndControl: String) = apply {
    this.monitoringAndControl = { monitoringAndControl }
  }

  fun withSupervision(supervision: String) = apply {
    this.supervision = { supervision }
  }

  fun withKeyInformationAboutCurrentSituation(keyInformationAboutCurrentSituation: String) = apply {
    this.keyInformationAboutCurrentSituation = { keyInformationAboutCurrentSituation }
  }

  override fun produce() = RiskManagementPlan(
    assessmentId = this.assessmentId(),
    assessmentType = this.assessmentType(),
    dateCompleted = this.dateCompleted(),
    assessorSignedDate = this.assessorSignedDate(),
    initiationDate = this.initiationDate(),
    assessmentStatus = this.assessmentStatus(),
    superStatus = this.superStatus(),
    limitedAccessOffender = this.limitedAccessOffender(),
    riskManagementPlan = RiskManagementPlanInner(
      furtherConsiderations = this.furtherConsiderations(),
      additionalComments = this.additionalComments(),
      contingencyPlans = this.contingencyPlans(),
      victimSafetyPlanning = this.victimSafetyPlanning(),
      interventionsAndTreatment = this.interventionsAndTreatment(),
      monitoringAndControl = this.monitoringAndControl(),
      supervision = this.supervision(),
      keyInformationAboutCurrentSituation = this.keyInformationAboutCurrentSituation(),
    ),
  )
}
