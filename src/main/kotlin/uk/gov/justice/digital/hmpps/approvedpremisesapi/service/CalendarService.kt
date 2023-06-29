package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBedInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBookingInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarOccupancyInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import java.time.LocalDate
import java.util.UUID

@Service
class CalendarService(
  private val offenderService: OffenderService,
  private val calendarRepository: CalendarRepository,
) {
  fun getCalendarInfo(user: UserEntity, premisesId: UUID, startDate: LocalDate, endDate: LocalDate): Map<CalendarBedInfo, List<CalendarOccupancyInfo>> {
    val calendarInfo = calendarRepository.getCalendarInfo(premisesId, startDate, endDate)

    calendarInfo.entries.forEach { bed ->
      bed.value.forEach {
        if (it is CalendarBookingInfo) {
          val offenderResult = offenderService.getOffenderByCrn(it.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

          it.personName = when (offenderResult) {
            is AuthorisableActionResult.NotFound -> "Unknown"
            is AuthorisableActionResult.Success -> "${offenderResult.entity.firstName} ${offenderResult.entity.surname}"
            is AuthorisableActionResult.Unauthorised -> "LAO Offender"
          }
        }
      }
    }

    return calendarInfo
  }
}
