package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2.Cas2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2JsonSchemaService
import java.util.UUID

@SuppressWarnings("UnusedPrivateProperty")
class Cas2JsonSchemaServiceTest {
  private val mockJsonSchemaRepository = mockk<JsonSchemaRepository>()

  private val jsonSchemaService = Cas2JsonSchemaService(
    jsonSchemaRepository = mockJsonSchemaRepository,
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

    every { mockJsonSchemaRepository.getSchemasForType(Cas2ApplicationJsonSchemaEntity::class.java) } returns listOf(newestJsonSchema)

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
  fun `validate always returns true`() {
    val schema = Cas2ApplicationJsonSchemaEntityFactory()
      .withSchema("doesntmatter")
      .produce()

    assertThat(
      jsonSchemaService.validate(
        schema,
        "irrelevant",
      ),
    ).isTrue
  }
}
