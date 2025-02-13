package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1KeyWorkerAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingNonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingAtPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.SpaceBookingDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.format.DateTimeFormatter

@Component
class Cas1SpaceBookingTransformer(
  private val personTransformer: PersonTransformer,
  private val spaceBookingRequirementsTransformer: Cas1SpaceBookingRequirementsTransformer,
  private val cancellationReasonTransformer: CancellationReasonTransformer,
  private val userTransformer: UserTransformer,
  private val spaceBookingStatusTransformer: Cas1SpaceBookingStatusTransformer,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @SuppressWarnings("UnusedPrivateProperty")
  fun transformJpaToApi(
    person: PersonInfoResult,
    jpa: Cas1SpaceBookingEntity,
    otherBookingsAtPremiseForCrn: List<Cas1SpaceBookingAtPremises>,
  ): Cas1SpaceBooking {
    log.info("transformJpaToApi cas1 space booking")
    val placementRequest = jpa.placementRequest
    val application = jpa.application
    val applicationId = jpa.applicationFacade.id
    val status = Cas1SpaceBookingStatusTransformer().transformToSpaceBookingSummaryStatus(
      SpaceBookingDates(
        jpa.expectedArrivalDate,
        jpa.expectedDepartureDate,
        jpa.actualArrivalDate,
        jpa.actualDepartureDate,
        jpa.nonArrivalConfirmedAt?.toLocalDateTime(),
      ),
    )
    log.info("Force lazy load placement request")
    val force = placementRequest?.assessment

    log.info("building space booking")
    val result = Cas1SpaceBooking(
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
      actualArrivalDate = jpa.actualArrivalAsDateTime(),
      actualArrivalDateOnly = jpa.actualArrivalDate,
      actualArrivalTime = jpa.actualArrivalTime?.format(DateTimeFormatter.ofPattern("HH:mm")),
      actualDepartureDate = jpa.actualDepartureAsDateTime(),
      actualDepartureDateOnly = jpa.actualDepartureDate,
      actualDepartureTime = jpa.actualDepartureTime?.format(DateTimeFormatter.ofPattern("HH:mm")),
      canonicalArrivalDate = jpa.canonicalArrivalDate,
      canonicalDepartureDate = jpa.canonicalDepartureDate,
      otherBookingsInPremisesForCrn = otherBookingsAtPremiseForCrn.map { it.toSpaceBookingDate() },
      cancellation = jpa.extractCancellation(),
      requestForPlacementId = jpa.placementRequest?.placementApplication?.id ?: jpa.placementRequest?.id,
      nonArrival = jpa.extractNonArrival(),
      deliusEventNumber = jpa.deliusEventNumber,
      departure = jpa.extractDeparture(),
      status = status,
      characteristics = jpa.criteria.toCas1SpaceCharacteristics(),
    )
    log.info("returning result")
    return result
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
    return if (staffCode != null && name != null) {
      Cas1KeyWorkerAllocation(
        keyWorker = StaffMember(
          code = staffCode,
          keyWorker = true,
          name = name,
        ),
        allocatedAt = assignedAt?.toLocalDate(),
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

  private fun Cas1SpaceBookingEntity.extractNonArrival(): Cas1SpaceBookingNonArrival? {
    return if (hasNonArrival()) {
      Cas1SpaceBookingNonArrival(
        confirmedAt = nonArrivalConfirmedAt,
        reason = nonArrivalReason!!.let {
          NamedId(
            id = it.id,
            name = it.name,
          )
        },
        notes = nonArrivalNotes,
      )
    } else {
      null
    }
  }

  private fun Cas1SpaceBookingEntity.extractDeparture(): Cas1SpaceBookingDeparture? {
    return if (hasDeparted()) {
      Cas1SpaceBookingDeparture(
        reason = NamedId(departureReason!!.id, departureReason!!.name),
        parentReason = departureReason!!.parentReasonId?.let { NamedId(it.id, it.name) },
        moveOnCategory = departureMoveOnCategory?.let { NamedId(it.id, it.name) },
        notes = departureNotes,
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
        allocatedAt = searchResult.keyWorkerAssignedAt?.toLocalDate(),
        keyWorker = StaffMember(
          code = staffCode,
          keyWorker = true,
          name = searchResult.keyWorkerName!!,
        ),
      )
    },
    status = spaceBookingStatusTransformer.transformToSpaceBookingSummaryStatus(
      SpaceBookingDates(
        searchResult.expectedArrivalDate,
        searchResult.expectedDepartureDate,
        searchResult.actualArrivalDate,
        searchResult.actualDepartureDate,
        searchResult.nonArrivalConfirmedAtDateTime,
      ),
    ),
  )

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun List<CharacteristicEntity>.toCas1SpaceCharacteristics() =
      this.mapNotNull { it.toCas1SpaceCharacteristicOrNull() }

    fun CharacteristicEntity.toCas1SpaceCharacteristicOrNull() =
      Cas1SpaceCharacteristic.entries.find { it.name == propertyName } ?: run {
        log.warn("Couldn't find a Cas1SpaceCharacteristic enum entry for propertyName $propertyName")
        null
      }
  }
}
