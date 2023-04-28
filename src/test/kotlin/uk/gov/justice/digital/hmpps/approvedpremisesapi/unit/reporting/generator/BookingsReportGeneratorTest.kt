package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.generator

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.count
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ConfirmationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DestinationProviderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class BookingsReportGeneratorTest {
  private val reportGenerator = BookingsReportGenerator()

  @Test
  fun `Only bookings from the specified service are returned in the report`() {
    val approvedPremisesBookings = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produceMany()
      .take(5)
      .toList()

    val temporaryAccommodationBookings = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produceMany()
      .take(4)
      .toList()

    val allBookings = approvedPremisesBookings + temporaryAccommodationBookings

    val actual1 = reportGenerator.createReport(allBookings, BookingsReportProperties(ServiceName.approvedPremises, null, null, null))
    assertThat(actual1.count()).isEqualTo(5)

    val actual2 = reportGenerator.createReport(allBookings, BookingsReportProperties(ServiceName.temporaryAccommodation, null, null, null))
    assertThat(actual2.count()).isEqualTo(4)
  }

  @Test
  fun `Only bookings from the specified probation region are returned in the report`() {
    val probationRegionId = UUID.randomUUID()

    val expectedBookings = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withId(probationRegionId)
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produceMany()
      .take(5)
      .toList()

    val unexpectedBookings = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produceMany()
      .take(4)
      .toList()

    val allBookings = expectedBookings + unexpectedBookings

    val actual = reportGenerator.createReport(allBookings, BookingsReportProperties(ServiceName.temporaryAccommodation, probationRegionId, null, null))
    assertThat(actual.count()).isEqualTo(5)
  }

  @Test
  fun `Bookings from all probation regions are returned in the report if no probation region ID is provided`() {
    val expectedBookings = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produceMany()
      .take(5)
      .toList()

    val actual = reportGenerator.createReport(expectedBookings, BookingsReportProperties(ServiceName.temporaryAccommodation, null, null, null))
    assertThat(actual.count()).isEqualTo(5)
  }

  @Test
  fun `The probation region name is returned in the report`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withName("East of England")
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.approvedPremises, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::probationRegion]).isEqualTo("East of England")
  }

  @Test
  fun `The CRN is returned in the report`() {
    val booking = BookingEntityFactory()
      .withCrn("X123456")
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.approvedPremises, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::crn]).isEqualTo("X123456")
  }

  @Test
  fun `The 'offer accepted' column is true in the report if the booking was confirmed`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.confirmation = ConfirmationEntityFactory()
      .withBooking(booking)
      .produce()

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.temporaryAccommodation, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::offerAccepted]).isTrue
  }

  @Test
  fun `The 'offer accepted' column is false in the report if the booking was not confirmed`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.temporaryAccommodation, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::offerAccepted]).isFalse
  }

  @Test
  fun `The 'is cancelled' column is true and the cancellation reason is returned in the report if the booking was cancelled`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.cancellations = mutableListOf(
      CancellationEntityFactory()
        .withYieldedReason {
          CancellationReasonEntityFactory()
            .withName("House exploded")
            .produce()
        }
        .withBooking(booking)
        .produce()
    )

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.approvedPremises, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::isCancelled]).isTrue
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::cancellationReason]).isEqualTo("House exploded")
  }

  @Test
  fun `The 'is cancelled' column is false and no cancellation reason is returned in the report if the booking was not cancelled`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.approvedPremises, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::isCancelled]).isFalse
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::cancellationReason]).isNull()
  }

  @Test
  fun `The start and end dates are returned from the arrival in the report if it exists`() {
    val today = LocalDate.now()
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.arrival = ArrivalEntityFactory()
      .withArrivalDate(today)
      .withExpectedDepartureDate(today.plusDays(84L))
      .withBooking(booking)
      .produce()

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.temporaryAccommodation, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::startDate]).isEqualTo(today)
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::endDate]).isEqualTo(today.plusDays(84L))
  }

  @Test
  fun `The start and end dates are not returned in the report if the booking has no arrival`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.temporaryAccommodation, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::startDate]).isNull()
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::endDate]).isNull()
  }

  @Test
  fun `The actual end date is returned in the report if the booking has a departure`() {
    val now = OffsetDateTime.now()
    val today = now.toLocalDate()

    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.departures = mutableListOf(
      DepartureEntityFactory()
        .withDateTime(now)
        .withYieldedReason {
          DepartureReasonEntityFactory()
            .produce()
        }
        .withYieldedMoveOnCategory {
          MoveOnCategoryEntityFactory()
            .produce()
        }
        .withYieldedDestinationProvider {
          DestinationProviderEntityFactory()
            .produce()
        }
        .withBooking(booking)
        .produce()
    )

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.approvedPremises, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::actualEndDate]).isEqualTo(today)
  }

  @Test
  fun `The actual end date is not returned in the report if the booking has no departure`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.approvedPremises, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::actualEndDate]).isNull()
  }

  @Test
  fun `The number of nights stayed so far is returned from the arrival in the report if it exists`() {
    val today = LocalDate.now()
    val arrivalDate = today.minusDays(37L)

    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.arrival = ArrivalEntityFactory()
      .withArrivalDate(arrivalDate)
      .withExpectedDepartureDate(arrivalDate.plusDays(84L))
      .withBooking(booking)
      .produce()

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.temporaryAccommodation, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::currentNightsStayed]).isEqualTo(37)
  }

  @Test
  fun `The number of nights stayed so far is not returned in the report if the booking has no arrival`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.temporaryAccommodation, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::currentNightsStayed]).isNull()
  }

  @Test
  fun `The number of nights stayed so far is not returned in the report if the booking has a departure`() {
    val now = OffsetDateTime.now()
    val today = now.toLocalDate()
    val expectedDepartureDate = today.minusDays(3L)
    val arrivalDate = expectedDepartureDate.minusDays(84L)

    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.arrival = ArrivalEntityFactory()
      .withArrivalDate(arrivalDate)
      .withExpectedDepartureDate(expectedDepartureDate)
      .withBooking(booking)
      .produce()

    booking.departures = mutableListOf(
      DepartureEntityFactory()
        .withDateTime(now)
        .withYieldedReason {
          DepartureReasonEntityFactory()
            .produce()
        }
        .withYieldedMoveOnCategory {
          MoveOnCategoryEntityFactory()
            .produce()
        }
        .withYieldedDestinationProvider {
          DestinationProviderEntityFactory()
            .produce()
        }
        .withBooking(booking)
        .produce()
    )

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.approvedPremises, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::currentNightsStayed]).isNull()
  }

  @Test
  fun `The actual number of nights stayed is returned in the report if the booking has a departure`() {
    val now = OffsetDateTime.now()
    val today = now.toLocalDate()
    val expectedDepartureDate = today.minusDays(3L)
    val arrivalDate = expectedDepartureDate.minusDays(84L)

    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.arrival = ArrivalEntityFactory()
      .withArrivalDate(arrivalDate)
      .withExpectedDepartureDate(expectedDepartureDate)
      .withBooking(booking)
      .produce()

    booking.departures = mutableListOf(
      DepartureEntityFactory()
        .withDateTime(now)
        .withYieldedReason {
          DepartureReasonEntityFactory()
            .produce()
        }
        .withYieldedMoveOnCategory {
          MoveOnCategoryEntityFactory()
            .produce()
        }
        .withYieldedDestinationProvider {
          DestinationProviderEntityFactory()
            .produce()
        }
        .withBooking(booking)
        .produce()
    )

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.approvedPremises, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::actualNightsStayed]).isEqualTo(87L)
  }

  @Test
  fun `The actual number of nights stayed is returned in the report if the booking has no departure`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.approvedPremises, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::actualNightsStayed]).isNull()
  }

  @Test
  fun `The accommodation outcome is returned from the departure in the report if it exists`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.departures = mutableListOf(
      DepartureEntityFactory()
        .withYieldedReason {
          DepartureReasonEntityFactory()
            .produce()
        }
        .withYieldedMoveOnCategory {
          MoveOnCategoryEntityFactory()
            .withName("Joined the Space Force")
            .produce()
        }
        .withYieldedDestinationProvider {
          DestinationProviderEntityFactory()
            .produce()
        }
        .withBooking(booking)
        .produce()
    )

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.temporaryAccommodation, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::accommodationOutcome]).isEqualTo("Joined the Space Force")
  }

  @Test
  fun `The accommodation outcome is not returned in the report if the booking has no departure`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(listOf(booking), BookingsReportProperties(ServiceName.temporaryAccommodation, null, null, null))
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::accommodationOutcome]).isNull()
  }
}
