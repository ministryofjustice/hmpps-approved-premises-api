package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Assessment for Approved Premises`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.time.OffsetDateTime
import java.util.UUID

class PlacementApplicationsTest : IntegrationTestBase() {

  @Nested
  inner class CreatePlacementApplicationTest {
    @Test
    fun `creating a placement application JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-applications")
        .bodyValue(
          NewPlacementApplication(
            applicationId = UUID.randomUUID(),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `creating a placement application when the application does not exist returns 404`() {
      `Given a User` { _, jwt ->
        webTestClient.post()
          .uri("/placement-applications")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewPlacementApplication(
              applicationId = UUID.randomUUID(),
            ),
          )
          .exchange()
          .expectStatus()
          .isNotFound()
      }
    }

    @Test
    fun `creating a placement application when the application does not belong to the user returns 401`() {
      `Given a User` { _, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Application`(createdByUser = otherUser) { application ->
            webTestClient.post()
              .uri("/placement-applications")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                NewPlacementApplication(
                  applicationId = application.id,
                ),
              )
              .exchange()
              .expectStatus()
              .isForbidden()
          }
        }
      }
    }

    @Test
    fun `creating a placement application when the application does not have an assessment returns an error`() {
      `Given a User` { user, jwt ->
        `Given an Application`(createdByUser = user) { application ->
          webTestClient.post()
            .uri("/placement-applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewPlacementApplication(
                applicationId = application.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest()
        }
      }
    }

    @Test
    fun `creating a placement application when the assessment has been rejected returns an error`() {
      `Given a User` { user, jwt ->
        `Given an Assessment for Approved Premises`(decision = AssessmentDecision.REJECTED, allocatedToUser = user, createdByUser = user) { _, application ->
          approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          webTestClient.post()
            .uri("/placement-applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewPlacementApplication(
                applicationId = application.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest()
        }
      }
    }

    @Test
    fun `creating a placement application when the application belongs to the user returns successfully`() {
      `Given a User` { user, jwt ->
        `Given an Assessment for Approved Premises`(decision = AssessmentDecision.ACCEPTED, allocatedToUser = user, createdByUser = user) { _, application ->
          val schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val rawResult = webTestClient.post()
            .uri("/placement-applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewPlacementApplication(
                applicationId = application.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

          assertThat(body.applicationId).isEqualTo(application.id)
          assertThat(body.applicationId).isEqualTo(application.id)
          assertThat(body.outdatedSchema).isEqualTo(false)
          assertThat(body.createdAt).isNotNull()
          assertThat(body.schemaVersion).isEqualTo(schema.id)
        }
      }
    }
  }

  @Nested
  inner class GetPlacementApplicationTest {
    @Test
    fun `getting a placement request application JWT returns 401`() {
      `Given a User` { user, _ ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        ) { placementApplicationEntity ->
          webTestClient.get()
            .uri("/placement-request-applications/${placementApplicationEntity.id}")
            .exchange()
            .expectStatus()
            .isUnauthorized
        }
      }
    }

    @Test
    fun `getting a nonexistent placement request application returns 404`() {
      `Given a User` { _, jwt ->
        webTestClient.get()
          .uri("/placement-applications/${UUID.randomUUID()}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `getting a placement application returns the transformed object`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        ) { placementApplicationEntity ->
          val rawResult = webTestClient.get()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

          assertThat(body.id).isEqualTo(placementApplicationEntity.id)
          assertThat(body.applicationId).isEqualTo(placementApplicationEntity.application.id)
          assertThat(body.createdByUserId).isEqualTo(placementApplicationEntity.createdByUser.id)
          assertThat(body.schemaVersion).isEqualTo(placementApplicationEntity.schemaVersion.id)
          assertThat(body.createdAt).isEqualTo(placementApplicationEntity.createdAt.toInstant())
          assertThat(body.submittedAt).isNull()
        }
      }
    }
  }

  @Nested
  inner class UpdatePlacementApplicationTest {
    @Test
    fun `updating a placement request application without a JWT returns 401`() {
      webTestClient.put()
        .uri("/placement-applications/${UUID.randomUUID()}")
        .bodyValue(
          UpdatePlacementApplication(
            data = mapOf("thingId" to 123),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `updating a submitted placement request application returns an error`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          webTestClient.put()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdatePlacementApplication(
                data = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `updating a placement request application created by a different user returns an error`() {
      `Given a User` { _, jwt ->
        `Given a Placement Application`(
          createdByUser = userEntityFactory.produceAndPersist {
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
              }
            }
          },
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          webTestClient.put()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdatePlacementApplication(
                data = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `updating a placement request application with an outdated schema returns an error`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          webTestClient.put()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdatePlacementApplication(
                data = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `updating an in-progress placement request application returns successfully and updates the application`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        ) { placementApplicationEntity ->
          val rawResult = webTestClient.put()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdatePlacementApplication(
                data = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)
          val expectedUpdatedPlacementApplication = placementApplicationEntity.copy(
            schemaUpToDate = true,
            data = "{\"thingId\":123}",
          )

          assertThat(body).matches {
            expectedUpdatedPlacementApplication.id == it.id &&
              expectedUpdatedPlacementApplication.application.id == it.applicationId &&
              expectedUpdatedPlacementApplication.createdByUser.id == it.createdByUserId &&
              expectedUpdatedPlacementApplication.schemaVersion.id == it.schemaVersion &&
              expectedUpdatedPlacementApplication.createdAt.toInstant() == it.createdAt &&
              serializableToJsonNode(expectedUpdatedPlacementApplication.data) == serializableToJsonNode(it.data)
          }

          val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)

          assertThat(updatedPlacementApplication!!.data).isEqualTo(expectedUpdatedPlacementApplication.data)
        }
      }
    }
  }

  @Nested
  inner class SubmitPlacementApplicationTest {
    @Test
    fun `submitting a placement request application without a JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-applications/${UUID.randomUUID()}/submission")
        .bodyValue(
          SubmitPlacementApplication(
            translatedDocument = mapOf("thingId" to 123),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `submitting a submitted placement request application returns an error`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/submission")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              SubmitPlacementApplication(
                translatedDocument = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `submitting a placement request application created by a different user returns an error`() {
      `Given a User` { _, jwt ->
        `Given a Placement Application`(
          createdByUser = userEntityFactory.produceAndPersist {
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
              }
            }
          },
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/submission")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              SubmitPlacementApplication(
                translatedDocument = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `submitting a placement request application with an outdated schema returns an error`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/submission")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              SubmitPlacementApplication(
                translatedDocument = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `submitting an in-progress placement request application returns successfully and updates the application`() {
      `Given a User`(roles = listOf(UserRole.CAS1_ASSESSOR), qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS)) { assessorUser, _ ->
        `Given a User` { user, jwt ->
          `Given a Placement Application`(
            createdByUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          ) { placementApplicationEntity ->
            val rawResult = webTestClient.post()
              .uri("/placement-applications/${placementApplicationEntity.id}/submission")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                SubmitPlacementApplication(
                  translatedDocument = mapOf("thingId" to 123),
                ),
              )
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)
            val expectedUpdatedPlacementApplication = placementApplicationEntity.copy(
              schemaUpToDate = true,
              document = "{\"thingId\":123}",
            )

            assertThat(body).matches {
              expectedUpdatedPlacementApplication.id == it.id &&
                expectedUpdatedPlacementApplication.application.id == it.applicationId &&
                expectedUpdatedPlacementApplication.createdByUser.id == it.createdByUserId &&
                expectedUpdatedPlacementApplication.schemaVersion.id == it.schemaVersion &&
                expectedUpdatedPlacementApplication.createdAt.toInstant() == it.createdAt &&
                serializableToJsonNode(expectedUpdatedPlacementApplication.document) == serializableToJsonNode(it.document)
            }

            val updatedPlacementApplication =
              placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)!!

            assertThat(updatedPlacementApplication.document).isEqualTo(expectedUpdatedPlacementApplication.document)
            assertThat(updatedPlacementApplication.submittedAt).isNotNull()
            assertThat(updatedPlacementApplication.allocatedToUser!!.id).isEqualTo(assessorUser.id)
            assertThat(updatedPlacementApplication.allocatedAt).isNotNull()
          }
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
