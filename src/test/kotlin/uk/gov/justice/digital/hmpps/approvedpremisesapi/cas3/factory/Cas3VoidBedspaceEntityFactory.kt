package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3VoidBedspaceEntityFactory : Factory<Cas3VoidBedspaceEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var startDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(6) }
  private var endDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter(6) }
  private var reason: Yielded<Cas3VoidBedspaceReasonEntity>? = null
  private var referenceNumber: Yielded<String?> = { UUID.randomUUID().toString() }
  private var notes: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var premises: Yielded<PremisesEntity>? = null
  private var voidBedspaceCancellation: Yielded<Cas3VoidBedspaceCancellationEntity>? = null
  private var bed: Yielded<BedEntity>? = null
  private var bedspace: Yielded<Cas3BedspacesEntity>? = null
  private var costCentre: Yielded<Cas3CostCentre>? = { Cas3CostCentre.HMPPS }
  private var cancellationDate: Yielded<OffsetDateTime>? = null
  private var cancellationNotes: Yielded<String>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withStartDate(startDate: LocalDate) = apply {
    this.startDate = { startDate }
  }

  fun withEndDate(endDate: LocalDate) = apply {
    this.endDate = { endDate }
  }

  fun withYieldedReason(reason: Yielded<Cas3VoidBedspaceReasonEntity>) = apply {
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

  fun withBed(bed: BedEntity) = apply {
    this.bed = { bed }
  }

  fun withBedspace(bedspace: Cas3BedspacesEntity) = apply {
    this.bedspace = { bedspace }
  }

  fun withYieldedBed(bed: Yielded<BedEntity>) = apply {
    this.bed = bed
  }

  fun withCancellationDate(cancellationDate: OffsetDateTime) = apply {
    this.cancellationDate = { cancellationDate }
  }

  fun withCancellationNotes(cancellationNotes: String) = apply {
    this.cancellationNotes = { cancellationNotes }
  }

  fun withCostCentre(costCentre: Cas3CostCentre) = apply {
    this.costCentre = { costCentre }
  }

  @Deprecated("This will be replaced by produceV2")
  @SuppressWarnings("TooGenericExceptionThrown")
  override fun produce() = Cas3VoidBedspaceEntity(
    id = this.id(),
    startDate = this.startDate(),
    endDate = this.endDate(),
    reason = this.reason?.invoke() ?: throw RuntimeException("Reason must be provided"),
    referenceNumber = this.referenceNumber(),
    notes = this.notes(),
    premises = this.premises?.let { it() },
    bed = this.bed?.let { it() },
    cancellation = this.voidBedspaceCancellation?.invoke(),
    bedspace = this.bedspace?.let { it() },
    costCentre = this.costCentre?.let { it() },
    cancellationDate = this.cancellationDate?.let { it() },
    cancellationNotes = this.cancellationNotes?.let { it() },
  )

  @SuppressWarnings("TooGenericExceptionThrown")
  fun produceV2() = Cas3VoidBedspaceEntity(
    id = this.id(),
    startDate = this.startDate(),
    endDate = this.endDate(),
    reason = this.reason!!.invoke(),
    referenceNumber = this.referenceNumber(),
    notes = this.notes(),
    premises = null,
    bed = null,
    cancellation = null,
    bedspace = this.bedspace?.let { it() } ?: Cas3BedspaceEntityFactory().produce(),
    costCentre = this.costCentre?.let { it() },
    cancellationDate = this.cancellationDate?.let { it() },
    cancellationNotes = this.cancellationNotes?.let { it() },
  )
}
