package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.MoveOnCategoryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3DepartureTransformer

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
