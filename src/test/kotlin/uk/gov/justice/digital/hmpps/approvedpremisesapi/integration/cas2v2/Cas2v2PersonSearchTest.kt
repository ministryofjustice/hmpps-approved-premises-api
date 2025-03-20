package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2v2

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOffenderDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.Cas2v2IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2DeliusUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockSuccessfulInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.probationOffenderSearchAPIMockForbiddenOffenderSearchCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.probationOffenderSearchAPIMockNotFoundSearchCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.probationOffenderSearchAPIMockServerErrorSearchCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.probationOffenderSearchAPIMockSuccessfulOffenderSearchCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.IDs
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.OffenderProfile
import java.time.LocalDate

class Cas2v2PersonSearchTest : Cas2v2IntegrationTestBase() {
  @Nested
  inner class Cas2v2PeopleSearchGet {

    @Nested
    inner class SearchByNomisId {
      @Nested
      inner class WhenThereIsAnError {

        @Test
        fun `Searching by NOMIS ID without a JWT returns 401`() {
          webTestClient.get()
            .uri("/cas2v2/people/search-by-noms/nomsNumber").exchange()
            .expectStatus()
            .isUnauthorized
        }

        @Test
        fun `Searching for a NOMIS ID with a non-Delius or NOMIS JWT returns 403`() {
          val jwt = jwtAuthHelper.createClientCredentialsJwt(
            username = "username",
            authSource = "other source",
          )

          webTestClient.get()
            .uri("/cas2v2/people/search-by-noms/nomsNumber")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }

        @Test
        fun `Searching for a NOMIS ID without ROLE_POM returns 403`() {
          val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
            subject = "username",
            authSource = "nomis",
            roles = listOf("ROLE_OTHER"),
          )

          webTestClient.get()
            .uri("/cas2v2/people/search-by-noms/nomsNumber")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }

        @Test
        fun `Searching for a NOMIS ID returns Unauthorised error when it is unauthorized by the API`() {
          givenACas2v2NomisUser { _, jwt ->
            probationOffenderSearchAPIMockForbiddenOffenderSearchCall()

            webTestClient.get()
              .uri("/cas2v2/people/search-by-noms/NOMS321")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isForbidden
          }
        }

        @Test
        fun `Searching for a NOMIS ID returns 404 error when it is not found`() {
          givenACas2v2DeliusUser { _, jwt ->
            probationOffenderSearchAPIMockNotFoundSearchCall()

            webTestClient.get()
              .uri("/cas2v2/people/search-by-noms/NOMS321")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isNotFound
          }
        }

        @Test
        fun `Searching for a NOMIS ID returns server error when there is a server error`() {
          givenACas2v2NomisUser { _, jwt ->
            probationOffenderSearchAPIMockServerErrorSearchCall()

            webTestClient.get()
              .uri("/cas2v2/people/search-by-noms/NOMS321")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .is5xxServerError
          }
        }
      }

      @Nested
      inner class WhenSuccessful {
        @Test
        fun `Searching for a NOMIS ID returns OK with correct body`() {
          givenACas2v2NomisUser(nomisUserDetailsConfigBlock = { withActiveCaseloadId("BRI") }) { _, jwt ->
            val offender = ProbationOffenderDetailFactory()
              .withOtherIds(IDs(crn = "CRN", nomsNumber = "NOMS321", pncNumber = "PNC123"))
              .withFirstName("James")
              .withSurname("Someone")
              .withDateOfBirth(
                LocalDate
                  .parse("1985-05-05"),
              )
              .withGender("Male")
              .withOffenderProfile(OffenderProfile(nationality = "English"))
              .produce()

            val inmateDetail = InmateDetailFactory().withOffenderNo("NOMS321")
              .withAssignedLivingUnit(
                AssignedLivingUnit(
                  agencyId = "BRI",
                  locationId = 5,
                  description = "B-2F-004",
                  agencyName = "HMP Bristol",
                ),
              )
              .produce()

            probationOffenderSearchAPIMockSuccessfulOffenderSearchCall("NOMS321", listOf(offender))
            prisonAPIMockSuccessfulInmateDetailsCall(inmateDetail = inmateDetail)

            webTestClient.get()
              .uri("/cas2v2/people/search-by-noms/NOMS321")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  FullPerson(
                    type = PersonType.fullPerson,
                    crn = "CRN",
                    name = "James Someone",
                    dateOfBirth = LocalDate.parse("1985-05-05"),
                    sex = "Male",
                    status = PersonStatus.unknown,
                    nomsNumber = "NOMS321",
                  ),
                ),
              )
          }
        }
      }
    }

    @Nested
    inner class SearchByCrn {
      @Nested
      inner class WhenThereIsAnError {
        @Test
        fun `Searching by CRN without a JWT returns 401`() {
          webTestClient.get()
            .uri("/cas2v2/people/search-by-crn/CRN")
            .exchange()
            .expectStatus()
            .isUnauthorized
        }

        @Test
        fun `Searching for a CRN with a non-Delius JWT returns 403`() {
          val jwt = jwtAuthHelper.createClientCredentialsJwt(
            username = "username",
            authSource = "other-auth-source",
          )

          webTestClient.get()
            .uri("/cas2v2/people/search?crn=CRN")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }

        @Test
        fun `Searching for a CRN with a NOMIS JWT returns 403`() {
          val jwt = jwtAuthHelper.createClientCredentialsJwt(
            username = "username",
            authSource = "nomis",
          )

          webTestClient.get()
            .uri("/cas2v2/people/search-by-crn/CRN")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }

        @Test
        fun `Searching for a CRN with ROLE_POM returns 403`() {
          val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
            subject = "username",
            authSource = "delius",
            roles = listOf("ROLE_POM"),
          )

          webTestClient.get()
            .uri("/cas2v2/people/search-by-crn/CRN")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }

        @Test
        fun `Searching for a CRN that does not exist returns 404`() {
          apDeliusContextAddCaseSummaryToBulkResponse(
            CaseSummaryFactory().produce(),
          )

          givenACas2v2DeliusUser { _, jwt ->
            wiremockServer.stubFor(
              get(WireMock.urlEqualTo("/secure/offenders/crn/CRN"))
                .willReturn(
                  aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(404),
                ),
            )

            webTestClient.get()
              .uri("/cas2v2/people/search-by-crn/CRN")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isNotFound
          }
        }
      }

      @Nested
      inner class WhenSuccessful {
        @Test
        fun `Searching for a CRN returns OK with correct body`() {
          probationOffenderSearchAPIMockSuccessfulOffenderSearchCall(
            nomsNumber = "NOMS321",
            response = listOf(
              ProbationOffenderDetailFactory()
                .withFirstName("James")
                .withSurname("Someone")
                .withOtherIds(IDs(crn = "CRN", nomsNumber = "NOMS321"))
                .withDateOfBirth(LocalDate.parse("1985-05-05"))
                .withGender("Male")
                .produce(),
            ),
          )

          givenACas2v2DeliusUser { _, jwt ->
            givenAnOffender(
              offenderDetailsConfigBlock = {
                withCrn("CRN")
                withDateOfBirth(LocalDate.parse("1985-05-05"))
                withNomsNumber("NOMS321")
                withFirstName("James")
                withLastName("Someone")
                withGender("Male")
                withEthnicity("White British")
                withNationality("English")
                withReligionOrBelief("Judaism")
                withGenderIdentity("Prefer to self-describe")
                withSelfDescribedGenderIdentity("This is a self described identity")
              },
              inmateDetailsConfigBlock = {
                withOffenderNo("NOMS321")
                withCustodyStatus(InmateStatus.IN)
                withAssignedLivingUnit(
                  AssignedLivingUnit(
                    agencyId = "BRI",
                    locationId = 5,
                    description = "B-2F-004",
                    agencyName = "HMP Bristol",
                  ),
                )
              },
            ) { _, _ ->
              webTestClient.get()
                .uri("/cas2v2/people/search-by-crn/CRN")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    FullPerson(
                      type = PersonType.fullPerson,
                      crn = "CRN",
                      name = "James Someone",
                      dateOfBirth = LocalDate.parse("1985-05-05"),
                      sex = "Male",
                      status = PersonStatus.unknown,
                      nomsNumber = "NOMS321",
                    ),
                  ),
                )
            }
          }
        }

        @Test
        fun `Searching for a CRN without a NomsNumber returns OK with correct body`() {
          givenACas2v2DeliusUser { _, jwt ->
            givenAnOffender(
              offenderDetailsConfigBlock = {
                withCrn("CRN")
                withDateOfBirth(LocalDate.parse("1985-05-05"))
                withNomsNumber(null)
                withFirstName("James")
                withLastName("Someone")
                withGender("Male")
                withEthnicity("White British")
                withCurrentRestriction(false)
                withCurrentExclusion(false)
                withNationality("English")
                withReligionOrBelief("Judaism")
                withGenderIdentity("Prefer to self-describe")
                withSelfDescribedGenderIdentity("This is a self described identity")
              },
            ) { _, _ ->
              webTestClient.get()
                .uri("/cas2v2/people/search-by-crn/CRN")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    FullPerson(
                      type = PersonType.fullPerson,
                      crn = "CRN",
                      name = "James Someone",
                      dateOfBirth = LocalDate.parse("1985-05-05"),
                      sex = "Male",
                      status = PersonStatus.unknown,
                      nomsNumber = null,
                    ),
                  ),
                )
            }
          }
        }
      }
    }
  }
}
