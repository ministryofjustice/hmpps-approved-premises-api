package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.model.oasyscontext

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RoshRatingsInner

class RoshRatingsTest {
  @Test
  fun `determineOverallRiskLevel returns LOW when highest individual risk is LOW`() {
    val roshRatings = RoshRatingsInner(
      riskChildrenCommunity = RiskLevel.LOW,
      riskChildrenCustody = RiskLevel.LOW,
      riskPrisonersCustody = RiskLevel.LOW,
      riskStaffCustody = RiskLevel.LOW,
      riskStaffCommunity = RiskLevel.LOW,
      riskKnownAdultCustody = RiskLevel.LOW,
      riskKnownAdultCommunity = RiskLevel.LOW,
      riskPublicCustody = RiskLevel.LOW,
      riskPublicCommunity = RiskLevel.LOW,
    )

    assertThat(roshRatings.determineOverallRiskLevel()).isEqualTo(RiskLevel.LOW)
  }

  @Test
  fun `determineOverallRiskLevel returns MEDIUM when highest individual risk is MEDIUM`() {
    val roshRatings = RoshRatingsInner(
      riskChildrenCommunity = RiskLevel.LOW,
      riskChildrenCustody = RiskLevel.LOW,
      riskPrisonersCustody = RiskLevel.LOW,
      riskStaffCustody = RiskLevel.LOW,
      riskStaffCommunity = RiskLevel.LOW,
      riskKnownAdultCustody = RiskLevel.LOW,
      riskKnownAdultCommunity = RiskLevel.LOW,
      riskPublicCustody = RiskLevel.LOW,
      riskPublicCommunity = RiskLevel.MEDIUM,
    )

    assertThat(roshRatings.determineOverallRiskLevel()).isEqualTo(RiskLevel.MEDIUM)
  }

  @Test
  fun `determineOverallRiskLevel returns HIGH when highest individual risk is HIGH`() {
    val roshRatings = RoshRatingsInner(
      riskChildrenCommunity = RiskLevel.LOW,
      riskChildrenCustody = RiskLevel.LOW,
      riskPrisonersCustody = RiskLevel.LOW,
      riskStaffCustody = RiskLevel.LOW,
      riskStaffCommunity = RiskLevel.LOW,
      riskKnownAdultCustody = RiskLevel.LOW,
      riskKnownAdultCommunity = RiskLevel.LOW,
      riskPublicCustody = RiskLevel.LOW,
      riskPublicCommunity = RiskLevel.HIGH,
    )

    assertThat(roshRatings.determineOverallRiskLevel()).isEqualTo(RiskLevel.HIGH)
  }

  @Test
  fun `determineOverallRiskLevel returns VERY_HIGH when highest individual risk is VERY_HIGH`() {
    val roshRatings = RoshRatingsInner(
      riskChildrenCommunity = RiskLevel.LOW,
      riskChildrenCustody = RiskLevel.LOW,
      riskPrisonersCustody = RiskLevel.LOW,
      riskStaffCustody = RiskLevel.LOW,
      riskStaffCommunity = RiskLevel.LOW,
      riskKnownAdultCustody = RiskLevel.LOW,
      riskKnownAdultCommunity = RiskLevel.LOW,
      riskPublicCustody = RiskLevel.LOW,
      riskPublicCommunity = RiskLevel.VERY_HIGH,
    )

    assertThat(roshRatings.determineOverallRiskLevel()).isEqualTo(RiskLevel.VERY_HIGH)
  }
}
