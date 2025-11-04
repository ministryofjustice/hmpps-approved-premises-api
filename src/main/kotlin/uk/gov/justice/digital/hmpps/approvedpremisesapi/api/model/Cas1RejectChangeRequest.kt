package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1RejectChangeRequest(

  val rejectionReasonId: java.util.UUID,

  val decisionJson: kotlin.collections.Map<kotlin.String, kotlin.Any>,
)
