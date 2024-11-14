package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity

fun IntegrationTestBase.givenACas1SpaceBooking(
  crn: String,
  premises: ApprovedPremisesEntity? = null,
  migratedFromBooking: BookingEntity? = null,
  application: ApprovedPremisesApplicationEntity? = null,
): Cas1SpaceBookingEntity {
  val (user) = givenAUser()
  val (placementRequest) = givenAPlacementRequest(
    placementRequestAllocatedTo = user,
    assessmentAllocatedTo = user,
    createdByUser = user,
    application = application,
  )

  return cas1SpaceBookingEntityFactory.produceAndPersist {
    withCrn(crn)
    withPlacementRequest(placementRequest)
    withApplication(placementRequest.application)
    withCreatedBy(user)
    withPremises(premises ?: givenAnApprovedPremises())
    withMigratedFromBooking(migratedFromBooking)
  }
}
