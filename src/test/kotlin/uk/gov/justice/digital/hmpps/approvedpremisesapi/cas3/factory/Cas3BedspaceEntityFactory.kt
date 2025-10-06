package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
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
  private var startDate: Yielded<LocalDate> = { LocalDate.now().minusDays(90) }
  private var endDate: Yielded<LocalDate?>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var createdDate: Yielded<LocalDate> = { LocalDate.now().minusDays(90) }

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

  fun withStartDate(startDate: LocalDate) = apply {
    this.startDate = { startDate }
  }

  fun withEndDate(endDate: LocalDate?) = apply {
    this.endDate = { endDate }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withCreatedDate(createdDate: LocalDate) = apply {
    this.createdDate = { createdDate }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun produce(): Cas3BedspacesEntity = Cas3BedspacesEntity(
    id = this.id(),
    premises = this.premises.invoke(),
    characteristics = this.characteristics(),
    reference = this.reference(),
    notes = this.notes(),
    startDate = this.startDate(),
    endDate = this.endDate?.invoke(),
    createdAt = this.createdAt(),
    createdDate = this.createdDate(),
  )

  fun onlineBedspace(premisesEntity: Cas3PremisesEntity) = this.withStartDate(LocalDate.now().minusDays(10))
    .withEndDate(LocalDate.now().plusDays(10))
    .withPremises(premisesEntity)
    .produce()

  fun archivedBedspace(premisesEntity: Cas3PremisesEntity) = this.withStartDate(LocalDate.now().minusDays(10))
    .withEndDate(LocalDate.now().minusDays(5))
    .withPremises(premisesEntity)
    .produce()

  fun upcomingBedspace(premisesEntity: Cas3PremisesEntity) = this.withStartDate(LocalDate.now().plusDays(10))
    .withEndDate(LocalDate.now().plusDays(100))
    .withPremises(premisesEntity)
    .produce()
}
