package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import java.time.Instant
import java.util.UUID

data class Cas2HdcReferralHistory(
  val type: ServiceType,
  val id: UUID,
  val applicationId: UUID,
  val status: String,
  val createdAt: Instant,
  val referralRejectionReason: String?,
  val localAuthorityArea: String?,
  val pdu: String?,
  val referredBy: Cas2HdcStaffDto,
  val placementAddress: String?,
  val placementStatus: String?,
)
