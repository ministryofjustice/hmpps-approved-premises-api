package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

/**
 *
 * @param id
 * @param nacroReferralId
 * @param assessorName
 * @param statusUpdates
 */
data class Cas2v2Assessment(

  val id: UUID,

  val nacroReferralId: String? = null,

  val assessorName: String? = null,

  val statusUpdates: List<Cas2v2StatusUpdate>? = null,
)
