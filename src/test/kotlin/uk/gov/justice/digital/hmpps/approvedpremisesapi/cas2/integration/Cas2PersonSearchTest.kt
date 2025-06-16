package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUnsuccessfulCaseSummaryCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockSuccessfulInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Profile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import java.time.LocalDate

class Cas2PersonSearchTest : IntegrationTestBase() {
  @Nested
  inner class PeopleSearchGet {

    @Nested
    inner class WhenThereIsAnError {
      @Test
      fun `Searching by NOMIS ID without a JWT returns 401`() {
        webTestClient.get()
          .uri("/cas2/people/search?nomsNumber=nomsNumber").exchange()
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
          .uri("/cas2/people/search?nomsNumber=nomsNumber")
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
          .uri("/cas2/people/search?nomsNumber=nomsNumber")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }

      @Test
      fun `Searching for a NOMIS ID returns Unauthorised error when it is unauthorized by the API`() {
        givenACas2PomUser { userEntity, jwt ->
          apDeliusContextMockUnsuccessfulCaseSummaryCall(403)

          webTestClient.get()
            .uri("/cas2/people/search?nomsNumber=NOMS321")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }

      @Test
      fun `Searching for a NOMIS ID returns unauthorised error when offender is in a different prison`() {
        givenACas2PomUser { userEntity, jwt ->

          val caseSummary = CaseSummaryFactory()
            .withCrn("CRN")
            .withNomsId("NOMS456")
            .withPnc("PNC456")
            .withName(Name("Jo", "AnotherPrison"))
            .withDateOfBirth(
              LocalDate
                .parse("1985-05-05"),
            )
            .withGender("Male")
            .withProfile(Profile(nationality = "English"))
            .produce()

          val inmateDetail = InmateDetailFactory().withOffenderNo("NOMS456")
            .withCustodyStatus(InmateStatus.IN)
            .withAssignedLivingUnit(
              AssignedLivingUnit(
                agencyId = "ANOTHER_PRISON",
                locationId = 5,
                description = "B-2F-004",
                agencyName = "HMP Example",
              ),
            )
            .produce()

          apDeliusContextMockCaseSummary(caseSummary)
          prisonAPIMockSuccessfulInmateDetailsCall(inmateDetail = inmateDetail)

          webTestClient.get()
            .uri("/cas2/people/search?nomsNumber=NOMS456")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }

      @Test
      fun `Searching for a NOMIS ID returns 404 error when it is not found`() {
        givenACas2PomUser { userEntity, jwt ->
          apDeliusContextMockUnsuccessfulCaseSummaryCall(404)

          webTestClient.get()
            .uri("/cas2/people/search?nomsNumber=NOMS321")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isNotFound
        }
      }

      @Test
      fun `Searching for a NOMIS ID returns server error when there is a server error`() {
        givenACas2PomUser { userEntity, jwt ->
          apDeliusContextMockUnsuccessfulCaseSummaryCall()

          webTestClient.get()
            .uri("/cas2/people/search?nomsNumber=NOMS321")
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
        givenACas2PomUser(nomisUserDetailsConfigBlock = { withActiveCaseloadId("BRI") }) { userEntity, jwt ->
          val caseSummary = CaseSummaryFactory()
            .withCrn("CRN")
            .withNomsId("NOMS321")
            .withPnc("PNC123")
            .withName(Name("James", "Someone"))
            .withDateOfBirth(
              LocalDate
                .parse("1985-05-05"),
            )
            .withGender("Male")
            .withProfile(Profile(nationality = "English"))
            .produce()

          val inmateDetail = InmateDetailFactory().withOffenderNo("NOMS321")
            .withCustodyStatus(InmateStatus.IN)
            .withAssignedLivingUnit(
              AssignedLivingUnit(
                agencyId = "BRI",
                locationId = 5,
                description = "B-2F-004",
                agencyName = "HMP Bristol",
              ),
            )
            .produce()

          apDeliusContextMockCaseSummary(caseSummary)
          prisonAPIMockSuccessfulInmateDetailsCall(inmateDetail = inmateDetail)

          webTestClient.get()
            .uri("/cas2/people/search?nomsNumber=NOMS321")
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
                  status = PersonStatus.inCustody,
                  nomsNumber = "NOMS321",
                  pncNumber = "PNC123",
                  nationality = "English",
                  isRestricted = false,
                  prisonName = "HMP Bristol",
                ),
              ),
            )
        }
      }
    }
  }
}
