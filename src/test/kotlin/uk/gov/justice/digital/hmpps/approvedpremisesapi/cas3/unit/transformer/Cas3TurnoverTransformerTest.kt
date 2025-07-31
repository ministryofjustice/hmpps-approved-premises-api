package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.v2.Cas3v2TurnaroundEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3TurnaroundTransformer

class Cas3TurnoverTransformerTest {

  private val transformer = Cas3TurnaroundTransformer()

  @Test
  fun `transformJpaToApi transforms a Turnover correctly`() {
    val turnoverEntity = Cas3v2TurnaroundEntityFactory().withDefaults().produce()
    val result = transformer.transformJpaToApi(turnoverEntity)
    assertThat(result).isEqualTo(
      Cas3Turnaround(
        id = turnoverEntity.id,
        bookingId = turnoverEntity.booking.id,
        workingDays = turnoverEntity.workingDayCount,
        createdAt = turnoverEntity.createdAt.toInstant(),
      ),
    )
  }
}
