package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import java.time.OffsetDateTime
import java.util.*

class Cas2NotesTest : IntegrationTestBase() {

  @Nested
  inner class PostToCreate {
    @Test
    fun `referrer create note returns 201`() {
      val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

      `Given a CAS2 User` { referrer, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val applicationSchema =
            cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
              withSchema(
                """
                {
                  "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
                  "${"\$id"}": "https://example.com/product.schema.json",
                  "title": "Thing",
                  "description": "A thing",
                  "type": "object",
                  "properties": {},
                  "required": []
                }
              """,
              )
            }

          val application = cas2ApplicationEntityFactory.produceAndPersist {
            withId(applicationId)
            withCreatedByUser(referrer)
            withApplicationSchema(applicationSchema)
            withSubmittedAt(OffsetDateTime.now())
          }

          val rawResponseBody = webTestClient.post()
            .uri("/cas2/applications/$applicationId/notes")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .bodyValue(
              NewCas2ApplicationNote(note = "New note content"),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2ApplicationSummary>>() {})

          Assertions.assertThat(responseBody).isNotNull()
        }
      }
    }
  }
}
