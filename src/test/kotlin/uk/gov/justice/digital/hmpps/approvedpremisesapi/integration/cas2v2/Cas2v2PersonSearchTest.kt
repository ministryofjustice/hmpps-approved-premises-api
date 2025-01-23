package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2v2

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOffenderDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.Cas2v2IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.cas2v2SearchAPIMockServerErrorSearchCall
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
    inner class WhenThereIsAnErrorWithNomsNumber {
      //FAILS
      @Test
      fun `Searching cas2v2 by NOMIS ID without a JWT returns 401`() {
        webTestClient.get()
          .uri("/cas2v2/people/search?nomsNumber=nomsNumber").exchange()
          .expectStatus()
          .isUnauthorized
      }

      @Test
      fun `Searching cas2v2 for a NOMIS ID with a non-Delius or NOMIS JWT returns 403`() {
        val jwt = jwtAuthHelper.createClientCredentialsJwt(
          username = "username",
          authSource = "other source",
        )

        webTestClient.get()
          .uri("/cas2v2/people/search?nomsNumber=nomsNumber")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }

      //FAILS
      @Test
      fun `Searching cas2v2 for a NOMIS ID without ROLE_POM returns 403`() {
        val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
          subject = "username",
          authSource = "nomis",
          roles = listOf("ROLE_OTHER"),
        )

        webTestClient.get()
          .uri("/cas2v2/people/search?nomsNumber=nomsNumber")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }

      @Test
      fun `Searching cas2v2 for a NOMIS ID returns Unauthorised error when it is unauthorized by the API`() {
        givenACas2PomUser { userEntity, jwt ->
          probationOffenderSearchAPIMockForbiddenOffenderSearchCall()

          webTestClient.get()
            .uri("/cas2v2/people/search?nomsNumber=NOMS321")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }

      @Test
      fun `Searching cas2v2 for a NOMIS ID returns unauthorised error when offender is in a different prison`() {
        givenACas2PomUser { userEntity, jwt ->

          val offender = ProbationOffenderDetailFactory()
            .withOtherIds(IDs(crn = "CRN", nomsNumber = "NOMS456", pncNumber = "PNC456"))
            .withFirstName("Jo")
            .withSurname("AnotherPrison")
            .withDateOfBirth(
              LocalDate
                .parse("1985-05-05"),
            )
            .withGender("Male")
            .withOffenderProfile(OffenderProfile(nationality = "English"))
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

          probationOffenderSearchAPIMockSuccessfulOffenderSearchCall("NOMS456", listOf(offender))
          prisonAPIMockSuccessfulInmateDetailsCall(inmateDetail = inmateDetail)

          webTestClient.get()
            .uri("/cas2v2/people/search?nomsNumber=NOMS456")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }

      @Test
      fun `Searching cas2v2 for a NOMIS ID returns 404 error when it is not found`() {
        givenACas2PomUser { userEntity, jwt ->
          probationOffenderSearchAPIMockNotFoundSearchCall()

          webTestClient.get()
            .uri("/cas2v2/people/search?nomsNumber=NOMS321")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isNotFound
        }
      }

      @Test
      fun `Searching cas2v2 for a NOMIS ID returns server error when there is a server error`() {
        givenACas2PomUser { userEntity, jwt ->
          probationOffenderSearchAPIMockServerErrorSearchCall()

          webTestClient.get()
            .uri("/cas2v2/people/search?nomsNumber=NOMS321")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .is5xxServerError
        }
      }
    }

    @Nested
    inner class WhenThereIsAnErrorWithCrnNumber {
      //FAILS
      @Test
      fun `Searching cas2v2 by crn without a JWT returns 401`() {
        webTestClient.get()
          .uri("/cas2v2/people/search?crn=crn").exchange()
          .expectStatus()
          .isUnauthorized
      }

      @Test
      fun `Searching cas2v2 for a CRN with a non-Delius or NOMIS JWT returns 403`() {
        val jwt = jwtAuthHelper.createClientCredentialsJwt(
          username = "username",
          authSource = "other source",
        )

        webTestClient.get()
          .uri("/cas2v2/people/search?crn=crn")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }

      @Test
      fun `Searching cas2v2 for a CRN without ROLE_POM returns 403`() {
        val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
          subject = "username",
          authSource = "nomis",
          roles = listOf("ROLE_OTHER"),
        )

        webTestClient.get()
          .uri("/cas2v2/people/search?crn=crn")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }

      @Test
      fun `Searching cas2v2 for a CRN returns Unauthorised error when it is unauthorized by the API`() {
        givenACas2PomUser { userEntity, jwt ->
          probationOffenderSearchAPIMockForbiddenOffenderSearchCall()

          webTestClient.get()
            .uri("/cas2v2/people/search?crn=X400001")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }

      @Test
      fun `Searching cas2v2 for a CRN returns unauthorised error when offender is in a different prison`() {
        givenACas2PomUser { userEntity, jwt ->
//TODO
          val offender = ProbationOffenderDetailFactory()
            .withOtherIds(IDs(crn = "CRN", nomsNumber = "NOMS456", pncNumber = "PNC456"))
            .withFirstName("Jo")
            .withSurname("AnotherPrison")
            .withDateOfBirth(
              LocalDate
                .parse("1985-05-05"),
            )
            .withGender("Male")
            .withOffenderProfile(OffenderProfile(nationality = "English"))
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

          probationOffenderSearchAPIMockSuccessfulOffenderSearchCall("NOMS456", listOf(offender))
          prisonAPIMockSuccessfulInmateDetailsCall(inmateDetail = inmateDetail)

          webTestClient.get()
            .uri("/cas2v2/people/search?crn=NOMS456")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }

      @Test
      fun `Searching cas2v2 for a CRN returns 404 error when it is not found`() {
//        givenACas2PomUser { userEntity, jwt ->
        givenAUser { _, jwt ->
          probationOffenderSearchAPIMockNotFoundSearchCall()

          webTestClient.get()
            .uri("/cas2v2/people/search?crn=CRN")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isNotFound
        }
      }

      //FAIL
      @Test
      fun `Searching cas2v2 for a CRN returns server error when there is a server error`() {
//        givenACas2PomUser { userEntity, jwt ->
        givenAUser { _, jwt ->
          cas2v2SearchAPIMockServerErrorSearchCall()

          webTestClient.get()
            .uri("/cas2v2/people/search?crn=crn")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .is5xxServerError
        }
      }
    }


    @Nested
    inner class WhenSuccessfulWithNomisNumber {
      @Test
      fun `Searching cas2v2 for a NOMIS ID returns OK with correct body`() {
        givenACas2PomUser(nomisUserDetailsConfigBlock = { withActiveCaseloadId("BRI") }) { userEntity, jwt ->
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

          probationOffenderSearchAPIMockSuccessfulOffenderSearchCall("NOMS321", listOf(offender))
          prisonAPIMockSuccessfulInmateDetailsCall(inmateDetail = inmateDetail)

          webTestClient.get()
            .uri("/cas2v2/people/search?nomsNumber=NOMS321")
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


    @Nested
    inner class WhenSuccessfulWithCrnNumber {
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
              .uri("/cas2v2/people/search?crn=CRN")
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
              .uri("/cas2v2/people/search?crn=CRN")
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
  }
}
