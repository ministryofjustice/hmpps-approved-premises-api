package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import java.time.LocalDate

class PersonSearchTest : IntegrationTestBase() {
  @Test
  fun `Searching by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/search?crn=CRN")
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
      .uri("/people/search?crn=CRN")
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
      .uri("/people/search?crn=CRN")
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
      .uri("/people/search?crn=CRN")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Searching for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_OTHER"),
    )

    webTestClient.get()
      .uri("/people/search?crn=CRN")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Searching for a CRN that does not exist returns 404`() {
    givenAUser { userEntity, jwt ->
      wiremockServer.stubFor(
        get(WireMock.urlEqualTo("/secure/offenders/crn/CRN"))
          .willReturn(
            aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(404),
          ),
      )

      webTestClient.get()
        .uri("/people/search?crn=CRN")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Searching for a CRN returns OK with correct body`() {
    givenAUser { _, jwt ->
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
      ) { offenderDetails, inmateDetails ->
        webTestClient.get()
          .uri("/people/search?crn=CRN")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              FullPerson(
                type = PersonType.FULL_PERSON,
                crn = "CRN",
                name = "James Someone",
                dateOfBirth = LocalDate.parse("1985-05-05"),
                sex = "Male",
                status = PersonStatus.inCustody,
                nomsNumber = "NOMS321",
                ethnicity = "White British",
                nationality = "English",
                religionOrBelief = "Judaism",
                genderIdentity = "This is a self described identity",
                prisonName = "HMP Bristol",
                isRestricted = false,
              ),
            ),
          )
      }
    }
  }

  @Test
  fun `Searching for a CRN without a NomsNumber returns OK with correct body`() {
    givenAUser { _, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withCrn("CRN")
          withDateOfBirth(LocalDate.parse("1985-05-05"))
          withNomsNumber(null)
          withFirstName("James")
          withLastName("Someone")
          withGender("Male")
          withEthnicity("White British")
          withNationality("English")
          withReligionOrBelief("Judaism")
          withGenderIdentity("Prefer to self-describe")
          withSelfDescribedGenderIdentity("This is a self described identity")
        },
      ) { offenderDetails, _ ->
        webTestClient.get()
          .uri("/people/search?crn=CRN")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              FullPerson(
                type = PersonType.FULL_PERSON,
                crn = "CRN",
                name = "James Someone",
                dateOfBirth = LocalDate.parse("1985-05-05"),
                sex = "Male",
                status = PersonStatus.unknown,
                nomsNumber = null,
                ethnicity = "White British",
                nationality = "English",
                religionOrBelief = "Judaism",
                genderIdentity = "This is a self described identity",
                prisonName = null,
                isRestricted = false,
              ),
            ),
          )
      }
    }
  }
}
