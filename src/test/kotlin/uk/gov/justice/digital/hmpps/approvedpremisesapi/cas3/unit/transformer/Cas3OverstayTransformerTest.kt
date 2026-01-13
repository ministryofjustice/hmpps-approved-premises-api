package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3OverstayEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Overstay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3OverstayTransformer

class Cas3OverstayTransformerTest {

  private val transformer = Cas3OverstayTransformer()

  @Test
  fun `transformJpaToApi transforms an Overstay correctly`() {
    val overstayEntity = Cas3OverstayEntityFactory().withDefaults().produce()
    val result = transformer.transformJpaToApi(overstayEntity)
    assertThat(result).isEqualTo(
      Cas3Overstay(
        id = overstayEntity.id,
        bookingId = overstayEntity.booking.id,
        previousDepartureDate = overstayEntity.previousDepartureDate,
        newDepartureDate = overstayEntity.newDepartureDate,
        reason = overstayEntity.reason,
        isAuthorised = overstayEntity.isAuthorised,
        createdAt = overstayEntity.createdAt.toInstant(),
      ),
    )
  }
}
