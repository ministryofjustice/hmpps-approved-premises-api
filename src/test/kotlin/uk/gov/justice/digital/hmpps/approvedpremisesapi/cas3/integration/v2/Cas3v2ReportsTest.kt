package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns.Remove
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.sortBy
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ReportType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.PersonInformationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.util.toBookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3v2ReportsTest : IntegrationTestBase() {

  @BeforeEach
  fun beforeEach() {
    mockFeatureFlagService.setFlag("cas3-reports-with-new-bedspace-model-tables-enabled", true)
  }

  @AfterEach
  fun afterEach() {
    mockFeatureFlagService.reset()
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report for all regions returns 403 Forbidden if user does not have all regions access`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=2023-04-01&endDate=2023-04-02")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report for a region returns 403 Forbidden if user cannot access the specified region`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=2023-04-01&endDate=2023-04-02&probationRegionId=${UUID.randomUUID()}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get bookings report returns 403 Forbidden for Temporary Accommodation if a user does not have the CAS3_ASSESSOR role`(
    reportType: Cas3ReportType,
  ) {
    givenAUser { user, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=2023-04-01&endDate=2023-04-02&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report returns 400 if dates provided is more than or equal to 3 months`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      val startDate = "2023-04-01"
      val endDate = "2023-08-02"
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=$startDate&endDate=$endDate&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("rangeTooLarge")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.endDate")
    }

    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      val startDate = "2023-04-01"
      val endDate = "2023-07-01"
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=$startDate&endDate=$endDate&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("rangeTooLarge")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.endDate")
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report returns 400 if start date is later than end date`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      val startDate = "2023-08-03"
      val endDate = "2023-08-02"
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=$startDate&endDate=$endDate&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("afterEndDate")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.startDate")
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report returns 400 if start date is the same as end date`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      val startDate = "2023-08-02"
      val endDate = "2023-08-02"
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=$startDate&endDate=$endDate&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("afterEndDate")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.startDate")
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report returns 400 if end date is in the future`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      val today = LocalDate.now()
      val startDate = "2023-08-02"
      val endDate = today.plusDays(1)
      webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=$startDate&endDate=$endDate&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].errorType").isEqualTo("inFuture")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.endDate")
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3ReportType::class)
  fun `Get report returns 400 if mandatory dates are not provided`(reportType: Cas3ReportType) {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/$reportType?probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("Missing required query parameter startDate")
    }
  }

  @Test
  fun `Get report returns 400 when requested for invalid report type`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      val startDate = "2023-08-01"
      val endDate = "2023-08-02"
      val actualBody = webTestClient.get()
        .uri("/cas3/reports/lostbed?startDate=$startDate&endDate=$endDate&probationRegionId=${user.probationRegion.id}")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectBody()

      assertThat(actualBody.returnResult().status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  @Nested
  inner class GetBookingReport {

    @Test
    fun `Get bookings report returns OK with correct body`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val bookings = mutableListOf<Cas3BookingEntity>()
          repeat(5) {
            bookings.add(
              setupPremisesWIthABedspaceAndABooking(
                crn = offenderDetails.otherIds.crn,
                user,
                startDate,
              ),
            )
          }

          bookings[1].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }
          bookings[2].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = cas3ExtensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = cas3DepartureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK with latest departure and arrivals when booking has updated with multiple departures and arrivals`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val bookings = mutableListOf<Cas3BookingEntity>()
          repeat(5) {
            bookings.add(
              setupPremisesWIthABedspaceAndABooking(
                crn = offenderDetails.otherIds.crn,
                user,
                startDate,
              ),
            )
          }

          bookings[1].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }

          bookings[2].let {
            val firstArrivalUpdate = cas3ArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withArrivalDate(LocalDate.now().randomDateBefore(14))
            }
            val secondArrivalUpdate = cas3ArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withArrivalDate(LocalDate.now())
            }

            it.arrivals = listOf(firstArrivalUpdate, secondArrivalUpdate).toMutableList()
            it.extensions = cas3ExtensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()

            val firstDepartureUpdate = cas3DepartureEntityFactory.produceAndPersist {
              withDateTime(OffsetDateTime.now().randomDateTimeBefore(14))
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }
            val secondDepartureUpdate = cas3DepartureEntityFactory.produceAndPersist {
              withDateTime(OffsetDateTime.now())
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }
            it.departures = listOf(firstDepartureUpdate, secondDepartureUpdate).toMutableList()
          }
          bookings[3].let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK for CAS3_REPORTER`() {
      givenAUser(roles = listOf(CAS3_REPORTER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val bookings = mutableListOf<Cas3BookingEntity>()
          repeat(5) {
            bookings.add(
              setupPremisesWIthABedspaceAndABooking(
                crn = offenderDetails.otherIds.crn,
                user,
                startDate,
              ),
            )
          }

          bookings[1].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }
          bookings[2].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = cas3ExtensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = cas3DepartureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK for CAS3_REPORTER for all region`() {
      givenAUser(roles = listOf(CAS3_REPORTER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val bookings = mutableListOf<Cas3BookingEntity>()
          repeat(5) {
            bookings.add(
              setupPremisesWIthABedspaceAndABooking(
                crn = offenderDetails.otherIds.crn,
                user,
                startDate,
              ),
            )
          }

          bookings[1].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }
          bookings[2].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = cas3ExtensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = cas3DepartureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns 403 Forbidden for CAS3_REPORTER with service-name as approved-premises`() {
      givenAUser(roles = listOf(CAS3_REPORTER)) { user, jwt ->

        webTestClient.get()
          .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get bookings report returns OK with only Bookings with at least one day in month when year and month are specified`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = givenACas3Premises(
            user.probationRegion,
            status = Cas3PremisesStatus.online,
          )
          val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(startDate.minusDays(100))
            withCreatedDate(startDate.minusDays(100))
            withEndDate(null)
          }

          val shouldNotBeIncludedBookings = mutableListOf<Cas3BookingEntity>()
          val shouldBeIncludedBookings = mutableListOf<Cas3BookingEntity>()

          // Straddling start of month
          shouldBeIncludedBookings += cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 3, 29))
            withDepartureDate(LocalDate.of(2023, 4, 1))
          }

          // Straddling end of month
          shouldBeIncludedBookings += cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 2))
            withDepartureDate(LocalDate.of(2023, 4, 3))
          }

          // Entirely within month
          shouldBeIncludedBookings += cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 30))
            withDepartureDate(LocalDate.of(2023, 5, 15))
          }

          // Encompassing month
          shouldBeIncludedBookings += cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 3, 28))
            withDepartureDate(LocalDate.of(2023, 5, 28))
          }

          // Before month
          shouldNotBeIncludedBookings += cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 3, 28))
            withDepartureDate(LocalDate.of(2023, 3, 30))
          }

          // After month
          shouldNotBeIncludedBookings += cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 5, 1))
            withDepartureDate(LocalDate.of(2023, 5, 3))
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              shouldBeIncludedBookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK with only bookings from the specified probation region`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val bookings = mutableListOf<Cas3BookingEntity>()
          repeat(5) {
            bookings.add(
              setupPremisesWIthABedspaceAndABooking(
                crn = offenderDetails.otherIds.crn,
                user,
                startDate,
              ),
            )
          }

          bookings[1].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }
          bookings[2].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = cas3ExtensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = cas3DepartureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          // Unexpected bookings
          val unexpectedPremises = givenACas3Premises(
            status = Cas3PremisesStatus.online,
          )
          val unexpectedBedspace = cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(unexpectedPremises)
            withStartDate(startDate.minusDays(100))
            withCreatedDate(startDate.minusDays(100))
            withEndDate(null)
          }
          cas3BookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(unexpectedPremises)
            withBedspace(unexpectedBedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK with correct body and correct duty to refer local authority area name`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = givenACas3Premises(
            user.probationRegion,
            status = Cas3PremisesStatus.online,
          )
          val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(startDate.minusDays(100))
            withCreatedDate(startDate.minusDays(100))
            withEndDate(null)
          }

          val accommodationApplication =
            createTemporaryAccommodationApplication(offenderDetails, user)

          val bookings = cas3BookingEntityFactory.produceAndPersistMultiple(1) {
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
            withApplication(accommodationApplication)
          }
          bookings[0].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK with only bookings from the specified service`() {
      givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val bookings = mutableListOf<Cas3BookingEntity>()
          repeat(5) {
            bookings.add(
              setupPremisesWIthABedspaceAndABooking(
                crn = offenderDetails.otherIds.crn,
                user,
                startDate,
              ),
            )
          }

          bookings[1].let { it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList() }
          bookings[2].let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.extensions = cas3ExtensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
            it.departures = cas3DepartureEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[3].let {
            it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
              withBooking(it)
              withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
            }.toMutableList()
          }
          bookings[4].let {
            it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }
          }

          val unexpectedPremises = givenAnApprovedPremises()

          // Unexpected bookings
          bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(unexpectedPremises)
            withServiceName(ServiceName.approvedPremises)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonInformationReportData(caseSummary.pnc, caseSummary.name, caseSummary.dateOfBirth, caseSummary.gender, caseSummary.profile?.ethnicity)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/cas3/reports/booking?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(Remove)
                .sortBy(BookingsReportRow::bookingId)
              assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    private fun setupPremisesWIthABedspaceAndABooking(crn: String, user: UserEntity, startDate: LocalDate): Cas3BookingEntity {
      val premises = givenACas3Premises(
        user.probationRegion,
        status = Cas3PremisesStatus.online,
      )
      val bedspaceStartDate = startDate.minusDays(100)
      val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
        withPremises(premises)
        withStartDate(bedspaceStartDate)
        withCreatedDate(bedspaceStartDate)
        withEndDate(null)
      }
      return cas3BookingEntityFactory.produceAndPersist {
        withServiceName(ServiceName.temporaryAccommodation)
        withPremises(premises)
        withBedspace(bedspace)
        withCrn(crn)
        withArrivalDate(LocalDate.of(2023, 4, 5))
        withDepartureDate(LocalDate.of(2023, 4, 7))
      }
    }

    private fun createTemporaryAccommodationApplication(
      offenderDetails: OffenderDetailSummary,
      user: UserEntity,
    ): TemporaryAccommodationApplicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(user)
      withProbationRegion(user.probationRegion)
      withDutyToReferLocalAuthorityAreaName("London")
      withSubmittedAt(OffsetDateTime.now())
    }
  }
}
