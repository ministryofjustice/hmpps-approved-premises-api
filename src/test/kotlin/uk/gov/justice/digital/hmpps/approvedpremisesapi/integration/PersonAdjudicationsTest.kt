package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationsPageFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AgencyFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockSuccessfulAdjudicationsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AdjudicationTransformer
import java.time.LocalDateTime

class PersonAdjudicationsTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var adjudicationTransformer: AdjudicationTransformer

  @Test
  fun `Getting adjudications by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/adjudications")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting adjudications for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/people/CRN/adjudications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting adjudications for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
    )

    webTestClient.get()
      .uri("/people/CRN/adjudications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting adjudications for a CRN that does not exist returns 404`() {
    `Given a User` { userEntity, jwt ->
      val crn = "CRN123"

      CommunityAPI_mockNotFoundOffenderDetailsCall(crn)

      webTestClient.get()
        .uri("/people/$crn/adjudications")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting adjudications alerts for a CRN without a NOMS number returns 404`() {
    `Given a User` { _, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = {
          withNomsNumber(null)
        },
      ) { offenderDetails, _ ->

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/acct-alerts")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }

  @Test
  fun `Getting adjudications for a CRN returns OK with correct body`() {
    `Given a User` { _, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val adjudicationsResponse = AdjudicationsPageFactory()
          .withResults(
            listOf(
              AdjudicationFactory().withAgencyId("AGNCY1").produce(),
              AdjudicationFactory().withAgencyId("AGNCY2").produce(),
            ),
          )
          .withAgencies(
            listOf(
              AgencyFactory().withAgencyId("AGNCY1").produce(),
              AgencyFactory().withAgencyId("AGNCY2").produce(),
            ),
          )
          .produce()

        PrisonAPI_mockSuccessfulAdjudicationsCall(offenderDetails.otherIds.nomsNumber!!, adjudicationsResponse)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/adjudications")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(adjudicationTransformer.transformToApi(adjudicationsResponse, false)),
          )
      }
    }
  }

  @Test
  fun `Getting adjudications for temporary accommodation returns correct records when 12 month time filter is not applied`() {
    `Given a User` { _, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val adjudicationsResponse = AdjudicationsPageFactory()
          .withResults(
            listOf(
              AdjudicationFactory()
                .withAgencyId("AGNCY1")
                .withReportTime(LocalDateTime.now().minusMonths(13))
                .produce(),
              AdjudicationFactory().withAgencyId("AGNCY2").produce(),
            ),
          )
          .withAgencies(
            listOf(
              AgencyFactory().withAgencyId("AGNCY1").produce(),
              AgencyFactory().withAgencyId("AGNCY2").produce(),
            ),
          )
          .produce()

        PrisonAPI_mockSuccessfulAdjudicationsCall(offenderDetails.otherIds.nomsNumber!!, adjudicationsResponse)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/adjudications")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(adjudicationTransformer.transformToApi(adjudicationsResponse, false)),
          )
      }
    }
  }

  @Test
  fun `Getting adjudications for approved premises returns correct records when 12 month time filter is applied`() {
    `Given a User` { _, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val adjudicationsResponse = AdjudicationsPageFactory()
          .withResults(
            listOf(
              AdjudicationFactory()
                .withAgencyId("AGNCY1")
                .withReportTime(LocalDateTime.now())
                .produce(),
              AdjudicationFactory().withAgencyId("AGNCY2").produce(),
              AdjudicationFactory()
                .withAgencyId("AGNCY1")
                .withReportTime(LocalDateTime.now().minusMonths(12))
                .produce(),
              AdjudicationFactory().withAgencyId("AGNCY2").produce(),
            ),
          )
          .withAgencies(
            listOf(
              AgencyFactory().withAgencyId("AGNCY1").produce(),
              AgencyFactory().withAgencyId("AGNCY2").produce(),
            ),
          )
          .produce()

        PrisonAPI_mockSuccessfulAdjudicationsCall(offenderDetails.otherIds.nomsNumber!!, adjudicationsResponse)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/adjudications")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(adjudicationTransformer.transformToApi(adjudicationsResponse, true)),
          )
      }
    }
  }
}
