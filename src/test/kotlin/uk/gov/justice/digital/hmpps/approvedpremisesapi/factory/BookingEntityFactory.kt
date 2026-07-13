package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Deprecated("See deprecations-bookings.md")
class BookingEntityFactory : Factory<BookingEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringUpperCase(6) }
  private var arrivalDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(14) }
  private var departureDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter(14) }
  private var originalArrivalDate: Yielded<LocalDate>? = null
  private var originalDepartureDate: Yielded<LocalDate>? = null
  private var keyWorkerStaffCode: Yielded<String?> = { null }
  private var premises: Yielded<PremisesEntity>? = null
  private var serviceName: Yielded<ServiceName> = { randomOf(listOf(ServiceName.approvedPremises, ServiceName.temporaryAccommodation)) }
  private var bed: Yielded<BedEntity?> = { null }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusDays(14L).randomDateTimeBefore(14) }
  private var application: Yielded<ApplicationEntity?> = { null }
  private var offlineApplication: Yielded<OfflineApplicationEntity?> = { null }
  private var nomsNumber: Yielded<String?> = { randomStringUpperCase(6) }
  private var status: Yielded<BookingStatus?> = { null }
  private var adhoc: Yielded<Boolean?> = { null }
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

  fun withDepartureDate(departureDate: LocalDate) = apply {
    this.departureDate = { departureDate }
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

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withApplication(application: ApplicationEntity?) = apply {
    this.application = { application }
  }

  fun withOfflineApplication(offlineApplication: OfflineApplicationEntity?) = apply {
    this.offlineApplication = { offlineApplication }
  }

  fun withNomsNumber(nomsNumber: String?) = apply {
    this.nomsNumber = { nomsNumber }
  }

  fun withStatus(status: BookingStatus) = apply {
    this.status = { status }
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
    premises = this.premises?.invoke() ?: throw RuntimeException("Must provide a Premises"),
    bed = this.bed(),
    service = this.serviceName.invoke().value,
    originalArrivalDate = this.originalArrivalDate?.invoke() ?: this.arrivalDate(),
    originalDepartureDate = this.originalDepartureDate?.invoke() ?: this.departureDate(),
    createdAt = this.createdAt(),
    application = this.application(),
    offlineApplication = this.offlineApplication(),
    nomsNumber = this.nomsNumber(),
    status = this.status.invoke(),
    adhoc = this.adhoc(),
    offenderName = this.offenderName(),
  )
}
