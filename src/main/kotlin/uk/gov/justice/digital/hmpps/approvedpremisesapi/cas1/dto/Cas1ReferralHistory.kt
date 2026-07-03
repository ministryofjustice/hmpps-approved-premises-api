package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import java.time.Instant
import java.util.UUID

data class Cas1ReferralHistory(
  val type: ServiceType,
  val id: UUID,
  val applicationId: UUID,
  val status: Cas1AssessmentStatus,
  val createdAt: Instant,
  val referralRejectionReason: String?,
  val localAuthorityArea: String?,
  val pdu: String?,
  val referredBy: Cas1StaffDto,
  val placementAddress: String?,
  val placementStatus: String?,
  val uiUrl: String,
)
