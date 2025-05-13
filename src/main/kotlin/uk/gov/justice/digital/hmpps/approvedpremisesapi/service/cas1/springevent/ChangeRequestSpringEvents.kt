package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity

data class PlacementAppealAccepted(
  val changeRequest: Cas1ChangeRequestEntity,
)

data class PlacementAppealCreated(
  val changeRequest: Cas1ChangeRequestEntity,
  val requestingUser: UserEntity,
)

data class PlacementAppealRejected(
  val changeRequest: Cas1ChangeRequestEntity,
  val rejectingUser: UserEntity,
)

data class PlannedTransferRequestAccepted(
  val changeRequest: Cas1ChangeRequestEntity,
  val requestingUser: UserEntity,
  val newBooking: Cas1SpaceBookingEntity,
)

data class PlannedTransferRequestCreated(
  val changeRequest: Cas1ChangeRequestEntity,
  val requestingUser: UserEntity,
)

data class PlannedTransferRequestRejected(
  val changeRequest: Cas1ChangeRequestEntity,
  val rejectingUser: UserEntity,
)
