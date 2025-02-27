package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import java.util.UUID

class Cas2JsonSchemaServiceTest {
  private val mockJsonSchemaRepository = mockk<JsonSchemaRepository>()
  private val mockApplicationRepository = mockk<ApplicationRepository>()

  private val jsonSchemaService = JsonSchemaService(
    objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper(),
    jsonSchemaRepository = mockJsonSchemaRepository,
    applicationRepository = mockApplicationRepository,
  )

  @Test
  fun `checkSchemaOutdated marks outdated correctly for Approved Premises applications`() {
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
        """,
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
        """,
      )
      .produce()

    val userId = UUID.fromString("8a0624b8-8e92-47ce-b645-b65ea5a197d0")
    val distinguishedName = "SOMEPERSON"
    val userEntity = UserEntityFactory()
      .withId(userId)
      .withDeliusUsername(distinguishedName)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val upToDateApplication = ApprovedPremisesApplicationEntityFactory()
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

    val outdatedApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(userEntity)
      .withApplicationSchema(olderJsonSchema)
      .withData("{}")
      .produce()

    val applicationEntities = listOf(upToDateApplication, outdatedApplication)

    every { mockJsonSchemaRepository.getSchemasForType(ApprovedPremisesApplicationJsonSchemaEntity::class.java) } returns listOf(newestJsonSchema)
    every { mockApplicationRepository.findAllByCreatedByUserId(userId, ApprovedPremisesApplicationEntity::class.java) } returns applicationEntities
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
  fun `checkSchemaOutdated marks outdated correctly for Temporary Accommodation applications`() {
    val newestJsonSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory()
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

    val olderJsonSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory()
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
    val userEntity = UserEntityFactory()
      .withId(userId)
      .withDeliusUsername(distinguishedName)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val upToDateApplication = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(userEntity)
      .withApplicationSchema(newestJsonSchema)
      .withData(
        """
          {
             "thingId": 123
          }
          """,
      )
      .withProbationRegion(userEntity.probationRegion)
      .produce()

    val outdatedApplication = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(userEntity)
      .withApplicationSchema(olderJsonSchema)
      .withData("{}")
      .withProbationRegion(userEntity.probationRegion)
      .produce()

    val applicationEntities = listOf(upToDateApplication, outdatedApplication)

    every { mockJsonSchemaRepository.getSchemasForType(TemporaryAccommodationApplicationJsonSchemaEntity::class.java) } returns listOf(newestJsonSchema)
    every { mockApplicationRepository.findAllByCreatedByUserId(userId, TemporaryAccommodationApplicationEntity::class.java) } returns applicationEntities
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
        """,
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
