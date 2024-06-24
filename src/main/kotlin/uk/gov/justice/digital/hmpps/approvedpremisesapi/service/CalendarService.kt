package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBedInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBookingInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarOccupancyInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNameFromPersonSummaryInfoResult
import java.time.LocalDate
import java.util.UUID

@Service
class CalendarService(
  private val offenderService: OffenderService,
  private val calendarRepository: CalendarRepository,
) {
  fun getCalendarInfo(user: UserEntity, premisesId: UUID, startDate: LocalDate, endDate: LocalDate): Map<CalendarBedInfo, List<CalendarOccupancyInfo>> {
    val calendarInfo = calendarRepository.getCalendarInfo(premisesId, startDate, endDate)

    val crns = calendarInfo.values.flatMap {
      it.filterIsInstance<CalendarBookingInfo>().map { it.crn }
    }.toSet()
    val offenderSummaries = offenderService.getOffenderSummariesByCrns(crns, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    calendarInfo.values.forEach { bedList ->
      bedList.filterIsInstance<CalendarBookingInfo>().forEach { bed ->
        val offenderSummary = offenderSummaries.first { it.crn == bed.crn }
        bed.personName = getNameFromPersonSummaryInfoResult(offenderSummary)
      }
    }

    return calendarInfo
  }
}
