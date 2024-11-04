package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity

fun IntegrationTestBase.`Given a CAS1 Space Booking`(
  crn: String,
  premises: ApprovedPremisesEntity? = null,
  migratedFromBooking: BookingEntity? = null,
): Cas1SpaceBookingEntity {
  val (user) = `Given a User`()
  val (placementRequest) = `Given a Placement Request`(
    placementRequestAllocatedTo = user,
    assessmentAllocatedTo = user,
    createdByUser = user,
  )

  return cas1SpaceBookingEntityFactory.produceAndPersist {
    withCrn(crn)
    withPlacementRequest(placementRequest)
    withApplication(placementRequest.application)
    withCreatedBy(user)
    withPremises(premises ?: `Given an Approved Premises`())
    withMigratedFromBooking(migratedFromBooking)
  }
}
