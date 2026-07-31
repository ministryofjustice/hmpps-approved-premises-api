package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PremisesBaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1RoomEntityFactory : Factory<Cas1RoomEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var code: Yielded<String?> = { randomStringMultiCaseWithNumbers(6) }
  private var notes: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var premises: Yielded<Cas1PremisesBaseEntity>? = null
  private var characteristics: Yielded<MutableList<Cas1CharacteristicEntity>> = { mutableListOf() }
  private var beds: Yielded<MutableList<Cas1BedEntity>> = { mutableListOf() }

  fun withDefaults() = apply {
    withPremises(ApprovedPremisesEntityFactory().withDefaults().produce())
  }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withCode(code: String) = apply {
    this.code = { code }
  }

  fun withNotes(notes: String?) = apply {
    this.notes = { notes }
  }

  fun withPremises(premises: Cas1PremisesBaseEntity) = apply {
    this.premises = { premises }
  }

  fun withYieldedPremises(premises: Yielded<Cas1PremisesBaseEntity>) = apply {
    this.premises = premises
  }

  fun withCharacteristics(characteristics: List<Cas1CharacteristicEntity>) = apply {
    this.characteristics = { characteristics.toMutableList() }
  }

  fun withCharacteristics(vararg characteristics: Cas1CharacteristicEntity) = apply {
    this.characteristics = { characteristics.toMutableList() }
  }

  fun withCharacteristicsList(characteristics: List<Cas1CharacteristicEntity>) = withCharacteristics(characteristics.toMutableList())

  fun withBeds(beds: MutableList<Cas1BedEntity>) = apply {
    this.beds = { beds }
  }

  fun withBeds(vararg beds: Cas1BedEntity) = apply {
    this.beds = { beds.toMutableList() }
  }

  override fun produce() = Cas1RoomEntity(
    id = this.id(),
    name = this.name(),
    code = this.code(),
    notes = this.notes(),
    beds = this.beds(),
    premises = this.premises?.invoke() ?: throw RuntimeException("Must provide a premises"),
    characteristics = this.characteristics(),
  )
}

class Cas1BedEntityFactory : Factory<Cas1BedEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var code: Yielded<String?> = { randomStringMultiCaseWithNumbers(6) }
  private var room: Yielded<Cas1RoomEntity>? = null
  private var createdDate: Yielded<LocalDate?>? = null
  private var startDate: Yielded<LocalDate> = { LocalDate.now().minusDays(90) }
  private var endDate: Yielded<LocalDate?>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }

  fun withDefaults() = apply {
    withRoom(Cas1RoomEntityFactory().withDefaults().produce())
  }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withCode(code: String) = apply {
    this.code = { code }
  }

  fun withRoom(room: Cas1RoomEntity) = apply {
    this.room = { room }
  }

  fun withRoom(configuration: Cas1RoomEntityFactory.() -> Unit) = apply {
    this.room = { Cas1RoomEntityFactory().apply(configuration).produce() }
  }

  fun withYieldedRoom(room: Yielded<Cas1RoomEntity>) = apply {
    this.room = room
  }

  fun withCreatedDate(createdDate: LocalDate?) = apply {
    this.createdDate = { createdDate }
  }

  fun withStartDate(startDate: LocalDate) = apply {
    this.startDate = { startDate }
  }

  fun withEndDate(endDate: LocalDate?) = apply {
    this.endDate = { endDate }
  }

  fun withEndDate(endDate: Yielded<LocalDate?>) = apply {
    this.endDate = endDate
  }

  fun withCreatedAt(createdAt: Yielded<OffsetDateTime>) = apply {
    this.createdAt = createdAt
  }

  override fun produce() = Cas1BedEntity(
    id = this.id(),
    name = this.name(),
    code = this.code(),
    room = this.room?.invoke() ?: throw java.lang.RuntimeException("Must provide a room"),
    endDate = this.endDate?.invoke(),
    createdDate = this.createdDate?.invoke(),
    startDate = this.startDate?.invoke(),
    createdAt = this.createdAt.invoke(),
  )
}
