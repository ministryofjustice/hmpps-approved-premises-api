package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchAttributes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas3BookingRepository
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.containsNone
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.countOverlappingDays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNameFromPersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.tryGetDetails
import java.time.LocalDate
import java.util.UUID

@Service
class Cas3BedspaceSearchService(
  private val bedSearchRepository: BedSearchRepository,
  private val cas3BookingRepository: Cas3BookingRepository,
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

    if ((searchParams.bedspaceFilters != null || searchParams.premisesFilters != null) && searchParams.attributes != null) {
      return CasResult.GeneralValidationError("Cannot use both filters and attributes")
    }

    val candidateResults =
      bedSearchRepository.findTemporaryAccommodationBeds(
        probationDeliveryUnits = probationDeliveryUnitIds,
        startDate = searchParams.startDate,
        endDate = endDate,
        probationRegionId = user.probationRegion.id,
      )

    val bedIds = candidateResults.map { it.bedId }
    val bedsWithABookingInTurnaround = cas3BookingRepository.findClosestBookingBeforeDateForBeds(searchParams.startDate, bedIds)
      .filter { workingDayService.addWorkingDays(it.departureDate, it.turnaround?.workingDayCount ?: 0) >= searchParams.startDate }
      .map { it.bed!!.id }

    val results = candidateResults.filter { !bedsWithABookingInTurnaround.contains(it.bedId) }

    val distinctIds = results.map { it.premisesId }.distinct()
    val overlappedBookings = cas3BookingRepository.findAllNotCancelledByPremisesIdsAndOverlappingDate(distinctIds, searchParams.startDate, endDate)
    val crns = overlappedBookings.map { it.crn }.distinct().toSet()
    val offenderSummaries = offenderService.getPersonSummaryInfoResults(
      crns = crns.toSet(),
      laoStrategy = user.cas3LaoStrategy(),
    )

    val groupedOverlappedBookings = overlappedBookings
      .map { transformBookingToOverlap(it, searchParams.startDate, endDate, offenderSummaries.forCrn(it.crn)) }
      .groupBy { it.premisesId }

    results.forEach {
      val overlappingBookings = groupedOverlappedBookings[it.premisesId]?.toList() ?: listOf()
      it.overlaps.addAll(overlappingBookings)
    }

    return success(applySearchFilters(searchParams, results))
  }

  /*
  this temporally adds new filter functionality to the search results and to unblock the UI whilst we await confirmation from central team on how
  bed search should work. This will be removed and be added to the query added earlier in the search process to improve performance.
   */
  private fun applySearchFilters(
    searchParams: TemporaryAccommodationBedSearchParameters,
    results: List<Cas3BedSearchResult>,
  ): List<Cas3BedSearchResult> {
    // use the legacy filters until the UI switches over.
    val attributes = searchParams.attributes
    if (attributes != null) {
      return applyLegacyFilters(attributes, results)
    }

    if (searchParams.bedspaceFilters == null && searchParams.premisesFilters == null) {
      return results
    }

    val characteristicPropertyNames =
      characteristicService.getCas3Characteristics().associateBy({ it.id }, { it.propertyName })

    // we will use IDs in the query in future refactoring, but for now it only returns names, so map IDs to characteristic propertyNames.
    val roomCharacteristicsToInclude =
      searchParams.bedspaceFilters?.includedCharacteristicIds?.map { characteristicPropertyNames[it]!! } ?: emptyList()
    val roomCharacteristicsToExclude =
      searchParams.bedspaceFilters?.excludedCharacteristicIds?.map { characteristicPropertyNames[it]!! } ?: emptyList()
    val premisesCharacteristicsToInclude =
      searchParams.premisesFilters?.includedCharacteristicIds?.map { characteristicPropertyNames[it]!! } ?: emptyList()
    val premisesCharacteristicsToExclude =
      searchParams.premisesFilters?.excludedCharacteristicIds?.map { characteristicPropertyNames[it]!! } ?: emptyList()

    return results
      .filter { result ->
        val premisesCharacteristics = result.premisesCharacteristics.map { it.propertyName }
        premisesCharacteristics.containsAll(premisesCharacteristicsToInclude) &&
          premisesCharacteristics.containsNone(premisesCharacteristicsToExclude)
      }.filter { result ->
        val roomCharacteristics = result.roomCharacteristics.map { it.propertyName }
        roomCharacteristics.containsAll(roomCharacteristicsToInclude) &&
          roomCharacteristics.containsNone(roomCharacteristicsToExclude)
      }
  }

  @Deprecated("adding to maintain functionality until the UI switches to use search filters")
  private fun applyLegacyFilters(
    attributes: List<BedSearchAttributes>,
    results: List<Cas3BedSearchResult>,
  ): List<Cas3BedSearchResult> = results
    .filter { bedspace ->
      !attributes.contains(BedSearchAttributes.WHEELCHAIR_ACCESSIBLE) ||
        bedspace.roomCharacteristics.any { it.propertyName == BedSearchAttributes.WHEELCHAIR_ACCESSIBLE.value }
    }
    .filter { premises ->
      !attributes.contains(BedSearchAttributes.SINGLE_OCCUPANCY) ||
        premises.premisesCharacteristics.any { it.propertyName == BedSearchAttributes.SINGLE_OCCUPANCY.value }
    }
    .filter { premises ->
      !attributes.contains(BedSearchAttributes.SHARED_PROPERTY) ||
        premises.premisesCharacteristics.any { it.propertyName == BedSearchAttributes.SHARED_PROPERTY.value }
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
      isSexualRisk = overlappedBooking.sexualRisk,
    )
  }

  private fun getPersonType(
    personSummaryInfo: PersonSummaryInfoResult,
  ): PersonType = when (personSummaryInfo) {
    is PersonSummaryInfoResult.Success.Full -> PersonType.fullPerson
    is PersonSummaryInfoResult.Success.Restricted -> PersonType.restrictedPerson
    is PersonSummaryInfoResult.NotFound, is PersonSummaryInfoResult.Unknown -> PersonType.unknownPerson
  }

  private fun TemporaryAccommodationBedSearchParameters.calculateEndDate(): LocalDate {
    // Adjust to include the start date in the duration, e.g. 1st January for 1 day should end on the 1st January
    val adjustedDuration = durationDays - 1
    return startDate.plusDays(adjustedDuration)
  }
}
