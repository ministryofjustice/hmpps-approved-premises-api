package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3ArrivalTransformer
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class Cas3ArrivalTransformerTest {

  private val transformer = Cas3ArrivalTransformer()

  @Test
  fun `transformJpaToApi transforms a Arrival correctly`() {
    val arrivalEntity = Cas3ArrivalEntityFactory().withDefaults().produce()
    val result = transformer.transformJpaToApi(arrivalEntity)
    assertThat(result).isEqualTo(
      Cas3Arrival(
        bookingId = arrivalEntity.booking.id,
        arrivalDate = arrivalEntity.arrivalDate,
        arrivalTime = DateTimeFormatter.ISO_LOCAL_TIME.format(arrivalEntity.arrivalDateTime.atZone(ZoneOffset.UTC)),
        expectedDepartureDate = arrivalEntity.expectedDepartureDate,
        notes = arrivalEntity.notes,
        createdAt = arrivalEntity.createdAt.toInstant(),
      ),
    )
  }
}
