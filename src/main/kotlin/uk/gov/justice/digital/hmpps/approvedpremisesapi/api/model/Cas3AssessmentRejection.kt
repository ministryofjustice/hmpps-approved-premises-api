package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.UUID

data class Cas3AssessmentRejection(
  val document: Any,
  val rejectionRationale: String,
  val referralRejectionReasonId: UUID? = null,
  val referralRejectionReasonDetail: String? = null,
  val isWithdrawn: Boolean? = null,
)
