package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.transformer.toDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TierFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesEmptyResponseForCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.hmppsTierMockSuccessfulTierCall
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier as UpstreamTier

class PersonSearchTest : IntegrationTestBase() {

  companion object {
    const val CRN = "CRN456"
  }

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
    givenAUser { _, jwt ->
      apDeliusContextCaseSummariesEmptyResponseForCrn("CRN1")

      webTestClient.get()
        .uri("/people/search?crn=CRN1")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Searching for a CRN returns OK with correct body including the tier from the local cases cache`() {
    givenAUser { _, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withCrn(CRN)
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

        val tier = TierFactory()
          .withTierScore("D4")
          .withCalculationDate(LocalDateTime.parse("2026-01-02T03:04:05"))
          .produce()
        givenACase(crn = CRN, tierV2 = tier, tierV3 = null)

        webTestClient.get()
          .uri("/people/search?crn=$CRN")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            jsonMapper.writeValueAsString(
              FullPerson(
                type = PersonType.fullPerson,
                crn = CRN,
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
                tier = tier.toDto(),
              ),
            ),
          )
      }
    }
  }

  @Test
  fun `Searching for a CRN with no cases entry returns OK with the tier fetched from upstream`() {
    givenAUser { _, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withCrn(CRN)
          withDateOfBirth(LocalDate.parse("1985-05-05"))
          withNomsNumber("NOMS321")
          withFirstName("James")
          withLastName("Someone")
          withGender("Male")
        },
        inmateDetailsConfigBlock = {
          withOffenderNo("NOMS321")
          withCustodyStatus(InmateStatus.IN)
        },
      ) { _, _ ->

        hmppsTierMockSuccessfulTierCall(
          CRN,
          UpstreamTier(
            tierScore = "B2",
            calculationId = UUID.randomUUID(),
            calculationDate = LocalDateTime.parse("2026-01-02T03:04:05"),
            changeReason = "Change Reason",
          ),
        )

        webTestClient.get()
          .uri("/people/search?crn=$CRN")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.crn").isEqualTo(CRN)
          .jsonPath("$.tier.tierScore").isEqualTo("B2")
          .jsonPath("$.tier.version").isEqualTo("V2")
      }
    }
  }

  @Test
  fun `Searching for a CRN without a NomsNumber & inmate details returns OK with correct body`() {
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
      ) { _, _ ->
        webTestClient.get()
          .uri("/people/search?crn=CRN")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            jsonMapper.writeValueAsString(
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
                tier = null,
              ),
            ),
          )
      }
    }
  }
}
