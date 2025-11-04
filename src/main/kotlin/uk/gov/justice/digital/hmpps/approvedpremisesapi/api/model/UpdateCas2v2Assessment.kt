package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdateCas2v2Assessment(

  @get:JsonProperty("nacroReferralId") val nacroReferralId: String? = null,

  @get:JsonProperty("assessorName") val assessorName: String? = null,
)
