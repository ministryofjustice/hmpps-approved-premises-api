package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import io.mockk.every
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 Assessor`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import java.time.OffsetDateTime
import java.util.UUID

class Cas2SubmissionTest : IntegrationTestBase() {
  @SpykBean
  lateinit var realApplicationRepository: ApplicationRepository

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realApplicationRepository)
  }

  @Nested
  inner class ControlsOnExternalUsers {
    @Test
    fun `submitting an application is forbidden to external users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.post()
        .uri("/cas2/submissions?applicationId=de6512fc-a225-4109-bdcd-86c6307a5237")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing submitted applications is forbidden to internal users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_PRISON"),
      )

      webTestClient.get()
        .uri("/cas2/submissions")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing a single submitted application is forbidden to internal users based on     role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_PRISON"),
      )

      webTestClient.get()
        .uri("/cas2/submissions/66911cf0-75b1-4361-84bd-501b176fd4fd")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `Get all submitted applications without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/submissions")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get single submitted application without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/submissions/9b785e59-b85c-4be0-b271-d9ac287684b6")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class GetToIndex {

    @Test
    fun `Assessor can view ALL submitted applications`() {
      `Given a CAS2 Assessor` { _externalUserEntity, jwt ->
        `Given a CAS2 User` { user, _ ->
          `Given an Offender` { offenderDetails, _ ->
            cas2ApplicationJsonSchemaRepository.deleteAll()

            val applicationSchema =
              cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

            val submittedCas2ApplicationEntity = cas2ApplicationEntityFactory
              .produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(user)
                withCrn(offenderDetails.otherIds.crn)
                withSubmittedAt(OffsetDateTime.parse("2023-01-01T09:00:00+01:00"))
                withData("{}")
              }

            val inProgressCas2ApplicationEntity = cas2ApplicationEntityFactory
              .produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(user)
                withCrn(offenderDetails.otherIds.crn)
                withSubmittedAt(null)
                withData("{}")
              }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2/submissions")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody =
              objectMapper.readValue(
                rawResponseBody,
                object : TypeReference<List<Cas2SubmittedApplicationSummary>>() {},
              )

            Assertions.assertThat(responseBody).anyMatch {
              submittedCas2ApplicationEntity.id == it.id &&
                submittedCas2ApplicationEntity.crn == it.person.crn &&
                submittedCas2ApplicationEntity.createdAt.toInstant() == it.createdAt &&
                submittedCas2ApplicationEntity.createdByUser.id == it.createdByUserId &&
                submittedCas2ApplicationEntity.submittedAt?.toInstant() == it.submittedAt
            }

            Assertions.assertThat(responseBody).noneMatch {
              inProgressCas2ApplicationEntity.id == it.id
            }
          }
        }
      }
    }
  }

  @Nested
  inner class GetToShow {
    @Test
    fun `Assessor can view single submitted application`() {
      `Given a CAS2 Assessor` { _, jwt ->
        `Given a CAS2 User` { user, _ ->
          `Given an Offender` { offenderDetails, _ ->
            cas2ApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = cas2ApplicationJsonSchemaEntityFactory
              .produceAndPersist {
                withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                withSchema(
                  """
          {
            "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
            "${"\$id"}": "https://example.com/product.schema.json",
            "title": "Thing",
            "description": "A thing",
            "type": "object",
            "properties": {
              "thingId": {
                "description": "The unique identifier for a thing",
                "type": "integer"
              }
            },
            "required": [ "thingId" ]
          }
          """,
                )
              }

            val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(user)
              withSubmittedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
              withData(
                """
            {
               "thingId": 123
            }
            """,
              )
            }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2/submissions/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(
              rawResponseBody,
              Cas2SubmittedApplication::class.java,
            )

            Assertions.assertThat(responseBody).matches {
              applicationEntity.id == it.id &&
                applicationEntity.crn == it.person.crn &&
                applicationEntity.createdAt.toInstant() == it.createdAt &&
                applicationEntity.createdByUser.id == it.createdByUserId &&
                applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
                serializableToJsonNode(applicationEntity.document) ==
                  serializableToJsonNode(it.document) &&
                newestJsonSchema.id == it.schemaVersion && !it.outdatedSchema
            }
          }
        }
      }
    }

    @Test
    fun `Assessor can NOT view single in-progress application`() {
      `Given a CAS2 Assessor` { _, jwt ->
        `Given a CAS2 User` { user, _ ->
          `Given an Offender` { offenderDetails, _ ->
            cas2ApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = cas2ApplicationJsonSchemaEntityFactory
              .produceAndPersist {
                withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                withSchema(
                  """
          {
            "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
            "${"\$id"}": "https://example.com/product.schema.json",
            "title": "Thing",
            "description": "A thing",
            "type": "object",
            "properties": {
              "thingId": {
                "description": "The unique identifier for a thing",
                "type": "integer"
              }
            },
            "required": [ "thingId" ]
          }
          """,
                )
              }

            val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(user)
              withSubmittedAt(null)
              withData(
                """
            {
               "thingId": 123
            }
            """,
              )
            }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2/submissions/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isNotFound
          }
        }
      }
    }
  }

  @Nested
  inner class PostToCreate {
    @Test
    fun `Submit Cas2 application returns 200`() {
      `Given a CAS2 User`() { submittingUser, jwt ->
        `Given a CAS2 User` { userEntity, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

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

            cas2ApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withApplicationSchema(applicationSchema)
              withCreatedByUser(submittingUser)
              withData(
                """
                {}
              """,
              )
            }

            webTestClient.post()
              .uri("/cas2/submissions")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .bodyValue(
                SubmitCas2Application(
                  applicationId = applicationId,
                  translatedDocument = {},
                ),
              )
              .exchange()
              .expectStatus()
              .isOk
          }
        }
      }
    }

    @Test
    fun `When several concurrent submit application requests occur, only one is successful, all others return 400`() {
      `Given a CAS2 User`() { submittingUser, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

          val applicationSchema = cas2ApplicationJsonSchemaEntityFactory
            .produceAndPersist {
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
              "properties": {}
              },
              "required": [  ]
            }
          """,
              )
            }

          cas2ApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withId(applicationId)
            withApplicationSchema(applicationSchema)
            withCreatedByUser(submittingUser)
            withData(
              """
                {}
              """,
            )
          }

          every { realApplicationRepository.save(any()) } answers {
            Thread.sleep(1000)
            it.invocation.args[0] as ApplicationEntity
          }

          val responseStatuses = mutableListOf<HttpStatus>()

          (1..10).map {
            val thread = Thread {
              webTestClient.post()
                .uri("/cas2/submissions")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  SubmitCas2Application(
                    applicationId = applicationId,
                    translatedDocument = {},
                  ),
                )
                .exchange()
                .returnResult<String>()
                .consumeWith {
                  synchronized(responseStatuses) {
                    responseStatuses += it.status
                  }
                }
            }

            thread.start()

            thread
          }.forEach(Thread::join)

          Assertions.assertThat(responseStatuses.count { it.value() == 200 }).isEqualTo(1)
          Assertions.assertThat(responseStatuses.count { it.value() == 400 }).isEqualTo(9)
        }
      }
    }
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
  }
}
