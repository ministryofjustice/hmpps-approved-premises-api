package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.fasterxml.jackson.core.type.TypeReference
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 Assessor`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas2ApplicationNotesTest(
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationUrlTemplate: String,
) : IntegrationTestBase() {

  @SpykBean
  lateinit var realNotesRepository: Cas2ApplicationNoteRepository

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realNotesRepository)
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `creating a note without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas2/submissions/de6512fc-a225-4109-bdcd-86c6307a5237/notes")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class ControlsOnExternalUsers {
    @Test
    fun `creating a note is forbidden to external users who are not Assessors`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "auth",
        roles = listOf("ROLE_CAS2_ADMIN"),
      )

      webTestClient.post()
        .uri("/cas2/submissions/de6512fc-a225-4109-bdcd-86c6307a5237/notes")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class ControlsOnInternalUsers {
    @Test
    fun `creating a note is forbidden to nomis users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_ADMIN"),
      )

      webTestClient.post()
        .uri("/cas2/submissions/de6512fc-a225-4109-bdcd-86c6307a5237/notes")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class PostToCreate {
    val schema = """
                {
                  "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
                  "${"\$id"}": "https://example.com/product.schema.json",
                  "title": "Thing",
                  "description": "A thing",
                  "type": "object",
                  "properties": {},
                  "required": []
                }
              """

    @Test
    fun `referrer create note returns 201`() {
      val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
      `Given a CAS2 User` { referrer, jwt ->
        val applicationSchema =
          cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
            withSchema(
              schema,
            )
          }

        cas2ApplicationEntityFactory.produceAndPersist {
          withId(applicationId)
          withCreatedByUser(referrer)
          withApplicationSchema(applicationSchema)
          withSubmittedAt(OffsetDateTime.now())
        }

        Assertions.assertThat(realNotesRepository.count()).isEqualTo(0)

        val rawResponseBody = webTestClient.post()
          .uri("/cas2/submissions/$applicationId/notes")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.cas2.value)
          .bodyValue(
            NewCas2ApplicationNote(note = "New note content"),
          )
          .exchange()
          .expectStatus()
          .isCreated()
          .returnResult<String>()
          .responseBody
          .blockFirst()

        Assertions.assertThat(realNotesRepository.count()).isEqualTo(1)

        val responseBody =
          objectMapper.readValue(rawResponseBody, object : TypeReference<Cas2ApplicationNote>() {})

        Assertions.assertThat(responseBody.body).isEqualTo("New note content")
      }
    }

    @Test
    fun `referrer cannot create note for an application they did not create`() {
      val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

      `Given a CAS2 User` { referrer, jwt ->
        `Given a CAS2 User` { aDifferentReferrer, _ ->
          val applicationSchema =
            cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
              withSchema(
                schema,
              )
            }

          cas2ApplicationEntityFactory.produceAndPersist {
            withId(applicationId)
            withCreatedByUser(aDifferentReferrer)
            withApplicationSchema(applicationSchema)
            withSubmittedAt(OffsetDateTime.now())
          }

          Assertions.assertThat(realNotesRepository.count()).isEqualTo(0)

          val rawResponseBody = webTestClient.post()
            .uri("/cas2/submissions/$applicationId/notes")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .bodyValue(
              NewCas2ApplicationNote(note = "New note content"),
            )
            .exchange()
            .expectStatus()
            .isForbidden

          Assertions.assertThat(realNotesRepository.count()).isEqualTo(0)
        }
      }
    }

    @Test
    fun `assessors create note returns 201`() {
      val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

      `Given a CAS2 User` { referrer, _ ->
        `Given a CAS2 Assessor` { assessor, jwt ->
          val applicationSchema =
            cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
              withSchema(
                schema,
              )
            }

          val application = cas2ApplicationEntityFactory.produceAndPersist {
            withId(applicationId)
            withCreatedByUser(referrer)
            withApplicationSchema(applicationSchema)
            withSubmittedAt(OffsetDateTime.now())
            withNomsNumber("123NOMS")
          }

          Assertions.assertThat(realNotesRepository.count()).isEqualTo(0)

          val rawResponseBody = webTestClient.post()
            .uri("/cas2/submissions/$applicationId/notes")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .bodyValue(
              NewCas2ApplicationNote(note = "New note content"),
            )
            .exchange()
            .expectStatus()
            .isCreated()
            .returnResult<String>()
            .responseBody
            .blockFirst()

          Assertions.assertThat(realNotesRepository.count()).isEqualTo(1)

          val responseBody =
            objectMapper.readValue(rawResponseBody, object : TypeReference<Cas2ApplicationNote>() {})

          Assertions.assertThat(responseBody.body).isEqualTo("New note content")

          emailAsserter.assertEmailsRequestedCount(1)
          emailAsserter.assertEmailRequested(
            toEmailAddress = referrer.email!!,
            templateId = "debe17a2-9f79-4d26-88a0-690dd73e2a5b",
            personalisation = mapOf(
              "dateNoteAdded" to OffsetDateTime.ofInstant(responseBody.createdAt, ZoneOffset.UTC).toLocalDate().toCas2UiFormat(),
              "timeNoteAdded" to OffsetDateTime.ofInstant(responseBody.createdAt, ZoneOffset.UTC).toCas2UiFormattedHourOfDay(),
              "nomsNumber" to "123NOMS",
              "applicationType" to "Home Detention Curfew (HDC)",
              "applicationURl" to applicationUrlTemplate.replace("#id", application.id.toString()),
            ),
          )
        }
      }
    }
  }
}
