package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.OffenceDetailsInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

class OffenceDetailsFactory : AssessmentInfoFactory<OffenceDetails>() {
  private var offenceAnalysis: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var othersInvolved: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var issueContributingToRisk: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var offenceMotivation: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var victimImpact: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var victimPerpetratorRel: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var victimInfo: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var patternOffending: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var acceptsResponsibility: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }

  fun withOffenceAnalysis(offenceAnalysis: String) = apply {
    this.offenceAnalysis = { offenceAnalysis }
  }

  fun withOthersInvolved(othersInvolved: String) = apply {
    this.othersInvolved = { othersInvolved }
  }

  fun withIssueContributingToRisk(issueContributingToRisk: String) = apply {
    this.issueContributingToRisk = { issueContributingToRisk }
  }

  fun withOffenceMotivation(offenceMotivation: String) = apply {
    this.offenceMotivation = { offenceMotivation }
  }

  fun withVictimImpact(victimImpact: String) = apply {
    this.victimImpact = { victimImpact }
  }

  fun withVictimPerpetratorRel(victimPerpetratorRel: String) = apply {
    this.victimPerpetratorRel = { victimPerpetratorRel }
  }

  fun withVictimInfo(victimInfo: String) = apply {
    this.victimInfo = { victimInfo }
  }

  fun withPatternOffending(patternOffending: String) = apply {
    this.patternOffending = { patternOffending }
  }

  fun withAcceptsResponsibility(acceptsResponsibility: String) = apply {
    this.acceptsResponsibility = { acceptsResponsibility }
  }

  override fun produce() = OffenceDetails(
    assessmentId = this.assessmentId(),
    assessmentType = this.assessmentType(),
    dateCompleted = this.dateCompleted(),
    assessorSignedDate = this.assessorSignedDate(),
    initiationDate = this.initiationDate(),
    assessmentStatus = this.assessmentStatus(),
    superStatus = this.superStatus(),
    limitedAccessOffender = this.limitedAccessOffender(),
    offence = OffenceDetailsInner(
      offenceAnalysis = this.offenceAnalysis(),
      othersInvolved = this.othersInvolved(),
      issueContributingToRisk = this.issueContributingToRisk(),
      offenceMotivation = this.offenceMotivation(),
      victimImpact = this.victimImpact(),
      victimPerpetratorRel = this.victimPerpetratorRel(),
      victimInfo = this.victimInfo(),
      patternOffending = this.patternOffending(),
      acceptsResponsibility = this.acceptsResponsibility(),
    ),
  )
}
