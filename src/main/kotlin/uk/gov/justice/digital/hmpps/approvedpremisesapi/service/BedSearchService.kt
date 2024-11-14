package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchAttributes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OverlapBookingsSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.forCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationBedSearchResultOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BedSearchService.Constants.MAX_NUMBER_PDUS
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.countOverlappingDays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNameFromPersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.tryGetDetails
import java.time.LocalDate
import java.util.UUID

@Service
class BedSearchService(
  private val bedSearchRepository: BedSearchRepository,
  private val postcodeDistrictRepository: PostcodeDistrictRepository,
  private val characteristicService: CharacteristicService,
  private val bookingRepository: BookingRepository,
  private val workingDayService: WorkingDayService,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val offenderService: OffenderService,
) {
  object Constants {
    const val MAX_NUMBER_PDUS = 5
  }

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

        val characteristics = characteristicService.getCharacteristicsByPropertyNames(requiredPropertyNames, ServiceName.approvedPremises)

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

  @Suppress("detekt:CyclomaticComplexMethod")
  fun findTemporaryAccommodationBeds(
    user: UserEntity,
    probationDeliveryUnits: List<UUID>,
    startDate: LocalDate,
    durationInDays: Int,
    propertyBedAttributes: List<BedSearchAttributes>?,
  ): AuthorisableActionResult<ValidatableActionResult<List<TemporaryAccommodationBedSearchResult>>> {
    return AuthorisableActionResult.Success(
      validated {
        val probationDeliveryUnitIds = mutableListOf<UUID>()

        if (durationInDays < 1) {
          "$.durationDays" hasValidationError "mustBeAtLeast1"
        }

        if (probationDeliveryUnits.isEmpty()) {
          "$.probationDeliveryUnits" hasValidationError "empty"
        } else if (probationDeliveryUnits.size > MAX_NUMBER_PDUS) {
          "$.probationDeliveryUnits" hasValidationError "maxNumberProbationDeliveryUnits"
        } else {
          probationDeliveryUnits.mapIndexed { index, id ->
            val probationDeliveryUnitEntityExist = probationDeliveryUnitRepository.existsById(id)
            if (!probationDeliveryUnitEntityExist) {
              "$.probationDeliveryUnits[$index]" hasValidationError "doesNotExist"
            } else {
              probationDeliveryUnitIds.add(id)
            }
          }
        }

        if (validationErrors.any()) {
          return@validated fieldValidationError
        }

        val premisesCharacteristicsNames = propertyBedAttributes?.map {
          when (it) {
            BedSearchAttributes.singleOccupancy -> "Single occupancy"
            BedSearchAttributes.sharedProperty -> "Shared property"
            BedSearchAttributes.wheelchairAccessible -> ""
          }
        }

        val premisesCharacteristicIds = getTemporaryAccommodationCharacteristicsIds(premisesCharacteristicsNames, "premises")

        val roomCharacteristicsNames = when {
          propertyBedAttributes?.contains(BedSearchAttributes.wheelchairAccessible) == true -> listOf("Wheelchair accessible")
          else -> null
        }

        val roomCharacteristicIds = getTemporaryAccommodationCharacteristicsIds(roomCharacteristicsNames, "room")

        val endDate = startDate.plusDays(durationInDays.toLong() - 1)

        val candidateResults = bedSearchRepository.findTemporaryAccommodationBeds(
          probationDeliveryUnits = probationDeliveryUnitIds,
          startDate = startDate,
          endDate = endDate,
          probationRegionId = user.probationRegion.id,
          premisesCharacteristicIds,
          roomCharacteristicIds,
        )

        val bedIds = candidateResults.map { it.bedId }
        val bedsWithABookingInTurnaround = bookingRepository.findClosestBookingBeforeDateForBeds(startDate, bedIds)
          .filter { workingDayService.addWorkingDays(it.departureDate, it.turnaround?.workingDayCount ?: 0) >= startDate }
          .map { it.bed!!.id }

        val results = candidateResults.filter { !bedsWithABookingInTurnaround.contains(it.bedId) }

        val distinctIds = results.map { it.premisesId }.distinct()
        val overlappedBookings = bookingRepository.findAllNotCancelledByPremisesIdsAndOverlappingDate(distinctIds, startDate, endDate)
        val crns = overlappedBookings.map { it.crn }.distinct().toSet()
        val offenderSummaries = offenderService.getPersonSummaryInfoResults(
          crns = crns.toSet(),
          limitedAccessStrategy = user.cas3LimitedAccessStrategy(),
        )

        val groupedOverlappedBookings = overlappedBookings
          .map { transformBookingToOverlap(it, startDate, endDate, offenderSummaries.forCrn(it.crn)) }
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

  fun transformBookingToOverlap(
    overlappedBooking: OverlapBookingsSearchResult,
    startDate: LocalDate,
    endDate: LocalDate,
    personSummaryInfo: PersonSummaryInfoResult,
  ): TemporaryAccommodationBedSearchResultOverlap {
    val queryDuration = startDate..endDate
    val bookingDuration = overlappedBooking.arrivalDate..overlappedBooking.departureDate

    return TemporaryAccommodationBedSearchResultOverlap(
      name = getNameFromPersonSummaryInfoResult(personSummaryInfo),
      crn = overlappedBooking.crn,
      sex = personSummaryInfo.tryGetDetails { it.gender },
      days = bookingDuration countOverlappingDays queryDuration,
      premisesId = overlappedBooking.premisesId,
      roomId = overlappedBooking.roomId,
      bookingId = overlappedBooking.bookingId,
      assessmentId = overlappedBooking.assessmentId,
    )
  }

  private fun getTemporaryAccommodationCharacteristicsIds(characteristicsNames: List<String>?, modelScope: String): List<UUID> {
    return characteristicsNames?.let {
      val characteristics = characteristicService.getCharacteristicsByNames(characteristicsNames)
      characteristics.filter {
        it.isActive && it.matches(ServiceName.temporaryAccommodation.value, modelScope)
      }.map { it.id }.toList()
    } ?: emptyList()
  }
}
