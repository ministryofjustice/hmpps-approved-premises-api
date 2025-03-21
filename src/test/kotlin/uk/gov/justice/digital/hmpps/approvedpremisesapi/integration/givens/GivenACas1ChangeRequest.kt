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
  val requestReason = cas1ChangeRequestReasonEntityFactory.produceAndPersist()

  return cas1ChangeRequestEntityFactory.produceAndPersist {
    withDecisionMadeByUser(null)
    withType(type)
    withChangeRequestReason(requestReason)
    withDecision(decision)
    withSpaceBooking(spaceBooking)
    withPlacementRequest(spaceBooking.placementRequest!!)
  }
}

/*

        apa.name as name,
        booking.crn as crn,
        cr.type as type,
        cr.created_at as createdAt,
        booking.expected_departure_date - booking.canonical_arrival_date as lengthOfStayDays,
        apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
        booking.canonical_arrival_date as canonicalArrivalDate,
        booking.expected_arrival_date as expectedArrivalDat,
        booking.actual_arrival_date as actualArrivalDat
      FROM cas1_change_requests cr
      INNER JOIN cas1_space_bookings booking on cr.cas1_space_booking_id = booking.id
      INNER JOIN placement_requests pr on cr.placement_request_id = pr.id
      INNER JOIN approved_premises_applications apa on apa.id = pr.application_id
      WHERE (CAST(:cruManagementAreaId AS pg_catalog.uuid) IS NULL) OR apa.cas1_cru_management_area_id = :cruManagementAreaId
 */
