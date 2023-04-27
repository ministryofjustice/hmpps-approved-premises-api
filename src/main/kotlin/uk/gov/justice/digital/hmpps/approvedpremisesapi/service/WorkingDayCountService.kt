package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.GovUKBankHolidaysApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWorkingDay
import java.time.LocalDate

@Service
class WorkingDayCountService(
  private val govUKBankHolidaysApiClient: GovUKBankHolidaysApiClient
) {

  fun getWorkingDaysCount(from: LocalDate, to: LocalDate): Int {
    val bankHolidays = when (val govUKBankHolidaysResponse = govUKBankHolidaysApiClient.getUKBankHolidays()) {
      is ClientResult.Success -> govUKBankHolidaysResponse.body.englandAndWales.events.map { it.date }
      is ClientResult.Failure -> govUKBankHolidaysResponse.throwException()
    }
    return from.getDaysUntilInclusive(to).filter { it.isWorkingDay(bankHolidays) }.size
  }
}
