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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNextWorkingDay
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
        events = listOf(),
      ),
      scotland = CountryBankHolidays(
        division = "scotland",
        events = listOf(),
      ),
      northernIreland = CountryBankHolidays(
        division = "northern-ireland",
        events = listOf(),
      ),
    ),
  )

  @ParameterizedTest(name = "getWorkingDaysCount returns 0 if from and to are the same date = {0} and it is a weekend day")
  @MethodSource("weekendDayProvider")
  fun `getWorkingDaysCount returns 0 if from and to are the same date and it is a weekend day`(
    from: LocalDate,
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
              bunting = true,
            ),
          ),
        ),
        scotland = CountryBankHolidays(
          division = "scotland",
          events = listOf(),
        ),
        northernIreland = CountryBankHolidays(
          division = "northern-ireland",
          events = listOf(),
        ),
      ),
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
              bunting = true,
            ),
            BankHolidayEvent(
              title = "sunny thursday bank holiday",
              date = aThursdayBankHoliday,
              notes = "",
              bunting = true,
            ),
          ),
        ),
        scotland = CountryBankHolidays(
          division = "scotland",
          events = listOf(),
        ),
        northernIreland = CountryBankHolidays(
          division = "northern-ireland",
          events = listOf(),
        ),
      ),
    )

    every {
      mockGovUKBankHolidaysApiClient.getUKBankHolidays()
    } returns bankHolidays

    assertThat(workingDayCountService.getWorkingDaysCount(aMonday, aSunday)).isEqualTo(3)
  }

  @Test
  fun `addWorkingDays returns the given date if the number of days to add is 0`() {
    every {
      mockGovUKBankHolidaysApiClient.getUKBankHolidays()
    } returns emptyBankHolidays

    val saturday = LocalDate.of(2023, 5, 2).with(TemporalAdjusters.next(DayOfWeek.SATURDAY))

    assertThat(workingDayCountService.addWorkingDays(saturday, 0)).isEqualTo(saturday)
  }

  @Test
  fun `addWorkingDays returns the next Monday if the given date is a weekend and the number of days to add is 1`() {
    every {
      mockGovUKBankHolidaysApiClient.getUKBankHolidays()
    } returns emptyBankHolidays

    val saturday = LocalDate.of(2023, 5, 2).with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
    val monday = LocalDate.of(2023, 5, 2).with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    assertThat(workingDayCountService.addWorkingDays(saturday, 1)).isEqualTo(monday)
  }

  @Test
  fun `addWorkingDays correctly handles bank holidays`() {
    val bankHolidays = ClientResult.Success(
      HttpStatus.OK,
      UKBankHolidays(
        englandAndWales = CountryBankHolidays(
          division = "england-and-wales",
          events = listOf(
            BankHolidayEvent(
              title = "Early May bank holiday",
              date = LocalDate.of(2023, 5, 1),
              notes = "",
              bunting = true,
            ),
            BankHolidayEvent(
              title = "Bank holiday for the coronation of King Charles III",
              date = LocalDate.of(2023, 5, 8),
              notes = "",
              bunting = true,
            ),
          ),
        ),
        scotland = CountryBankHolidays(
          division = "scotland",
          events = listOf(),
        ),
        northernIreland = CountryBankHolidays(
          division = "northern-ireland",
          events = listOf(),
        ),
      ),
    )

    every {
      mockGovUKBankHolidaysApiClient.getUKBankHolidays()
    } returns bankHolidays

    val thursday = LocalDate.of(2023, 4, 27)
    val expected = LocalDate.of(2023, 5, 15)

    assertThat(workingDayCountService.addWorkingDays(thursday, 10)).isEqualTo(expected)
  }

  @Test
  fun `getNextWorking day returns next working day`() {
    every {
      mockGovUKBankHolidaysApiClient.getUKBankHolidays()
    } returns emptyBankHolidays

    val date = LocalDate.now()

    assertThat(
      workingDayCountService.nextWorkingDay(date),
    ).isEqualTo(
      date.getNextWorkingDay(workingDayCountService.bankHolidays),
    )
  }
}
