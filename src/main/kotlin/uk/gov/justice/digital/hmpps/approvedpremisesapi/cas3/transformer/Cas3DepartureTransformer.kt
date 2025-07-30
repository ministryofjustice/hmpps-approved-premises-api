package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.MoveOnCategoryTransformer

@Component
class Cas3DepartureTransformer(
  private val departureReasonTransformer: DepartureReasonTransformer,
  private val moveOnCategoryTransformer: MoveOnCategoryTransformer,
) {
  fun transformJpaToApi(jpa: DepartureEntity?) = jpa?.let {
    Cas3Departure(
      id = jpa.id,
      bookingId = jpa.booking.id,
      dateTime = jpa.dateTime.toInstant(),
      reason = departureReasonTransformer.transformJpaToApi(jpa.reason),
      moveOnCategory = moveOnCategoryTransformer.transformJpaToApi(jpa.moveOnCategory),
      notes = jpa.notes,
      createdAt = jpa.createdAt.toInstant(),
    )
  }

  fun transformJpaToApi(jpa: Cas3DepartureEntity?) = jpa?.let {
    Cas3Departure(
      id = jpa.id,
      bookingId = jpa.booking.id,
      dateTime = jpa.dateTime.toInstant(),
      reason = departureReasonTransformer.transformJpaToApi(jpa.reason),
      moveOnCategory = moveOnCategoryTransformer.transformJpaToApi(jpa.moveOnCategory),
      notes = jpa.notes,
      createdAt = jpa.createdAt.toInstant(),
    )
  }
}
