package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskManagementPlanFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1OAsysTest.Companion.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockRiskManagementPlan404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulRiskManagementPlanCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import java.time.OffsetDateTime

class Cas3OasysTest : InitialiseDatabasePerClassTestBase() {

  @Nested
  inner class OASysRiskManagement {
    @Test
    fun `No JWT returns 401`() {
      webTestClient.get()
        .uri("/cas3/people/$CRN/oasys/riskManagement")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Return forbidden if user can't access the CRN`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(
        CaseAccessFactory()
          .withCrn(CRN)
          .withUserExcluded(true)
          .produce(),
      )

      webTestClient.get()
        .uri("/cas3/people/$CRN/oasys/riskManagement")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `When no risk management plan exists for the CRN returns OK with empty answers`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())
      apAndOASysMockRiskManagementPlan404Call(CRN)

      val result = webTestClient.get()
        .uri("/cas3/people/$CRN/oasys/riskManagement")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas3OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = null,
        ),
      )
    }

    @Test
    fun `When a risk management plan exists for the CRN, returns OK with the plan answers`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val riskManagementPlan = RiskManagementPlanFactory()
        .withSupervision("The supervision answer")
        .produce()
      apAndOASysMockSuccessfulRiskManagementPlanCall(CRN, riskManagementPlan)

      val result = webTestClient.get()
        .uri("/cas3/people/$CRN/oasys/riskManagement")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas3OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = "The supervision answer",
        ),
      )
    }

    @Test
    fun `Default 6 month suitability strategy, assessment is less than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val riskManagementPlan = RiskManagementPlanFactory()
        .withSupervision("The supervision answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(5))
        .produce()
      apAndOASysMockSuccessfulRiskManagementPlanCall(CRN, riskManagementPlan)

      val result = webTestClient.get()
        .uri("/cas3/people/$CRN/oasys/riskManagement")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas3OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = "The supervision answer",
        ),
      )
    }

    @Test
    fun `Default 6 month suitability strategy, assessment is more than 6 months old, return empty answers`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val riskManagementPlan = RiskManagementPlanFactory()
        .withSupervision("The supervision answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(7))
        .produce()
      apAndOASysMockSuccessfulRiskManagementPlanCall(CRN, riskManagementPlan)

      val result = webTestClient.get()
        .uri("/cas3/people/$CRN/oasys/riskManagement")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas3OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = null,
        ),
      )
    }

    @Test
    fun `Allow all suitability strategy, assessment is more than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val riskManagementPlan = RiskManagementPlanFactory()
        .withSupervision("The supervision answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(7))
        .produce()
      apAndOASysMockSuccessfulRiskManagementPlanCall(CRN, riskManagementPlan)

      val result = webTestClient.get()
        .uri("/cas3/people/$CRN/oasys/riskManagement?suitabilityStrategy=allow_all")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas3OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = "The supervision answer",
        ),
      )
    }

    @Test
    fun `suitabilityStrategy is optional, a blank value defaults to the 6 month strategy`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val riskManagementPlan = RiskManagementPlanFactory()
        .withSupervision("The supervision answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(7))
        .produce()
      apAndOASysMockSuccessfulRiskManagementPlanCall(CRN, riskManagementPlan)

      val result = webTestClient.get()
        .uri("/cas3/people/$CRN/oasys/riskManagement?suitabilityStrategy=")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas3OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = null,
        ),
      )
    }
  }
}
