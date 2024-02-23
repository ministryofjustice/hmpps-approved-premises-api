package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity

enum class ApprovedPremisesApplicationStatus {
  STARTED,
  SUBMITTED,
  REJECTED,
  AWAITING_ASSESSMENT,
  UNALLOCATED_ASSESSMENT,
  ASSESSMENT_IN_PROGRESS,

  /**
   * An application has been assessed and a [PlacementRequestEntity] has been created which requires a [BookingEntity].
   *
   * Note - If a [PlacementApplicationEntity] is assessed the application _will not_ enter this state
   * (it will remain as PENDING_PLACEMENT_REQUEST)
   */
  AWAITING_PLACEMENT,

  /**
   * A [BookingEntity] has been created for a [PlacementRequestEntity]
   */
  PLACEMENT_ALLOCATED,
  INAPPLICABLE,
  WITHDRAWN,
  REQUESTED_FURTHER_INFORMATION,

  /**
   * An application has been assessed. Because no arrival date was defined,
   * one or more [PlacementApplicationEntity]s are required
   */
  PENDING_PLACEMENT_REQUEST,
}
