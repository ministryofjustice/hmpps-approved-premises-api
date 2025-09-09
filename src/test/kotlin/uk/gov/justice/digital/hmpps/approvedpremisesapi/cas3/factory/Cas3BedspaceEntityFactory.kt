package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3BedspaceEntityFactory : Factory<Cas3BedspacesEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var premises: Yielded<Cas3PremisesEntity> = { Cas3PremisesEntityFactory().produce() }
  private var characteristics: Yielded<MutableList<Cas3BedspaceCharacteristicEntity>> = { mutableListOf() }
  private var reference: Yielded<String> = { randomStringUpperCase(6) }
  private var notes: Yielded<String?> = { null }
  private var startDate: Yielded<LocalDate?> = { LocalDate.now().randomDateBefore(6) }
  private var endDate: Yielded<LocalDate?> = { LocalDate.now().randomDateAfter(6) }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withPremises(premises: Cas3PremisesEntity) = apply {
    this.premises = { premises }
  }

  fun withCharacteristics(characteristics: MutableList<Cas3BedspaceCharacteristicEntity>) = apply {
    this.characteristics = { characteristics }
  }

  fun withReference(reference: String) = apply {
    this.reference = { reference }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withStartDate(startDate: LocalDate?) = apply {
    this.startDate = { startDate }
  }

  fun withEndDate(endDate: LocalDate?) = apply {
    this.endDate = { endDate }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun produce(): Cas3BedspacesEntity = Cas3BedspacesEntity(
    id = this.id(),
    premises = this.premises.invoke(),
    characteristics = this.characteristics(),
    reference = this.reference(),
    notes = this.notes(),
    startDate = this.startDate(),
    endDate = this.endDate(),
    createdAt = this.createdAt(),
  )
}
