package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2OverlapBookingsSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.Cas3BedspaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.Cas3v2CandidateBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.Cas3v2CandidateBedspaceOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS3_PROPERTY_NAME_MEN_ONLY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS3_PROPERTY_NAME_WOMEN_ONLY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.forCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
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
class Cas3v2BedspaceSearchService(
  private val cas3BedspaceSearchRepository: Cas3BedspaceSearchRepository,
  private val cas3v2BookingRepository: Cas3v2BookingRepository,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val characteristicService: CharacteristicService,
  private val workingDayService: WorkingDayService,
  private val offenderService: OffenderService,
) {
  companion object {
    const val MAX_NUMBER_PDUS = 5
  }

  @Suppress("detekt:CyclomaticComplexMethod")
  fun searchBedspaces(
    user: UserEntity,
    searchParams: Cas3BedspaceSearchParameters,
  ): CasResult<List<Cas3v2CandidateBedspace>> = validatedCasResult {
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

    val candidateResults =
      cas3BedspaceSearchRepository.searchBedspaces(
        probationDeliveryUnits = probationDeliveryUnitIds,
        startDate = searchParams.startDate,
        endDate = endDate,
        probationRegionId = user.probationRegion.id,
      )

    val bedspaceIds = candidateResults.map { it.bedspaceId }
    val bedspacesWithABookingInTurnaround = cas3v2BookingRepository.findClosestBookingBeforeDateForBedspaces(searchParams.startDate, bedspaceIds)
      .filter { workingDayService.addWorkingDays(it.departureDate, it.turnaround?.workingDayCount ?: 0) >= searchParams.startDate }
      .map { it.bedspace.id }

    val results = candidateResults.filter { !bedspacesWithABookingInTurnaround.contains(it.bedspaceId) }

    val distinctIds = results.map { it.premisesId }.distinct()
    val overlappedBookings = cas3v2BookingRepository.findAllNotCancelledByPremisesIdsAndOverlappingDate(distinctIds, searchParams.startDate, endDate)
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
  This temporally adds new filter functionality to the search results and to unblock the UI whilst we await confirmation from the central team on how
  bed search should work. This will be removed and be added to the query added earlier in the search process to improve performance.
   */
  private fun applySearchFilters(
    searchParams: Cas3BedspaceSearchParameters,
    results: List<Cas3v2CandidateBedspace>,
  ): List<Cas3v2CandidateBedspace> {
    if (searchParams.bedspaceFilters == null && searchParams.premisesFilters == null) {
      return results
    }

    val bedspaceCharacteristicNames =
      characteristicService.getCas3BedspaceCharacteristics().associateBy({ it.id }, { it.name })

    // we will use IDs in the query in future refactoring, but for now it only returns names, so map IDs to characteristic propertyNames.
    val bedspaceCharacteristicsToInclude =
      searchParams.bedspaceFilters?.includedCharacteristicIds?.map { bedspaceCharacteristicNames[it]!! } ?: emptyList()
    val bedspaceCharacteristicsToExclude =
      searchParams.bedspaceFilters?.excludedCharacteristicIds?.map { bedspaceCharacteristicNames[it]!! } ?: emptyList()

    val premisesCharacteristicNames =
      characteristicService.getCas3PremisesCharacteristics().associateBy({ it.id }, { it.name })

    var premisesCharacteristicsToInclude =
      searchParams.premisesFilters?.includedCharacteristicIds?.map { premisesCharacteristicNames[it]!! } ?: emptyList()
    val premisesCharacteristicsToExclude =
      searchParams.premisesFilters?.excludedCharacteristicIds?.map { premisesCharacteristicNames[it]!! } ?: emptyList()

    val excludePremisesIds = when {
      premisesCharacteristicsToInclude.contains(CAS3_PROPERTY_NAME_MEN_ONLY) -> {
        // remove isMenOnly characteristic to ensure it is not filtered by later
        premisesCharacteristicsToInclude = premisesCharacteristicsToInclude.filter { it != CAS3_PROPERTY_NAME_MEN_ONLY }

        // filter properties that have women only characteristic or have female occupants to exclude them
        premisesIdsNotSuitableForGender(results, CAS3_PROPERTY_NAME_WOMEN_ONLY, "female")
      }

      premisesCharacteristicsToInclude.contains(CAS3_PROPERTY_NAME_WOMEN_ONLY) -> {
        // remove isWomenOnly characteristic to ensure it is not filtered by later
        premisesCharacteristicsToInclude = premisesCharacteristicsToInclude.filter { it != CAS3_PROPERTY_NAME_WOMEN_ONLY }

        // filter properties that have men only characteristic or have man occupants to exclude them
        premisesIdsNotSuitableForGender(results, CAS3_PROPERTY_NAME_MEN_ONLY, "male")
      }

      else -> emptyList()
    }

    return results
      .filter { result ->
        val premisesCharacteristics = result.premisesCharacteristics.map { it.name }
        premisesCharacteristics.containsAll(premisesCharacteristicsToInclude) &&
          premisesCharacteristics.containsNone(premisesCharacteristicsToExclude)
      }.filter { result ->
        val bedspaceCharacteristics = result.bedspaceCharacteristics.map { it.name }
        bedspaceCharacteristics.containsAll(bedspaceCharacteristicsToInclude) &&
          bedspaceCharacteristics.containsNone(bedspaceCharacteristicsToExclude)
      }.filter { result ->
        !excludePremisesIds.contains(result.premisesId)
      }
  }

  fun transformBookingToOverlap(
    overlappedBooking: Cas3v2OverlapBookingsSearchResult,
    startDate: LocalDate,
    endDate: LocalDate,
    personSummaryInfo: PersonSummaryInfoResult,
  ): Cas3v2CandidateBedspaceOverlap {
    val queryDuration = startDate..endDate
    val bookingDuration = overlappedBooking.arrivalDate..overlappedBooking.departureDate

    return Cas3v2CandidateBedspaceOverlap(
      name = getNameFromPersonSummaryInfoResult(personSummaryInfo),
      crn = overlappedBooking.crn,
      personType = getPersonType(personSummaryInfo),
      sex = personSummaryInfo.tryGetDetails { it.gender },
      days = bookingDuration countOverlappingDays queryDuration,
      premisesId = overlappedBooking.premisesId,
      bedspaceId = overlappedBooking.bedspaceId,
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

  private fun Cas3BedspaceSearchParameters.calculateEndDate(): LocalDate {
    // Adjust to include the start date in the duration, e.g. 1st January for 1 day should end on the 1st January
    val adjustedDuration = durationDays - 1
    return startDate.plusDays(adjustedDuration)
  }

  private fun premisesIdsNotSuitableForGender(
    bedspaces: List<Cas3v2CandidateBedspace>,
    excludeCharacteristic: String,
    excludeOverlapsGender: String,
  ) = bedspaces
    .asSequence()
    .filter { bedspace ->
      bedspace.premisesCharacteristics.any { it.name == excludeCharacteristic } ||
        bedspace.overlaps.any { it.sex?.lowercase() == excludeOverlapsGender.lowercase() }
    }
    .map { it.premisesId }
    .toSet()
}
