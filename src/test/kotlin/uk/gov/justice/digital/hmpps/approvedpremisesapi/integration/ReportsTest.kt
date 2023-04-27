package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulStaffMembersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import java.time.LocalDate
import java.util.UUID

class ReportsTest : IntegrationTestBase() {
  @Test
  fun `Get bookings report for all regions returns 403 Forbidden if user does not have all regions access`() {
    `Given a User` { _, jwt ->
      webTestClient.get()
        .uri("/reports/bookings")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get bookings report for a region returns 403 Forbidden if user cannot access the specified region`() {
    `Given a User` { _, jwt ->
      webTestClient.get()
        .uri("/reports/bookings?probationRegionId=${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get bookings report returns 400 if month is provided and not within 1-12`() {
    `Given a User` { user, jwt ->
      webTestClient.get()
        .uri("/reports/bookings?probationRegionId=${user.probationRegion.id}&year=2023&month=-1")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("month must be between 1 and 12")
    }
  }

  @Test
  fun `Get bookings report returns 400 if month is provided without year`() {
    `Given a User` { user, jwt ->
      webTestClient.get()
        .uri("/reports/bookings?probationRegionId=${user.probationRegion.id}&month=1")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("month and year must be provided together")
    }
  }

  @Test
  fun `Get bookings report returns 400 if year is provided without month`() {
    `Given a User` { user, jwt ->
      webTestClient.get()
        .uri("/reports/bookings?probationRegionId=${user.probationRegion.id}&year=2023")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("month and year must be provided together")
    }
  }

  @Test
  fun `Get bookings report returns OK with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val keyWorker = ContextStaffMemberFactory().produce()
        APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, premises.qCode)

        val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
          withPremises(premises)
          withServiceName(ServiceName.approvedPremises)
          withStaffKeyWorkerCode(keyWorker.code)
          withCrn(offenderDetails.otherIds.crn)
        }

        bookings[1].let { it.arrival = arrivalEntityFactory.produceAndPersist { withBooking(it) } }
        bookings[2].let {
          it.arrival = arrivalEntityFactory.produceAndPersist { withBooking(it) }
          it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          it.departures = departureEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
            withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
            withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[3].let {
          it.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[4].let {
          it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
            withBooking(it)
            withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
          }
        }

        val expectedDataFrame = BookingsReportGenerator()
          .createReport(bookings, BookingsReportProperties(ServiceName.approvedPremises, null, null, null))

        webTestClient.get()
          .uri("/reports/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
            Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bookings report returns OK with only Bookings with at least one day in month when year and month are specified`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val keyWorker = ContextStaffMemberFactory().produce()
        APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, premises.qCode)

        val shouldNotBeIncludedBookings = mutableListOf<BookingEntity>()
        val shouldBeIncludedBookings = mutableListOf<BookingEntity>()

        // Straddling start of month
        shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.approvedPremises)
          withStaffKeyWorkerCode(keyWorker.code)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.of(2023, 3, 29))
          withDepartureDate(LocalDate.of(2023, 4, 1))
        }

        // Straddling end of month
        shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.approvedPremises)
          withStaffKeyWorkerCode(keyWorker.code)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.of(2023, 4, 2))
          withDepartureDate(LocalDate.of(2023, 4, 3))
        }

        // Entirely within month
        shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.approvedPremises)
          withStaffKeyWorkerCode(keyWorker.code)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.of(2023, 4, 30))
          withDepartureDate(LocalDate.of(2023, 5, 15))
        }

        // Encompassing month
        shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.approvedPremises)
          withStaffKeyWorkerCode(keyWorker.code)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.of(2023, 3, 28))
          withDepartureDate(LocalDate.of(2023, 5, 28))
        }

        // Before month
        shouldNotBeIncludedBookings += bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.approvedPremises)
          withStaffKeyWorkerCode(keyWorker.code)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.of(2023, 3, 28))
          withDepartureDate(LocalDate.of(2023, 3, 30))
        }

        // After month
        shouldNotBeIncludedBookings += bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.approvedPremises)
          withStaffKeyWorkerCode(keyWorker.code)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.of(2023, 5, 1))
          withDepartureDate(LocalDate.of(2023, 5, 3))
        }

        val expectedDataFrame = BookingsReportGenerator()
          .createReport(shouldBeIncludedBookings, BookingsReportProperties(ServiceName.approvedPremises, null, null, null))

        webTestClient.get()
          .uri("/reports/bookings?year=2023&month=4")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
            Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }
}
