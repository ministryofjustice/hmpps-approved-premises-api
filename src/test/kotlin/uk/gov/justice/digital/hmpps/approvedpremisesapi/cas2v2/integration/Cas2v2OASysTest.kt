package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model.Cas2v2OASysAssessmentMetadataDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model.Cas2v2OAsysRiskToSelfDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.client.apandoasys.OASysAssessmentSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockAssessmentSummaryNotFound
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockRiskToTheIndividual404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulRiskToTheIndividualCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOAsysMockAssessmentSummaryResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject

class Cas2v2OASysTest : Cas2v2IntegrationTestBase() {

  @Nested
  @DisplayName("GET /cas2v2/people/{crn}/oasys/metadata")
  inner class GetAssessmentMetadata {

    @Test
    fun `Get without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2v2/people/CRN123/oasys/metadata")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Cas2v2NonReferrerRoles
    @ParameterizedTest
    fun `Get without referrer role returns 403`(role: RoleAndAuthSource) {
      webTestClient.get()
        .uri("/cas2v2/people/CRN123/oasys/metadata")
        .addJwtForRoleAndAuthSource(role)
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Cas2v2ReferrerRoles
    @ParameterizedTest
    fun `Assessment not available`(role: RoleAndAuthSource) {
      apAndOASysMockAssessmentSummaryNotFound("CRN123")

      val result = webTestClient.get()
        .uri("/cas2v2/people/CRN123/oasys/metadata")
        .addJwtForRoleAndAuthSource(role)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas2v2OASysAssessmentMetadataDto>()

      assertThat(result.hasApplicableAssessment).isFalse
    }

    @Cas2v2ReferrerRoles
    @ParameterizedTest
    fun `Assessment available`(role: RoleAndAuthSource) {
      apAndOAsysMockAssessmentSummaryResponse(
        crn = "CRN123",
        response = OASysAssessmentSummaryFactory().produce(),
      )

      val result = webTestClient.get()
        .uri("/cas2v2/people/CRN123/oasys/metadata")
        .addJwtForRoleAndAuthSource(role)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas2v2OASysAssessmentMetadataDto>()

      assertThat(result.hasApplicableAssessment).isTrue
    }
  }

  @Nested
  @DisplayName("GET /cas2v2/people/{crn}/oasys/risk-to-self")
  inner class GetRiskToSelf {

    @Test
    fun `Get without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2v2/people/CRN123/oasys/risk-to-self")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Cas2v2NonReferrerRoles
    @ParameterizedTest
    fun `Get without referrer role returns 403`(role: RoleAndAuthSource) {
      webTestClient.get()
        .uri("/cas2v2/people/CRN123/oasys/risk-to-self")
        .addJwtForRoleAndAuthSource(role)
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Cas2v2ReferrerRoles
    @ParameterizedTest
    fun `Assessment not available`(role: RoleAndAuthSource) {
      apAndOASysMockRiskToTheIndividual404Call("CRN123")

      val result = webTestClient.get()
        .uri("/cas2v2/people/CRN123/oasys/risk-to-self")
        .addJwtForRoleAndAuthSource(role)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas2v2OAsysRiskToSelfDto>()

      assertThat(result.metadata.hasApplicableAssessment).isFalse
    }

    @Cas2v2ReferrerRoles
    @ParameterizedTest
    fun `Assessment available`(role: RoleAndAuthSource) {
      apAndOASysMockSuccessfulRiskToTheIndividualCall(
        crn = "CRN123",
        response = RiskToTheIndividualFactory().produce(),
      )

      val result = webTestClient.get()
        .uri("/cas2v2/people/CRN123/oasys/risk-to-self")
        .addJwtForRoleAndAuthSource(role)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas2v2OAsysRiskToSelfDto>()

      assertThat(result.metadata.hasApplicableAssessment).isTrue
    }
  }
}
