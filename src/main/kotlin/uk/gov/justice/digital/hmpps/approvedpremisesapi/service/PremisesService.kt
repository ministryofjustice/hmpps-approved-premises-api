package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Availability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd
import java.time.LocalDate
import java.util.UUID

@Service
class PremisesService(
  private val premisesRepository: PremisesRepository,
  private val lostBedsRepository: LostBedsRepository,
  private val bookingRepository: BookingRepository,
  private val lostBedReasonRepository: LostBedReasonRepository
) {
  fun getAllPremises(): List<PremisesEntity> = premisesRepository.findAll()
  fun getPremises(premisesId: UUID): PremisesEntity? = premisesRepository.findByIdOrNull(premisesId)

  fun getLastBookingDate(premises: PremisesEntity) = bookingRepository.getHighestBookingDate(premises.id)
  fun getLastLostBedsDate(premises: PremisesEntity) = lostBedsRepository.getHighestBookingDate(premises.id)

  fun getAvailabilityForRange(
    premises: PremisesEntity,
    startDate: LocalDate,
    endDate: LocalDate
  ): Map<LocalDate, Availability> {
    if (endDate.isBefore(startDate)) throw RuntimeException("startDate must be before endDate when calculating availability for range")

    val bookings = bookingRepository.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)
    val lostBeds = lostBedsRepository.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)

    return startDate.getDaysUntilExclusiveEnd(endDate).map { date ->
      val bookingsOnDay = bookings.filter { booking -> booking.arrivalDate <= date && booking.departureDate > date }
      val lostBedsOnDay = lostBeds.filter { lostBed -> lostBed.startDate <= date && lostBed.endDate > date }

      Availability(
        date = date,
        pendingBookings = bookingsOnDay.count { it.arrival == null && it.nonArrival == null && it.cancellation == null },
        arrivedBookings = bookingsOnDay.count { it.arrival != null },
        nonArrivedBookings = bookingsOnDay.count { it.nonArrival != null },
        cancelledBookings = bookingsOnDay.count { it.cancellation != null },
        lostBeds = lostBedsOnDay.sumOf { it.numberOfBeds }
      )
    }.associateBy { it.date }
  }

  fun createLostBeds(
    premises: PremisesEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    numberOfBeds: Int,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?
  ): ValidatableActionResult<LostBedsEntity> =
    validated {
      if (endDate.isBefore(startDate)) {
        "$.endDate" hasValidationError "beforeStartDate"
      }

      if (numberOfBeds <= 0) {
        "$.numberOfBeds" hasValidationError "isZero"
      }

      val reason = lostBedReasonRepository.findByIdOrNull(reasonId)
      if (reason == null) {
        "$.reason" hasValidationError "doesNotExist"
      }

      if (validationErrors.any()) {
        return fieldValidationError
      }

      val lostBedsEntity = lostBedsRepository.save(
        LostBedsEntity(
          id = UUID.randomUUID(),
          premises = premises,
          startDate = startDate,
          endDate = endDate,
          numberOfBeds = numberOfBeds,
          reason = reason!!,
          referenceNumber = referenceNumber,
          notes = notes
        )
      )

      return success(lostBedsEntity)
    }

  fun createNewPremises(
    addressLine1: String,
    postcode: String,
    service: String,
    localAuthorityAreaId: UUID,
    name: String,
    notes: String
  ) = validated<PremisesEntity> {

    /**
     * Start of setting up some dummy data to spike the implementation.
     * TODO: This will be removed once it's established how to dynamically get this data
     */
    val apAreaEntity = ApAreaEntity(
      id = UUID.randomUUID(),
      name = "arbitrary_ap_area",
      identifier = "arbitrary_identifier",
      probationRegions = mutableListOf()
    )

    val probationRegion = ProbationRegionEntity(
      id = UUID.fromString("afee0696-8df3-4d9f-9d0c-268f17772e2c"), // Wales in db
      name = "arbitrary_probation_region",
      apArea = apAreaEntity,
      premises = mutableListOf()
    )
    // end of dummy data

    // start of validation
    if (addressLine1.isEmpty()) {
      "$.address" hasValidationError "empty"
    }

    if (postcode.isEmpty()) {
      "$.postcode" hasValidationError "empty"
    }

    if (service.isEmpty()) {
      "$.service" hasValidationError "empty"
    }

    if (localAuthorityAreaId == null) {
      "$.localAuthorityAreaId" hasValidationError "invalid"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }
    // end of validation

    val localAuthorityArea = LocalAuthorityAreaEntity(
      id = localAuthorityAreaId,
      identifier = "arbitrary_identifier",
      name = "arbitrary_local_authority_area",
      premises = mutableListOf()
    )

    val premisesNotes = when (notes.isNullOrEmpty()) {
      true -> ""
      false -> notes
    }

    val premisesName = when (name.isNullOrEmpty()) {
      true -> ""
      false -> name
    }

    val premisesEntity = premisesRepository.save(
      PremisesEntity(
        id = UUID.randomUUID(),
        name = premisesName,
        apCode = "UNKNOWN",
        addressLine1 = addressLine1,
        postcode = postcode,
        deliusTeamCode = "UNKNOWN",
        probationRegion = probationRegion,
        localAuthorityArea = localAuthorityArea,
        bookings = mutableListOf(),
        lostBeds = mutableListOf(),
        service = service,
        notes = premisesNotes,
        totalBeds = 0
      )
    )

    return success(premisesEntity)
  }
}
