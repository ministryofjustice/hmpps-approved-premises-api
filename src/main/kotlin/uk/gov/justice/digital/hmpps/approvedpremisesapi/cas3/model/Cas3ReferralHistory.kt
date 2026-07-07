package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentStatus
import java.time.Instant
import java.util.UUID

data class Cas3ReferralHistory(
  val type: ServiceType,
  val id: UUID,
  val applicationId: UUID,
  val applicationStatus: ApplicationStatus,
  val assessmentStatus: TemporaryAccommodationAssessmentStatus?,
  val createdAt: Instant,
  val referralRejectionReason: String?,
  val referralRejectionReasonDetail: String?,
  val localAuthorityArea: String?,
  val pdu: String?,
  val referredBy: Cas3StaffDto,
  val placementAddress: String?,
  val bookingStatus: Cas3BookingStatus?,
  val uiUrl: String,
)
