package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockOffenderUserAccessCall
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationTest : IntegrationTestBase() {
  @Nested
  inner class MissingJwt {
    @Test
    fun `Get all applications without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get single application without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/applications/9b785e59-b85c-4be0-b271-d9ac287684b6")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Create new application without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas2/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class GetToIndex {
    @Test
    fun `Get all applications returns 200 with correct body - when the service is CAS2`() {
      `Given a User` { userEntity, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            cas2ApplicationJsonSchemaRepository.deleteAll()

            val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
            }

            val cas2ApplicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(userEntity)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
            }

            val otherCas2ApplicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(otherUser)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
            }

            CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

            val rawResponseBody = webTestClient.get()
              .uri("/cas2/applications")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody =
              objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2ApplicationSummary>>() {})

            Assertions.assertThat(responseBody).anyMatch {
              cas2ApplicationEntity.id == it.id &&
                cas2ApplicationEntity.crn == it.person.crn &&
                cas2ApplicationEntity.createdAt.toInstant() == it.createdAt &&
                cas2ApplicationEntity.createdByUser.id == it.createdByUserId &&
                cas2ApplicationEntity.submittedAt?.toInstant() == it.submittedAt
            }

            Assertions.assertThat(responseBody).noneMatch {
              otherCas2ApplicationEntity.id == it.id
            }
          }
        }
      }
    }
  }

  @Test
  fun `Create new application for CAS-2 returns 201 with correct body and Location header`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        val result = webTestClient.post()
          .uri("/cas2/applications")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.cas2.value)
          .bodyValue(
            NewApplication(
              crn = offenderDetails.otherIds.crn,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .returnResult(Cas2Application::class.java)

        Assertions.assertThat(result.responseHeaders["Location"]).anyMatch {
          it.matches(Regex("/applications/.+"))
        }

        Assertions.assertThat(result.responseBody.blockFirst()).matches {
          it.person.crn == offenderDetails.otherIds.crn &&
            it.schemaVersion == applicationSchema.id
        }
      }
    }
  }

  @Test
  fun `Update existing CAS2 application returns 200 with correct body`() {
    `Given a User` { submittingUser, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

        val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
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

        cas2ApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withId(applicationId)
          withApplicationSchema(applicationSchema)
          withCreatedByUser(submittingUser)
        }

        val resultBody = webTestClient.put()
          .uri("/cas2/applications/$applicationId")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateCas2Application(
              data = mapOf("thingId" to 123),
              type = UpdateApplicationType.CAS2,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .returnResult(String::class.java)
          .responseBody
          .blockFirst()

        val result = objectMapper.readValue(resultBody, Application::class.java)

        Assertions.assertThat(result.person.crn).isEqualTo(offenderDetails.otherIds.crn)
      }
    }
  }

  @Test
  fun `Submit Cas2 application returns 200`() {
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(
          listOf(
            StaffUserTeamMembershipFactory().produce(),
          ),
        )
      },
    ) { submittingUser, jwt ->
      `Given a User` { userEntity, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

          val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
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
            .uri("/cas2/applications/$applicationId/submission")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .bodyValue(
              SubmitCas2Application(
                translatedDocument = {},
                type = "CAS2",
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
        }
      }
    }
  }
}
