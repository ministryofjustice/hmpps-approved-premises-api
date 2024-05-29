package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1OutOfServiceBedEntityFactory : Factory<Cas1OutOfServiceBedEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var reason: Yielded<Cas1OutOfServiceBedReasonEntity> = { Cas1OutOfServiceBedReasonEntityFactory().produce() }
  private var bed: Yielded<BedEntity> = { BedEntityFactory().produce() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var startDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(6) }
  private var endDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter(6) }
  private var referenceNumber: Yielded<String?> = { UUID.randomUUID().toString() }
  private var notes: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withReason(reason: Cas1OutOfServiceBedReasonEntity) = apply {
    this.reason = { reason }
  }

  fun withReason(configuration: Cas1OutOfServiceBedReasonEntityFactory.() -> Unit) = apply {
    this.reason = { Cas1OutOfServiceBedReasonEntityFactory().apply(configuration).produce() }
  }

  fun withBed(bed: BedEntity) = apply {
    this.bed = { bed }
  }

  fun withBed(configuration: BedEntityFactory.() -> Unit) = apply {
    this.bed = { BedEntityFactory().apply(configuration).produce() }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withStartDate(startDate: LocalDate) = apply {
    this.startDate = { startDate }
  }

  fun withEndDate(endDate: LocalDate) = apply {
    this.endDate = { endDate }
  }

  fun withReferenceNumber(referenceNumber: String?) = apply {
    this.referenceNumber = { referenceNumber }
  }

  fun withNotes(notes: String?) = apply {
    this.notes = { notes }
  }

  override fun produce(): Cas1OutOfServiceBedEntity {
    val bed = this.bed()

    return Cas1OutOfServiceBedEntity(
      id = this.id(),
      premises = bed.room.premises as ApprovedPremisesEntity,
      reason = this.reason(),
      bed = bed,
      createdAt = this.createdAt(),
      startDate = this.startDate(),
      endDate = this.endDate(),
      referenceNumber = this.referenceNumber(),
      notes = this.notes(),
    )
  }
}
