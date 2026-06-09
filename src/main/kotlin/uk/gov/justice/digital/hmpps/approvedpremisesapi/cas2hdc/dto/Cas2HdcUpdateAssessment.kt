package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param nacroReferralId
 * @param assessorName
 */
data class Cas2HdcUpdateAssessment(

  @get:JsonProperty("nacroReferralId") val nacroReferralId: String? = null,

  @get:JsonProperty("assessorName") val assessorName: String? = null,
)
