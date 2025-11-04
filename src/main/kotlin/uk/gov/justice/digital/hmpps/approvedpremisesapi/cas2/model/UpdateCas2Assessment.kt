package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

/**
 *
 * @param nacroReferralId
 * @param assessorName
 */
data class UpdateCas2Assessment(

  val nacroReferralId: String? = null,

  val assessorName: String? = null,
)
