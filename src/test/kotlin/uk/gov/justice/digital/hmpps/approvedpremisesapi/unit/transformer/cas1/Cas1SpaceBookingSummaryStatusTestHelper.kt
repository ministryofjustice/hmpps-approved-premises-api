package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.junit.jupiter.params.provider.Arguments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummaryStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream

data class TestCaseForSpaceBookingSummaryStatus(
  val expectedArrivalDate: LocalDate,
  val actualArrivalDateTime: LocalDateTime?,
  val expectedDepartureDate: LocalDate,
  val actualDepartureDateTime: LocalDateTime?,
  val nonArrivalConfirmedAtDateTime: LocalDateTime?,
)

class Cas1SpaceBookingSummaryStatusTestHelper {
  private val nowDateTime: LocalDateTime = LocalDateTime.now()
  private val dateWayInTheFuture: LocalDateTime = nowDateTime.plusDays(800)

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
        expectedArrivalDate = nowDateTime.toLocalDate(),
        actualArrivalDateTime = nowDateTime,
        expectedDepartureDate = dateWayInTheFuture.toLocalDate(),
        actualDepartureDateTime = null,
        nonArrivalConfirmedAtDateTime = null,
      ),
      Cas1SpaceBookingSummaryStatus.arrived,
    ),
  )

  private fun getNotArrivedCase() = listOf(
    Arguments.of(
      TestCaseForSpaceBookingSummaryStatus(
        expectedArrivalDate = nowDateTime.toLocalDate(),
        actualArrivalDateTime = null,
        expectedDepartureDate = dateWayInTheFuture.toLocalDate(),
        actualDepartureDateTime = null,
        nonArrivalConfirmedAtDateTime = nowDateTime,
      ),
      Cas1SpaceBookingSummaryStatus.notArrived,
    ),
  )

  private fun getArrivalTodayCase() = listOf(
    Arguments.of(
      TestCaseForSpaceBookingSummaryStatus(
        expectedArrivalDate = nowDateTime.toLocalDate(),
        actualArrivalDateTime = null,
        expectedDepartureDate = dateWayInTheFuture.toLocalDate(),
        actualDepartureDateTime = null,
        nonArrivalConfirmedAtDateTime = null,
      ),
      Cas1SpaceBookingSummaryStatus.arrivingToday,
    ),
  )

  private fun getOverdueArrivalCases() = (1L..14L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDateTime.toLocalDate().minusDays(it),
          actualArrivalDateTime = null,
          expectedDepartureDate = dateWayInTheFuture.toLocalDate(),
          actualDepartureDateTime = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.overdueArrival,
      )
    }

  private fun getArrivalWithin2WeeksCases() = (1L..14L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDateTime.toLocalDate().plusDays(it),
          actualArrivalDateTime = null,
          expectedDepartureDate = dateWayInTheFuture.toLocalDate(),
          actualDepartureDateTime = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.arrivingWithin2Weeks,
      )
    }

  private fun getArrivalWithin6WeeksCases() = (15L..42L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDateTime.toLocalDate().plusDays(it),
          actualArrivalDateTime = null,
          expectedDepartureDate = dateWayInTheFuture.toLocalDate(),
          actualDepartureDateTime = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.arrivingWithin6Weeks,
      )
    }

  private fun getDepartingTodayCase(): List<Arguments> {
    val date1dayAgo = nowDateTime.minusDays(1)
    return listOf(
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = date1dayAgo.toLocalDate(),
          actualArrivalDateTime = date1dayAgo,
          expectedDepartureDate = nowDateTime.toLocalDate(),
          actualDepartureDateTime = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.departingToday,
      ),
    )
  }

  private fun getDepartedCase(): List<Arguments> {
    val date1dayAgo = nowDateTime.minusDays(1)
    return listOf(
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = date1dayAgo.toLocalDate(),
          actualArrivalDateTime = date1dayAgo,
          expectedDepartureDate = nowDateTime.toLocalDate(),
          actualDepartureDateTime = nowDateTime,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.departed,
      ),
    )
  }

  private fun getOverdueDepartureCases() = (1L..14L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDateTime.minusDays(100).toLocalDate(),
          actualArrivalDateTime = nowDateTime.minusDays(100),
          expectedDepartureDate = nowDateTime.minusDays(it).toLocalDate(),
          actualDepartureDateTime = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.overdueDeparture,
      )
    }

  private fun getDepartingWithin2WeeksCases() = (1L..14L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDateTime.minusDays(100).toLocalDate(),
          actualArrivalDateTime = nowDateTime.minusDays(100),
          expectedDepartureDate = nowDateTime.plusDays(it).toLocalDate(),
          actualDepartureDateTime = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.departingWithin2Weeks,
      )
    }
}
