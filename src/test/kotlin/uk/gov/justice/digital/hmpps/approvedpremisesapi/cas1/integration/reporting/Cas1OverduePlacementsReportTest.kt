package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.reporting

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.sortBy
import org.jetbrains.kotlinx.dataframe.api.toList
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks.ClockConfiguration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.function.Consumer

class Cas1OverduePlacementsReportTest : InitialiseDatabasePerClassTestBase() {

  @Autowired
  private lateinit var mutableClock: ClockConfiguration.MutableClock

  private fun createBookings() {
    // overdueForArrival
    givenACas1SpaceBooking(
      crn = "CRN001",
      actualArrivalDate = null,
      expectedArrivalDate = LocalDate.of(2025, 3, 25),
      expectedDepartureDate = LocalDate.of(2025, 4, 10),
      cancellationOccurredAt = null,
      nonArrivalConfirmedAt = null,
    )

    // overdueForDeparture
    givenACas1SpaceBooking(
      crn = "CRN002",
      actualArrivalDate = LocalDate.of(2025, 3, 20),
      expectedArrivalDate = LocalDate.of(2025, 3, 20),
      expectedDepartureDate = LocalDate.of(2025, 4, 5),
      cancellationOccurredAt = null,
      nonArrivalConfirmedAt = null,
    )

    // withinDateRangeArrival
    givenACas1SpaceBooking(
      crn = "CRN003",
      actualArrivalDate = null,
      expectedArrivalDate = LocalDate.of(2025, 4, 15),
      expectedDepartureDate = LocalDate.of(2025, 4, 25),
      cancellationOccurredAt = null,
      nonArrivalConfirmedAt = null,
    )

    // withinDateRangeDeparture
    givenACas1SpaceBooking(
      crn = "CRN004",
      actualArrivalDate = LocalDate.of(2025, 3, 28),
      expectedArrivalDate = LocalDate.of(2025, 3, 28),
      expectedDepartureDate = LocalDate.of(2025, 4, 20),
      cancellationOccurredAt = null,
      nonArrivalConfirmedAt = null,
    )

    // thresholdDepartureDateBooking
    givenACas1SpaceBooking(
      crn = "CRN005",
      actualArrivalDate = LocalDate.of(2025, 2, 1),
      expectedArrivalDate = LocalDate.of(2025, 2, 1),
      expectedDepartureDate = LocalDate.of(2025, 4, 8),
      cancellationOccurredAt = null,
      nonArrivalConfirmedAt = null,
    )

    // belowThreshold
    givenACas1SpaceBooking(
      crn = "CRN006",
      actualArrivalDate = LocalDate.of(2024, 11, 1),
      expectedArrivalDate = LocalDate.of(2024, 11, 1),
      expectedDepartureDate = LocalDate.of(2024, 12, 30),
      cancellationOccurredAt = null,
      nonArrivalConfirmedAt = null,
    )

    // bookingDepartureBeforeStart
    givenACas1SpaceBooking(
      crn = "CRN007",
      actualArrivalDate = LocalDate.of(2025, 3, 1),
      expectedArrivalDate = LocalDate.of(2025, 3, 1),
      expectedDepartureDate = LocalDate.of(2025, 3, 31),
      cancellationOccurredAt = null,
      nonArrivalConfirmedAt = null,
    )

    // bookingArrivalAfterEnd
    givenACas1SpaceBooking(
      crn = "CRN008",
      actualArrivalDate = null,
      expectedArrivalDate = LocalDate.of(2025, 5, 1),
      expectedDepartureDate = LocalDate.of(2025, 5, 15),
      cancellationOccurredAt = null,
      nonArrivalConfirmedAt = null,
    )
  }

