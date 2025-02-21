package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Component
class ArrivalTransformer {
  fun transformJpaToApi(jpa: ArrivalEntity?) = jpa?.let {
    Arrival(
      bookingId = jpa.booking.id,
      arrivalDate = jpa.arrivalDate,
      arrivalTime = DateTimeFormatter.ISO_LOCAL_TIME.format(jpa.arrivalDateTime.atZone(ZoneOffset.UTC)),
      expectedDepartureDate = jpa.expectedDepartureDate,
      notes = jpa.notes,
      createdAt = jpa.createdAt.toInstant(),
    )
  }
}
