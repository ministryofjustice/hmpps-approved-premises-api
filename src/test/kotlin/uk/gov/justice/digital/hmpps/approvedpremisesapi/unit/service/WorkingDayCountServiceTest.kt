package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.GovUKBankHolidaysApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.bankholidaysapi.BankHolidayEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.bankholidaysapi.CountryBankHolidays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.bankholidaysapi.UKBankHolidays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayCountService
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.stream.Stream

class WorkingDayCountServiceTest {

  private val mockGovUKBankHolidaysApiClient = mockk<GovUKBankHolidaysApiClient>()

  private val workingDayCountService = WorkingDayCountService(mockGovUKBankHolidaysApiClient)

  private val emptyBankHolidays = ClientResult.Success(
    HttpStatus.OK,
    UKBankHolidays(
      englandAndWales = CountryBankHolidays(
        division = "england-and-wales",
        events = listOf()
      ),
      scotland = CountryBankHolidays(
        division = "scotland",
        events = listOf()
      ),
      northernIreland = CountryBankHolidays(
        division = "northern-ireland",
        events = listOf()
      )
    )
  )

  @ParameterizedTest(name = "getWorkingDaysCount returns 0 if from and to are the same date = {0} and it is a weekend day")
  @MethodSource("weekendDayProvider")
  fun `getWorkingDaysCount returns 0 if from and to are the same date and it is a weekend day`(
    from: LocalDate
  ) {
    every {
      mockGovUKBankHolidaysApiClient.getUKBankHolidays()
    } returns emptyBankHolidays

    assertThat(workingDayCountService.getWorkingDaysCount(from, from)).isEqualTo(0)
  }

  @Test
  fun `getWorkingDaysCount returns 0 if from and to are the same date and it is a week day bank holiday`() {

    val weekDayBankHoliday = LocalDate.of(2023, 4, 27).with(TemporalAdjusters.next(DayOfWeek.TUESDAY))

    val bankHolidays = ClientResult.Success(
      HttpStatus.OK,
      UKBankHolidays(
        englandAndWales = CountryBankHolidays(
          division = "england-and-wales",
          events = listOf(
            BankHolidayEvent(
              title = "sunny bank holiday",
              date = weekDayBankHoliday,
              notes = "",
              bunting = true
            )
          )
        ),
        scotland = CountryBankHolidays(
          division = "scotland",
          events = listOf()
        ),
        northernIreland = CountryBankHolidays(
          division = "northern-ireland",
          events = listOf()
        )
      )
    )

    every {
      mockGovUKBankHolidaysApiClient.getUKBankHolidays()
    } returns bankHolidays

    assertThat(workingDayCountService.getWorkingDaysCount(weekDayBankHoliday, weekDayBankHoliday)).isEqualTo(0)
  }

  @Test
  fun `getWorkingDaysCount returns 1 if from and to are the same date and it is not a weekend day and it is not a bank holiday`() {

    every {
      mockGovUKBankHolidaysApiClient.getUKBankHolidays()
    } returns emptyBankHolidays

    val weekDay = LocalDate.of(2023, 4, 27).with(TemporalAdjusters.next(DayOfWeek.TUESDAY))

    assertThat(workingDayCountService.getWorkingDaysCount(weekDay, weekDay)).isEqualTo(1)
  }

  private companion object {
    @JvmStatic
    fun weekendDayProvider(): Stream<Arguments> {
      val from = LocalDate.of(2023, 4, 27)
      return Stream.of(
        Arguments.of(from.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))),
        Arguments.of(from.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))),
      )
    }
  }

  @Test
  fun `getWorkingDaysCount returns 3 for a week period that includes 2 bank holidays`() {
    val aMonday = LocalDate.of(2023, 4, 27).with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    val aSunday = aMonday.plusDays(6)
    val aTuesdayBankHoliday = aMonday.plusDays(1)
    val aThursdayBankHoliday = aMonday.plusDays(3)

    val bankHolidays = ClientResult.Success(
      HttpStatus.OK,
      UKBankHolidays(
        englandAndWales = CountryBankHolidays(
          division = "england-and-wales",
          events = listOf(
            BankHolidayEvent(
              title = "sunny tuesday bank holiday",
              date = aTuesdayBankHoliday,
              notes = "",
              bunting = true
            ),
            BankHolidayEvent(
              title = "sunny thursday bank holiday",
              date = aThursdayBankHoliday,
              notes = "",
              bunting = true
            )
          )
        ),
        scotland = CountryBankHolidays(
          division = "scotland",
          events = listOf()
        ),
        northernIreland = CountryBankHolidays(
          division = "northern-ireland",
          events = listOf()
        )
      )
    )

    every {
      mockGovUKBankHolidaysApiClient.getUKBankHolidays()
    } returns bankHolidays

    assertThat(workingDayCountService.getWorkingDaysCount(aMonday, aSunday)).isEqualTo(3)
  }
}
