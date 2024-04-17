package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailOffenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ConvictionFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ConvictionTransformer
import java.time.LocalDate

class PersonOffencesTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var convictionTransformer: ConvictionTransformer

  @Test
  fun `Getting offences by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/offences")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting offences for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/people/CRN/offences")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting offences for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
    )

    webTestClient.get()
      .uri("/people/CRN/offences")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting offences for a CRN that does not exist returns 404`() {
    `Given a User` { _, jwt ->
      val crn = "CRN123"

      CommunityAPI_mockNotFoundOffenderDetailsCall(crn)
      loadPreemptiveCacheForOffenderDetails(crn)

      webTestClient.get()
        .uri("/people/$crn/offences")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting offences for a CRN returns OK with only active offences`() {
    `Given a User` { _, jwt ->
      `Given an Offender` { offenderDetails, _ ->

        val activeConviction = ConvictionFactory()
          .withConvictionId(12345)
          .withIndex("5")
          .withActive(true)
          .withOffences(
            listOf(
              OffenceFactory()
                .withOffenceId("1")
                .withMainCategoryDescription("Main Category 1")
                .withSubCategoryDescription("Sub Category 1")
                .produce(),
              OffenceFactory()
                .withOffenceId("2")
                .withMainCategoryDescription("Main Category 2")
                .withSubCategoryDescription("Sub Category 2")
                .produce(),
            ),
          )
          .produce()

        val inactiveConviction = ConvictionFactory()
          .withConvictionId(6789)
          .withIndex("2")
          .withActive(false)
          .withOffences(
            listOf(
              OffenceFactory()
                .withOffenceId("3")
                .withMainCategoryDescription("Main Category 1")
                .withSubCategoryDescription("Sub Category 1")
                .produce(),
              OffenceFactory()
                .withOffenceId("4")
                .withMainCategoryDescription("Main Category 2")
                .withSubCategoryDescription("Sub Category 2")
                .produce(),
            ),
          )
          .produce()

        APDeliusContext_mockSuccessfulCaseDetailCall(
          offenderDetails.otherIds.crn,
          CaseDetailFactory()
            .withOffences(
              listOf(
                CaseDetailOffenceFactory()
                  .withEventNumber("the delius event number 1")
                  .withDescription("the offence description 1")
                  .withDate(LocalDate.of(2023, 1, 2))
                  .produce(),
                CaseDetailOffenceFactory()
                  .withEventNumber("the delius event number 2")
                  .withDescription("the offence description 2")
                  .withDate(LocalDate.of(2024, 3, 4))
                  .produce(),
              ),
            )
            .produce(),
        )

        val expectedValues = listOf(
          ActiveOffence(
            deliusEventNumber = "the delius event number 1",
            offenceDescription = "the offence description 1",
            offenceDate = LocalDate.of(2023, 1, 2),
          ),
          ActiveOffence(
            deliusEventNumber = "the delius event number 2",
            offenceDescription = "the offence description 2",
            offenceDate = LocalDate.of(2024, 3, 4),
          ),
        )

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/offences")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(expectedValues),
          )
      }
    }
  }
}
