package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskManagementPlanFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockSuccessfulNeedsDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockSuccessfulOffenceDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockSuccessfulRiskManagementPlanCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockSuccessfulRiskToTheIndividualCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockSuccessfulRoSHSummaryCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockUnsuccessfulNeedsDetailsCallWithDelay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer

class PersonOASysSectionsTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var oaSysSectionsTransformer: OASysSectionsTransformer

  @Test
  fun `Getting oasys sections by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/oasys/sections")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting oasys sections for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/sections")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting oasys sections for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/sections")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting oasys sections for a CRN that does not exist returns 404`() {
    `Given a User` { userEntity, jwt ->
      val crn = "CRN123"

      CommunityAPI_mockNotFoundOffenderDetailsCall(crn)

      webTestClient.get()
        .uri("/people/$crn/oasys/sections")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting oasys sections for a CRN returns OK with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val offenceDetails = OffenceDetailsFactory().produce()
        APOASysContext_mockSuccessfulOffenceDetailsCall(offenderDetails.otherIds.crn, offenceDetails)

        val roshSummary = RoshSummaryFactory().produce()
        APOASysContext_mockSuccessfulRoSHSummaryCall(offenderDetails.otherIds.crn, roshSummary)

        val risksToTheIndividual = RiskToTheIndividualFactory().produce()
        APOASysContext_mockSuccessfulRiskToTheIndividualCall(offenderDetails.otherIds.crn, risksToTheIndividual)

        val riskManagementPlan = RiskManagementPlanFactory().produce()
        APOASysContext_mockSuccessfulRiskManagementPlanCall(offenderDetails.otherIds.crn, riskManagementPlan)

        val needsDetails = NeedsDetailsFactory().apply {
          withAssessmentId(34853487)
          withAccommodationIssuesDetails("Accommodation", true, false)
          withAttitudeIssuesDetails("Attitude", false, true)
          withFinanceIssuesDetails(null, null, null)
        }.produce()

        APOASysContext_mockSuccessfulNeedsDetailsCall(offenderDetails.otherIds.crn, needsDetails)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/oasys/sections?selected-sections=11&selected-sections=12")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              oaSysSectionsTransformer.transformToApi(
                offenceDetails,
                roshSummary,
                risksToTheIndividual,
                riskManagementPlan,
                needsDetails,
                listOf(11, 12),
              ),
            ),
          )
      }
    }
  }

  @Test
  fun `Getting oasys sections when upstream times out returns 404`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val needsDetails = NeedsDetailsFactory().apply {
          withAssessmentId(34853487)
          withAccommodationIssuesDetails("Accommodation", true, false)
          withAttitudeIssuesDetails("Attitude", false, true)
          withFinanceIssuesDetails(null, null, null)
        }.produce()

        APOASysContext_mockUnsuccessfulNeedsDetailsCallWithDelay(offenderDetails.otherIds.crn, needsDetails, 2500)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/oasys/sections?selected-sections=11&selected-sections=12")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
