package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1BedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository.OutOfServiceBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1PlanningBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.LocalDate
import java.util.UUID

@Service
class SpacePlanningService(
  private val spacePlanningModelsFactory: SpacePlanningModelsFactory,
  private val outOfServiceBedService: Cas1OutOfServiceBedService,
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1BedsRepository: Cas1BedsRepository,
) {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  fun capacity(
    premises: ApprovedPremisesEntity,
    rangeInclusive: DateRange,
    excludeSpaceBookingId: UUID?,
  ) = capacity(listOf(premises), rangeInclusive, excludeSpaceBookingId)[0]

  fun capacity(
    forPremises: List<ApprovedPremisesEntity>,
    rangeInclusive: DateRange,
    excludeSpaceBookingId: UUID?,
  ): List<PremiseCapacitySummary> {
    log.info("Determine capacity for ${forPremises.size} premises between $rangeInclusive")

    val rangeList = rangeInclusive.orderedDatesInRange().toList()

    val allPremisesDayBedStates = bedStatesForEachDay(forPremises, rangeList)
    log.info("Have retrieved ${allPremisesDayBedStates.size} bed states")

    val allPremisesDayBookings = spaceBookingsForEachDay(forPremises, rangeList, excludeSpaceBookingId)
    log.info("Have retrieved ${allPremisesDayBookings.size} bookings")

    return forPremises.map { premises ->
      calculatePremisesCapacity(
        premises = premises,
        rangeInclusive = rangeInclusive,
        premisesDayBedStates = allPremisesDayBedStates.forPremises(premises),
        premisesDayBookings = allPremisesDayBookings.forPremises(premises),
      )
    }
  }

  private fun calculatePremisesCapacity(
    premises: ApprovedPremisesEntity,
    rangeInclusive: DateRange,
    premisesDayBedStates: PremisesDayBedStates,
    premisesDayBookings: PremisesDayBookings,
  ): PremiseCapacitySummary {
    val capacityForEachDay = rangeInclusive.orderedDatesInRange().map { day ->
      log.info("Calculating capacity for day $day")

      val bedStates = premisesDayBedStates.forDay(day)
      val bookings = premisesDayBookings.forDay(day)

      val availableBeds = bedStates.findActive()
      PremiseCapacityForDay(
        day = day,
        totalBedCount = bedStates.size,
        availableBedCount = availableBeds.size,
        bookingCount = bookings.size,
        characteristicAvailability = Cas1SpaceBookingEntity.ROOM_CHARACTERISTICS_OF_INTEREST.map {
          determineCharacteristicAvailability(
            characteristicPropertyName = it,
            availableBeds = availableBeds,
            bookings = bookings,
          )
        },
      )
    }.toList()

    return PremiseCapacitySummary(
      premise = premises,
      range = rangeInclusive,
      byDay = capacityForEachDay,
    )
  }

  private fun determineCharacteristicAvailability(
    characteristicPropertyName: String,
    availableBeds: List<BedDayState>,
    bookings: List<Cas1SpaceBookingEntity>,
  ) = PremiseCharacteristicAvailability(
    characteristicPropertyName = characteristicPropertyName,
    availableBedCount = availableBeds.count { it.bed.characteristicsPropertyNames.contains(characteristicPropertyName) },
    bookingCount = bookings.count { booking -> booking.criteria.any { it.propertyName == characteristicPropertyName } },
  )

  private fun bedStatesForEachDay(
    forPremises: List<ApprovedPremisesEntity>,
    orderedRange: List<LocalDate>,
  ): List<PremisesDayBedStates> {
    val allPremisesIds = forPremises.map { it.id }
    val allPremisesOosbRecords = outOfServiceBedService.getActiveOutOfServiceBedsForPremisesIds(allPremisesIds)
    log.info("Processing ${allPremisesOosbRecords.size} OOSB records")
    val allPremisesBeds = cas1BedsRepository.bedSummary(allPremisesIds)
    log.info("Processing ${allPremisesBeds.size} Beds")

    return forPremises.map { premises ->
      log.info("Loading bed state for premises ${premises.name}")

      val premisesBeds = allPremisesBeds.bedsForPremises(premises)
      val premisesOosbRecords = allPremisesOosbRecords.oosbForPremises(premises)

      PremisesDayBedStates(
        premises = premises,
        dayBedStates = orderedRange.map { day ->
          DayBedStates(
            date = day,
            bedStates = spacePlanningModelsFactory.allBedsDayState(
              day = day,
              beds = premisesBeds,
              outOfServiceBedRecordsToConsider = premisesOosbRecords,
            ),
          )
        }.toList(),
      )
    }
  }

  private fun spaceBookingsForEachDay(
    forPremises: List<ApprovedPremisesEntity>,
    orderedRange: List<LocalDate>,
    excludeSpaceBookingId: UUID? = null,
  ): List<PremisesDayBookings> {
    val allPremisesBookingsInRange = spaceBookingRepository.findNonCancelledBookingsInRange(
      premisesIds = forPremises.map { it.id },
      rangeStartInclusive = orderedRange.first(),
      rangeEndInclusive = orderedRange.last(),
    ).filter { it.id != excludeSpaceBookingId }

    return forPremises.map { premises ->
      log.info("Loading bookings for premises ${premises.name}")

      val premisesBookingsInRange = allPremisesBookingsInRange.bookingsForPremises(premises)

      PremisesDayBookings(
        premises = premises,
        dayBookings = orderedRange.map { day ->
          DayBookings(
            date = day,
            bookings = spacePlanningModelsFactory.spaceBookingsForDay(
              day = day,
              spaceBookingsToConsider = premisesBookingsInRange,
            ),
          )
        }.toList(),
      )
    }
  }

  data class PremiseCapacitySummary(
    val premise: PremisesEntity,
    val range: DateRange,
    val byDay: List<PremiseCapacityForDay>,
  )

  data class PremiseCapacityForDay(
    val day: LocalDate,
    val totalBedCount: Int,
    val availableBedCount: Int,
    val bookingCount: Int,
    val characteristicAvailability: List<PremiseCharacteristicAvailability>,
  ) {
    fun isPremiseOverbooked(): Boolean = isPremisesCapacityOverbooked() || characteristicAvailability.any { it.isCharacteristicOverbooked() }

    private fun isPremisesCapacityOverbooked(): Boolean = bookingCount > availableBedCount
  }

  data class PremiseCharacteristicAvailability(
    val characteristicPropertyName: String,
    val availableBedCount: Int,
    val bookingCount: Int,
  ) {

    fun isCharacteristicOverbooked(): Boolean = bookingCount > availableBedCount
  }

  private data class PremisesDayBedStates(
    val premises: ApprovedPremisesEntity,
    val dayBedStates: List<DayBedStates>,
  ) {
    fun forDay(day: LocalDate) = dayBedStates.first { it.date == day }.bedStates
  }

  private fun List<PremisesDayBedStates>.forPremises(premises: ApprovedPremisesEntity) = this.first { it.premises.id == premises.id }

  private data class DayBedStates(
    val date: LocalDate,
    val bedStates: List<BedDayState>,
  )

  private fun List<BedDayState>.findActive() = this.filter { it.isActive() }

  private data class PremisesDayBookings(
    val premises: ApprovedPremisesEntity,
    val dayBookings: List<DayBookings>,
  ) {
    fun forDay(day: LocalDate) = dayBookings.first { it.date == day }.bookings
  }

  private fun List<PremisesDayBookings>.forPremises(premises: ApprovedPremisesEntity) = this.first { it.premises.id == premises.id }

  private data class DayBookings(
    val date: LocalDate,
    val bookings: List<Cas1SpaceBookingEntity>,
  )

  private fun List<Cas1SpaceBookingEntity>.bookingsForPremises(premises: ApprovedPremisesEntity) = filter { it.premises.id == premises.id }
  private fun List<Cas1PlanningBedSummary>.bedsForPremises(premises: ApprovedPremisesEntity) = filter { it.premisesId == premises.id }
  private fun List<OutOfServiceBedSummary>.oosbForPremises(premises: ApprovedPremisesEntity) = filter { it.getPremisesId() == premises.id }
}
