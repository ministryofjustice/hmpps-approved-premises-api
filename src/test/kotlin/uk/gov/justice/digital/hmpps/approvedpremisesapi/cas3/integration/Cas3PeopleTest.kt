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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockNeedsDetails404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulRiskManagementPlanCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject

class Cas3PeopleTest : InitialiseDatabasePerClassTestBase() {

  @Nested
  inner class OASysRiskManagement {

    @Test
    fun `No JWT returns 401`() {
      webTestClient.get()
        .uri("/cas3/people/CRN/oasys/riskManagement")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Return 404 if needs record can't be found for the CRN`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())
      apAndOASysMockNeedsDetails404Call(CRN)

      webTestClient.get()
        .uri("/cas3/people/$CRN/oasys/riskManagement")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun success() {
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
  }
}
