package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionChangeType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.EnumSet
import java.util.UUID

class Cas1OutOfServiceBedRevisionEntityFactory : Factory<Cas1OutOfServiceBedRevisionEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var detailType: Yielded<Cas1OutOfServiceBedRevisionType> = { Cas1OutOfServiceBedRevisionType.INITIAL }
  private var startDate: Yielded<LocalDate> = { LocalDate.now() }
  private var endDate: Yielded<LocalDate> = { LocalDate.now() }
  private var referenceNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(9) }
  private var notes: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var reason: Yielded<Cas1OutOfServiceBedReasonEntity> = { Cas1OutOfServiceBedReasonEntityFactory().produce() }
  private var outOfServiceBed: Yielded<Cas1OutOfServiceBedEntity> = { Cas1OutOfServiceBedEntityFactory().produce() }
  private var createdBy: Yielded<UserEntity?> = { UserEntityFactory().withDefaults().produce() }
  private var changeType: Yielded<EnumSet<Cas1OutOfServiceBedRevisionChangeType>> =
    { EnumSet.noneOf(Cas1OutOfServiceBedRevisionChangeType::class.java) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withDetailType(detailType: Cas1OutOfServiceBedRevisionType) = apply {
    this.detailType = { detailType }
  }

  fun withStartDate(startDate: LocalDate) = apply {
    this.startDate = { startDate }
  }

  fun withEndDate(endDate: LocalDate) = apply {
    this.endDate = { endDate }
  }

  fun withReferenceNumber(referenceNumber: String) = apply {
    this.referenceNumber = { referenceNumber }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withReason(reason: Cas1OutOfServiceBedReasonEntity) = apply {
    this.reason = { reason }
  }

  fun withReason(configuration: Cas1OutOfServiceBedReasonEntityFactory.() -> Unit) = apply {
    this.reason = { Cas1OutOfServiceBedReasonEntityFactory().apply(configuration).produce() }
  }

  fun withYieldedReason(reason: Yielded<Cas1OutOfServiceBedReasonEntity>) = apply {
    this.reason = reason
  }

  fun withOutOfServiceBed(outOfServiceBed: Cas1OutOfServiceBedEntity) = apply {
    this.outOfServiceBed = { outOfServiceBed }
  }

  fun withOutOfServiceBed(configuration: Cas1OutOfServiceBedEntityFactory.() -> Unit) = apply {
    this.outOfServiceBed = { Cas1OutOfServiceBedEntityFactory().apply(configuration).produce() }
  }

  fun withCreatedBy(createdBy: UserEntity?) = apply {
    this.createdBy = { createdBy }
  }

  fun withCreatedBy(configuration: UserEntityFactory.() -> Unit) = apply {
    this.createdBy = { UserEntityFactory().apply(configuration).produce() }
  }

  fun withChangeType(changeType: EnumSet<Cas1OutOfServiceBedRevisionChangeType>) = apply {
    this.changeType = { changeType }
  }

  override fun produce() = Cas1OutOfServiceBedRevisionEntity(
    id = this.id(),
    createdAt = this.createdAt(),
    revisionType = this.detailType(),
    startDate = this.startDate(),
    endDate = this.endDate(),
    referenceNumber = this.referenceNumber(),
    notes = this.notes(),
    reason = this.reason(),
    outOfServiceBed = this.outOfServiceBed(),
    createdBy = this.createdBy(),
    changeTypePacked = Cas1OutOfServiceBedRevisionChangeType.pack(this.changeType()),
  )
}
