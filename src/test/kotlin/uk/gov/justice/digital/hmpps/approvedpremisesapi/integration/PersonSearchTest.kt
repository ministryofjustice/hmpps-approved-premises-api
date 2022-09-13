package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
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
      authSource = "nomis"
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
      authSource = "delius"
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
    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")

    wiremockServer.stubFor(
      get(WireMock.urlEqualTo("/secure/offenders/crn/CRN"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404)
        )
    )

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_PROBATION")
    )

    webTestClient.get()
      .uri("/people/search?crn=CRN")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Searching for a CRN returns OK with correct body`() {
    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")
    mockOffenderDetailsCommunityApiCall(
      OffenderDetailsSummaryFactory()
        .withCrn("CRN")
        .withDateOfBirth(LocalDate.parse("1985-05-05"))
        .withNomsNumber("NOMS321")
        .withFirstName("James")
        .withLastName("Someone")
        .withGender("Male")
        .withNationality("English")
        .withReligionOrBelief("Judaism")
        .withGenderIdentity("Prefer to self-describe")
        .withSelfDescribedGenderIdentity("This is a self described identity")
        .produce()
    )
    mockInmateDetailPrisonsApiCall(
      InmateDetailFactory()
        .withOffenderNo("NOMS321")
        .withInOutStatus(InOutStatus.IN)
        .withAssignedLivingUnit(
          AssignedLivingUnit(
            agencyId = "Agency ID",
            locationId = 5,
            description = "SOMEPLACE",
            agencyName = "Agency Name"
          )
        )
        .produce()
    )

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_PROBATION")
    )

    webTestClient.get()
      .uri("/people/search?crn=CRN")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          Person(
            crn = "CRN",
            name = "James Someone",
            dateOfBirth = LocalDate.parse("1985-05-05"),
            sex = "Male",
            status = Person.Status.inCustody,
            nomsNumber = "NOMS321",
            nationality = "English",
            religionOrBelief = "Judaism",
            genderIdentity = "This is a self described identity",
            prisonName = "SOMEPLACE"
          )
        )
      )
  }

  private fun mockOffenderDetailsCommunityApiCall(offenderDetails: OffenderDetailSummary) = wiremockServer.stubFor(
    WireMock.get(WireMock.urlEqualTo("/secure/offenders/crn/${offenderDetails.otherIds.crn}"))
      .willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(
            objectMapper.writeValueAsString(offenderDetails)
          )
      )
  )

  private fun mockInmateDetailPrisonsApiCall(inmateDetail: InmateDetail) = wiremockServer.stubFor(
    WireMock.get(WireMock.urlEqualTo("/api/offenders/${inmateDetail.offenderNo}"))
      .willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(
            objectMapper.writeValueAsString(inmateDetail)
          )
      )
  )
}
