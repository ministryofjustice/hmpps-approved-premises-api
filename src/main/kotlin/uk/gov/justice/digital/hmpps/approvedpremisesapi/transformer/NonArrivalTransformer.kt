package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Nonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity

@Component
class NonArrivalTransformer() {
  fun transformJpaToApi(jpa: NonArrivalEntity?) = jpa?.let {
    Nonarrival(
      bookingId = jpa.booking.id,
      date = jpa.date,
      reason = jpa.reason,
      notes = jpa.notes
    )
  }
}
