package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class BedSummaryFactory : Factory<DomainBedSummary> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringUpperCase(6) }
  private var roomName: Yielded<String> = { randomStringUpperCase(6) }
  private var roomId: Yielded<UUID> = { UUID.randomUUID() }
  private var bedBooked: Boolean = false
  private var bedOutOfService: Boolean = false

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withRoomName(roomName: String) = apply {
    this.roomName = { roomName }
  }

  fun withBedBooked(bedBooked: Boolean) = apply {
    this.bedBooked = bedBooked
  }

  fun withBedOutOfService(bedOutOfService: Boolean) = apply {
    this.bedOutOfService = bedOutOfService
  }

  override fun produce(): DomainBedSummary = DomainBedSummary(
    id = this.id(),
    name = this.name(),
    roomId = this.roomId(),
    roomName = this.roomName(),
    bedBooked = this.bedBooked,
    bedOutOfService = this.bedOutOfService,
  )
}
