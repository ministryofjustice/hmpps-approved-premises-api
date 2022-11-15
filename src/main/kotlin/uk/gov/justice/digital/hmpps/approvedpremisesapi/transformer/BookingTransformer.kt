package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import java.lang.RuntimeException

@Component
class BookingTransformer(
  private val personTransformer: PersonTransformer,
  private val staffMemberTransformer: StaffMemberTransformer,
  private val arrivalTransformer: ArrivalTransformer,
  private val departureTransformer: DepartureTransformer,
  private val nonArrivalTransformer: NonArrivalTransformer,
  private val cancellationTransformer: CancellationTransformer,
  private val extensionTransformer: ExtensionTransformer
) {
  fun transformJpaToApi(jpa: BookingEntity, offender: OffenderDetailSummary, inmateDetail: InmateDetail, staffMember: StaffMember?) = Booking(
    id = jpa.id,
    person = personTransformer.transformModelToApi(offender, inmateDetail),
    arrivalDate = jpa.arrivalDate,
    departureDate = jpa.departureDate,
    serviceName = ServiceName.approvedPremises,
    keyWorker = staffMember?.let(staffMemberTransformer::transformDomainToApi),
    status = determineStatus(jpa),
    arrival = arrivalTransformer.transformJpaToApi(jpa.arrival),
    departure = departureTransformer.transformJpaToApi(jpa.departure),
    nonArrival = nonArrivalTransformer.transformJpaToApi(jpa.nonArrival),
    cancellation = cancellationTransformer.transformJpaToApi(jpa.cancellation),
    extensions = jpa.extensions.map(extensionTransformer::transformJpaToApi)
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
