package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Component
class Cas3ArrivalTransformer {
  fun transformJpaToArrival(jpa: ArrivalEntity?) = jpa?.let {
    Arrival(
      bookingId = jpa.booking.id,
      arrivalDate = jpa.arrivalDate,
      arrivalTime = DateTimeFormatter.ISO_LOCAL_TIME.format(jpa.arrivalDateTime.atZone(ZoneOffset.UTC)),
      expectedDepartureDate = jpa.expectedDepartureDate,
      notes = jpa.notes,
      createdAt = jpa.createdAt.toInstant(),
    )
  }

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
