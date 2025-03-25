package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import java.time.Instant
import java.time.LocalDate

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenACas1SpaceBooking(
  crn: String,
  premises: ApprovedPremisesEntity? = null,
  application: ApprovedPremisesApplicationEntity? = null,
  deliusEventNumber: String? = null,
  offlineApplication: OfflineApplicationEntity? = null,
  criteria: List<CharacteristicEntity>? = null,
  placementRequest: PlacementRequestEntity? = null,
  expectedArrivalDate: LocalDate = LocalDate.now(),
  canonicalArrivalDate: LocalDate = expectedArrivalDate,
  expectedDepartureDate: LocalDate = LocalDate.now(),
  canonicalDepartureDate: LocalDate = expectedDepartureDate,
  nonArrivalConfirmedAt: Instant? = null,
  cancellationOccurredAt: LocalDate? = null,
  actualArrivalDate: LocalDate? = null,
): Cas1SpaceBookingEntity {
  val (user) = givenAUser()
  val placementRequestToUse = placementRequest ?: if (offlineApplication == null) {
    givenAPlacementRequest(
      placementRequestAllocatedTo = user,
      assessmentAllocatedTo = user,
      createdByUser = user,
      application = application,
    ).first
  } else {
    null
  }

  return cas1SpaceBookingEntityFactory.produceAndPersist {
    withCrn(crn)
    withExpectedArrivalDate(expectedArrivalDate)
    withActualArrivalDate(actualArrivalDate)
    withCanonicalArrivalDate(canonicalArrivalDate)
    withExpectedDepartureDate(expectedDepartureDate)
    withCanonicalDepartureDate(canonicalDepartureDate)
    withPlacementRequest(placementRequestToUse)
    withApplication(placementRequestToUse?.application)
    withOfflineApplication(offlineApplication)
    withCreatedBy(user)
    withDeliusEventNumber(deliusEventNumber)
    withPremises(premises ?: givenAnApprovedPremises())
    withCriteria(criteria?.toMutableList() ?: emptyList<CharacteristicEntity>().toMutableList())
    withNonArrivalConfirmedAt(nonArrivalConfirmedAt)
    withCancellationOccurredAt(cancellationOccurredAt)
  }
}
