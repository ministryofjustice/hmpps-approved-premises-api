package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import java.time.OffsetDateTime

class ApplicationTest : IntegrationTestBase() {
  @Autowired
  lateinit var applicationsTransformer: ApplicationsTransformer

  @Test
  fun `Get all applications without JWT returns 401`() {
    webTestClient.get()
      .uri("/applications")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get all applications returns 200 with correct body, outdated applications upgraded where possible`() {
    applicationSchemaRepository.deleteAll()

    val newestJsonSchema = applicationSchemaEntityFactory.produceAndPersist {
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
        """
      )
    }

    val olderJsonSchema = applicationSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T09:45:00+01:00"))
      withSchema(
        """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": { }
        }
        """
      )
    }

    val probationOfficerEntity = probationOfficerEntityFactory.produceAndPersist { withDistinguishedName("PROBATIONPERSON") }

    val upgradableApplicationEntity = applicationEntityFactory.produceAndPersist {
      withApplicationSchema(olderJsonSchema)
      withCreatedByProbationOfficer(probationOfficerEntity)
      withData(
        """
          {
             "thingId": 123
          }
          """
      )
    }

    val nonUpgradableApplicationEntity = applicationEntityFactory.produceAndPersist {
      withApplicationSchema(olderJsonSchema)
      withCreatedByProbationOfficer(probationOfficerEntity)
      withData("{}")
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    val rawResponseBody = webTestClient.get()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .returnResult<String>()
      .responseBody
      .blockFirst()

    val responseBody = objectMapper.readValue(rawResponseBody, object : TypeReference<List<Application>>() {})

    assertThat(responseBody).anyMatch {
      nonUpgradableApplicationEntity.id == it.id &&
        nonUpgradableApplicationEntity.crn == it.crn &&
        nonUpgradableApplicationEntity.createdAt.toInstant() == it.createdAt.toInstant() &&
        nonUpgradableApplicationEntity.createdByProbationOfficer.id == it.createdByProbationOfficerId &&
        nonUpgradableApplicationEntity.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
        serializableToJsonNode(nonUpgradableApplicationEntity.data) == serializableToJsonNode(it.data) &&
        olderJsonSchema.id == it.schemaVersion &&
        it.outdatedSchema == true
    }

    assertThat(responseBody).anyMatch {
      upgradableApplicationEntity.id == it.id &&
        upgradableApplicationEntity.crn == it.crn &&
        upgradableApplicationEntity.createdAt.toInstant() == it.createdAt.toInstant() &&
        upgradableApplicationEntity.createdByProbationOfficer.id == it.createdByProbationOfficerId &&
        upgradableApplicationEntity.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
        serializableToJsonNode(upgradableApplicationEntity.data) == serializableToJsonNode(it.data) &&
        newestJsonSchema.id == it.schemaVersion &&
        it.outdatedSchema == false
    }
  }

  @Test
  fun `Get single application without JWT returns 401`() {
    webTestClient.get()
      .uri("/applications/9b785e59-b85c-4be0-b271-d9ac287684b6")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get single application returns 200 with correct body, outdated application upgraded`() {
    applicationSchemaRepository.deleteAll()

    val newestJsonSchema = applicationSchemaEntityFactory.produceAndPersist {
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
        """
      )
    }

    val olderJsonSchema = applicationSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T09:45:00+01:00"))
      withSchema(
        """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": { }
        }
        """
      )
    }

    val probationOfficerEntity = probationOfficerEntityFactory.produceAndPersist { withDistinguishedName("PROBATIONPERSON") }

    val upgradableApplicationEntity = applicationEntityFactory.produceAndPersist {
      withApplicationSchema(olderJsonSchema)
      withCreatedByProbationOfficer(probationOfficerEntity)
      withData(
        """
          {
             "thingId": 123
          }
          """
      )
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    val rawResponseBody = webTestClient.get()
      .uri("/applications/${upgradableApplicationEntity.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .returnResult<String>()
      .responseBody
      .blockFirst()

    val responseBody = objectMapper.readValue(rawResponseBody, Application::class.java)

    assertThat(responseBody).matches {
      upgradableApplicationEntity.id == it.id &&
        upgradableApplicationEntity.crn == it.crn &&
        upgradableApplicationEntity.createdAt.toInstant() == it.createdAt.toInstant() &&
        upgradableApplicationEntity.createdByProbationOfficer.id == it.createdByProbationOfficerId &&
        upgradableApplicationEntity.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
        serializableToJsonNode(upgradableApplicationEntity.data) == serializableToJsonNode(it.data) &&
        newestJsonSchema.id == it.schemaVersion &&
        it.outdatedSchema == false
    }
  }

  @Test
  fun `Get single application returns 200 with correct body, non-upgradable outdated application marked as such`() {
    applicationSchemaRepository.deleteAll()

    applicationSchemaEntityFactory.produceAndPersist {
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
        """
      )
    }

    val olderJsonSchema = applicationSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T09:45:00+01:00"))
      withSchema(
        """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": { }
        }
        """
      )
    }

    val probationOfficerEntity = probationOfficerEntityFactory.produceAndPersist { withDistinguishedName("PROBATIONPERSON") }

    val nonUpgradableApplicationEntity = applicationEntityFactory.produceAndPersist {
      withApplicationSchema(olderJsonSchema)
      withCreatedByProbationOfficer(probationOfficerEntity)
      withData("{}")
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    val rawResponseBody = webTestClient.get()
      .uri("/applications/${nonUpgradableApplicationEntity.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .returnResult<String>()
      .responseBody
      .blockFirst()

    val responseBody = objectMapper.readValue(rawResponseBody, Application::class.java)

    assertThat(responseBody).matches {
      nonUpgradableApplicationEntity.id == it.id &&
        nonUpgradableApplicationEntity.crn == it.crn &&
        nonUpgradableApplicationEntity.createdAt.toInstant() == it.createdAt.toInstant() &&
        nonUpgradableApplicationEntity.createdByProbationOfficer.id == it.createdByProbationOfficerId &&
        nonUpgradableApplicationEntity.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
        serializableToJsonNode(nonUpgradableApplicationEntity.data) == serializableToJsonNode(it.data) &&
        olderJsonSchema.id == it.schemaVersion &&
        it.outdatedSchema == true
    }
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
  }
}
