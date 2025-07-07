package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService

class JsonSchemaServiceTest {
  private val mockJsonSchemaRepository = mockk<JsonSchemaRepository>()

  private val jsonSchemaService = JsonSchemaService(
    jsonSchemaRepository = mockJsonSchemaRepository,
  )

  @Test
  fun `validate always returns true`() {
    val schema = ApprovedPremisesApplicationJsonSchemaEntityFactory()
      .withSchema("doesntmatter")
      .produce()

    assertThat(
      jsonSchemaService.validate(
        schema,
        """irrelevant""",
      ),
    ).isTrue
  }
}
