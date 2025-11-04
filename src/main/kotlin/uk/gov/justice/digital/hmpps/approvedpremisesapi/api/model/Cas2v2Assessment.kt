package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class Cas2v2Assessment(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("nacroReferralId") val nacroReferralId: String? = null,

  @get:JsonProperty("assessorName") val assessorName: String? = null,

  @get:JsonProperty("statusUpdates") val statusUpdates: List<Cas2v2StatusUpdate>? = null,
)
