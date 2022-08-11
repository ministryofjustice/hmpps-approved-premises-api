package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import java.lang.RuntimeException

@Component
class BookingTransformer(
  private val personTransformer: PersonTransformer,
  private val arrivalTransformer: ArrivalTransformer,
  private val departureTransformer: DepartureTransformer,
  private val nonArrivalTransformer: NonArrivalTransformer,
  private val cancellationTransformer: CancellationTransformer
) {
  fun transformJpaToApi(jpa: BookingEntity) = Booking(
    id = jpa.id,
    person = personTransformer.transformJpaToApi(jpa.person!!),
    arrivalDate = jpa.arrivalDate,
    departureDate = jpa.departureDate,
    keyWorker = jpa.keyWorker,
    status = determineStatus(jpa),
    arrival = arrivalTransformer.transformJpaToApi(jpa.arrival),
    departure = departureTransformer.transformJpaToApi(jpa.departure),
    nonArrival = nonArrivalTransformer.transformJpaToApi(jpa.nonArrival),
    cancellation = cancellationTransformer.transformJpaToApi(jpa.cancellation)
  )

  private fun determineStatus(jpa: BookingEntity) = when {
    jpa.nonArrival != null -> Booking.Status.notMinusArrived
    jpa.arrival != null && jpa.departure == null -> Booking.Status.arrived
    jpa.departure != null -> Booking.Status.departed
    jpa.cancellation != null -> Booking.Status.cancelled
    jpa.arrival == null && jpa.nonArrival == null -> Booking.Status.awaitingMinusArrival
    else -> throw RuntimeException("Could not determine status for Booking ${jpa.id}")
  }
}
