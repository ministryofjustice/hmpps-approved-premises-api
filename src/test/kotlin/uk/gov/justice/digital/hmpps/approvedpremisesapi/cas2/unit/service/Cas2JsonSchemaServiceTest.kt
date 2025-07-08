package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaRepository

@SuppressWarnings("UnusedPrivateProperty")
class Cas2JsonSchemaServiceTest {
  private val mockJsonSchemaRepository = mockk<JsonSchemaRepository>()

  private val jsonSchemaService = Cas2JsonSchemaService(
    jsonSchemaRepository = mockJsonSchemaRepository,
  )

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
