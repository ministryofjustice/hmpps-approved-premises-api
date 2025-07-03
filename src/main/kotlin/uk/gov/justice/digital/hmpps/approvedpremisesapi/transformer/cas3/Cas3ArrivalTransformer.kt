package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3ArrivalEntity
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Component
class Cas3ArrivalTransformer {
  fun transformJpaToApi(jpa: Cas3ArrivalEntity?) = jpa?.let {
    Cas3Arrival(
      bookingId = jpa.booking.id,
      arrivalDate = jpa.arrivalDate,
      arrivalTime = DateTimeFormatter.ISO_LOCAL_TIME.format(jpa.arrivalDateTime.atZone(ZoneOffset.UTC)),
      expectedDepartureDate = jpa.expectedDepartureDate,
      notes = jpa.notes,
      createdAt = jpa.createdAt.toInstant(),
    )
  }
}
