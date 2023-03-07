package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationLostBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class TemporaryAccommodationLostBedEntityFactory : Factory<TemporaryAccommodationLostBedEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var startDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(6) }
  private var endDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter(6) }
  private var reason: Yielded<LostBedReasonEntity>? = null
  private var referenceNumber: Yielded<String?> = { UUID.randomUUID().toString() }
  private var notes: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var premises: Yielded<PremisesEntity>? = null
  private var bed: Yielded<BedEntity>? = null
  private var lostBedCancellation: Yielded<LostBedCancellationEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withStartDate(startDate: LocalDate) = apply {
    this.startDate = { startDate }
  }

  fun withEndDate(endDate: LocalDate) = apply {
    this.endDate = { endDate }
  }

  fun withYieldedReason(reason: Yielded<LostBedReasonEntity>) = apply {
    this.reason = reason
  }

  fun withReferenceNumber(referenceNumber: String?) = apply {
    this.referenceNumber = { referenceNumber }
  }

  fun withNotes(notes: String?) = apply {
    this.notes = { notes }
  }

  fun withYieldedPremises(premises: Yielded<PremisesEntity>) = apply {
    this.premises = premises
  }

  fun withPremises(premises: PremisesEntity) = apply {
    this.premises = { premises }
  }

  fun withYieldedBed(bed: Yielded<BedEntity>) = apply {
    this.bed = bed
  }

  fun withBed(bed: BedEntity) = apply {
    this.bed = { bed }
  }

  fun withYieledLostBedCancellation(lostBedCancellation: Yielded<LostBedCancellationEntity>) = apply {
    this.lostBedCancellation = lostBedCancellation
  }

  fun withLostBedCancellation(lostBedCancellation: LostBedCancellationEntity) = apply {
    this.lostBedCancellation = { lostBedCancellation }
  }

  override fun produce(): TemporaryAccommodationLostBedEntity = TemporaryAccommodationLostBedEntity(
    id = this.id(),
    startDate = this.startDate(),
    endDate = this.endDate(),
    reason = this.reason?.invoke() ?: throw RuntimeException("Reason must be provided"),
    referenceNumber = this.referenceNumber(),
    notes = this.notes(),
    premises = this.premises?.invoke() ?: throw RuntimeException("Must provide a Premises"),
    bed = this.bed?.invoke() ?: throw RuntimeException("Must provide a Bed"),
    lostBedCancellation = this.lostBedCancellation?.invoke(),
  )
}
