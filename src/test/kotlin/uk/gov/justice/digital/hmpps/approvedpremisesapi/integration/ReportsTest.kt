package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.sortBy
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_addResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.LostBedsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUsageType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUtilisationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.LostBedReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.LostBedReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toBookingsReportDataAndPersonInfo
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ReportsTest : IntegrationTestBase() {
  @Autowired
  lateinit var bookingTransformer: BookingTransformer

  @Autowired
  lateinit var realLostBedsRepository: LostBedsRepository

  @Nested
  inner class GetBookingReport {
    @Test
    fun `Get bookings report for all regions returns 403 Forbidden if user does not have all regions access`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        webTestClient.get()
          .uri("/reports/bookings?year=2023&month=4")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get bookings report for a region returns 403 Forbidden if user cannot access the specified region`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        webTestClient.get()
          .uri("/reports/bookings?year=2023&month=4&probationRegionId=${UUID.randomUUID()}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get bookings report returns 403 Forbidden for Temporary Accommodation if a user does not have the CAS3_ASSESSOR role`() {
      `Given a User` { user, jwt ->
        webTestClient.get()
          .uri("/reports/bookings?year=2023&month=4&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get bookings report returns 400 if month is provided and not within 1-12`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
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
    fun `Get bookings report returns OK with correct body`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          bookings[1].let { it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList() }
          bookings[2].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
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

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/reports/bookings?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
                .sortBy(BookingsReportRow::bookingId)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK with latest departure and arrivals when booking has updated with multiple departures and arrivals`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          bookings[1].let { it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList() }

          bookings[2].let {
            val firstArrivalUpdate = arrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withArrivalDate(LocalDate.now().randomDateBefore(14))
            }
            val secondArrivalUpdate = arrivalEntityFactory.produceAndPersist {
              withBooking(it)
              withArrivalDate(LocalDate.now())
            }

            it.arrivals = listOf(firstArrivalUpdate, secondArrivalUpdate).toMutableList()
            it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()

            val firstDepartureUpdate = departureEntityFactory.produceAndPersist {
              withDateTime(OffsetDateTime.now().randomDateTimeBefore(14))
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }
            val secondDepartureUpdate = departureEntityFactory.produceAndPersist {
              withDateTime(OffsetDateTime.now())
              withBooking(it)
              withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
              withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
              withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
            }
            it.departures = listOf(firstDepartureUpdate, secondDepartureUpdate).toMutableList()
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

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/reports/bookings?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
                .sortBy(BookingsReportRow::bookingId)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK for CAS3_REPORTER`() {
      `Given a User`(roles = listOf(UserRole.CAS3_REPORTER)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          bookings[1].let { it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList() }
          bookings[2].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
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

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/reports/bookings?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
                .sortBy(BookingsReportRow::bookingId)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK for CAS3_REPORTER for all region`() {
      `Given a User`(roles = listOf(UserRole.CAS3_REPORTER)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val pdu = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
            withProbationDeliveryUnit(pdu)
          }

          val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          bookings[1].let { it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList() }
          bookings[2].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
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

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/reports/bookings?year=2023&month=4")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
                .sortBy(BookingsReportRow::bookingId)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns 403 Forbidden for CAS3_REPORTER with service-name as approved-premises`() {
      `Given a User`(roles = listOf(UserRole.CAS3_REPORTER)) { userEntity, jwt ->

        webTestClient.get()
          .uri("/reports/bookings?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get bookings report returns OK with only Bookings with at least one day in month when year and month are specified`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val shouldNotBeIncludedBookings = mutableListOf<BookingEntity>()
          val shouldBeIncludedBookings = mutableListOf<BookingEntity>()

          // Straddling start of month
          shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 3, 29))
            withDepartureDate(LocalDate.of(2023, 4, 1))
          }

          // Straddling end of month
          shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 2))
            withDepartureDate(LocalDate.of(2023, 4, 3))
          }

          // Entirely within month
          shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 30))
            withDepartureDate(LocalDate.of(2023, 5, 15))
          }

          // Encompassing month
          shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 3, 28))
            withDepartureDate(LocalDate.of(2023, 5, 28))
          }

          // Before month
          shouldNotBeIncludedBookings += bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 3, 28))
            withDepartureDate(LocalDate.of(2023, 3, 30))
          }

          // After month
          shouldNotBeIncludedBookings += bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 5, 1))
            withDepartureDate(LocalDate.of(2023, 5, 3))
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              shouldBeIncludedBookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/reports/bookings?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
                .sortBy(BookingsReportRow::bookingId)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK with only bookings from the specified service`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          bookings[1].let { it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList() }
          bookings[2].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
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

          val unexpectedPremises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

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

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/reports/bookings?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
                .sortBy(BookingsReportRow::bookingId)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK with only bookings from the specified probation region`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          bookings[1].let { it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList() }
          bookings[2].let {
            it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
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

          val unexpectedPremises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withYieldedApArea {
                  apAreaEntityFactory.produceAndPersist()
                }
              }
            }
          }

          // Unexpected bookings
          bookingEntityFactory.produceAndPersistMultiple(5) {
            withPremises(unexpectedPremises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
          }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/reports/bookings?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
                .sortBy(BookingsReportRow::bookingId)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bookings report returns OK with correct body and correct duty to refer local authority area name`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val startDate = LocalDate.of(2023, 4, 1)
          val endDate = LocalDate.of(2023, 4, 30)
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val accommodationApplication =
            createTemporaryAccommodationApplication(offenderDetails, userEntity)

          val bookings = bookingEntityFactory.produceAndPersistMultiple(1) {
            withPremises(premises)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.of(2023, 4, 5))
            withDepartureDate(LocalDate.of(2023, 4, 7))
            withApplication(accommodationApplication)
          }
          bookings[0].let { it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList() }

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          ApDeliusContext_addResponseToUserAccessCall(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
            userEntity.deliusUsername,
          )

          val expectedDataFrame = BookingsReportGenerator()
            .createReport(
              bookings.toBookingsReportDataAndPersonInfo { crn ->
                PersonSummaryInfoResult.Success.Full(crn, caseSummary)
              },
              BookingsReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
            )

          webTestClient.get()
            .uri("/reports/bookings?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
                .sortBy(BookingsReportRow::bookingId)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }
  }

  @Nested
  inner class GetBedUsageReport {
    @Test
    fun `Get bed usage report for all regions returns 403 Forbidden if user does not have all regions access`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        webTestClient.get()
          .uri("/reports/bed-usage?year=2023&month=4&")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get bed usage report for a region returns 403 Forbidden if user cannot access the specified region`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        webTestClient.get()
          .uri("/reports/bed-usage?year=2023&month=4&probationRegionId=${UUID.randomUUID()}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get bed usage report returns 403 Forbidden for Temporary Accommodation if a user does not have the CAS3_ASSESSOR role`() {
      `Given a User` { user, jwt ->
        webTestClient.get()
          .uri("/reports/bed-usage?year=2023&month=4&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get bed usage report returns 400 if month is not within 1-12`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        webTestClient.get()
          .uri("/reports/bed-usage?probationRegionId=${user.probationRegion.id}&year=2023&month=-1")
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
    fun `Get bed usage report returns OK with correct body`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val room = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bed = bedEntityFactory.produceAndPersist {
            withRoom(room)
          }

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.parse("2023-04-05"))
            withDepartureDate(LocalDate.parse("2023-04-15"))
          }

          val expectedReportRows = listOf(
            BedUsageReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = userEntity.probationDeliveryUnit?.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              crn = offenderDetails.otherIds.crn,
              type = BedUsageType.Booking,
              startDate = LocalDate.parse("2023-04-05"),
              endDate = LocalDate.parse("2023-04-15"),
              durationOfBookingDays = 10,
              bookingStatus = BookingStatus.provisional,
              voidCategory = null,
              voidNotes = null,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/reports/bed-usage?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUsageReportRow>(ExcessiveColumns.Remove)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bed usage report returns OK with correct body with pdu and local authority`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
            withProbationDeliveryUnit(probationDeliveryUnit)
            withLocalAuthorityArea(localAuthorityArea)
          }

          val room = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bed = bedEntityFactory.produceAndPersist {
            withRoom(room)
          }

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.parse("2023-04-05"))
            withDepartureDate(LocalDate.parse("2023-04-15"))
          }

          val expectedReportRows = listOf(
            BedUsageReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              crn = offenderDetails.otherIds.crn,
              type = BedUsageType.Booking,
              startDate = LocalDate.parse("2023-04-05"),
              endDate = LocalDate.parse("2023-04-15"),
              durationOfBookingDays = 10,
              bookingStatus = BookingStatus.provisional,
              voidCategory = null,
              voidNotes = null,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/reports/bed-usage?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUsageReportRow>(ExcessiveColumns.Remove)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }
  }

  @Nested
  inner class GetBedUtilizationReport {
    @Test
    fun `Get bed utilisation report for all regions returns 403 Forbidden if user does not have all regions access`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        webTestClient.get()
          .uri("/reports/bed-utilisation?year=2023&month=4&")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get bed utilisation report for a region returns 403 Forbidden if user cannot access the specified region`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        webTestClient.get()
          .uri("/reports/bed-utilisation?year=2023&month=4&probationRegionId=${UUID.randomUUID()}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get bed utilisation report returns 403 Forbidden for Temporary Accommodation if a user does not have the CAS3_ASSESSOR role`() {
      `Given a User` { user, jwt ->
        webTestClient.get()
          .uri("/reports/bed-utilisation?year=2023&month=4&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get bed utilisation report returns 400 if month is not within 1-12`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        webTestClient.get()
          .uri("/reports/bed-utilisation?probationRegionId=${user.probationRegion.id}&year=2023&month=-1")
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
    fun `Get bed utilisation report returns OK with correct body`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val room = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bed = bedEntityFactory.produceAndPersist {
            withRoom(room)
          }

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.parse("2023-04-05"))
            withDepartureDate(LocalDate.parse("2023-04-15"))
          }

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = userEntity.probationDeliveryUnit?.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 0,
              confirmedDays = 0,
              provisionalDays = 11,
              scheduledTurnaroundDays = 0,
              effectiveTurnaroundDays = 0,
              voidDays = 0,
              totalBookedDays = 0,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 0.0,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/reports/bed-utilisation?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(ExcessiveColumns.Remove)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }

    @Test
    fun `Get bed utilisation report returns OK with correct body with pdu and local authority`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
          }
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
            withProbationDeliveryUnit(probationDeliveryUnit)
            withLocalAuthorityArea(localAuthorityArea)
          }

          val room = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bed = bedEntityFactory.produceAndPersist {
            withRoom(room)
          }

          bed.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
          bedRepository.save(bed)

          GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withArrivalDate(LocalDate.parse("2023-04-05"))
            withDepartureDate(LocalDate.parse("2023-04-15"))
          }

          val expectedReportRows = listOf(
            BedUtilisationReportRow(
              probationRegion = userEntity.probationRegion.name,
              pdu = probationDeliveryUnit.name,
              localAuthority = premises.localAuthorityArea?.name,
              propertyRef = premises.name,
              addressLine1 = premises.addressLine1,
              town = premises.town,
              postCode = premises.postcode,
              bedspaceRef = room.name,
              bookedDaysActiveAndClosed = 0,
              confirmedDays = 0,
              provisionalDays = 11,
              scheduledTurnaroundDays = 0,
              effectiveTurnaroundDays = 0,
              voidDays = 0,
              totalBookedDays = 0,
              bedspaceStartDate = bed.createdAt?.toLocalDate(),
              bedspaceEndDate = bed.endDate,
              bedspaceOnlineDays = 30,
              occupancyRate = 0.0,
              uniquePropertyRef = premises.id.toShortBase58(),
              uniqueBedspaceRef = room.id.toShortBase58(),
            ),
          )

          val expectedDataFrame = expectedReportRows.toDataFrame()

          webTestClient.get()
            .uri("/reports/bed-utilisation?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<BedUtilisationReportRow>(ExcessiveColumns.Remove)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }
  }

  @Nested
  inner class GetLostBedsReport {
    private val lostBedsEndpoint = "/cas1/reports/${Cas1ReportName.lostBeds.value}"

    @Test
    fun `Get lost beds report returns OK with correct body`() {
      `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val bed1 = bedEntityFactory.produceAndPersist {
            withRoom(
              roomEntityFactory.produceAndPersist {
                withPremises(premises)
              },
            )
          }

          val bed2 = bedEntityFactory.produceAndPersist {
            withRoom(
              roomEntityFactory.produceAndPersist {
                withPremises(premises)
              },
            )
          }

          val bed3 = bedEntityFactory.produceAndPersist {
            withRoom(
              roomEntityFactory.produceAndPersist {
                withPremises(premises)
              },
            )
          }

          lostBedsEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed1)
            withStartDate(LocalDate.of(2023, 4, 5))
            withEndDate(LocalDate.of(2023, 7, 8))
            withYieldedReason {
              lostBedReasonEntityFactory.produceAndPersist()
            }
          }

          lostBedsEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed2)
            withStartDate(LocalDate.of(2023, 4, 12))
            withEndDate(LocalDate.of(2023, 7, 5))
            withYieldedReason {
              lostBedReasonEntityFactory.produceAndPersist()
            }
          }

          val lostBed3 = lostBedsEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed3)
            withStartDate(LocalDate.of(2023, 4, 1))
            withEndDate(LocalDate.of(2023, 7, 5))
            withYieldedReason {
              lostBedReasonEntityFactory.produceAndPersist()
            }
          }

          lostBedCancellationEntityFactory.produceAndPersist {
            withLostBed(lostBed3)
          }

          val expectedDataFrame = LostBedsReportGenerator(realLostBedsRepository)
            .createReport(
              listOf(bed1, bed2),
              LostBedReportProperties(ServiceName.approvedPremises, null, 2023, 4),
            )

          webTestClient.get()
            .uri("$lostBedsEndpoint?year=2023&month=4")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<LostBedReportRow>(ExcessiveColumns.Remove)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }
  }

  private fun createTemporaryAccommodationApplication(
    offenderDetails: OffenderDetailSummary,
    userEntity: UserEntity,
  ): TemporaryAccommodationApplicationEntity {
    val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }
    return temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(userEntity)
      withProbationRegion(userEntity.probationRegion)
      withApplicationSchema(applicationSchema)
      withDutyToReferLocalAuthorityAreaName("London")
    }
  }
}
