package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1KeyWorkerAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingAtPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate

@Component
class Cas1SpaceBookingTransformer(
  private val personTransformer: PersonTransformer,
  private val spaceBookingRequirementsTransformer: Cas1SpaceBookingRequirementsTransformer,
  private val cancellationReasonTransformer: CancellationReasonTransformer,
  private val userTransformer: UserTransformer,
) {
  fun transformJpaToApi(
    person: PersonInfoResult,
    jpa: Cas1SpaceBookingEntity,
    otherBookingsAtPremiseForCrn: List<Cas1SpaceBookingAtPremises>,
  ): Cas1SpaceBooking {
    val placementRequest = jpa.placementRequest
    val application = jpa.application
    val applicationId = application?.id ?: jpa.offlineApplication!!.id
    return Cas1SpaceBooking(
      id = jpa.id,
      applicationId = applicationId,
      assessmentId = placementRequest?.assessment?.id,
      person = personTransformer.transformModelToPersonApi(person),
      requirements = spaceBookingRequirementsTransformer.transformJpaToApi(
        cas1SpaceBookingEntity = jpa,
      ),
      premises = NamedId(
        id = jpa.premises.id,
        name = jpa.premises.name,
      ),
      apArea = jpa.premises.probationRegion.apArea!!.let {
        NamedId(
          id = it.id,
          name = it.name,
        )
      },
      bookedBy = jpa.createdBy?.let { userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) },
      expectedArrivalDate = jpa.expectedArrivalDate,
      expectedDepartureDate = jpa.expectedDepartureDate,
      createdAt = jpa.createdAt.toInstant(),
      tier = application?.riskRatings?.tier?.value?.level,
      keyWorkerAllocation = jpa.extractKeyWorkerAllocation(),
      actualArrivalDate = jpa.actualArrivalDateTime,
      actualDepartureDate = jpa.actualDepartureDateTime,
      canonicalArrivalDate = jpa.canonicalArrivalDate,
      canonicalDepartureDate = jpa.canonicalDepartureDate,
      otherBookingsInPremisesForCrn = otherBookingsAtPremiseForCrn.map { it.toSpaceBookingDate() },
      cancellation = jpa.extractCancellation(),
      requestForPlacementId = jpa.placementRequest?.placementApplication?.id ?: jpa.placementRequest?.id,
      deliusEventNumber = jpa.deliusEventNumber,
    )
  }

  private fun Cas1SpaceBookingAtPremises.toSpaceBookingDate() =
    Cas1SpaceBookingDates(
      id = this.id,
      canonicalArrivalDate = this.canonicalArrivalDate,
      canonicalDepartureDate = this.canonicalDepartureDate,
    )

  private fun Cas1SpaceBookingEntity.extractKeyWorkerAllocation(): Cas1KeyWorkerAllocation? {
    val staffCode = keyWorkerStaffCode
    val name = keyWorkerName
    val assignedAt = keyWorkerAssignedAt
    return if (staffCode != null && name != null && assignedAt != null) {
      Cas1KeyWorkerAllocation(
        keyWorker = StaffMember(
          code = staffCode,
          keyWorker = true,
          name = name,
        ),
        allocatedAt = assignedAt.toLocalDate(),
      )
    } else {
      null
    }
  }

  private fun Cas1SpaceBookingEntity.extractCancellation(): Cas1SpaceBookingCancellation? {
    val occurredAt = cancellationOccurredAt
    val recordedAt = cancellationRecordedAt
    val reason = cancellationReason
    return if (occurredAt != null && recordedAt != null && reason != null) {
      Cas1SpaceBookingCancellation(
        occurredAt = occurredAt,
        recordedAt = recordedAt,
        reason = cancellationReasonTransformer.transformJpaToApi(reason),
        reasonNotes = cancellationReasonNotes,
      )
    } else {
      null
    }
  }

  fun transformSearchResultToSummary(
    searchResult: Cas1SpaceBookingSearchResult,
    personSummaryInfo: PersonSummaryInfoResult,
  ) = Cas1SpaceBookingSummary(
    id = searchResult.id,
    person = personTransformer.personSummaryInfoToPersonSummary(personSummaryInfo),
    canonicalArrivalDate = searchResult.canonicalArrivalDate,
    canonicalDepartureDate = searchResult.canonicalDepartureDate,
    tier = searchResult.tier,
    keyWorkerAllocation = searchResult.keyWorkerStaffCode?.let { staffCode ->
      Cas1KeyWorkerAllocation(
        allocatedAt = searchResult.keyWorkerAssignedAt!!.toLocalDate(),
        keyWorker = StaffMember(
          code = staffCode,
          keyWorker = true,
          name = searchResult.keyWorkerName!!,
        ),
      )
    },
  )
}
