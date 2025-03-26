package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRejectionReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import java.time.OffsetDateTime

fun IntegrationTestBase.givenACas1ChangeRequest(
  type: ChangeRequestType,
  decision: ChangeRequestDecision? = null,
  decisionMadeAt: OffsetDateTime = OffsetDateTime.now(),
  spaceBooking: Cas1SpaceBookingEntity,
  rejectReason: Cas1ChangeRequestRejectionReasonEntity? = null,
): Cas1ChangeRequestEntity {
  val requestReason = cas1ChangeRequestReasonEntityFactory.produceAndPersist {
    withChangeRequestType(type)
  }

  return cas1ChangeRequestEntityFactory.produceAndPersist {
    withDecisionMadeByUser(null)
    withType(type)
    withChangeRequestReason(requestReason)
    withDecision(decision)
    withDecisionMadeAt(decisionMadeAt)
    withSpaceBooking(spaceBooking)
    withPlacementRequest(spaceBooking.placementRequest!!)
    withRejectionReason(rejectReason)
  }
}
