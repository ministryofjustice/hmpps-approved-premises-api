package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.allocations.rules

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocatorRuleOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.rules.EmergencyAndShortNoticeAssessmentRule
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.rules.EmergencyAndShortNoticeAssessmentRuleConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import java.time.OffsetDateTime

class EmergencyAndShortNoticeAssessmentRuleTest {
  val emergencyAssessmentRule = EmergencyAndShortNoticeAssessmentRule(
    0,
    EmergencyAndShortNoticeAssessmentRuleConfig(
      mapOf(
        "wales" to "WALES-USER",
        "north-west" to "NORTHWEST-USER",
        "south-west-south-central" to "SWSC-USER",
      ),
    ),
  )

  @Nested
  inner class EvaluateAssessment {
    @CsvSource(value = ["Wales,WALES-USER", "North West,NORTHWEST-USER", "South West & South Central,SWSC-USER"])
    @ParameterizedTest
    fun `Returns AllocateToUser with configured username for the application's region when the application is for Approved Premises, is submitted, and the application is an emergency`(
      regionName: String,
      expectedUser: String,
    ) {
      val apArea = ApAreaEntityFactory()
        .withName(regionName)
        .produce()

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(apArea)
        .produce()

      val createdByUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(OffsetDateTime.now())
        .withIsEmergencyApplication(true)
        .produce()
        .apply {
          this.apArea = apArea
        }

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = emergencyAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.AllocateToUser(expectedUser))
    }

    @Test
    fun `Returns Skip if the application is not for Approved Premises`() {
      val apArea = ApAreaEntityFactory()
        .produce()

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(apArea)
        .produce()

      val createdByUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withProbationRegion(probationRegion)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = emergencyAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @Test
    fun `Returns Skip if the application is not submitted`() {
      val apArea = ApAreaEntityFactory()
        .produce()

      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea {
          ApAreaEntityFactory()
            .produce()
        }
        .produce()

      val createdByUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(null)
        .withIsEmergencyApplication(true)
        .produce()
        .apply {
          this.apArea = apArea
        }

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = emergencyAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @Test
    fun `Returns Skip if the application is not emergency`() {
      val apArea = ApAreaEntityFactory()
        .produce()

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(apArea)
        .produce()

      val createdByUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(OffsetDateTime.now())
        .withIsEmergencyApplication(false)
        .produce()
        .apply {
          this.apArea = apArea
        }

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = emergencyAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @Test
    fun `Returns Skip with when no region is assigned to the application`() {
      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea {
          ApAreaEntityFactory()
            .produce()
        }
        .produce()

      val createdByUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(OffsetDateTime.now())
        .withIsEmergencyApplication(true)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = emergencyAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @Test
    fun `Returns Skip with when no user is configured for the region`() {
      val apArea = ApAreaEntityFactory()
        .produce()

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(apArea)
        .withName("Unknown Region")
        .produce()

      val createdByUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(OffsetDateTime.now())
        .withIsEmergencyApplication(true)
        .produce()
        .apply {
          this.apArea = apArea
        }

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = emergencyAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }
  }

  @Nested
  inner class EvaluatePlacementApplication {
    @Test
    fun `Always returns Skip`() {
      val result = emergencyAssessmentRule.evaluatePlacementApplication(mockk<PlacementApplicationEntity>())

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }
  }

  @Nested
  inner class EvaluatePlacementRequest {
    @Test
    fun `Always returns Skip`() {
      val result = emergencyAssessmentRule.evaluatePlacementRequest(mockk<PlacementRequestEntity>())

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }
  }
}