  @Test
  fun `Get overdue placements report requires report viewer role`() {
    val (_, jwt) = givenAUser(roles = listOf())
    val startDate = LocalDate.of(2025, 4, 1)
    val endDate = LocalDate.of(2025, 4, 30)

    webTestClient.get()
      .uri("/cas1/reports/${Cas1ReportName.overduePlacements}?startDate=$startDate&endDate=$endDate")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @BeforeAll
  fun setup() {
    createBookings()
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_REPORT_VIEWER", "CAS1_OPERATIONAL_REPORTS_VIEWER", "CAS1_MANAGEMENT_REPORTS_VIEWER"], mode = EnumSource.Mode.INCLUDE)
  fun `Get overdue placements report`(role: UserRole) {
    val (_, jwt) = givenAUser(roles = listOf(role))
    val startDate = LocalDate.of(2025, 4, 1)
    val endDate = LocalDate.of(2025, 4, 30)

    mutableClock.setNow(LocalDateTime.of(2025, 6, 16, 0, 0))

    webTestClient.get()
      .uri("/cas1/reports/${Cas1ReportName.overduePlacements}?startDate=$startDate&endDate=$endDate")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().valuesMatch(
        "content-disposition",
        "attachment; filename=\"overdue-placements-$startDate-to-$endDate-\\d{8}_\\d{4}.csv\"",
      )
      .expectBody()
      .consumeWith {
        val actual = DataFrame
          .readCSV(it.responseBody!!.inputStream())
          .convertTo<Cas1OverduePlacementsReportRow>(ExcessiveColumns.Remove)
          .sortBy { row -> row["crn"] }

        val actualRows = actual.toList()
        assertThat(actualRows).hasSize(5)

        assertThat(actualRows).noneMatch { row ->
          row.crn == "CRN006" || row.crn == "CRN007" || row.crn == "CRN008"
        }

        assertThat(actualRows[0]).satisfies(
          Consumer { row ->
            assertThat(row.premises_name).isNotNull
            assertThat(row.premises_area).isNotNull
            assertThat(row.premises_gender).isNotNull
            assertThat(row.crn).isEqualTo("CRN001")
            assertThat(row.arrival_expected_date).isEqualTo(LocalDate.of(2025, 3, 25))
            assertThat(row.arrival_overdue_by_days).isEqualTo(83)
            assertThat(row.departure_expected_date).isEqualTo(LocalDate.of(2025, 4, 10))
            assertThat(row.departure_overdue_by_days).isNull()
          },
        )

        assertThat(actualRows[1]).satisfies(
          Consumer { row ->
            assertThat(row.premises_name).isNotNull
            assertThat(row.premises_area).isNotNull
            assertThat(row.premises_gender).isNotNull
            assertThat(row.crn).isEqualTo("CRN002")
            assertThat(row.arrival_expected_date).isEqualTo(LocalDate.of(2025, 3, 20))
            assertThat(row.arrival_overdue_by_days).isNull()
            assertThat(row.departure_expected_date).isEqualTo(LocalDate.of(2025, 4, 5))
            assertThat(row.departure_overdue_by_days).isEqualTo(72)
          },
        )

        assertThat(actualRows[2]).satisfies(
          Consumer { row ->
            assertThat(row.premises_name).isNotNull
            assertThat(row.premises_area).isNotNull
            assertThat(row.premises_gender).isNotNull
            assertThat(row.crn).isEqualTo("CRN003")
            assertThat(row.arrival_expected_date).isEqualTo(LocalDate.of(2025, 4, 15))
            assertThat(row.arrival_overdue_by_days).isEqualTo(62)
            assertThat(row.departure_expected_date).isEqualTo(LocalDate.of(2025, 4, 25))
            assertThat(row.departure_overdue_by_days).isNull()
          },
        )

        assertThat(actualRows[3]).satisfies(
          Consumer { row ->
            assertThat(row.premises_name).isNotNull
            assertThat(row.premises_area).isNotNull
            assertThat(row.premises_gender).isNotNull
            assertThat(row.crn).isEqualTo("CRN004")
            assertThat(row.arrival_expected_date).isEqualTo(LocalDate.of(2025, 3, 28))
            assertThat(row.arrival_overdue_by_days).isNull()
            assertThat(row.departure_expected_date).isEqualTo(LocalDate.of(2025, 4, 20))
            assertThat(row.departure_overdue_by_days).isEqualTo(57)
          },
        )

        assertThat(actualRows[4]).satisfies(
          Consumer { row ->
            assertThat(row.premises_name).isNotNull
            assertThat(row.premises_area).isNotNull
            assertThat(row.premises_gender).isNotNull
            assertThat(row.crn).isEqualTo("CRN005")
            assertThat(row.arrival_expected_date).isEqualTo(LocalDate.of(2025, 2, 1))
            assertThat(row.arrival_overdue_by_days).isNull()
            assertThat(row.departure_expected_date).isEqualTo(LocalDate.of(2025, 4, 8))
            assertThat(row.departure_overdue_by_days).isEqualTo(69)
          },
        )
      }
  }
}

@SuppressWarnings("ConstructorParameterNaming")
data class Cas1OverduePlacementsReportRow(
  val premises_name: String,
  val premises_area: String,
  val premises_gender: String,
  val crn: String,
  val arrival_expected_date: LocalDate,
  val arrival_overdue_by_days: Int?,
  val departure_expected_date: LocalDate,
  val departure_overdue_by_days: Int?,
)
