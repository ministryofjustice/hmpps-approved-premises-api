package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity

@Component
class ArrivalTransformer() {
  fun transformJpaToApi(jpa: ArrivalEntity?) = jpa?.let {
    Arrival(
      bookingId = jpa.booking.id,
      arrivalDate = jpa.arrivalDate,
      expectedDepartureDate = jpa.expectedDepartureDate,
      notes = jpa.notes
    )
  }
}
