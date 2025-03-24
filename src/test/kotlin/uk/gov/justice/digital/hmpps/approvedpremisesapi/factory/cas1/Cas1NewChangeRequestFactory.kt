package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewChangeRequest
import java.util.UUID

class Cas1NewChangeRequestFactory : Factory<Cas1NewChangeRequest> {
  private var spaceBookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var type = { Cas1ChangeRequestType.APPEAL }
  private var requestJson = { "{test: 1}" }
  private var reasonId: Yielded<UUID> = { UUID.randomUUID() }

  fun withReasonId(id: UUID) = apply {
    this.reasonId = { id }
  }

  fun withSpaceBookingId(id: UUID) = apply {
    this.spaceBookingId = { id }
  }

  fun withType(type: Cas1ChangeRequestType) = apply {
    this.type = { type }
  }

  override fun produce(): Cas1NewChangeRequest = Cas1NewChangeRequest(
    spaceBookingId = this.spaceBookingId(),
    type = this.type(),
    requestJson = this.requestJson(),
    reasonId = this.reasonId(),
  )
}
