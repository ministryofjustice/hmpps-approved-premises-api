package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class RoomEntityFactory : Factory<RoomEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var code: Yielded<String?> = { randomStringMultiCaseWithNumbers(6) }
  private var notes: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var premises: Yielded<PremisesEntity>? = null
  private var characteristics: Yielded<MutableList<CharacteristicEntity>> = { mutableListOf() }
  private var beds: Yielded<MutableList<BedEntity>> = { mutableListOf() }

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

  fun withPremises(premises: PremisesEntity) = apply {
    this.premises = { premises }
  }

  fun withYieldedPremises(premises: Yielded<PremisesEntity>) = apply {
    this.premises = premises
  }

  fun withCharacteristics(characteristics: MutableList<CharacteristicEntity>) = apply {
    this.characteristics = { characteristics }
  }

  fun withCharacteristics(vararg characteristics: CharacteristicEntity) = apply {
    this.characteristics = { characteristics.toMutableList() }
  }

  fun withCharacteristicsList(characteristics: List<CharacteristicEntity>) = withCharacteristics(characteristics.toMutableList())

  fun withBeds(beds: MutableList<BedEntity>) = apply {
    this.beds = { beds }
  }

  fun withBeds(vararg beds: BedEntity) = apply {
    this.beds = { beds.toMutableList() }
  }

  override fun produce() = RoomEntity(
    id = this.id(),
    name = this.name(),
    code = this.code(),
    notes = this.notes(),
    beds = this.beds(),
    premises = this.premises?.invoke() ?: throw RuntimeException("Must provide a premises"),
    characteristics = this.characteristics(),
  )
}

class BedEntityFactory : Factory<BedEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var code: Yielded<String?> = { randomStringMultiCaseWithNumbers(6) }
  private var room: Yielded<RoomEntity>? = null
  private var endDate: Yielded<LocalDate?>? = null
  private var createdAt: Yielded<OffsetDateTime>? = { OffsetDateTime.now() }

  fun withDefaults() = apply {
    withRoom(RoomEntityFactory().withDefaults().produce())
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

  fun withRoom(room: RoomEntity) = apply {
    this.room = { room }
  }

  fun withRoom(configuration: RoomEntityFactory.() -> Unit) = apply {
    this.room = { RoomEntityFactory().apply(configuration).produce() }
  }

  fun withYieldedRoom(room: Yielded<RoomEntity>) = apply {
    this.room = room
  }

  fun withEndDate(endDate: LocalDate?) = apply {
    this.endDate = { endDate }
  }

  fun withEndDate(endDate: Yielded<LocalDate?>) = apply {
    this.endDate = endDate
  }

  fun withCreatedAt(createdAt: Yielded<OffsetDateTime>?) = apply {
    this.createdAt = createdAt
  }

  override fun produce() = BedEntity(
    id = this.id(),
    name = this.name(),
    code = this.code(),
    room = this.room?.invoke() ?: throw java.lang.RuntimeException("Must provide a room"),
    endDate = this.endDate?.invoke(),
    createdAt = this.createdAt?.invoke(),
  )
}
