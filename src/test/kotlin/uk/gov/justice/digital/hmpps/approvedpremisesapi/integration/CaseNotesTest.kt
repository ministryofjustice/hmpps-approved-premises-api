package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseNoteFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PrisonCaseNoteTransformer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CaseNotesTest : IntegrationTestBase() {
  @Autowired
  lateinit var caseNoteTransformer: PrisonCaseNoteTransformer

  @Test
  fun `Getting case notes without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/prison-case-notes")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting case notes with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis"
    )

    webTestClient.get()
      .uri("/people/CRN/prison-case-notes")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting case notes without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius"
    )

    webTestClient.get()
      .uri("/people/CRN/prison-case-notes")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting case notes for a CRN that does not exist returns 404`() {
    val crn = "CRN345"

    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")

    wiremockServer.stubFor(
      get(WireMock.urlEqualTo("/secure/offenders/crn/$crn"))
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
      .uri("/people/$crn/prison-case-notes")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Getting case notes returns OK with correct body`() {
    val crn = "CRN345"
    val nomsNumber = "NOMS789"

    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")
    mockOffenderDetailsCommunityApiCall(
      OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withDateOfBirth(LocalDate.parse("1985-05-05"))
        .withNomsNumber(nomsNumber)
        .produce()
    )

    val caseNotes = listOf(
      CaseNoteFactory().produce(),
      CaseNoteFactory().produce(),
      CaseNoteFactory().produce(),
      CaseNoteFactory().produce()
    )

    mockCaseNotesPrisonApiCall(
      0,
      LocalDate.now().minusDays(365),
      nomsNumber,
      CaseNotesPage(
        totalElements = 4,
        totalPages = 1,
        number = 1,
        content = caseNotes
      )
    )

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_PROBATION")
    )

    webTestClient.get()
      .uri("/people/$crn/prison-case-notes")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(caseNotes.map(caseNoteTransformer::transformModelToApi))
      )
  }

  private fun mockCaseNotesPrisonApiCall(page: Int, from: LocalDate, nomsNumber: String, result: CaseNotesPage): StubMapping? {
    val fromLocalDateTime = LocalDateTime.of(from, LocalTime.MIN)

    return wiremockServer.stubFor(
      WireMock.get(WireMock.urlEqualTo("/case-notes/$nomsNumber?startDate=$fromLocalDateTime&page=$page&size=30"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(result)
            )
        )
    )
  }
}
