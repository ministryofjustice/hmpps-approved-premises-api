package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASarClientCredentialsApiCall

@TestPropertySource(
  properties = [
    "hmpps.sar.template.path=template_hmpps-approved-premises-api.mustache",
    "hmpps.sar.template.enabled=true",
  ],
)
class SubjectAccessRequestTemplateTest : IntegrationTestBase() {

  @Test
  fun `Get SAR template returns 200 OK with template content for user with SAR_DATA_ACCESS role`() {
    givenASarClientCredentialsApiCall { jwt ->
      webTestClient.get()
        .uri("/subject-access-request/template")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType("text/plain;charset=UTF-8")
        .expectBody(String::class.java).value { content ->
          assert(content.contains("<h1 class=\"title\">Approved Premises</h1>"))
        }
    }
  }

  @Test
  fun `Get SAR template returns 403 Forbidden for user without required role`() {
    givenASarClientCredentialsApiCall(role = "OTHER") { jwt ->
      webTestClient.get()
        .uri("/subject-access-request/template")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus().isForbidden
    }
  }
}
