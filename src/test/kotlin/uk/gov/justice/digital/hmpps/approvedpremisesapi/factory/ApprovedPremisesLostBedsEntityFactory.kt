package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesLostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class ApprovedPremisesLostBedsEntityFactory : Factory<ApprovedPremisesLostBedsEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var startDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(6) }
  private var endDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter(6) }
  private var numberOfBeds: Yielded<Int> = { randomInt(1, 10) }
  private var reason: Yielded<LostBedReasonEntity>? = null
  private var referenceNumber: Yielded<String?> = { UUID.randomUUID().toString() }
  private var notes: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var premises: Yielded<PremisesEntity>? = null
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

  fun withNumberOfBeds(numberOfBeds: Int) = apply {
    this.numberOfBeds = { numberOfBeds }
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

  fun withYieledLostBedCancellation(lostBedCancellation: Yielded<LostBedCancellationEntity>) = apply {
    this.lostBedCancellation = lostBedCancellation
  }

  fun withLostBedCancellation(lostBedCancellation: LostBedCancellationEntity) = apply {
    this.lostBedCancellation = { lostBedCancellation }
  }

  override fun produce(): ApprovedPremisesLostBedsEntity = ApprovedPremisesLostBedsEntity(
    id = this.id(),
    startDate = this.startDate(),
    endDate = this.endDate(),
    numberOfBeds = this.numberOfBeds(),
    reason = this.reason?.invoke() ?: throw RuntimeException("Reason must be provided"),
    referenceNumber = this.referenceNumber(),
    notes = this.notes(),
    premises = this.premises?.invoke() ?: throw RuntimeException("Must provide a Premises"),
    lostBedCancellation = this.lostBedCancellation?.invoke(),
  )
}
