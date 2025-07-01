package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1PlanningBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.util.UUID

class Cas1PlanningBedSummaryFactory : Factory<Cas1PlanningBedSummary> {
  private var bedId = { UUID.randomUUID() }
  private var bedName = { randomStringUpperCase(10) }
  private var bedEndDate = { LocalDate.now() }
  private var roomId = { UUID.randomUUID() }
  private var roomName = { randomStringUpperCase(10) }
  private var characteristicsPropertyNames = { emptyList<String>() }
  private var premisesId = { UUID.randomUUID() }

  fun withBedId(id: UUID) = apply {
    this.bedId = { id }
  }

  fun withBedName(name: String) = apply {
    this.bedName = { name }
  }

  fun withBedEndDate(bedEndDate: LocalDate) = apply {
    this.bedEndDate = { bedEndDate }
  }

  fun withRoomName(name: String) = apply {
    this.roomName = { name }
  }

  fun withRoomId(id: UUID) = apply {
    this.roomId = { id }
  }

  fun withCharacteristicsPropertyNames(characteristicsPropertyNames: List<String>) = apply {
    this.characteristicsPropertyNames = { characteristicsPropertyNames }
  }

  override fun produce(): Cas1PlanningBedSummary = Cas1PlanningBedSummary(
    this.bedId(),
    this.bedName(),
    this.bedEndDate(),
    this.roomId(),
    this.roomName(),
    this.characteristicsPropertyNames(),
    this.premisesId(),
  )
}
