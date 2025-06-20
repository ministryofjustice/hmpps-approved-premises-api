package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3CancellationEntityFactory : Factory<Cas3CancellationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var date: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(14) }
  private var reason: Yielded<CancellationReasonEntity>? = null
  private var notes: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var booking: Yielded<Cas3BookingEntity>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusDays(14L).randomDateTimeBefore(14) }
  private var otherReason: Yielded<String?> = { null }

  fun withDefaults() = apply {
    withReason(CancellationReasonEntityFactory().produce())
  }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withDate(date: LocalDate) = apply {
    this.date = { date }
  }

  fun withYieldedReason(reason: Yielded<CancellationReasonEntity>) = apply {
    this.reason = reason
  }

  fun withReason(reason: CancellationReasonEntity) = apply {
    this.reason = { reason }
  }

  fun withDefaultReason() = withReason(CancellationReasonEntityFactory().produce())

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withYieldedBooking(booking: Yielded<Cas3BookingEntity>) = apply {
    this.booking = booking
  }

  fun withBooking(booking: Cas3BookingEntity) = apply {
    this.booking = { booking }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withOtherReason(otherReason: String?) = apply {
    this.otherReason = { otherReason }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun produce(): Cas3CancellationEntity = Cas3CancellationEntity(
    id = this.id(),
    notes = this.notes(),
    date = this.date(),
    reason = this.reason?.invoke() ?: throw RuntimeException("Reason must be provided"),
    booking = this.booking?.invoke() ?: throw RuntimeException("Booking must be provided"),
    createdAt = this.createdAt(),
    otherReason = this.otherReason(),
  )
}
