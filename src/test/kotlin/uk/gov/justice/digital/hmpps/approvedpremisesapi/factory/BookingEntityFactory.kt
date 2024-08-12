package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class BookingEntityFactory : Factory<BookingEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringUpperCase(6) }
  private var arrivalDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore() }
  private var departureDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter() }
  private var originalArrivalDate: Yielded<LocalDate>? = null
  private var originalDepartureDate: Yielded<LocalDate>? = null
  private var keyWorkerStaffCode: Yielded<String?> = { null }
  private var arrivals: Yielded<MutableList<ArrivalEntity>>? = null
  private var departures: Yielded<MutableList<DepartureEntity>>? = null
  private var nonArrival: Yielded<NonArrivalEntity>? = null
  private var cancellations: Yielded<MutableList<CancellationEntity>>? = null
  private var confirmation: Yielded<ConfirmationEntity>? = null
  private var extensions: Yielded<MutableList<ExtensionEntity>>? = null
  private var dateChanges: Yielded<MutableList<DateChangeEntity>>? = null
  private var premises: Yielded<PremisesEntity>? = null
  private var serviceName: Yielded<ServiceName> = { randomOf(listOf(ServiceName.approvedPremises, ServiceName.temporaryAccommodation)) }
  private var bed: Yielded<BedEntity?> = { null }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusDays(14L).randomDateTimeBefore(14) }
  private var application: Yielded<ApplicationEntity?> = { null }
  private var offlineApplication: Yielded<OfflineApplicationEntity?> = { null }
  private var turnarounds: Yielded<MutableList<TurnaroundEntity>>? = null
  private var nomsNumber: Yielded<String?> = { randomStringUpperCase(6) }
  private var placementRequest: Yielded<PlacementRequestEntity?> = { null }
  private var status: Yielded<BookingStatus?> = { null }
  private var adhoc: Yielded<Boolean?> = { null }

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

  fun withYieldedArrivals(arrivals: Yielded<MutableList<ArrivalEntity>>) = apply {
    this.arrivals = arrivals
  }

  fun withArrivals(arrivals: MutableList<ArrivalEntity>) = apply {
    this.arrivals = { arrivals }
  }

  fun withYieldedDepartures(departures: Yielded<MutableList<DepartureEntity>>) = apply {
    this.departures = departures
  }

  fun withDepartures(departures: MutableList<DepartureEntity>) = apply {
    this.departures = { departures }
  }

  fun withYieldedNonArrival(nonArrival: Yielded<NonArrivalEntity>) = apply {
    this.nonArrival = nonArrival
  }

  fun withNonArrival(nonArrival: NonArrivalEntity) = apply {
    this.nonArrival = { nonArrival }
  }

  fun withYieldedCancellations(cancellations: Yielded<MutableList<CancellationEntity>>) = apply {
    this.cancellations = cancellations
  }

  fun withCancellations(cancellations: MutableList<CancellationEntity>) = apply {
    this.cancellations = { cancellations }
  }

  fun withYieldedConfirmation(confirmation: Yielded<ConfirmationEntity>) = apply {
    this.confirmation = confirmation
  }

  fun withConfirmation(confirmation: ConfirmationEntity) = apply {
    this.confirmation = { confirmation }
  }

  fun withYieldedExtensions(extensions: Yielded<MutableList<ExtensionEntity>>) = apply {
    this.extensions = extensions
  }

  fun withExtensions(extensions: MutableList<ExtensionEntity>) = apply {
    this.extensions = { extensions }
  }

  fun withDateChanges(dateChanges: MutableList<DateChangeEntity>) = apply {
    this.dateChanges = { dateChanges }
  }

  fun withYieldedPremises(premises: Yielded<PremisesEntity>) = apply {
    this.premises = premises
  }

  fun withPremises(premises: PremisesEntity) = apply {
    this.premises = { premises }
  }

  fun withDefaults() = apply {
    withDefaultPremises()
  }

  fun withDefaultPremises() = withPremises(
    ApprovedPremisesEntityFactory()
      .withDefaultProbationRegion()
      .withDefaultLocalAuthorityArea()
      .produce(),
  )

  fun withServiceName(serviceName: ServiceName) = apply {
    this.serviceName = { serviceName }
  }

  fun withBed(bed: BedEntity?) = apply {
    this.bed = { bed }
  }

  fun withYieldedBed(bed: Yielded<BedEntity>) = apply {
    this.bed = bed
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withApplication(application: ApplicationEntity?) = apply {
    this.application = { application }
  }

  fun withOfflineApplication(offlineApplication: OfflineApplicationEntity?) = apply {
    this.offlineApplication = { offlineApplication }
  }

  fun withYieldedTurnarounds(turnarounds: Yielded<MutableList<TurnaroundEntity>>) = apply {
    this.turnarounds = turnarounds
  }

  fun withTurnarounds(turnarounds: MutableList<TurnaroundEntity>) = apply {
    this.turnarounds = { turnarounds }
  }

  fun withNomsNumber(nomsNumber: String?) = apply {
    this.nomsNumber = { nomsNumber }
  }

  fun withPlacementRequest(placementRequest: PlacementRequestEntity) = apply {
    this.placementRequest = { placementRequest }
  }

  fun withStatus(departed: BookingStatus) = apply {
    this.status = { departed }
  }

  fun withAdhoc(adhoc: Boolean?) = apply {
    this.adhoc = { adhoc }
  }

  override fun produce(): BookingEntity = BookingEntity(
    id = this.id(),
    crn = this.crn(),
    arrivalDate = this.arrivalDate(),
    departureDate = this.departureDate(),
    keyWorkerStaffCode = this.keyWorkerStaffCode(),
    arrivals = this.arrivals?.invoke() ?: mutableListOf(),
    departures = this.departures?.invoke() ?: mutableListOf(),
    nonArrival = this.nonArrival?.invoke(),
    cancellations = this.cancellations?.invoke() ?: mutableListOf(),
    confirmation = this.confirmation?.invoke(),
    extensions = this.extensions?.invoke() ?: mutableListOf(),
    dateChanges = this.dateChanges?.invoke() ?: mutableListOf(),
    premises = this.premises?.invoke() ?: throw RuntimeException("Must provide a Premises"),
    bed = this.bed(),
    service = this.serviceName.invoke().value,
    originalArrivalDate = this.originalArrivalDate?.invoke() ?: this.arrivalDate(),
    originalDepartureDate = this.originalDepartureDate?.invoke() ?: this.departureDate(),
    createdAt = this.createdAt(),
    application = this.application(),
    offlineApplication = this.offlineApplication(),
    turnarounds = this.turnarounds?.invoke() ?: mutableListOf(),
    nomsNumber = this.nomsNumber(),
    placementRequest = this.placementRequest(),
    status = this.status.invoke(),
    adhoc = this.adhoc(),
  )
}
