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

  val id: UUID,

  val nacroReferralId: String? = null,

  val assessorName: String? = null,

  val statusUpdates: List<Cas2StatusUpdate>? = null,
)
