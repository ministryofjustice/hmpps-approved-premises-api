package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RoshRatings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RoshRatingsInner

class RoshRatingsFactory : AssessmentInfoFactory<RoshRatings>() {
  private var riskChildrenCommunity: Yielded<RiskLevel> = { RiskLevel.LOW }
  private var riskChildrenCustody: Yielded<RiskLevel> = { RiskLevel.LOW }
  private var riskPrisonersCustody: Yielded<RiskLevel> = { RiskLevel.LOW }
  private var riskStaffCustody: Yielded<RiskLevel> = { RiskLevel.LOW }
  private var riskStaffCommunity: Yielded<RiskLevel> = { RiskLevel.LOW }
  private var riskKnownAdultCustody: Yielded<RiskLevel> = { RiskLevel.LOW }
  private var riskKnownAdultCommunity: Yielded<RiskLevel> = { RiskLevel.LOW }
  private var riskPublicCustody: Yielded<RiskLevel> = { RiskLevel.LOW }
  private var riskPublicCommunity: Yielded<RiskLevel> = { RiskLevel.LOW }

  fun withRiskChildrenCommunity(riskChildrenCommunity: RiskLevel) = apply {
    this.riskChildrenCommunity = { riskChildrenCommunity }
  }

  fun withRiskChildrenCustody(riskChildrenCustody: RiskLevel) = apply {
    this.riskChildrenCustody = { riskChildrenCustody }
  }

  fun withRiskPrisonersCustody(riskPrisonersCustody: RiskLevel) = apply {
    this.riskPrisonersCustody = { riskPrisonersCustody }
  }

  fun withRiskStaffCustody(riskStaffCustody: RiskLevel) = apply {
    this.riskStaffCustody = { riskStaffCustody }
  }

  fun withRiskStaffCommunity(riskStaffCommunity: RiskLevel) = apply {
    this.riskStaffCommunity = { riskStaffCommunity }
  }

  fun withRiskKnownAdultCustody(riskKnownAdultCustody: RiskLevel) = apply {
    this.riskKnownAdultCustody = { riskKnownAdultCustody }
  }

  fun withRiskKnownAdultCommunity(riskKnownAdultCommunity: RiskLevel) = apply {
    this.riskKnownAdultCommunity = { riskKnownAdultCommunity }
  }

  fun withRiskPublicCustody(riskPublicCustody: RiskLevel) = apply {
    this.riskPublicCustody = { riskPublicCustody }
  }

  fun withRiskPublicCommunity(riskPublicCommunity: RiskLevel) = apply {
    this.riskPublicCommunity = { riskPublicCommunity }
  }

  override fun produce() = RoshRatings(
    assessmentId = this.assessmentId(),
    assessmentType = this.assessmentType(),
    dateCompleted = this.dateCompleted(),
    assessorSignedDate = this.assessorSignedDate(),
    initiationDate = this.initiationDate(),
    assessmentStatus = this.assessmentStatus(),
    superStatus = this.superStatus(),
    limitedAccessOffender = this.limitedAccessOffender(),
    rosh = RoshRatingsInner(
      riskChildrenCommunity = this.riskChildrenCommunity(),
      riskChildrenCustody = this.riskChildrenCustody(),
      riskPrisonersCustody = this.riskPrisonersCustody(),
      riskStaffCustody = this.riskStaffCustody(),
      riskStaffCommunity = this.riskStaffCommunity(),
      riskKnownAdultCustody = this.riskKnownAdultCustody(),
      riskKnownAdultCommunity = this.riskKnownAdultCommunity(),
      riskPublicCustody = this.riskPublicCustody(),
      riskPublicCommunity = this.riskPublicCommunity(),
    ),
  )
}
