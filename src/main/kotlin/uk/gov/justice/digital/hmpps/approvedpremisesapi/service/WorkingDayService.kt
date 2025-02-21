package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.GovUKBankHolidaysApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class WorkingDayService(
  private val bankHolidaysProvider: BankHolidaysProvider,
  private val timeService: TimeService,
) {

  val bankHolidays: List<LocalDate> by lazy {
    bankHolidaysProvider.getUKBankHolidays()
  }

  fun getWorkingDaysCount(from: LocalDate, to: LocalDate): Int = from.getDaysUntilInclusive(to).filter { it.isWorkingDay(bankHolidays) }.size

  fun getCompleteWorkingDaysFromNowUntil(to: LocalDate): Int = timeService.nowAsLocalDate().getDaysUntilExclusiveEnd(to).filter { it.isWorkingDay(bankHolidays) }.size

  @SuppressWarnings("UnusedPrivateProperty")
  fun addWorkingDays(date: LocalDate, daysToAdd: Int): LocalDate {
    var result = date
    for (i in 0 until daysToAdd) {
      result = result.getNextWorkingDay(bankHolidays)
    }

    return result
  }

  fun nextWorkingDay(date: LocalDate): LocalDate = date.getNextWorkingDay(bankHolidays)
}

fun interface BankHolidaysProvider {
  fun getUKBankHolidays(): List<LocalDate>
}

@Service
class GovUkBankHolidaysProvider(
  private val govUKBankHolidaysApiClient: GovUKBankHolidaysApiClient,
) : BankHolidaysProvider {
  override fun getUKBankHolidays() = when (val govUKBankHolidaysResponse = this.govUKBankHolidaysApiClient.getUKBankHolidays()) {
    is ClientResult.Success -> govUKBankHolidaysResponse.body.englandAndWales.events.map { it.date }
    is ClientResult.Failure -> govUKBankHolidaysResponse.throwException()
  }
}

fun LocalDate.isWorkingDay(bankHolidays: List<LocalDate>) = this.dayOfWeek != DayOfWeek.SATURDAY &&
  this.dayOfWeek != DayOfWeek.SUNDAY &&
  !bankHolidays.contains(this)

fun LocalDate.getNextWorkingDay(bankHolidays: List<LocalDate>): LocalDate {
  var result = this.plusDays(1)
  while (!result.isWorkingDay(bankHolidays)) {
    result = result.plusDays(1)
  }

  return result
}
