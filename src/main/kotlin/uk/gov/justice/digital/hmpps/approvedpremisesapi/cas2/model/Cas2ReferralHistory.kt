package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import java.time.LocalDate
import java.util.UUID

data class Cas2ReferralHistory(
  val type: ServiceType,
  val id: UUID,
  val applicationId: UUID,
  val applicationStatus: Cas2AssessmentStatus?,
  val applicationSubmittedDate: LocalDate,
  val applicationLastUpdatedDate: LocalDate?,
  val referralRejectionReason: String?,
  val localAuthorityArea: String?,
  val pdu: String?,
  val referredBy: String,
  val placementAddress: String?,
  val uiUrl: String,
)
