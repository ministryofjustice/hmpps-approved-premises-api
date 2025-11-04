package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param rejectionReasonId
 * @param decisionJson
 */
data class Cas1RejectChangeRequest(

  val rejectionReasonId: java.util.UUID,

  val decisionJson: kotlin.collections.Map<kotlin.String, kotlin.Any>,
)
