package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedOccupancyBookingEntry
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedOccupancyEntryType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedOccupancyLostBedEntry
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedOccupancyOpenEntry
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBedInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBookingInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarLostBedInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CalendarTransformer
import java.time.LocalDate
import java.util.UUID

class CalendarTransformerTest {
  private val calendarTransformer = CalendarTransformer()

  @Test
  fun `transformDomainToApi transforms empty Bed correctly`() {
    val result = calendarTransformer.transformDomainToApi(
      startDate = LocalDate.of(2023, 6, 1),
      endDate = LocalDate.of(2023, 6, 5),
      domain = mapOf(
        CalendarBedInfo(
          bedId = UUID.fromString("81b2ae11-a375-4464-8662-840a435ed3cb"),
          bedName = "BED1",
        ) to listOf(),
      ),
    )

    assertThat(result).singleElement()
    val bedResult = result.first()

    assertThat(bedResult.bedId).isEqualTo(UUID.fromString("81b2ae11-a375-4464-8662-840a435ed3cb"))
    assertThat(bedResult.bedName).isEqualTo("BED1")
    assertThat(bedResult.schedule).usingRecursiveComparison().isEqualTo(
      listOf(
        BedOccupancyOpenEntry(
          type = BedOccupancyEntryType.OPEN,
          length = 5,
          startDate = LocalDate.of(2023, 6, 1),
          endDate = LocalDate.of(2023, 6, 5),
        ),
      ),
    )
  }

  @Test
  fun `transformDomainToApi transforms Booking spanning entire period for Bed correctly`() {
    val result = calendarTransformer.transformDomainToApi(
      startDate = LocalDate.of(2023, 6, 1),
      endDate = LocalDate.of(2023, 6, 5),
      domain = mapOf(
        CalendarBedInfo(
          bedId = UUID.fromString("81b2ae11-a375-4464-8662-840a435ed3cb"),
          bedName = "BED1",
        ) to listOf(
          CalendarBookingInfo(
            startDate = LocalDate.of(2023, 5, 15),
            endDate = LocalDate.of(2023, 7, 15),
            bookingId = UUID.fromString("f2244c68-7a42-44ac-b690-4064e009de84"),
            crn = "CRN1",
            personName = "Person Name",
          ),
        ),
      ),
    )

    assertThat(result).singleElement()
    val bedResult = result.first()

    assertThat(bedResult.bedId).isEqualTo(UUID.fromString("81b2ae11-a375-4464-8662-840a435ed3cb"))
    assertThat(bedResult.bedName).isEqualTo("BED1")
    assertThat(bedResult.schedule).usingRecursiveComparison().isEqualTo(
      listOf(
        BedOccupancyBookingEntry(
          type = BedOccupancyEntryType.BOOKING,
          length = 62,
          startDate = LocalDate.of(2023, 5, 15),
          endDate = LocalDate.of(2023, 7, 15),
          bookingId = UUID.fromString("f2244c68-7a42-44ac-b690-4064e009de84"),
          personName = "Person Name",
        ),
      ),
    )
  }

  @Test
  fun `transformDomainToApi transforms Lost Bed spanning entire period for Bed correctly`() {
    val result = calendarTransformer.transformDomainToApi(
      startDate = LocalDate.of(2023, 6, 1),
      endDate = LocalDate.of(2023, 6, 5),
      domain = mapOf(
        CalendarBedInfo(
          bedId = UUID.fromString("81b2ae11-a375-4464-8662-840a435ed3cb"),
          bedName = "BED1",
        ) to listOf(
          CalendarLostBedInfo(
            startDate = LocalDate.of(2023, 5, 15),
            endDate = LocalDate.of(2023, 7, 15),
            lostBedId = UUID.fromString("87548acc-5da0-478d-abd2-0b17e904b97e"),
          ),
        ),
      ),
    )

    assertThat(result).singleElement()
    val bedResult = result.first()

    assertThat(bedResult.bedId).isEqualTo(UUID.fromString("81b2ae11-a375-4464-8662-840a435ed3cb"))
    assertThat(bedResult.bedName).isEqualTo("BED1")
    assertThat(bedResult.schedule).usingRecursiveComparison().isEqualTo(
      listOf(
        BedOccupancyLostBedEntry(
          type = BedOccupancyEntryType.LOST_BED,
          length = 62,
          startDate = LocalDate.of(2023, 5, 15),
          endDate = LocalDate.of(2023, 7, 15),
          lostBedId = UUID.fromString("87548acc-5da0-478d-abd2-0b17e904b97e"),
        ),
      ),
    )
  }

