package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.allocations.rules

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocatorRuleOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.rules.FemaleAssessmentRule
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import java.time.OffsetDateTime

class FemaleAssessmentRuleTest {
  val femaleAssessmentRule = FemaleAssessmentRule(0)

  @Nested
  inner class EvaluateAssessment {
    @Test
    fun `Returns AllocateByQualification when the application is for Approved Premises, is submitted, and the application is a women's application`() {
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
        .withIsWomensApplication(true)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = femaleAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.AllocateByQualification(UserQualification.WOMENS))
    }

    @Test
    fun `Returns Skip if the application is not for Approved Premises`() {
      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea {
          ApAreaEntityFactory()
            .produce()
        }
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

      val result = femaleAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @Test
    fun `Returns Skip if the application is not submitted`() {
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
        .withIsWomensApplication(true)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = femaleAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @Test
    fun `Returns Skip if the application is not a women's application`() {
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
        .withIsWomensApplication(false)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = femaleAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }
  }

  @Nested
  inner class EvaluatePlacementApplication {
    @Test
    fun `Always returns Skip`() {
      val result = femaleAssessmentRule.evaluatePlacementApplication(mockk<PlacementApplicationEntity>())

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }
  }

  @Nested
  inner class EvaluatePlacementRequest {
    @Test
    fun `Always returns Skip`() {
      val result = femaleAssessmentRule.evaluatePlacementRequest(mockk<PlacementRequestEntity>())

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }
  }
}
