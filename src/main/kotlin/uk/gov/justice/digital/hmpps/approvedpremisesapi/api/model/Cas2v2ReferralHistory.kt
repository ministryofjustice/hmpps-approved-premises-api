package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.time.Instant
import java.util.UUID

data class Cas2v2ReferralHistory(
  val type: ServiceType,
  val id: UUID,
  val applicationId: UUID,
  val status: String,
  val createdAt: Instant,
  val referralRejectionReason: String?,
  val localAuthorityArea: String?,
  val pdu: String?,
  val referredBy: Cas2v2StaffDto,
  val placementAddress: String?,
  val placementStatus: String?,
)
