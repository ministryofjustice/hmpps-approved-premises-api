package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.MoveOnCategoryTransformer

class Cas3DepartureTransformerTest {

  private val departureReasonTransformer = DepartureReasonTransformer()
  private val moveOnCategoryTransformer = MoveOnCategoryTransformer()
  private val transformer = Cas3DepartureTransformer(departureReasonTransformer, moveOnCategoryTransformer)

  @Test
  fun `transformJpaToApi transforms a Departure correctly`() {
    val arrivalEntity = Cas3DepartureEntityFactory().withDefaults().produce()
    val result = transformer.transformJpaToApi(arrivalEntity)
    assertThat(result).isEqualTo(
      Cas3Departure(
        id = arrivalEntity.id,
        bookingId = arrivalEntity.booking.id,
        dateTime = arrivalEntity.dateTime.toInstant(),
        reason = departureReasonTransformer.transformJpaToApi(arrivalEntity.reason),
        moveOnCategory = moveOnCategoryTransformer.transformJpaToApi(arrivalEntity.moveOnCategory),
        notes = arrivalEntity.notes,
        createdAt = arrivalEntity.createdAt.toInstant(),
      ),
    )
  }
}
