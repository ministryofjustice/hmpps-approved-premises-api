package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummaryInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

class RoshSummaryFactory : AssessmentInfoFactory<RoshSummary>() {
  private var whoAtRisk: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var natureOfRisk: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var riskGreatest: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var riskIncreaseLikelyTo: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var riskReductionLikelyTo: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var factorsAnalysisOfRisk: Yielded<String?> = { null }
  private var factorsStrengthsAndProtective: Yielded<String?> = { null }
  private var factorsSituationsLikelyToOffend: Yielded<String?> = { null }

  fun withWhoAtRisk(whoAtRisk: String?) = apply {
    this.whoAtRisk = { whoAtRisk }
  }

  fun withNatureOfRisk(natureOfRisk: String?) = apply {
    this.natureOfRisk = { natureOfRisk }
  }

  fun withRiskGreatest(riskGreatest: String?) = apply {
    this.riskGreatest = { riskGreatest }
  }

  fun withRiskIncreaseLikelyTo(riskIncreaseLikelyTo: String?) = apply {
    this.riskIncreaseLikelyTo = { riskIncreaseLikelyTo }
  }

  fun withRiskReductionLikelyTo(riskReductionLikelyTo: String?) = apply {
    this.riskReductionLikelyTo = { riskReductionLikelyTo }
  }

  fun withFactorsAnalysisOfRisk(factorsAnalysisOfRisk: String) = apply {
    this.factorsAnalysisOfRisk = { factorsAnalysisOfRisk }
  }

  fun withFactorsStrengthsAndProtective(factorsStrengthsAndProtective: String) = apply {
    this.factorsStrengthsAndProtective = { factorsStrengthsAndProtective }
  }

  fun withFactorsSituationsLikelyToOffend(factorsSituationsLikelyToOffend: String) = apply {
    this.factorsSituationsLikelyToOffend = { factorsSituationsLikelyToOffend }
  }

  override fun produce() = RoshSummary(
    assessmentId = this.assessmentId(),
    assessmentType = this.assessmentType(),
    dateCompleted = this.dateCompleted(),
    assessorSignedDate = this.assessorSignedDate(),
    initiationDate = this.initiationDate(),
    assessmentStatus = this.assessmentStatus(),
    superStatus = this.superStatus(),
    limitedAccessOffender = this.limitedAccessOffender(),
    roshSummary = RoshSummaryInner(
      whoIsAtRisk = this.whoAtRisk(),
      natureOfRisk = this.natureOfRisk(),
      riskGreatest = this.riskGreatest(),
      riskIncreaseLikelyTo = this.riskIncreaseLikelyTo(),
      riskReductionLikelyTo = this.riskReductionLikelyTo(),
      factorsAnalysisOfRisk = this.factorsAnalysisOfRisk(),
      factorsStrengthsAndProtective = this.factorsStrengthsAndProtective(),
      factorsSituationsLikelyToOffend = this.factorsSituationsLikelyToOffend(),
    ),
  )
}
