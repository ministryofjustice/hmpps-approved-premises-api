package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.junit.jupiter.params.provider.Arguments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummaryStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream

data class TestCaseForSpaceBookingSummaryStatus(
  val expectedArrivalDate: LocalDate,
  val actualArrivalDate: LocalDate?,
  val expectedDepartureDate: LocalDate,
  val actualDepartureDate: LocalDate?,
  val nonArrivalConfirmedAtDateTime: LocalDateTime?,
)

class Cas1SpaceBookingSummaryStatusTestHelper {
  private val nowDate: LocalDate = LocalDate.now()
  private val dateWayInTheFuture: LocalDate = nowDate.plusDays(800)

  fun spaceBookingSummaryStatusCases(): Stream<Arguments> {
    return (
      getArrivedCase() +
        getArrivalTodayCase() +
        getOverdueArrivalCases() +
        getArrivalWithin2WeeksCases() +
        getArrivalWithin6WeeksCases() +
        getDepartingTodayCase() +
        getDepartedCase() +
        getOverdueDepartureCases() +
        getDepartingWithin2WeeksCases() +
        getNotArrivedCase()
      ).stream()
  }

  private fun getArrivedCase() = listOf(
    Arguments.of(
      TestCaseForSpaceBookingSummaryStatus(
        expectedArrivalDate = nowDate,
        actualArrivalDate = nowDate,
        expectedDepartureDate = dateWayInTheFuture,
        actualDepartureDate = null,
        nonArrivalConfirmedAtDateTime = null,
      ),
      Cas1SpaceBookingSummaryStatus.ARRIVED,
    ),
  )

  private fun getNotArrivedCase() = listOf(
    Arguments.of(
      TestCaseForSpaceBookingSummaryStatus(
        expectedArrivalDate = nowDate,
        actualArrivalDate = null,
        expectedDepartureDate = dateWayInTheFuture,
        actualDepartureDate = null,
        nonArrivalConfirmedAtDateTime = LocalDateTime.now(),
      ),
      Cas1SpaceBookingSummaryStatus.NOT_ARRIVED,
    ),
  )

  private fun getArrivalTodayCase() = listOf(
    Arguments.of(
      TestCaseForSpaceBookingSummaryStatus(
        expectedArrivalDate = nowDate,
        actualArrivalDate = null,
        expectedDepartureDate = dateWayInTheFuture,
        actualDepartureDate = null,
        nonArrivalConfirmedAtDateTime = null,
      ),
      Cas1SpaceBookingSummaryStatus.ARRIVING_TODAY,
    ),
  )

  private fun getOverdueArrivalCases() = (1L..14L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDate.minusDays(it),
          actualArrivalDate = null,
          expectedDepartureDate = dateWayInTheFuture,
          actualDepartureDate = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.OVERDUE_ARRIVAL,
      )
    }

  private fun getArrivalWithin2WeeksCases() = (1L..14L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDate.plusDays(it),
          actualArrivalDate = null,
          expectedDepartureDate = dateWayInTheFuture,
          actualDepartureDate = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.ARRIVING_WITHIN2_WEEKS,
      )
    }

  private fun getArrivalWithin6WeeksCases() = (15L..42L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDate.plusDays(it),
          actualArrivalDate = null,
          expectedDepartureDate = dateWayInTheFuture,
          actualDepartureDate = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.ARRIVING_WITHIN6_WEEKS,
      )
    }

  private fun getDepartingTodayCase(): List<Arguments> {
    val date1dayAgo = nowDate.minusDays(1)
    return listOf(
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = date1dayAgo,
          actualArrivalDate = date1dayAgo,
          expectedDepartureDate = nowDate,
          actualDepartureDate = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.DEPARTING_TODAY,
      ),
    )
  }

  private fun getDepartedCase(): List<Arguments> {
    val date1dayAgo = nowDate.minusDays(1)
    return listOf(
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = date1dayAgo,
          actualArrivalDate = date1dayAgo,
          expectedDepartureDate = nowDate,
          actualDepartureDate = nowDate,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.DEPARTED,
      ),
    )
  }

  private fun getOverdueDepartureCases() = (1L..14L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDate.minusDays(100),
          actualArrivalDate = nowDate.minusDays(100),
          expectedDepartureDate = nowDate.minusDays(it),
          actualDepartureDate = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.OVERDUE_DEPARTURE,
      )
    }

  private fun getDepartingWithin2WeeksCases() = (1L..14L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDate.minusDays(100),
          actualArrivalDate = nowDate.minusDays(100),
          expectedDepartureDate = nowDate.plusDays(it),
          actualDepartureDate = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.DEPARTING_WITHIN2_WEEKS,
      )
    }
}
