package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.convert.EnumConverterFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem

@Component
class BookingTransformer(
  private val personTransformer: PersonTransformer,
  private val staffMemberTransformer: StaffMemberTransformer,
  private val arrivalTransformer: ArrivalTransformer,
  private val departureTransformer: DepartureTransformer,
  private val nonArrivalTransformer: NonArrivalTransformer,
  private val cancellationTransformer: CancellationTransformer,
  private val confirmationTransformer: ConfirmationTransformer,
  private val extensionTransformer: ExtensionTransformer,
  private val bedTransformer: BedTransformer,
  private val enumConverterFactory: EnumConverterFactory,
) {
  fun transformJpaToApi(jpa: BookingEntity, offender: OffenderDetailSummary, inmateDetail: InmateDetail, staffMember: StaffMember?) = Booking(
    id = jpa.id,
    person = personTransformer.transformModelToApi(offender, inmateDetail),
    arrivalDate = jpa.arrivalDate,
    departureDate = jpa.departureDate,
    serviceName = enumConverterFactory.getConverter(ServiceName::class.java).convert(jpa.service) ?: throw InternalServerErrorProblem("Could not convert '${jpa.service}' to a ServiceName"),
    keyWorker = staffMember?.let(staffMemberTransformer::transformDomainToApi),
    status = determineStatus(jpa),
    arrival = arrivalTransformer.transformJpaToApi(jpa.arrival),
    departure = departureTransformer.transformJpaToApi(jpa.departure),
    departures = jpa.departures.map { departureTransformer.transformJpaToApi(it)!! },
    nonArrival = nonArrivalTransformer.transformJpaToApi(jpa.nonArrival),
    cancellation = cancellationTransformer.transformJpaToApi(jpa.cancellation),
    cancellations = jpa.cancellations.map { cancellationTransformer.transformJpaToApi(it)!! },
    confirmation = confirmationTransformer.transformJpaToApi(jpa.confirmation),
    extensions = jpa.extensions.map(extensionTransformer::transformJpaToApi),
    bed = jpa.bed?.let { bedTransformer.transformJpaToApi(it) },
    originalArrivalDate = jpa.originalArrivalDate,
    originalDepartureDate = jpa.originalDepartureDate,
    createdAt = jpa.createdAt.toInstant(),
  )

  private fun determineStatus(jpa: BookingEntity) = when {
    jpa.service == ServiceName.approvedPremises.value -> determineApprovedPremisesStatus(jpa)
    jpa.service == ServiceName.temporaryAccommodation.value -> determineTemporaryAccommodationStatus(jpa)
    else -> throw RuntimeException("Could not determine service for Booking ${jpa.id}")
  }

  private fun determineApprovedPremisesStatus(jpa: BookingEntity) = when {
    jpa.nonArrival != null -> BookingStatus.notMinusArrived
    jpa.arrival != null && jpa.departure == null -> BookingStatus.arrived
    jpa.departure != null -> BookingStatus.departed
    jpa.cancellation != null -> BookingStatus.cancelled
    jpa.arrival == null && jpa.nonArrival == null -> BookingStatus.awaitingMinusArrival
    else -> throw RuntimeException("Could not determine status for Booking ${jpa.id}")
  }

  private fun determineTemporaryAccommodationStatus(jpa: BookingEntity) = when {
    jpa.cancellation != null -> BookingStatus.cancelled
    jpa.departure != null -> BookingStatus.departed
    jpa.arrival != null -> BookingStatus.arrived
    jpa.nonArrival != null -> BookingStatus.notMinusArrived
    jpa.confirmation != null -> BookingStatus.confirmed
    else -> BookingStatus.provisional
  }
}
