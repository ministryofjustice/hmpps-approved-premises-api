package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.Instant
import java.time.LocalDate

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenACas1SpaceBooking(
  crn: String = randomStringUpperCase(6),
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
  actualDepartureDate: LocalDate? = null,
  caseManager: Cas1ApplicationUserDetailsEntity? = null,
  cruManagementArea: Cas1CruManagementAreaEntity? = null,
  transferredFrom: Cas1SpaceBookingEntity? = null,
  keyWorkerStaffCode: String? = null,
  keyWorkerUser: UserEntity? = null,
  additionalInformation: String? = null,
): Cas1SpaceBookingEntity {
  val (user) = givenAUser()
  val placementRequestToUse = placementRequest ?: if (offlineApplication == null) {
    givenAPlacementRequest(
      assessmentAllocatedTo = user,
      createdByUser = user,
      application = application,
      caseManager = caseManager,
      cruManagementArea = cruManagementArea,
    ).first
  } else {
    null
  }

  return cas1SpaceBookingEntityFactory.produceAndPersist {
    withCrn(crn)
    withExpectedArrivalDate(expectedArrivalDate)
    withActualArrivalDate(actualArrivalDate)
    withActualDepartureDate(actualDepartureDate)
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
    withTransferredFrom(transferredFrom)
    withKeyworkerStaffCode(keyWorkerStaffCode)
    withKeyWorkerUser(keyWorkerUser)
    withAdditionalInformation(additionalInformation)
  }
}
