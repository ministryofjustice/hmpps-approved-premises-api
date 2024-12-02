package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedOccupancyBookingEntry
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedOccupancyEntryType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedOccupancyLostBedEntry
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedOccupancyOpenEntry
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedOccupancyRange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBedInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBookingInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarLostBedInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarOccupancyInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive
import java.time.LocalDate

@Component
class CalendarTransformer {
  fun transformDomainToApi(startDate: LocalDate, endDate: LocalDate, domain: Map<CalendarBedInfo, List<CalendarOccupancyInfo>>): List<BedOccupancyRange> {
    return domain.entries.map { bedCalendarInfo ->
      val transformedSchedule = bedCalendarInfo.value.map {
        when (it) {
          is CalendarBookingInfo -> BedOccupancyBookingEntry(
            bookingId = it.bookingId,
            personName = it.personName!!,
            type = BedOccupancyEntryType.BOOKING,
            length = it.startDate.getDaysUntilInclusive(it.endDate).size,
            startDate = it.startDate,
            endDate = it.endDate,
          )
          is CalendarLostBedInfo -> BedOccupancyLostBedEntry(
            lostBedId = it.lostBedId,
            type = BedOccupancyEntryType.LOST_BED,
            length = it.startDate.getDaysUntilInclusive(it.endDate).size,
            startDate = it.startDate,
            endDate = it.endDate,
          )
        }
      }.toMutableList()

      var startOfOpenPeriod: LocalDate? = null
      var endOfOpenPeriod: LocalDate? = null
      startDate.getDaysUntilInclusive(endDate).forEach { dateInRange ->
        val dateIsOpen = bedCalendarInfo.value.none { it.startDate.getDaysUntilInclusive(it.endDate).contains(dateInRange) }

        if (dateIsOpen) {
          if (startOfOpenPeriod == null) {
            startOfOpenPeriod = dateInRange
          }

          endOfOpenPeriod = dateInRange
        }

        if ((!dateIsOpen || dateInRange == endDate) && endOfOpenPeriod != null) {
          transformedSchedule += BedOccupancyOpenEntry(
            type = BedOccupancyEntryType.OPEN,
            length = startOfOpenPeriod!!.getDaysUntilInclusive(endOfOpenPeriod!!).size,
            startDate = startOfOpenPeriod!!,
            endDate = endOfOpenPeriod!!,
          )

          startOfOpenPeriod = null
          endOfOpenPeriod = null
        }
      }

      BedOccupancyRange(
        bedId = bedCalendarInfo.key.bedId,
        bedName = bedCalendarInfo.key.bedName,
        schedule = transformedSchedule,
      )
    }.sortedBy { it.bedName }
  }
}
