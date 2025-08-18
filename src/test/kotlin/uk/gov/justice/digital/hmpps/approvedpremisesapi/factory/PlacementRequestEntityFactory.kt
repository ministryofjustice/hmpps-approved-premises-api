package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeAfter
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PlacementRequestEntityFactory : Factory<PlacementRequestEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var expectedArrival: Yielded<LocalDate> = { LocalDate.now() }
  private var duration: Yielded<Int> = { 12 }
  private var placementRequirements: Yielded<PlacementRequirementsEntity>? = null
  private var application: Yielded<ApprovedPremisesApplicationEntity>? = null
  private var assessment: Yielded<ApprovedPremisesAssessmentEntity>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var booking: Yielded<BookingEntity?> = { null }
  private var spaceBookings: Yielded<MutableList<Cas1SpaceBookingEntity>> = { mutableListOf() }
  private var bookingNotMades: Yielded<MutableList<BookingNotMadeEntity>> = { mutableListOf() }
  private var notes: Yielded<String?> = { null }
  private var isParole: Yielded<Boolean> = { false }
  private var isWithdrawn: Yielded<Boolean> = { false }
  private var placementApplication: () -> PlacementApplicationEntity? = { null }
  private var withdrawalReason: Yielded<PlacementRequestWithdrawalReason?> = { null }
  private var dueAt: Yielded<OffsetDateTime?> = { OffsetDateTime.now().randomDateTimeAfter(10) }

  fun withDefaults() = apply {
    this.placementRequirements = { PlacementRequirementsEntityFactory().withDefaults().produce() }
    this.application = { ApprovedPremisesApplicationEntityFactory().withDefaults().produce() }
    this.assessment = { ApprovedPremisesAssessmentEntityFactory().withDefaults().produce() }
  }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withExpectedArrival(expectedArrival: LocalDate) = apply {
    this.expectedArrival = { expectedArrival }
  }

  fun withBooking(booking: BookingEntity?) = apply {
    this.booking = { booking }
  }

  fun withBooking(configuration: BookingEntityFactory.() -> Unit) = apply {
    this.booking = { BookingEntityFactory().apply(configuration).produce() }
  }

  fun withSpaceBookings(bookings: MutableList<Cas1SpaceBookingEntity>) = apply {
    this.spaceBookings = { bookings }
  }

  fun withApplication(application: ApprovedPremisesApplicationEntity) = apply {
    this.application = { application }
  }

  fun withAssessment(assessment: ApprovedPremisesAssessmentEntity) = apply {
    this.assessment = { assessment }
  }

  fun withPlacementRequirements(placementRequirements: PlacementRequirementsEntity) = apply {
    this.placementRequirements = { placementRequirements }
  }

  fun withNotes(notes: String?) = apply {
    this.notes = { notes }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withIsParole(isParole: Boolean) = apply {
    this.isParole = { isParole }
  }

  fun withIsWithdrawn(isWithdrawn: Boolean) = apply {
    this.isWithdrawn = { isWithdrawn }
  }

  fun withDuration(duration: Int) = apply {
    this.duration = { duration }
  }

  fun withPlacementApplication(placementApplication: PlacementApplicationEntity?) = apply {
    this.placementApplication = { placementApplication }
  }

  fun withWithdrawalReason(withdrawalReason: PlacementRequestWithdrawalReason?) = apply {
    this.withdrawalReason = { withdrawalReason }
  }

  fun withDueAt(dueAt: OffsetDateTime?) = apply {
    this.dueAt = { dueAt }
  }

  override fun produce(): PlacementRequestEntity = PlacementRequestEntity(
    id = this.id(),
    expectedArrival = this.expectedArrival(),
    duration = this.duration(),
    placementRequirements = this.placementRequirements?.invoke() ?: throw RuntimeException("Must provide Placement Requirements"),
    application = this.application?.invoke() ?: throw RuntimeException("Must provide an Application"),
    assessment = this.assessment?.invoke() ?: throw RuntimeException("Must provide an Assessment"),
    createdAt = this.createdAt(),
    spaceBookings = this.spaceBookings(),
    bookingNotMades = this.bookingNotMades(),
    notes = this.notes(),
    isParole = this.isParole(),
    isWithdrawn = this.isWithdrawn(),
    placementApplication = this.placementApplication(),
    withdrawalReason = this.withdrawalReason(),
  )
}
