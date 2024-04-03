package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.GovUkBankHolidaysProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TimeService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.getNextWorkingDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.stream.Stream

class WorkingDayServiceTest {

  private val govUkBankHolidaysProvider = mockk<GovUKBankHolidaysApiClient>()
  private val mockTimeService = mockk<TimeService>()

  private val workingDayService = WorkingDayService(
    GovUkBankHolidaysProvider(govUkBankHolidaysProvider),
    mockTimeService,
  )

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
      govUkBankHolidaysProvider.getUKBankHolidays()
    } returns emptyBankHolidays

    assertThat(workingDayService.getWorkingDaysCount(from, from)).isEqualTo(0)
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
      govUkBankHolidaysProvider.getUKBankHolidays()
    } returns bankHolidays

    assertThat(workingDayService.getWorkingDaysCount(weekDayBankHoliday, weekDayBankHoliday)).isEqualTo(0)
  }

  @Test
  fun `getWorkingDaysCount returns 1 if from and to are the same date and it is not a weekend day and it is not a bank holiday`() {
    every {
      govUkBankHolidaysProvider.getUKBankHolidays()
    } returns emptyBankHolidays

    val weekDay = LocalDate.of(2023, 4, 27).with(TemporalAdjusters.next(DayOfWeek.TUESDAY))

    assertThat(workingDayService.getWorkingDaysCount(weekDay, weekDay)).isEqualTo(1)
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
      govUkBankHolidaysProvider.getUKBankHolidays()
    } returns bankHolidays

    assertThat(workingDayService.getWorkingDaysCount(aMonday, aSunday)).isEqualTo(3)
  }

  @Test
  fun `addWorkingDays returns the given date if the number of days to add is 0`() {
    every {
      govUkBankHolidaysProvider.getUKBankHolidays()
    } returns emptyBankHolidays

    val saturday = LocalDate.of(2023, 5, 2).with(TemporalAdjusters.next(DayOfWeek.SATURDAY))

    assertThat(workingDayService.addWorkingDays(saturday, 0)).isEqualTo(saturday)
  }

  @Test
  fun `addWorkingDays returns the next Monday if the given date is a weekend and the number of days to add is 1`() {
    every {
      govUkBankHolidaysProvider.getUKBankHolidays()
    } returns emptyBankHolidays

    val saturday = LocalDate.of(2023, 5, 2).with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
    val monday = LocalDate.of(2023, 5, 2).with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    assertThat(workingDayService.addWorkingDays(saturday, 1)).isEqualTo(monday)
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
      govUkBankHolidaysProvider.getUKBankHolidays()
    } returns bankHolidays

    val thursday = LocalDate.of(2023, 4, 27)
    val expected = LocalDate.of(2023, 5, 15)

    assertThat(workingDayService.addWorkingDays(thursday, 10)).isEqualTo(expected)
  }

  @Test
  fun `getNextWorking day returns next working day`() {
    every {
      govUkBankHolidaysProvider.getUKBankHolidays()
    } returns emptyBankHolidays

    val date = LocalDate.now()

    assertThat(
      workingDayService.nextWorkingDay(date),
    ).isEqualTo(
      date.getNextWorkingDay(workingDayService.bankHolidays),
    )
  }

  @Nested
  inner class GetCompleteWorkingDaysFromNowUntil {

    @Test
    fun `getCompleteWorkingDaysFromNowUntil returns the number of days between two working days with no holidays in between`() {
      every {
        govUkBankHolidaysProvider.getUKBankHolidays()
      } returns emptyBankHolidays

      val aMonday = LocalDate.of(2023, 4, 27).with(TemporalAdjusters.next(DayOfWeek.MONDAY))
      val aFriday = aMonday.with(TemporalAdjusters.next(DayOfWeek.FRIDAY))

      every { mockTimeService.nowAsLocalDate() } returns aMonday

      assertThat(workingDayService.getCompleteWorkingDaysFromNowUntil(aFriday)).isEqualTo(4)
    }

    @Test
    fun `getDifferenceInWorkingDays returns the number of days between two working days with a weekend in between`() {
      every {
        govUkBankHolidaysProvider.getUKBankHolidays()
      } returns emptyBankHolidays

      val aMonday = LocalDate.of(2023, 4, 27).with(TemporalAdjusters.next(DayOfWeek.MONDAY))
      val nextMonday = aMonday.with(TemporalAdjusters.next(DayOfWeek.MONDAY))

      every { mockTimeService.nowAsLocalDate() } returns aMonday

      assertThat(workingDayService.getCompleteWorkingDaysFromNowUntil(nextMonday)).isEqualTo(5)
    }

    @Test
    fun `getDifferenceInWorkingDays returns the number of days between two working days with a bank holiday in between`() {
      val aThursday = LocalDate.of(2023, 4, 27).with(TemporalAdjusters.next(DayOfWeek.THURSDAY))
      val nextTuesday = aThursday.with(TemporalAdjusters.next(DayOfWeek.TUESDAY))

      val aMondayBankHoliday = aThursday.with(TemporalAdjusters.next(DayOfWeek.MONDAY))

      val bankHolidays = ClientResult.Success(
        HttpStatus.OK,
        UKBankHolidays(
          englandAndWales = CountryBankHolidays(
            division = "england-and-wales",
            events = listOf(
              BankHolidayEvent(
                title = "bank holiday monday",
                date = aMondayBankHoliday,
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
        govUkBankHolidaysProvider.getUKBankHolidays()
      } returns bankHolidays

      every { mockTimeService.nowAsLocalDate() } returns aThursday

      assertThat(workingDayService.getCompleteWorkingDaysFromNowUntil(nextTuesday)).isEqualTo(2)
    }

    @Test
    fun `getDifferenceInWorkingDays returns the number of days between two working days with two bank holidays in between`() {
      val aThursday = LocalDate.of(2023, 4, 27).with(TemporalAdjusters.next(DayOfWeek.THURSDAY))
      val nextTuesday = aThursday.with(TemporalAdjusters.next(DayOfWeek.TUESDAY))

      val aFridayBankHoliday = aThursday.with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
      val aMondayBankHoliday = aThursday.with(TemporalAdjusters.next(DayOfWeek.MONDAY))

      val bankHolidays = ClientResult.Success(
        HttpStatus.OK,
        UKBankHolidays(
          englandAndWales = CountryBankHolidays(
            division = "england-and-wales",
            events = listOf(
              BankHolidayEvent(
                title = "good friday",
                date = aFridayBankHoliday,
                notes = "",
                bunting = true,
              ),
              BankHolidayEvent(
                title = "bank holiday monday",
                date = aMondayBankHoliday,
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
        govUkBankHolidaysProvider.getUKBankHolidays()
      } returns bankHolidays

      every { mockTimeService.nowAsLocalDate() } returns aThursday

      assertThat(workingDayService.getCompleteWorkingDaysFromNowUntil(nextTuesday)).isEqualTo(1)
    }
  }
}
