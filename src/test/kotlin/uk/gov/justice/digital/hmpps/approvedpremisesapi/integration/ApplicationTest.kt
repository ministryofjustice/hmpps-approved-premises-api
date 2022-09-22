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
  fun `Get all applications returns 200 with correct body`() {
    applicationSchemaRepository.deleteAll()

    val schemaEntity = applicationSchemaEntityFactory.produceAndPersist()
    val probationOfficerEntity = probationOfficerEntityFactory.produceAndPersist { withDistinguishedName("PROBATIONPERSON") }

    val applicationEntities = applicationEntityFactory.produceAndPersistMultiple(5) {
      withApplicationSchema(schemaEntity)
      withCreatedByProbationOfficer(probationOfficerEntity)
    }

    applicationEntities.forEach {
      // Temporary until schema validation is in place
      it.schemaUpToDate = true
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

    applicationEntities.map(applicationsTransformer::transformJpaToApi).forEach { expectedApplication ->
      assertThat(responseBody).anyMatch {
        expectedApplication.id == it.id &&
          expectedApplication.crn == it.crn &&
          expectedApplication.createdAt.toInstant() == it.createdAt.toInstant() &&
          expectedApplication.createdByProbationOfficerId == it.createdByProbationOfficerId &&
          expectedApplication.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
          serializableToJsonNode(expectedApplication.data) == serializableToJsonNode(it.data) &&
          expectedApplication.schemaVersion == it.schemaVersion &&
          expectedApplication.outdatedSchema == it.outdatedSchema
      }
    }
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode = if (serializable == null) NullNode.instance else objectMapper.readTree(objectMapper.writeValueAsString(serializable))
}
