package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import java.util.UUID

class JsonSchemaServiceTest {
  private val mockJsonSchemaRepository = mockk<JsonSchemaRepository>()
  private val mockApplicationRepository = mockk<ApplicationRepository>()

  private val jsonSchemaService = JsonSchemaService(
    objectMapper = jacksonObjectMapper(),
    jsonSchemaRepository = mockJsonSchemaRepository,
    applicationRepository = mockApplicationRepository
  )

  @Test
  fun `checkSchemaOutdated marks outdated correctly`() {
    val newestJsonSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory()
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

    val olderJsonSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory()
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

    val userId = UUID.fromString("8a0624b8-8e92-47ce-b645-b65ea5a197d0")
    val distinguishedName = "SOMEPERSON"
    val userEntity = UserEntityFactory()
      .withId(userId)
      .withDeliusUsername(distinguishedName)
      .produce()

    val upToDateApplication = ApplicationEntityFactory()
      .withCreatedByUser(userEntity)
      .withApplicationSchema(newestJsonSchema)
      .withData(
        """
          {
             "thingId": 123
          }
          """
      )
      .produce()

    val outdatedApplication = ApplicationEntityFactory()
      .withCreatedByUser(userEntity)
      .withApplicationSchema(olderJsonSchema)
      .withData("{}")
      .produce()

    val applicationEntities = listOf(upToDateApplication, outdatedApplication)

    every { mockJsonSchemaRepository.getSchemasForType(ApprovedPremisesApplicationJsonSchemaEntity::class.java) } returns listOf(newestJsonSchema)
    every { mockApplicationRepository.findAllByCreatedByUser_Id(userId) } returns applicationEntities
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    assertThat(jsonSchemaService.checkSchemaOutdated(upToDateApplication)).matches {
      it.id == upToDateApplication.id &&
        it.schemaVersion == newestJsonSchema &&
        it.schemaUpToDate
    }

    assertThat(jsonSchemaService.checkSchemaOutdated(outdatedApplication)).matches {
      it.id == outdatedApplication.id &&
        it.schemaVersion == olderJsonSchema &&
        !it.schemaUpToDate
    }
  }

  @Test
  fun `validate returns false for JSON that does not satisfy schema`() {
    val schema = ApprovedPremisesApplicationJsonSchemaEntityFactory()
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
    val schema = ApprovedPremisesApplicationJsonSchemaEntityFactory()
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
