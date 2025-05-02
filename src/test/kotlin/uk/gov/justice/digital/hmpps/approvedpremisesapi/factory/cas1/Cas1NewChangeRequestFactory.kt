package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewChangeRequest
import java.util.UUID

class Cas1NewChangeRequestFactory : Factory<Cas1NewChangeRequest> {
  private var spaceBookingId = { UUID.randomUUID() }
  private var type = { Cas1ChangeRequestType.PLACEMENT_APPEAL }
  private var requestJson = { "{test: 1}" }
  private var reasonId = { UUID.randomUUID() }

  fun withReasonId(id: UUID) = apply { this.reasonId = { id } }
  fun withSpaceBookingId(id: UUID) = apply { this.spaceBookingId = { id } }
  fun withType(type: Cas1ChangeRequestType) = apply { this.type = { type } }
  fun withRequestJson(requestJson: String) = apply { this.requestJson = { requestJson } }

  override fun produce(): Cas1NewChangeRequest = Cas1NewChangeRequest(
    spaceBookingId = this.spaceBookingId(),
    type = this.type(),
    requestJson = this.requestJson(),
    reasonId = this.reasonId(),
  )
}
