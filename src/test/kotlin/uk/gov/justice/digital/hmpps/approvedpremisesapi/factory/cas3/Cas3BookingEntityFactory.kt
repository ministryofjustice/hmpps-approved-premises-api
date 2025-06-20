package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3BookingEntityFactory : Factory<Cas3BookingEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringUpperCase(6) }
  private var arrivalDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(14) }
  private var departureDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter(14) }
  private var originalArrivalDate: Yielded<LocalDate>? = null
  private var originalDepartureDate: Yielded<LocalDate>? = null
  private var keyWorkerStaffCode: Yielded<String?> = { null }
  private var arrivals: Yielded<MutableList<Cas3ArrivalEntity>>? = null
  private var departures: Yielded<MutableList<Cas3DepartureEntity>>? = null
  private var nonArrival: Yielded<Cas3NonArrivalEntity>? = null
  private var cancellations: Yielded<MutableList<Cas3CancellationEntity>>? = null
  private var confirmation: Yielded<Cas3v2ConfirmationEntity>? = null
  private var extensions: Yielded<MutableList<Cas3ExtensionEntity>>? = null
  private var dateChanges: Yielded<MutableList<DateChangeEntity>>? = null
  private var premises: Yielded<Cas3PremisesEntity>? = null
  private var serviceName: Yielded<ServiceName> = { randomOf(listOf(ServiceName.approvedPremises, ServiceName.temporaryAccommodation)) }
  private var bedspace: Yielded<Cas3BedspacesEntity>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusDays(14L).randomDateTimeBefore(14) }
  private var application: Yielded<TemporaryAccommodationApplicationEntity?> = { null }
  private var turnarounds: Yielded<MutableList<Cas3v2TurnaroundEntity>>? = null
  private var nomsNumber: Yielded<String?> = { randomStringUpperCase(6) }
  private var placementRequest: Yielded<PlacementRequestEntity?> = { null }
  private var status: Yielded<BookingStatus?> = { null }
  private var offenderName: Yielded<String?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withArrivalDate(arrivalDate: LocalDate) = apply {
    this.arrivalDate = { arrivalDate }
  }

  fun withOriginalArrivalDate(arrivalDate: LocalDate) = apply {
    this.originalArrivalDate = { arrivalDate }
  }

  fun withDepartureDate(departureDate: LocalDate) = apply {
    this.departureDate = { departureDate }
  }

  fun withOriginalDepartureDate(departureDate: LocalDate) = apply {
    this.originalDepartureDate = { departureDate }
  }

  fun withStaffKeyWorkerCode(staffKeyWorkerCode: String?) = apply {
    this.keyWorkerStaffCode = { staffKeyWorkerCode }
  }

  fun withYieldedArrivals(arrivals: Yielded<MutableList<Cas3ArrivalEntity>>) = apply {
    this.arrivals = arrivals
  }

  fun withArrivals(arrivals: MutableList<Cas3ArrivalEntity>) = apply {
    this.arrivals = { arrivals }
  }

  fun withYieldedDepartures(departures: Yielded<MutableList<Cas3DepartureEntity>>) = apply {
    this.departures = departures
  }

  fun withDepartures(departures: MutableList<Cas3DepartureEntity>) = apply {
    this.departures = { departures }
  }

  fun withYieldedNonArrival(nonArrival: Yielded<Cas3NonArrivalEntity>) = apply {
    this.nonArrival = nonArrival
  }

  fun withNonArrival(nonArrival: Cas3NonArrivalEntity) = apply {
    this.nonArrival = { nonArrival }
  }

  fun withYieldedCancellations(cancellations: Yielded<MutableList<Cas3CancellationEntity>>) = apply {
    this.cancellations = cancellations
  }

  fun withCancellations(cancellations: MutableList<Cas3CancellationEntity>) = apply {
    this.cancellations = { cancellations }
  }

  fun withYieldedConfirmation(confirmation: Yielded<Cas3v2ConfirmationEntity>) = apply {
    this.confirmation = confirmation
  }

  fun withConfirmation(confirmation: Cas3v2ConfirmationEntity) = apply {
    this.confirmation = { confirmation }
  }

  fun withYieldedExtensions(extensions: Yielded<MutableList<Cas3ExtensionEntity>>) = apply {
    this.extensions = extensions
  }

  fun withExtensions(extensions: MutableList<Cas3ExtensionEntity>) = apply {
    this.extensions = { extensions }
  }

  fun withDateChanges(dateChanges: MutableList<DateChangeEntity>) = apply {
    this.dateChanges = { dateChanges }
  }

  fun withYieldedPremises(premises: Yielded<Cas3PremisesEntity>) = apply {
    this.premises = premises
  }

  fun withPremises(premises: Cas3PremisesEntity) = apply {
    this.premises = { premises }
  }

  fun withServiceName(serviceName: ServiceName) = apply {
    this.serviceName = { serviceName }
  }

  fun withBedspace(bedspace: Cas3BedspacesEntity) = apply {
    this.bedspace = { bedspace }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withApplication(application: TemporaryAccommodationApplicationEntity?) = apply {
    this.application = { application }
  }

  fun withNomsNumber(nomsNumber: String?) = apply {
    this.nomsNumber = { nomsNumber }
  }

  fun withPlacementRequest(placementRequest: PlacementRequestEntity) = apply {
    this.placementRequest = { placementRequest }
  }

  fun withStatus(status: BookingStatus) = apply {
    this.status = { status }
  }

  fun withOffenderName(offenderName: String?) = apply {
    this.offenderName = { offenderName }
  }

  fun withDefaults() = apply {
    val premises = Cas3PremisesEntityFactory()
      .withDefaults()
      .produce()
    withPremises(premises)
    withDefaultBedspace(premises)
  }

  private fun withDefaultBedspace(premises: Cas3PremisesEntity) = withBedspace(
    Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .produce(),
  )

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun produce(): Cas3BookingEntity = Cas3BookingEntity(
    id = this.id(),
    crn = this.crn(),
    arrivalDate = this.arrivalDate(),
    departureDate = this.departureDate(),
    arrivals = this.arrivals?.invoke() ?: mutableListOf(),
    departures = this.departures?.invoke() ?: mutableListOf(),
    nonArrival = this.nonArrival?.invoke(),
    cancellations = this.cancellations?.invoke() ?: mutableListOf(),
    confirmation = this.confirmation?.invoke(),
    extensions = this.extensions?.invoke() ?: mutableListOf(),
    dateChanges = this.dateChanges?.invoke() ?: mutableListOf(),
    premises = this.premises?.invoke() ?: throw RuntimeException("Must provide a Premises"),
    bedspace = this.bedspace?.invoke() ?: throw RuntimeException("Must provide a Bedspace"),
    service = this.serviceName.invoke().value,
    originalArrivalDate = this.originalArrivalDate?.invoke() ?: this.arrivalDate(),
    originalDepartureDate = this.originalDepartureDate?.invoke() ?: this.departureDate(),
    createdAt = this.createdAt(),
    application = this.application(),
    turnarounds = this.turnarounds?.invoke() ?: mutableListOf(),
    nomsNumber = this.nomsNumber(),
    status = this.status.invoke(),
    offenderName = this.offenderName(),
  )
}
