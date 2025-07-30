package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3ExtensionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Extension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3ExtensionTransformer

class Cas3ExtensionTransformerTest {

  private val transformer = Cas3ExtensionTransformer()

  @Test
  fun `transformJpaToApi transforms an Extension correctly`() {
    val extensionEntity = Cas3ExtensionEntityFactory().withDefaults().produce()
    val result = transformer.transformJpaToApi(extensionEntity)
    assertThat(result).isEqualTo(
      Cas3Extension(
        id = extensionEntity.id,
        bookingId = extensionEntity.booking.id,
        previousDepartureDate = extensionEntity.previousDepartureDate,
        newDepartureDate = extensionEntity.newDepartureDate,
        notes = extensionEntity.notes,
        createdAt = extensionEntity.createdAt.toInstant(),
      ),
    )
  }
}