  @Test
  fun `transformDomainToApi transforms double-Booking and Lost Bed with gaps (for open entry) correctly`() {
    val result = calendarTransformer.transformDomainToApi(
      startDate = LocalDate.of(2023, 6, 1),
      endDate = LocalDate.of(2023, 6, 15),
      domain = mapOf(
        CalendarBedInfo(
          bedId = UUID.fromString("81b2ae11-a375-4464-8662-840a435ed3cb"),
          bedName = "BED1",
        ) to listOf(
          CalendarLostBedInfo(
            startDate = LocalDate.of(2023, 6, 1),
            endDate = LocalDate.of(2023, 6, 4),
            lostBedId = UUID.fromString("87548acc-5da0-478d-abd2-0b17e904b97e"),
          ),
          CalendarLostBedInfo(
            startDate = LocalDate.of(2023, 6, 14),
            endDate = LocalDate.of(2023, 6, 15),
            lostBedId = UUID.fromString("5fd97cbf-0006-473d-8265-a155d54c8f54"),
          ),
          CalendarBookingInfo(
            startDate = LocalDate.of(2023, 6, 7),
            endDate = LocalDate.of(2023, 6, 10),
            bookingId = UUID.fromString("d49e500d-588e-4f67-8445-9779f20cee43"),
            crn = "CRN1",
            personName = "Person 1",
          ),
          CalendarBookingInfo(
            startDate = LocalDate.of(2023, 6, 8),
            endDate = LocalDate.of(2023, 6, 10),
            bookingId = UUID.fromString("a8e6afc3-8eae-4c9a-a16f-112a902e695d"),
            crn = "CRN2",
            personName = "Person 2",
          ),
        ),
      ),
    )

    assertThat(result).singleElement()
    val bedResult = result.first()

    assertThat(bedResult.bedId).isEqualTo(UUID.fromString("81b2ae11-a375-4464-8662-840a435ed3cb"))
    assertThat(bedResult.bedName).isEqualTo("BED1")
    assertThat(bedResult.schedule).usingRecursiveComparison().isEqualTo(
      listOf(
        BedOccupancyLostBedEntry(
          type = BedOccupancyEntryType.LOST_BED,
          length = 4,
          startDate = LocalDate.of(2023, 6, 1),
          endDate = LocalDate.of(2023, 6, 4),
          lostBedId = UUID.fromString("87548acc-5da0-478d-abd2-0b17e904b97e"),
        ),
        BedOccupancyLostBedEntry(
          type = BedOccupancyEntryType.LOST_BED,
          length = 2,
          startDate = LocalDate.of(2023, 6, 14),
          endDate = LocalDate.of(2023, 6, 15),
          lostBedId = UUID.fromString("5fd97cbf-0006-473d-8265-a155d54c8f54"),
        ),
        BedOccupancyBookingEntry(
          type = BedOccupancyEntryType.BOOKING,
          length = 4,
          startDate = LocalDate.of(2023, 6, 7),
          endDate = LocalDate.of(2023, 6, 10),
          bookingId = UUID.fromString("d49e500d-588e-4f67-8445-9779f20cee43"),
          personName = "Person 1",
        ),
        BedOccupancyBookingEntry(
          type = BedOccupancyEntryType.BOOKING,
          length = 3,
          startDate = LocalDate.of(2023, 6, 8),
          endDate = LocalDate.of(2023, 6, 10),
          bookingId = UUID.fromString("a8e6afc3-8eae-4c9a-a16f-112a902e695d"),
          personName = "Person 2",
        ),
        BedOccupancyOpenEntry(
          type = BedOccupancyEntryType.OPEN,
          length = 2,
          startDate = LocalDate.of(2023, 6, 5),
          endDate = LocalDate.of(2023, 6, 6),
        ),
        BedOccupancyOpenEntry(
          type = BedOccupancyEntryType.OPEN,
          length = 3,
          startDate = LocalDate.of(2023, 6, 11),
          endDate = LocalDate.of(2023, 6, 13),
        ),
      ),
    )
  }
}
