package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import java.util.UUID

class JsonSchemaServiceTest {
  private val mockJsonSchemaRepository = mockk<JsonSchemaRepository>()
  private val mockApplicationRepository = mockk<Cas2ApplicationRepository>()

  private val jsonSchemaService = JsonSchemaService(
    objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper(),
    jsonSchemaRepository = mockJsonSchemaRepository,
    applicationRepository = mockApplicationRepository,
  )

  @Test
  fun `checkSchemaOutdated marks outdated correctly for CAS2 applications`() {
    val newestJsonSchema = Cas2ApplicationJsonSchemaEntityFactory()
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
        """,
      )
      .produce()

    val olderJsonSchema = Cas2ApplicationJsonSchemaEntityFactory()
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
        """,
      )
      .produce()

    val userId = UUID.fromString("8a0624b8-8e92-47ce-b645-b65ea5a197d0")
    val distinguishedName = "SOMEPERSON"
    val userEntity = NomisUserEntityFactory()
      .withId(userId)
      .withNomisUsername(distinguishedName)
      .produce()

    val upToDateApplication = Cas2ApplicationEntityFactory()
      .withCreatedByUser(userEntity)
      .withApplicationSchema(newestJsonSchema)
      .withData(
        """
          {
             "thingId": 123
          }
          """,
      )
      .produce()

    val outdatedApplication = Cas2ApplicationEntityFactory()
      .withCreatedByUser(userEntity)
      .withApplicationSchema(olderJsonSchema)
      .withData("{}")
      .produce()

    val applicationEntities = listOf(upToDateApplication, outdatedApplication)

    every { mockJsonSchemaRepository.getSchemasForType(Cas2ApplicationJsonSchemaEntity::class.java) } returns listOf(newestJsonSchema)
    every { mockApplicationRepository.findAllByCreatedByUserId(userId) } returns
      applicationEntities
    every { mockApplicationRepository.save(any()) } answers {
      it.invocation.args[0] as
        Cas2ApplicationEntity
    }

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
    val schema = Cas2ApplicationJsonSchemaEntityFactory()
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
        """,
      )
      .produce()

    assertThat(jsonSchemaService.validate(schema, "{}")).isFalse
  }

  @Test
  fun `validate returns true for JSON that does satisfy schema`() {
    val schema = Cas2ApplicationJsonSchemaEntityFactory()
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
        """,
      )
      .produce()

    assertThat(
      jsonSchemaService.validate(
        schema,
        """
        {
           "thingId": 123
        }
      """,
      ),
    ).isTrue
  }
}
