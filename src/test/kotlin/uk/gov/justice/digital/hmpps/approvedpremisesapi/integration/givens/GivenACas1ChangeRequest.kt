package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType

fun IntegrationTestBase.givenACas1ChangeRequest(
  type: ChangeRequestType,
  decision: ChangeRequestDecision? = null,
  spaceBooking: Cas1SpaceBookingEntity,
): Cas1ChangeRequestEntity {
  val requestReason = cas1ChangeRequestReasonEntityFactory.produceAndPersist {
    withChangeRequestType(type)
  }

  return cas1ChangeRequestEntityFactory.produceAndPersist {
    withDecisionMadeByUser(null)
    withType(type)
    withChangeRequestReason(requestReason)
    withDecision(decision)
    withSpaceBooking(spaceBooking)
    withPlacementRequest(spaceBooking.placementRequest!!)
  }
}
