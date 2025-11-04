package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

/**
 *
 * @param id
 * @param nacroReferralId
 * @param assessorName
 * @param statusUpdates
 */
data class Cas2Assessment(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("nacroReferralId") val nacroReferralId: String? = null,

  @get:JsonProperty("assessorName") val assessorName: String? = null,

  @get:JsonProperty("statusUpdates") val statusUpdates: List<Cas2StatusUpdate>? = null,
)
