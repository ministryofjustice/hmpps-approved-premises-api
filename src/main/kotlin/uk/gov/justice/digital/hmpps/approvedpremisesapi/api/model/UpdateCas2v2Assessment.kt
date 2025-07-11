package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param nacroReferralId
 * @param assessorName
 */
data class UpdateCas2v2Assessment(

  @Schema(example = "null", description = "")
  @get:JsonProperty("nacroReferralId") val nacroReferralId: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("assessorName") val assessorName: String? = null,
)
