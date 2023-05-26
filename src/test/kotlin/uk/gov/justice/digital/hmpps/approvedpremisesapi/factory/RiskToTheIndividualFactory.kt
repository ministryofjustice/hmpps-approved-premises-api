package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskToTheIndividualInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

class RiskToTheIndividualFactory : AssessmentInfoFactory<RisksToTheIndividual>() {
  private var currentConcernsSelfHarmSuicide: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var previousConcernsSelfHarmSuicide: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var currentCustodyHostelCoping: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var previousCustodyHostelCoping: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var currentVulnerability: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var previousVulnerability: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var riskOfSeriousHarm: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var currentConcernsBreachOfTrustText: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }

  fun withCurrentConcernsSelfHarmSuicide(currentConcernsSelfHarmSuicide: String) = apply {
    this.currentConcernsSelfHarmSuicide = { currentConcernsSelfHarmSuicide }
  }

  fun withPreviousConcernsSelfHarmSuicide(previousConcernsSelfHarmSuicide: String) = apply {
    this.previousConcernsSelfHarmSuicide = { previousConcernsSelfHarmSuicide }
  }

  fun withCurrentCustodyHostelCoping(currentCustodyHostelCoping: String) = apply {
    this.currentCustodyHostelCoping = { currentCustodyHostelCoping }
  }

  fun withPreviousCustodyHostelCoping(previousCustodyHostelCoping: String) = apply {
    this.previousCustodyHostelCoping = { previousCustodyHostelCoping }
  }

  fun withCurrentVulnerability(currentVulnerability: String) = apply {
    this.currentVulnerability = { currentVulnerability }
  }

  fun withPreviousVulnerability(previousVulnerability: String) = apply {
    this.previousVulnerability = { previousVulnerability }
  }

  fun withRiskOfSeriousHarm(riskOfSeriousHarm: String) = apply {
    this.riskOfSeriousHarm = { riskOfSeriousHarm }
  }

  fun withCurrentConcernsBreachOfTrustText(currentConcernsBreachOfTrustText: String) = apply {
    this.currentConcernsBreachOfTrustText = { currentConcernsBreachOfTrustText }
  }

  override fun produce() = RisksToTheIndividual(
    assessmentId = this.assessmentId(),
    assessmentType = this.assessmentType(),
    dateCompleted = this.dateCompleted(),
    assessorSignedDate = this.assessorSignedDate(),
    initiationDate = this.initiationDate(),
    assessmentStatus = this.assessmentStatus(),
    superStatus = this.superStatus(),
    limitedAccessOffender = this.limitedAccessOffender(),
    riskToTheIndividual = RiskToTheIndividualInner(
      currentConcernsSelfHarmSuicide = this.currentConcernsSelfHarmSuicide(),
      previousConcernsSelfHarmSuicide = this.previousConcernsSelfHarmSuicide(),
      currentCustodyHostelCoping = this.currentCustodyHostelCoping(),
      previousCustodyHostelCoping = this.previousCustodyHostelCoping(),
      currentVulnerability = this.currentVulnerability(),
      previousVulnerability = this.previousVulnerability(),
      riskOfSeriousHarm = this.riskOfSeriousHarm(),
      currentConcernsBreachOfTrustText = this.currentConcernsBreachOfTrustText(),
    ),
  )
}
