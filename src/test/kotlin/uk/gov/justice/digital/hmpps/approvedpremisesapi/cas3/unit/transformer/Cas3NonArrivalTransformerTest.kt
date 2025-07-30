package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3NonArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3NonArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalReasonTransformer

class Cas3NonArrivalTransformerTest {

  private val nonArrivalReasonTransformer = NonArrivalReasonTransformer()
  private val transformer = Cas3NonArrivalTransformer(nonArrivalReasonTransformer)

  @Test
  fun `transformJpaToApi transforms a Non-arrival correctly`() {
    val nonArrivalEntity = Cas3NonArrivalEntityFactory().withDefaults().produce()
    val result = transformer.transformJpaToApi(nonArrivalEntity)
    assertThat(result).isEqualTo(
      Cas3NonArrival(
        id = nonArrivalEntity.id,
        bookingId = nonArrivalEntity.booking.id,
        date = nonArrivalEntity.date,
        reason = nonArrivalReasonTransformer.transformJpaToApi(nonArrivalEntity.reason),
        notes = nonArrivalEntity.notes,
        createdAt = nonArrivalEntity.createdAt.toInstant(),
      ),
    )
  }
}
