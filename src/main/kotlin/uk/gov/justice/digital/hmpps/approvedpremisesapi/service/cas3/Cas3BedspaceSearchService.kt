package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchAttributes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OverlapBookingsSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.forCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas3BedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationBedSearchResultOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LimitedAccessStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.countOverlappingDays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNameFromPersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.tryGetDetails
import java.time.LocalDate
import java.util.UUID

@Service
class Cas3BedspaceSearchService(
  private val bedSearchRepository: BedSearchRepository,
  private val bookingRepository: BookingRepository,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val characteristicService: CharacteristicService,
  private val workingDayService: WorkingDayService,
  private val offenderService: OffenderService,
) {
  companion object {
    const val MAX_NUMBER_PDUS = 5
  }

  @Suppress("detekt:CyclomaticComplexMethod")
  fun findBedspaces(
    user: UserEntity,
    searchParams: TemporaryAccommodationBedSearchParameters,
  ): CasResult<List<Cas3BedSearchResult>> = validatedCasResult {
    val probationDeliveryUnitIds = mutableListOf<UUID>()

    if (searchParams.durationDays < 1) {
      "$.durationDays" hasValidationError "mustBeAtLeast1"
    }

    if (searchParams.probationDeliveryUnits.isEmpty()) {
      "$.probationDeliveryUnits" hasValidationError "empty"
    } else if (searchParams.probationDeliveryUnits.size > MAX_NUMBER_PDUS) {
      "$.probationDeliveryUnits" hasValidationError "maxNumberProbationDeliveryUnits"
    } else {
      searchParams.probationDeliveryUnits.mapIndexed { index, id ->
        val probationDeliveryUnitEntityExist = probationDeliveryUnitRepository.existsById(id)
        if (!probationDeliveryUnitEntityExist) {
          "$.probationDeliveryUnits[$index]" hasValidationError "doesNotExist"
        } else {
          probationDeliveryUnitIds.add(id)
        }
      }
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val endDate = searchParams.calculateEndDate()

    val candidateResults = if (searchParams.premisesFilters != null || searchParams.bedspaceFilters != null) {
      return CasResult.GeneralValidationError("Filters not implemented")
    } else {
      val premisesCharacteristicsPropertyNames = searchParams.attributes?.map {
        when (it) {
          BedSearchAttributes.SINGLE_OCCUPANCY, BedSearchAttributes.SHARED_PROPERTY -> it.value
          else -> ""
        }
      }

      val premisesCharacteristicIds = getCharacteristicsIds(premisesCharacteristicsPropertyNames, "premises")

      val roomCharacteristicsPropertyNames = searchParams.attributes?.map {
        when (it) {
          BedSearchAttributes.WHEELCHAIR_ACCESSIBLE -> it.value
          else -> ""
        }
      }

      val roomCharacteristicIds = getCharacteristicsIds(roomCharacteristicsPropertyNames, "room")

      bedSearchRepository.findTemporaryAccommodationBeds(
        probationDeliveryUnits = probationDeliveryUnitIds,
        startDate = searchParams.startDate,
        endDate = endDate,
        probationRegionId = user.probationRegion.id,
        premisesCharacteristicIds,
        roomCharacteristicIds,
      )
    }

    val bedIds = candidateResults.map { it.bedId }
    val bedsWithABookingInTurnaround = bookingRepository.findClosestBookingBeforeDateForBeds(searchParams.startDate, bedIds)
      .filter { workingDayService.addWorkingDays(it.departureDate, it.turnaround?.workingDayCount ?: 0) >= searchParams.startDate }
      .map { it.bed!!.id }

    val results = candidateResults.filter { !bedsWithABookingInTurnaround.contains(it.bedId) }

    val distinctIds = results.map { it.premisesId }.distinct()
    val overlappedBookings = bookingRepository.findAllNotCancelledByPremisesIdsAndOverlappingDate(distinctIds, searchParams.startDate, endDate)
    val crns = overlappedBookings.map { it.crn }.distinct().toSet()
    val offenderSummaries = offenderService.getPersonSummaryInfoResults(
      crns = crns.toSet(),
      limitedAccessStrategy = user.cas3LimitedAccessStrategy(),
    )

    val groupedOverlappedBookings = overlappedBookings
      .map { transformBookingToOverlap(it, searchParams.startDate, endDate, offenderSummaries.forCrn(it.crn)) }
      .groupBy { it.premisesId }

    results.forEach {
      val overlappingBookings = groupedOverlappedBookings[it.premisesId]?.toList() ?: listOf()
      it.overlaps.addAll(overlappingBookings)
    }

    return success(results)
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
      personType = getPersonType(personSummaryInfo),
      sex = personSummaryInfo.tryGetDetails { it.gender },
      days = bookingDuration countOverlappingDays queryDuration,
      premisesId = overlappedBooking.premisesId,
      roomId = overlappedBooking.roomId,
      bookingId = overlappedBooking.bookingId,
      assessmentId = overlappedBooking.assessmentId,
    )
  }

  private fun getPersonType(
    personSummaryInfo: PersonSummaryInfoResult,
  ): PersonType = when (personSummaryInfo) {
    is PersonSummaryInfoResult.Success.Full -> PersonType.fullPerson
    is PersonSummaryInfoResult.Success.Restricted -> PersonType.restrictedPerson
    is PersonSummaryInfoResult.NotFound, is PersonSummaryInfoResult.Unknown -> PersonType.unknownPerson
  }

  private fun getCharacteristicsIds(characteristicsPropertyNames: List<String>?, modelScope: String): List<UUID> {
    if (characteristicsPropertyNames.isNullOrEmpty()) return emptyList()
    return characteristicsPropertyNames.let {
      val characteristics = characteristicService.getCharacteristicsByPropertyNames(characteristicsPropertyNames, ServiceName.temporaryAccommodation)
      characteristics.filter {
        it.isActive && it.matches(ServiceName.temporaryAccommodation.value, modelScope)
      }.map { it.id }.toList()
    }
  }

  private fun TemporaryAccommodationBedSearchParameters.calculateEndDate(): LocalDate {
    // Adjust to include the start date in the duration, e.g. 1st January for 1 day should end on the 1st January
    val adjustedDuration = durationDays - 1
    return startDate.plusDays(adjustedDuration)
  }
}
