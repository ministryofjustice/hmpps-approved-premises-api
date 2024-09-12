package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1SpaceBookingEntityFactory : Factory<Cas1SpaceBookingEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var premises: Yielded<ApprovedPremisesEntity> = { ApprovedPremisesEntityFactory().withDefaults().produce() }
  private var placementRequest: Yielded<PlacementRequestEntity> = { PlacementRequestEntityFactory().withDefaults().produce() }
  private var application: Yielded<ApprovedPremisesApplicationEntity> = { ApprovedPremisesApplicationEntityFactory().withDefaults().produce() }
  private var createdBy: Yielded<UserEntity> = { UserEntityFactory().withDefaults().produce() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var expectedArrivalDate: Yielded<LocalDate> = { LocalDate.now() }
  private var expectedDepartureDate: Yielded<LocalDate> = { LocalDate.now() }
  private var actualArrivalDateTime: Yielded<Instant?> = { null }
  private var actualDepartureDateTime: Yielded<Instant?> = { null }
  private var canonicalArrivalDate: Yielded<LocalDate> = { LocalDate.now() }
  private var canonicalDepartureDate: Yielded<LocalDate> = { LocalDate.now() }
  private var crn: Yielded<String> = { randomStringUpperCase(6) }
  private var keyWorkerStaffCode: Yielded<String?> = { null }
  private var keyWorkerName: Yielded<String?> = { null }
  private var keyWorkerAssignedAt: Yielded<Instant?> = { null }
  private var cancellationOccurredAt: Yielded<LocalDate?> = { null }
  private var cancellationRecordedAt: Yielded<Instant?> = { null }
  private var cancellationReason: Yielded<CancellationReasonEntity?> = { null }
  private var cancellationReasonNotes: Yielded<String?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withPremises(premises: ApprovedPremisesEntity) = apply {
    this.premises = { premises }
  }

  fun withPremises(configuration: ApprovedPremisesEntityFactory.() -> Unit) = apply {
    this.premises = { ApprovedPremisesEntityFactory().apply(configuration).produce() }
  }

  fun withYieldedPremises(premises: Yielded<ApprovedPremisesEntity>) = apply {
    this.premises = premises
  }

  fun withPlacementRequest(placementRequest: PlacementRequestEntity) = apply {
    this.placementRequest = { placementRequest }
  }

  fun withPlacementRequest(configuration: PlacementRequestEntityFactory.() -> Unit) = apply {
    this.placementRequest = { PlacementRequestEntityFactory().apply(configuration).produce() }
  }

  fun withYieldedPlacementRequest(placementRequest: Yielded<PlacementRequestEntity>) = apply {
    this.placementRequest = placementRequest
  }

  fun withCreatedBy(createdBy: UserEntity) = apply {
    this.createdBy = { createdBy }
  }

  fun withCreatedBy(configuration: UserEntityFactory.() -> Unit) = apply {
    this.createdBy = { UserEntityFactory().apply(configuration).produce() }
  }

  fun withYieldedCreatedBy(createdBy: Yielded<UserEntity>) = apply {
    this.createdBy = createdBy
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withExpectedArrivalDate(expectedArrivalDate: LocalDate) = apply {
    this.expectedArrivalDate = { expectedArrivalDate }
  }

  fun withExpectedDepartureDate(expectedDepartureDate: LocalDate) = apply {
    this.expectedDepartureDate = { expectedDepartureDate }
  }

  fun withCanonicalArrivalDate(canonicalArrivalDate: LocalDate) = apply {
    this.canonicalArrivalDate = { canonicalArrivalDate }
  }

  fun withCanonicalDepartureDate(canonicalDepartureDate: LocalDate) = apply {
    this.canonicalDepartureDate = { canonicalDepartureDate }
  }

  @Deprecated(message = "Use more specific expected arrival date", replaceWith = ReplaceWith("withExpectedArrivalDate(expectedArrivalDate)"))
  fun withArrivalDate(expectedArrivalDate: LocalDate) = apply {
    this.expectedArrivalDate = { expectedArrivalDate }
  }

  @Deprecated(message = "Use more specific expected departure date", replaceWith = ReplaceWith("withExpectedDepartureDate(expectedDepartureDate)"))
  fun withDepartureDate(expectedDepartureDate: LocalDate) = apply {
    this.expectedDepartureDate = { expectedDepartureDate }
  }

  fun withActualArrivalDateTime(actualArrivalDateTime: Instant?) = apply {
    this.actualArrivalDateTime = { actualArrivalDateTime }
  }

  fun withActualDepartureDateTime(actualDepartureDateTime: Instant?) = apply {
    this.actualDepartureDateTime = { actualDepartureDateTime }
  }

  fun withApplication(application: ApprovedPremisesApplicationEntity) = apply {
    this.application = { application }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withKeyworkerName(keyWorkerName: String?) = apply {
    this.keyWorkerName = { keyWorkerName }
  }

  fun withKeyworkerStaffCode(keyWorkerStaffCode: String?) = apply {
    this.keyWorkerStaffCode = { keyWorkerStaffCode }
  }

  fun withKeyworkerAssignedAt(keyWorkerAssignedAt: Instant?) = apply {
    this.keyWorkerAssignedAt = { keyWorkerAssignedAt }
  }

  fun withCancellationOccurredAt(occurredAt: LocalDate?) = apply {
    this.cancellationOccurredAt = { occurredAt }
  }

  fun withCancellationRecordedAt(recordedAt: Instant?) = apply {
    this.cancellationRecordedAt = { recordedAt }
  }

  fun withCancellationReason(reason: CancellationReasonEntity?) = apply {
    this.cancellationReason = { reason }
  }

  fun withCancellationReasonNotes(reasonNotes: String?) = apply {
    this.cancellationReasonNotes = { reasonNotes }
  }

  override fun produce() = Cas1SpaceBookingEntity(
    id = this.id(),
    premises = this.premises(),
    placementRequest = this.placementRequest(),
    createdBy = this.createdBy(),
    createdAt = this.createdAt(),
    expectedArrivalDate = this.expectedArrivalDate(),
    expectedDepartureDate = this.expectedDepartureDate(),
    actualArrivalDateTime = this.actualArrivalDateTime(),
    actualDepartureDateTime = this.actualDepartureDateTime(),
    canonicalArrivalDate = this.canonicalArrivalDate(),
    canonicalDepartureDate = this.canonicalDepartureDate(),
    crn = this.crn(),
    keyWorkerStaffCode = this.keyWorkerStaffCode(),
    keyWorkerName = this.keyWorkerName(),
    keyWorkerAssignedAt = this.keyWorkerAssignedAt(),
    application = this.application(),
    cancellationOccurredAt = cancellationOccurredAt(),
    cancellationRecordedAt = cancellationRecordedAt(),
    cancellationReason = cancellationReason(),
    cancellationReasonNotes = cancellationReasonNotes(),
  )
}
