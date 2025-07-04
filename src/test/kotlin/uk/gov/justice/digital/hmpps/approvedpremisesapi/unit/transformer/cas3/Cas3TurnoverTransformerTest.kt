package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.v2.Cas3TurnaroundEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3TurnaroundTransformer

class Cas3TurnoverTransformerTest {

  private val transformer = Cas3TurnaroundTransformer()

  @Test
  fun `transformJpaToApi transforms a Turnover correctly`() {
    val turnoverEntity = Cas3TurnaroundEntityFactory().withDefaults().produce()
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
