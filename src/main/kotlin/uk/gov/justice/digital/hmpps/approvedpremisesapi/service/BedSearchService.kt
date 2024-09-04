package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchAttributes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationBedSearchResultOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.countOverlappingDays
import java.time.LocalDate
import java.util.UUID

@Service
class BedSearchService(
  private val bedSearchRepository: BedSearchRepository,
  private val postcodeDistrictRepository: PostcodeDistrictRepository,
  private val characteristicService: CharacteristicService,
  private val bookingRepository: BookingRepository,
  private val workingDayService: WorkingDayService,
) {
  fun findApprovedPremisesBeds(
    user: UserEntity,
    postcodeDistrictOutcode: String,
    maxDistanceMiles: Int,
    startDate: LocalDate,
    durationInDays: Int,
    requiredCharacteristics: List<PlacementCriteria>,
  ): AuthorisableActionResult<ValidatableActionResult<List<ApprovedPremisesBedSearchResult>>> {
    if (!user.hasRole(UserRole.CAS1_MATCHER)) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(
      validated {
        val characteristicErrors = mutableListOf<String>()
        val premisesCharacteristicIds = mutableListOf<UUID>()
        val roomCharacteristicIds = mutableListOf<UUID>()
        val requiredPropertyNames = requiredCharacteristics.map { it.toString() }

        val characteristics = characteristicService.getCharacteristicsByPropertyNames(requiredPropertyNames)

        requiredPropertyNames.forEach { propertyName ->
          val characteristic = characteristics.firstOrNull { it.propertyName == propertyName }
          when {
            characteristic == null -> characteristicErrors += "$propertyName doesNotExist"
            characteristic.matches(ServiceName.approvedPremises.value, "premises") -> premisesCharacteristicIds += characteristic.id
            characteristic.matches(ServiceName.approvedPremises.value, "room") -> roomCharacteristicIds += characteristic.id
            else -> characteristicErrors += "$propertyName scopeInvalid"
          }
        }

        if (characteristicErrors.any()) {
          "$.requiredCharacteristics" hasValidationError characteristicErrors.joinToString(", ")
        }

        postcodeDistrictRepository.findByOutcode(postcodeDistrictOutcode)
          ?: ("$.postcodeDistrictOutcode" hasValidationError "doesNotExist")

        if (durationInDays < 1) {
          "$.durationDays" hasValidationError "mustBeAtLeast1"
        }

        if (maxDistanceMiles < 1) {
          "$.maxDistanceMiles" hasValidationError "mustBeAtLeast1"
        }

        if (validationErrors.any()) {
          return@validated fieldValidationError
        }

        return@validated success(
          bedSearchRepository.findApprovedPremisesBeds(
            postcodeDistrictOutcode = postcodeDistrictOutcode,
            maxDistanceMiles = maxDistanceMiles,
            startDate = startDate,
            durationInDays = durationInDays,
            requiredPremisesCharacteristics = premisesCharacteristicIds,
            requiredRoomCharacteristics = roomCharacteristicIds,
          ),
        )
      },
    )
  }

  fun findTemporaryAccommodationBeds(
    user: UserEntity,
    probationDeliveryUnit: String,
    startDate: LocalDate,
    durationInDays: Int,
    propertyBedAttributes: List<BedSearchAttributes>?,
  ): AuthorisableActionResult<ValidatableActionResult<List<TemporaryAccommodationBedSearchResult>>> {
    return AuthorisableActionResult.Success(
      validated {
        if (durationInDays < 1) {
          "$.durationDays" hasValidationError "mustBeAtLeast1"
        }

        if (validationErrors.any()) {
          return@validated fieldValidationError
        }

        val endDate = startDate.plusDays(durationInDays.toLong() - 1)

        val candidateResults = bedSearchRepository.findTemporaryAccommodationBeds(
          probationDeliveryUnit = probationDeliveryUnit,
          startDate = startDate,
          endDate = endDate,
          probationRegionId = user.probationRegion.id,
          filterBySharedProperty = propertyBedAttributes?.contains(BedSearchAttributes.sharedProperty) ?: false,
          filterBySingleOccupancy = propertyBedAttributes?.contains(BedSearchAttributes.singleOccupancy) ?: false,
        )

        val bedIds = candidateResults.map { it.bedId }
        val bedsWithABookingInTurnaround = bookingRepository.findClosestBookingBeforeDateForBeds(startDate, bedIds)
          .filter { workingDayService.addWorkingDays(it.departureDate, it.turnaround?.workingDayCount ?: 0) >= startDate }
          .map { it.bed!!.id }

        val results = candidateResults.filter { !bedsWithABookingInTurnaround.contains(it.bedId) }

        val distinctIds = results.map { it.premisesId }.distinct()
        val overlappedBookings = bookingRepository.findAllNotCancelledByPremisesIdsAndOverlappingDate(distinctIds, startDate, endDate)
        val groupedOverlappedBookings = overlappedBookings
          .map { transformBookingToOverlap(it, startDate, endDate) }
          .groupBy { it.premisesId }

        results.forEach {
          val overlappingBookings = groupedOverlappedBookings[it.premisesId]?.toList() ?: listOf()
          it.overlaps.addAll(overlappingBookings)
        }

        return@validated success(
          results,
        )
      },
    )
  }

  fun transformBookingToOverlap(booking: BookingEntity, startDate: LocalDate, endDate: LocalDate): TemporaryAccommodationBedSearchResultOverlap {
    val queryDuration = startDate..endDate
    val bookingDuration = booking.arrivalDate..booking.departureDate

    return TemporaryAccommodationBedSearchResultOverlap(
      crn = booking.crn,
      days = bookingDuration countOverlappingDays queryDuration,
      premisesId = booking.premises.id,
      roomId = booking.bed?.room!!.id,
      bookingId = booking.id,
    )
  }
}
