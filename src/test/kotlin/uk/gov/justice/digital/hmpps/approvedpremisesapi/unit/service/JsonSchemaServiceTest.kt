package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOfficerEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSchemaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import java.util.UUID

class JsonSchemaServiceTest {
  private val mockApplicationSchemaRepository = mockk<ApplicationSchemaRepository>()
  private val mockApplicationRepository = mockk<ApplicationRepository>()

  private val jsonSchemaService = JsonSchemaService(
    objectMapper = jacksonObjectMapper(),
    applicationSchemaRepository = mockApplicationSchemaRepository,
    applicationRepository = mockApplicationRepository
  )

  @Test
  fun `attemptSchemaUpgrade upgrades schema version where possible, marks outdated where not`() {
    val newestJsonSchema = ApplicationSchemaEntityFactory()
      .withSchema(
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
      .produce()

    val olderJsonSchema = ApplicationSchemaEntityFactory()
      .withSchema(
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
      .produce()

    val probationOfficerId = UUID.fromString("8a0624b8-8e92-47ce-b645-b65ea5a197d0")
    val distinguishedName = "SOMEPERSON"
    val probationOfficerEntity = ProbationOfficerEntityFactory()
      .withId(probationOfficerId)
      .withDistinguishedName(distinguishedName)
      .produce()

    val upgradableApplication = ApplicationEntityFactory()
      .withCreatedByProbationOfficer(probationOfficerEntity)
      .withApplicationSchema(olderJsonSchema)
      .withData(
        """
          {
             "thingId": 123
          }
          """
      )
      .produce()

    val nonUpgradableApplication = ApplicationEntityFactory()
      .withCreatedByProbationOfficer(probationOfficerEntity)
      .withApplicationSchema(olderJsonSchema)
      .withData("{}")
      .produce()

    val applicationEntities = listOf(upgradableApplication, nonUpgradableApplication)

    every { mockApplicationSchemaRepository.findFirstByOrderByAddedAtDesc() } returns newestJsonSchema
    every { mockApplicationRepository.findAllByCreatedByProbationOfficer_Id(probationOfficerId) } returns applicationEntities
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    assertThat(jsonSchemaService.attemptSchemaUpgrade(upgradableApplication)).matches {
      it.id == upgradableApplication.id &&
        it.schemaVersion == newestJsonSchema &&
        it.schemaUpToDate
    }

    assertThat(jsonSchemaService.attemptSchemaUpgrade(nonUpgradableApplication)).matches {
      it.id == nonUpgradableApplication.id &&
        it.schemaVersion == olderJsonSchema &&
        !it.schemaUpToDate
    }
  }

  @Test
  fun `validate returns false for JSON that does not satisfy schema`() {
    val schema = ApplicationSchemaEntityFactory()
      .withSchema(
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
      .produce()

    assertThat(jsonSchemaService.validate(schema, "{}")).isFalse
  }

  @Test
  fun `validate returns true for JSON that does satisfy schema`() {
    val schema = ApplicationSchemaEntityFactory()
      .withSchema(
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
      .produce()

    assertThat(
      jsonSchemaService.validate(
        schema,
        """
        {
           "thingId": 123
        }
      """
      )
    ).isTrue
  }
}
