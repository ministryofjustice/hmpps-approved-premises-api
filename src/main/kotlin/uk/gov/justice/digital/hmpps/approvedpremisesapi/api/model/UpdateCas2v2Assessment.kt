package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param nacroReferralId
 * @param assessorName
 */
data class UpdateCas2v2Assessment(

  val nacroReferralId: String? = null,

  val assessorName: String? = null,
)
