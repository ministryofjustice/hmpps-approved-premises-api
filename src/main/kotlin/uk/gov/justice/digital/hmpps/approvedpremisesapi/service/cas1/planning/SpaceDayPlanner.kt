package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import org.springframework.stereotype.Service

@Service
class SpaceBookingDayPlanner {
  /**
   * Given a list of available beds and required bookings for a given day, will attempt to optimally
   * assign bookings to beds to determine a premise's capacity on a given day.
   *
   * We do not currently intend to present the 'booking to room', instead we are only using this planning
   * to determine the number of free spaces after bookings are planned.
   *
   * If we were to present 'booking to room' planning, we may wish to discuss further optimisation
   * with premises e.g. would they want to plan bookings into the fullest multiple occupancy room possible,
   * Or is the goal to minimise shared rooms? What's more important, minimising characteristic surplus, or
   * minimising single bookings into room with the least other beds?
   *
   * Planning goals:
   *
   * <ol>
   *   <li>Priority is given to bookings with constraints, those with the most constraints being planned first</li>
   *   <li>Bookings are placed into rooms with the least surplus characteristics</li>
   *   <li>Single occupancy bookings are never placed in rooms with other bookings</li>
   *   <li>If there are multiple competing rooms (after above goals are satisfied), single occupancy bookings will
   *   be placed into rooms with the least beds (ensuring minimum 'wastage' in these scenarios)</li>
   * </ol>
   *
   * @param availableBeds All beds available on the day being planned (i.e. won't include out of service beds or beds inactive according to start/end date)
   * @param bookings All required bookings on the day being planned. The code assumes that the premise level characteristics have already been met
   */
  fun plan(
    availableBeds: Set<Bed>,
    bookings: Set<SpaceBooking>,
  ): DayPlannerResult {
    val bedLedger = BedLedger(availableBeds)
    val planned = mutableListOf<BedBooking>()
    val unplanned = mutableListOf<UnplannedBooking>()

    val sortedBookings = bookings.toList()
      .sortedByDescending { b -> b.requiredCharacteristics.sumOf { c -> c.weighting } }
      .toMutableList()

    sortedBookings.forEach { booking ->
      val requiresSingleRoom = booking.requiredCharacteristics.any { it.singleRoom }
      val characteristicsExcludingSingleRoom = booking.requiredCharacteristics.filter { !it.singleRoom }.toSet()

      when (
        val findResult = bedLedger.findBed(
          characteristics = characteristicsExcludingSingleRoom,
          requiresSingleRoom = requiresSingleRoom,
        )
      ) {
        is FindBedResult.BedsFound -> {
          findResult.beds.forEach { bed ->
            planned.add(
              BedBooking(
                bed = bed,
                booking = booking,
              ),
            )
            bedLedger.reserve(bed)
          }
        }

        is FindBedResult.BedNotFound -> {
          unplanned.add(
            UnplannedBooking(
              booking = booking,
            ),
          )
        }
      }
    }

    return DayPlannerResult(
      planned = planned.toList(),
      unplanned = unplanned.toSet(),
    )
  }
}

private class BedLedger(initialState: Set<Bed>) {
  private val allBeds = initialState.sortedBy { it.label }.toMutableList()
  private val availableBeds = initialState.sortedBy { it.label }.toMutableList()
  private val reservedBeds = mutableListOf<Bed>()
  private val bedsByRoom = allBeds.groupBy { it.room }

  fun findBed(
    characteristics: Set<Characteristic>,
    requiresSingleRoom: Boolean,
  ): FindBedResult {
    val bedsWithRequiredCharacteristics = findBedsWithRequiredCharacteristics(characteristics, requiresSingleRoom)
    val bedWithLeastSurplusCharacteristics = findBedWithLeastSurplusCharacteristics(bedsWithRequiredCharacteristics, characteristics)

    if (bedWithLeastSurplusCharacteristics == null) {
      return FindBedResult.BedNotFound
    }

    return if (requiresSingleRoom) {
      FindBedResult.BedsFound(bedsByRoom[bedWithLeastSurplusCharacteristics.room]!!.toSet())
    } else {
      FindBedResult.BedsFound(setOf(bedWithLeastSurplusCharacteristics))
    }
  }

  private fun findBedsWithRequiredCharacteristics(
    characteristics: Set<Characteristic>,
    requiresSingleRoom: Boolean,
  ): List<Bed> {
    val bedsWithMatchingCharacteristics = availableBeds
      .filter { availableBed -> availableBed.room.characteristics.containsAll(characteristics) }

    return if (requiresSingleRoom) {
      bedsWithMatchingCharacteristics
        .filter { availableBed -> !isRoomOccupied(availableBed) }
        .sortedBy { availableBed -> bedsByRoom.getOrDefault(availableBed.room, emptyList()).size }
    } else {
      bedsWithMatchingCharacteristics
    }
  }

  private fun findBedWithLeastSurplusCharacteristics(beds: List<Bed>, characteristics: Set<Characteristic>) =
    beds
      .map { bed -> Pair(bed, bed.room.characteristics.minus(characteristics).size) }
      .minByOrNull { it.second }?.first

  fun reserve(bed: Bed) {
    availableBeds.remove(bed)
    reservedBeds.add(bed)
  }

  private fun isRoomOccupied(bed: Bed) = reservedBeds.any { reservedBed -> reservedBed.room == bed.room }
}

sealed interface FindBedResult {
  class BedsFound(val beds: Set<Bed>) : FindBedResult
  data object BedNotFound : FindBedResult
}

data class DayPlannerResult(
  val planned: List<BedBooking>,
  val unplanned: Set<UnplannedBooking>,
)

data class BedBooking(
  val bed: Bed,
  val booking: SpaceBooking,
)

data class UnplannedBooking(
  val booking: SpaceBooking,
)
