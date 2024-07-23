package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 POM User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockSuccessfulOffenceDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockSuccessfulRoSHSummaryCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockUnsuccessfulRoshCallWithDelay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer

class Cas2PersonOASysRoshTest : IntegrationTestBase() {
  @Autowired
  lateinit var oaSysSectionsTransformer: OASysSectionsTransformer

  @Test
  fun `Getting RoSH by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/cas2/people/CRN/oasys/rosh")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting RoSH  for a CRN with an invalid auth-source JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "bananas",
    )

    webTestClient.get()
      .uri("/cas2/people/CRN/oasys/rosh")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting RoSH for a CRN without ROLE_PROBATION or ROLE_POM returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "nomis",
      roles = listOf("ROLE_OTHER"),
    )

    webTestClient.get()
      .uri("/cas2/people/CRN/oasys/rosh")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting Rosh for a CRN that does not exist returns 404`() {
    `Given a CAS2 POM User` { userEntity, jwt ->
      val crn = "CRN123"

      CommunityAPI_mockNotFoundOffenderDetailsCall(crn)

      webTestClient.get()
        .uri("/cas2/people/$crn/oasys/rosh")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting RoSH for a CRN returns OK with correct body`() {
    `Given a CAS2 POM User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val offenceDetails = OffenceDetailsFactory().produce()
        APOASysContext_mockSuccessfulOffenceDetailsCall(offenderDetails.otherIds.crn, offenceDetails)

        val rosh = RoshSummaryFactory().produce()
        APOASysContext_mockSuccessfulRoSHSummaryCall(offenderDetails.otherIds.crn, rosh)

        webTestClient.get()
          .uri("/cas2/people/${offenderDetails.otherIds.crn}/oasys/rosh")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              oaSysSectionsTransformer.transformRiskOfSeriousHarm(
                offenceDetails,
                rosh,
              ),
            ),
          )
      }
    }
  }

  @Test
  fun `Getting RoSH when upstream times out returns 404`() {
    `Given a CAS2 POM User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val rosh = RoshSummaryFactory().produce()
        APOASysContext_mockUnsuccessfulRoshCallWithDelay(offenderDetails.otherIds.crn, rosh, 2500)

        webTestClient.get()
          .uri("/cas2/people/${offenderDetails.otherIds.crn}/oasys/rosh")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
