package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param nacroReferralId
 * @param assessorName
 */
data class UpdateCas2Assessment(

  @get:JsonProperty("nacroReferralId") val nacroReferralId: String? = null,

  @get:JsonProperty("assessorName") val assessorName: String? = null,
)
