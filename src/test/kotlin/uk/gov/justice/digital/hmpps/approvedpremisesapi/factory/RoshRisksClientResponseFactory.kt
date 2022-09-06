package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.OtherRoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.Response
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.Risk
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.RoshRiskToSelf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.RoshRisksSummary
import java.time.LocalDateTime

class RoshRisksClientResponseFactory : Factory<RoshRisks> {
  private var roshRiskToSelf: Yielded<RoshRiskToSelf> = {
    RoshRiskToSelf(
      suicide = Risk(
        risk = Response.NO,
        previous = null,
        previousConcernsText = null,
        current = Response.NO,
        currentConcernsText = null
      ),
      custody = Risk(
        risk = Response.NO,
        previous = null,
        previousConcernsText = null,
        current = Response.NO,
        currentConcernsText = null
      ),
      hostelSetting = Risk(
        risk = Response.NO,
        previous = null,
        previousConcernsText = null,
        current = Response.NO,
        currentConcernsText = null
      ),
      vulnerability = Risk(
        risk = Response.NO,
        previous = null,
        previousConcernsText = null,
        current = Response.NO,
        currentConcernsText = null
      ),
      assessedOn = LocalDateTime.now()
    )
  }

  private var otherRoshRisks: Yielded<OtherRoshRisks> = {
    OtherRoshRisks(
      escapeOrAbscond = Response.NO,
      controlIssuesDisruptiveBehaviour = Response.NO,
      breachOfTrust = Response.NO,
      riskToOtherPrisoners = Response.NO,
      assessedOn = LocalDateTime.now()
    )
  }

  private var summary: Yielded<RoshRisksSummary> = {
    RoshRisksSummary(
      whoIsAtRisk = null,
      natureOfRisk = null,
      riskImminence = null,
      riskIncreaseFactors = null,
      riskMitigationFactors = null,
      riskInCommunity = mapOf(),
      riskInCustody = mapOf(),
      assessedOn = null,
      overallRiskLevel = null
    )
  }

  private var assessedOn: Yielded<LocalDateTime?> = { LocalDateTime.now() }

  fun withRiskToSelf(roshRiskToSelf: RoshRiskToSelf) = apply {
    this.roshRiskToSelf = { roshRiskToSelf }
  }

  fun withOtherRoshRisks(otherRoshRisks: OtherRoshRisks) = apply {
    this.otherRoshRisks = { otherRoshRisks }
  }

  fun withSummary(summary: RoshRisksSummary) = apply {
    this.summary = { summary }
  }

  override fun produce() = RoshRisks(
    riskToSelf = this.roshRiskToSelf(),
    otherRisks = this.otherRoshRisks(),
    summary = this.summary(),
    assessedOn = this.assessedOn()
  )
}
