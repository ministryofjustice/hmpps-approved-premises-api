package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Nonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity

@Component
class NonArrivalTransformer(private val nonArrivalReasonTransformer: NonArrivalReasonTransformer) {
  fun transformJpaToApi(jpa: NonArrivalEntity?) = jpa?.let {
    Nonarrival(
      id = jpa.id,
      bookingId = jpa.booking.id,
      date = jpa.date,
      reason = nonArrivalReasonTransformer.transformJpaToApi(jpa.reason),
      notes = jpa.notes
    )
  }
}
