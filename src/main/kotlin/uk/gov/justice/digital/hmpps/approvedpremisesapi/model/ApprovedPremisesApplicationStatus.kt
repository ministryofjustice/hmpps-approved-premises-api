package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus as ApiApprovedPremisesApplicationStatus

enum class ApprovedPremisesApplicationStatus(val apiValue: ApiApprovedPremisesApplicationStatus) {
  STARTED(ApiApprovedPremisesApplicationStatus.STARTED),
  SUBMITTED(ApiApprovedPremisesApplicationStatus.SUBMITTED),
  REJECTED(ApiApprovedPremisesApplicationStatus.REJECTED),
  AWAITING_ASSESSMENT(ApiApprovedPremisesApplicationStatus.AWAITING_ASSESMENT),
  UNALLOCATED_ASSESSMENT(ApiApprovedPremisesApplicationStatus.UNALLOCATED_ASSESMENT),
  ASSESSMENT_IN_PROGRESS(ApiApprovedPremisesApplicationStatus.ASSESMENT_IN_PROGRESS),

  /**
   * An application has been assessed and a [PlacementRequestEntity] has been created which requires a [BookingEntity].
   *
   * Note - If a [PlacementApplicationEntity] is assessed the application _will not_ enter this state
   * (it will remain as PENDING_PLACEMENT_REQUEST)
   */
  AWAITING_PLACEMENT(ApiApprovedPremisesApplicationStatus.AWAITING_PLACEMENT),

  /**
   * A [BookingEntity] has been created for a [PlacementRequestEntity]
   */
  PLACEMENT_ALLOCATED(ApiApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED),
  INAPPLICABLE(ApiApprovedPremisesApplicationStatus.INAPPLICABLE),
  WITHDRAWN(ApiApprovedPremisesApplicationStatus.WITHDRAWN),
  REQUESTED_FURTHER_INFORMATION(ApiApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION),

  /**
   * An application has been assessed. Because no arrival date was defined,
   * one or more [PlacementApplicationEntity]s are required
   */
  PENDING_PLACEMENT_REQUEST(ApiApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST),

  EXPIRED(ApiApprovedPremisesApplicationStatus.EXPIRED),
  ;

  companion object {
    fun valueOf(apiValue: ApiApprovedPremisesApplicationStatus): ApprovedPremisesApplicationStatus =
      ApprovedPremisesApplicationStatus.entries.first { it.apiValue == apiValue }
  }
}
