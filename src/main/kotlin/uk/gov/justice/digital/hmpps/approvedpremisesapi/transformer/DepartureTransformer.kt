package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity

@Component
class DepartureTransformer(
  private val destinationProviderTransformer: DestinationProviderTransformer,
  private val departureReasonTransformer: DepartureReasonTransformer,
  private val moveOnCategoryTransformer: MoveOnCategoryTransformer
) {
  fun transformJpaToApi(jpa: DepartureEntity?) = jpa?.let {
    Departure(
      id = jpa.id,
      bookingId = jpa.booking.id,
      dateTime = jpa.dateTime,
      reason = departureReasonTransformer.transformJpaToApi(jpa.reason),
      moveOnCategory = moveOnCategoryTransformer.transformJpaToApi(jpa.moveOnCategory),
      destinationProvider = destinationProviderTransformer.transformJpaToApi(jpa.destinationProvider),
      notes = jpa.notes
    )
  }
}
