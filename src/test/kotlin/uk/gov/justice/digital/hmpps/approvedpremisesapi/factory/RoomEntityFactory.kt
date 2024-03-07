package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class RoomEntityFactory : Factory<RoomEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var code: Yielded<String?> = { randomStringMultiCaseWithNumbers(6) }
  private var notes: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var premises: Yielded<PremisesEntity>? = null
  private var characteristics: Yielded<MutableList<CharacteristicEntity>> = { mutableListOf() }

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

  fun withCharacteristicsList(characteristics: List<CharacteristicEntity>) = withCharacteristics(characteristics.toMutableList())

  override fun produce() = RoomEntity(
    id = this.id(),
    name = this.name(),
    code = this.code(),
    notes = this.notes(),
    beds = mutableListOf(),
    premises = this.premises?.invoke() ?: throw RuntimeException("Must provide a premises"),
    characteristics = this.characteristics(),
  )
}

class BedEntityFactory : Factory<BedEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var code: Yielded<String?> = { randomStringMultiCaseWithNumbers(6) }
  private var room: Yielded<RoomEntity>? = null
  private var endDate: Yielded<LocalDate>? = null

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

  fun withYieldedRoom(room: Yielded<RoomEntity>) = apply {
    this.room = room
  }
  fun withEndDate(endDate: Yielded<LocalDate>) = apply {
    this.endDate = endDate
  }

  override fun produce() = BedEntity(
    id = this.id(),
    name = this.name(),
    code = this.code(),
    room = this.room?.invoke() ?: throw java.lang.RuntimeException("Must provide a room"),
    endDate = this.endDate?.invoke(),
  )
}
