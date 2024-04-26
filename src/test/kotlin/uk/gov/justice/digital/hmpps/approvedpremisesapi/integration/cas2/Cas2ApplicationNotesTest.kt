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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 Licence Case Admin User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 POM User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas2ApplicationNotesTest(
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.submitted-application-overview}") private val assessmentUrlTemplate: String,
) : IntegrationTestBase() {

  @SpykBean
  lateinit var realNotesRepository: Cas2ApplicationNoteRepository

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

    @Test
    fun `assessors create note returns 201`() {
      val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

      `Given a CAS2 POM User` { referrer, _ ->
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

          cas2AssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withNacroReferralId("OH123")
            withAssessorName("Anne Assessor")
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
              "applicationUrl" to applicationUrlTemplate.replace("#id", application.id.toString()),
            ),
            replyToEmailId = "cbe00c2d-387b-4283-9b9c-13c8b7a61444",
          )
        }
      }
    }
  }

  @Nested
  inner class PostToCreate {

    @Nested
    inner class ControlsOnInternalUsers {

      @Nested
      inner class WhenUserIsPom {
        @Nested
        inner class WhenApplicationCreatedByUser {
          @Test
          fun `referrer create note returns 201`() {
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
            `Given a CAS2 POM User` { referrer, jwt ->
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
              }

              cas2AssessmentEntityFactory.produceAndPersist {
                withApplication(application)
                withNacroReferralId("OH123")
                withAssessorName("Anne Assessor")
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
                toEmailAddress = "assessors@example.com",
                templateId = "0d646bf0-d40f-4fe7-aa74-dd28b10d04f1",
                personalisation = mapOf(
                  "nacroReferenceId" to "OH123",
                  "nacroReferenceIdInSubject" to "(OH123)",
                  "dateNoteAdded" to OffsetDateTime.ofInstant(responseBody.createdAt, ZoneOffset.UTC).toLocalDate().toCas2UiFormat(),
                  "timeNoteAdded" to OffsetDateTime.ofInstant(responseBody.createdAt, ZoneOffset.UTC).toCas2UiFormattedHourOfDay(),
                  "assessorName" to "Anne Assessor",
                  "applicationType" to "Home Detention Curfew (HDC)",
                  "applicationUrl" to assessmentUrlTemplate.replace("#applicationId", application.id.toString()),
                ),
                replyToEmailId = "cbe00c2d-387b-4283-9b9c-13c8b7a61444",
              )
            }
          }
        }

        @Nested
        inner class WhenApplicationCreatedByDifferentUser {
          @Nested
          inner class WhenDifferentPrison {

            @Test
            fun `referrer cannot create note`() {
              val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

              `Given a CAS2 POM User` { referrer, jwt ->
                val applicationSchema =
                  cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
                    withAddedAt(OffsetDateTime.now())
                    withId(UUID.randomUUID())
                    withSchema(
                      schema,
                    )
                  }

                val otherUser = nomisUserEntityFactory.produceAndPersist {
                  withActiveCaseloadId("another-prison")
                }

                cas2ApplicationEntityFactory.produceAndPersist {
                  withId(applicationId)
                  withCreatedByUser(otherUser)
                  withApplicationSchema(applicationSchema)
                  withSubmittedAt(OffsetDateTime.now())
                  withReferringPrisonCode("another-prison")
                }

                Assertions.assertThat(realNotesRepository.count()).isEqualTo(0)

                webTestClient.post()
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

          @Nested
          inner class WhenSamePrison {

            @Test
            fun `referrer can create a note for an application they did not create`() {
              `Given a CAS2 POM User` { referrer, jwt ->
                val applicationSchema =
                  cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
                    withAddedAt(OffsetDateTime.now())
                    withId(UUID.randomUUID())
                    withSchema(
                      schema,
                    )
                  }

                val otherUser = nomisUserEntityFactory.produceAndPersist {
                  withActiveCaseloadId(referrer.activeCaseloadId!!)
                }

                val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(applicationSchema)
                  withCreatedByUser(otherUser)
                  withSubmittedAt(OffsetDateTime.now().minusDays(1))
                  withReferringPrisonCode(referrer.activeCaseloadId!!)
                }

                cas2AssessmentEntityFactory.produceAndPersist {
                  withApplication(applicationEntity)
                  withNacroReferralId("OH456")
                  withAssessorName("Anne Other Assessor")
                }

                Assertions.assertThat(realNotesRepository.count()).isEqualTo(0)

                val rawResponseBody = webTestClient.post()
                  .uri("/cas2/submissions/${applicationEntity.id}/notes")
                  .header("Authorization", "Bearer $jwt")
                  .header("X-Service-Name", ServiceName.cas2.value)
                  .bodyValue(
                    NewCas2ApplicationNote(note = "New prison note content"),
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

                Assertions.assertThat(responseBody.body).isEqualTo("New prison note content")

                emailAsserter.assertEmailsRequestedCount(1)
                emailAsserter.assertEmailRequested(
                  toEmailAddress = "assessors@example.com",
                  templateId = "0d646bf0-d40f-4fe7-aa74-dd28b10d04f1",
                  personalisation = mapOf(
                    "nacroReferenceId" to "OH456",
                    "nacroReferenceIdInSubject" to "(OH456)",
                    "dateNoteAdded" to OffsetDateTime.ofInstant(responseBody.createdAt, ZoneOffset.UTC).toLocalDate().toCas2UiFormat(),
                    "timeNoteAdded" to OffsetDateTime.ofInstant(responseBody.createdAt, ZoneOffset.UTC).toCas2UiFormattedHourOfDay(),
                    "assessorName" to "Anne Other Assessor",
                    "applicationType" to "Home Detention Curfew (HDC)",
                    "applicationUrl" to assessmentUrlTemplate.replace("#applicationId", applicationEntity.id.toString()),
                  ),
                  replyToEmailId = "cbe00c2d-387b-4283-9b9c-13c8b7a61444",
                )
              }
            }
          }
        }
      }

      @Nested
      inner class WhenUserIsLicenceCA {
        @Nested
        inner class WhenApplicationCreatedByUser {
          @Test
          fun `referrer create note returns 201`() {
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
            `Given a CAS2 Licence Case Admin User` { referrer, jwt ->
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
              }

              cas2AssessmentEntityFactory.produceAndPersist {
                withApplication(application)
                withNacroReferralId("OH123")
                withAssessorName("Anne Assessor")
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
                toEmailAddress = "assessors@example.com",
                templateId = "0d646bf0-d40f-4fe7-aa74-dd28b10d04f1",
                personalisation = mapOf(
                  "nacroReferenceId" to "OH123",
                  "nacroReferenceIdInSubject" to "(OH123)",
                  "dateNoteAdded" to OffsetDateTime.ofInstant(responseBody.createdAt, ZoneOffset.UTC).toLocalDate().toCas2UiFormat(),
                  "timeNoteAdded" to OffsetDateTime.ofInstant(responseBody.createdAt, ZoneOffset.UTC).toCas2UiFormattedHourOfDay(),
                  "assessorName" to "Anne Assessor",
                  "applicationType" to "Home Detention Curfew (HDC)",
                  "applicationUrl" to assessmentUrlTemplate.replace("#applicationId", application.id.toString()),
                ),
                replyToEmailId = "cbe00c2d-387b-4283-9b9c-13c8b7a61444",
              )
            }
          }
        }

        @Nested
        inner class WhenApplicationCreatedByDifferentUser {
          @Nested
          inner class WhenDifferentPrison {

            @Test
            fun `referrer cannot create note`() {
              val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

              `Given a CAS2 Licence Case Admin User` { _, jwt ->
                val applicationSchema =
                  cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
                    withAddedAt(OffsetDateTime.now())
                    withId(UUID.randomUUID())
                    withSchema(
                      schema,
                    )
                  }

                val otherUser = nomisUserEntityFactory.produceAndPersist {
                  withActiveCaseloadId("another-prison")
                }

                cas2ApplicationEntityFactory.produceAndPersist {
                  withId(applicationId)
                  withCreatedByUser(otherUser)
                  withApplicationSchema(applicationSchema)
                  withSubmittedAt(OffsetDateTime.now())
                  withReferringPrisonCode("another-prison")
                }

                Assertions.assertThat(realNotesRepository.count()).isEqualTo(0)

                webTestClient.post()
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

          @Nested
          inner class WhenSamePrison {

            @Test
            fun `referrer can create a note for an application they did not create`() {
              `Given a CAS2 POM User` { referrer, jwt ->
                val applicationSchema =
                  cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
                    withAddedAt(OffsetDateTime.now())
                    withId(UUID.randomUUID())
                    withSchema(
                      schema,
                    )
                  }

                val otherUser = nomisUserEntityFactory.produceAndPersist {
                  withActiveCaseloadId(referrer.activeCaseloadId!!)
                }

                val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(applicationSchema)
                  withCreatedByUser(otherUser)
                  withSubmittedAt(OffsetDateTime.now().minusDays(1))
                  withReferringPrisonCode(referrer.activeCaseloadId!!)
                }

                cas2AssessmentEntityFactory.produceAndPersist {
                  withApplication(applicationEntity)
                  withNacroReferralId("OH456")
                  withAssessorName("Anne Other Assessor")
                }

                Assertions.assertThat(realNotesRepository.count()).isEqualTo(0)

                val rawResponseBody = webTestClient.post()
                  .uri("/cas2/submissions/${applicationEntity.id}/notes")
                  .header("Authorization", "Bearer $jwt")
                  .header("X-Service-Name", ServiceName.cas2.value)
                  .bodyValue(
                    NewCas2ApplicationNote(note = "New prison note content"),
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

                Assertions.assertThat(responseBody.body).isEqualTo("New prison note content")

                emailAsserter.assertEmailsRequestedCount(1)
                emailAsserter.assertEmailRequested(
                  toEmailAddress = "assessors@example.com",
                  templateId = "0d646bf0-d40f-4fe7-aa74-dd28b10d04f1",
                  personalisation = mapOf(
                    "nacroReferenceId" to "OH456",
                    "nacroReferenceIdInSubject" to "(OH456)",
                    "dateNoteAdded" to OffsetDateTime.ofInstant(responseBody.createdAt, ZoneOffset.UTC).toLocalDate().toCas2UiFormat(),
                    "timeNoteAdded" to OffsetDateTime.ofInstant(responseBody.createdAt, ZoneOffset.UTC).toCas2UiFormattedHourOfDay(),
                    "assessorName" to "Anne Other Assessor",
                    "applicationType" to "Home Detention Curfew (HDC)",
                    "applicationUrl" to assessmentUrlTemplate.replace("#applicationId", applicationEntity.id.toString()),
                  ),
                  replyToEmailId = "cbe00c2d-387b-4283-9b9c-13c8b7a61444",
                )
              }
            }
          }
        }
      }
    }
  }
}
