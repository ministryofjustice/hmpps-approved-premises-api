package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.GovUKBankHolidaysApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNextWorkingDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWorkingDay
import java.time.LocalDate

@Service
class WorkingDayCountService(
  private val govUKBankHolidaysApiClient: GovUKBankHolidaysApiClient,
  private val timeService: TimeService,
) {

  val bankHolidays: List<LocalDate> by lazy {
    when (val govUKBankHolidaysResponse = this.govUKBankHolidaysApiClient.getUKBankHolidays()) {
      is ClientResult.Success -> govUKBankHolidaysResponse.body.englandAndWales.events.map { it.date }
      is ClientResult.Failure -> govUKBankHolidaysResponse.throwException()
    }
  }

  fun getWorkingDaysCount(from: LocalDate, to: LocalDate): Int {
    return from.getDaysUntilInclusive(to).filter { it.isWorkingDay(bankHolidays) }.size
  }

  fun getCompleteWorkingDaysFromNowUntil(to: LocalDate): Int {
    return timeService.nowAsLocalDate().getDaysUntilExclusiveEnd(to).filter { it.isWorkingDay(bankHolidays) }.size
  }

  fun addWorkingDays(date: LocalDate, daysToAdd: Int): LocalDate {
    var result = date
    for (i in 0 until daysToAdd) {
      result = result.getNextWorkingDay(bankHolidays)
    }

    return result
  }

  fun nextWorkingDay(date: LocalDate): LocalDate {
    return date.getNextWorkingDay(bankHolidays)
  }
}
