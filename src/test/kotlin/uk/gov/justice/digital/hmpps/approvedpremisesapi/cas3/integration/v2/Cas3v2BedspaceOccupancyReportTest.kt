package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns.Remove
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas3v2BedspaceOccupancyReportTest : IntegrationTestBase() {

  @BeforeEach
  fun beforeEach() {
    mockFeatureFlagService.setFlag("cas3-reports-with-new-bedspace-model-tables-enabled", true)
  }

  @AfterEach
  fun afterEach() {
    mockFeatureFlagService.reset()
  }

  @Test
  fun `Get bedspace occupancy report returns OK with correct body`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val localAuthority = localAuthorityEntityFactory.produceAndPersist()
        val expectedReportRows = ArrayList<BedspaceOccupancyReportRow>()
        repeat(times = 2) { index ->
          val bedspaceReference = if (index == 0) {
            "ZZZ bedspace"
          } else {
            "AAA bedspace"
          }
          expectedReportRows.add(
            setupBedspaceOccupancyReportData(
              user,
              localAuthority,
              offenderDetails,
              bedspaceReference
            )
          )
        }

        val expectedDataFrame = expectedReportRows
          .reversed()
          .toDataFrame()

        webTestClient.get()
          .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedspaceOccupancyReportRow>(Remove)
            assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  private fun setupBedspaceOccupancyReportData(
    user: UserEntity,
    localAuthority: LocalAuthorityAreaEntity,
    offenderDetails: OffenderDetailSummary,
    bedspaceReference: String,
  ): BedspaceOccupancyReportRow {
    val (premisesOne, bedspaceOne) = setupPremisesWithABedspace(user, localAuthority, bedspaceReference = bedspaceReference)
    bedspaceOne.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
    cas3BedspacesRepository.save(bedspaceOne)

    val (_, bedspaceTwo) = setupPremisesWithABedspace(user)
    bedspaceTwo.apply {
      createdAt = OffsetDateTime.parse("2023-01-09T08:31:00+00:00")
      endDate = LocalDate.parse("2023-03-27")
    }
    cas3BedspacesRepository.save(bedspaceTwo)

    val (_, bedspaceThree) = setupPremisesWithABedspace(user)
    bedspaceThree.apply { createdAt = OffsetDateTime.parse("2023-07-11T13:07:00+00:00") }
    cas3BedspacesRepository.save(bedspaceThree)

    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

    cas3BookingEntityFactory.produceAndPersist {
      withPremises(premisesOne)
      withBedspace(bedspaceOne)
      withServiceName(ServiceName.temporaryAccommodation)
      withCrn(offenderDetails.otherIds.crn)
      withArrivalDate(LocalDate.parse("2023-03-25"))
      withDepartureDate(LocalDate.parse("2023-04-17"))
    }

    return BedspaceOccupancyReportRow(
      probationRegion = user.probationRegion.name,
      pdu = premisesOne.probationDeliveryUnit.name,
      localAuthority = premisesOne.localAuthorityArea?.name,
      propertyRef = premisesOne.name,
      addressLine1 = premisesOne.addressLine1,
      town = premisesOne.town,
      postCode = premisesOne.postcode,
      bedspaceRef = bedspaceOne.reference,
      bookedDaysActiveAndClosed = 0,
      confirmedDays = 0,
      provisionalDays = 17,
      scheduledTurnaroundDays = 0,
      effectiveTurnaroundDays = 0,
      voidDays = 0,
      totalBookedDays = 0,
      bedspaceStartDate = bedspaceOne.createdAt.toLocalDate(),
      bedspaceEndDate = bedspaceOne.endDate,
      bedspaceOnlineDays = 30,
      occupancyRate = 0.0,
      uniquePropertyRef = premisesOne.id.toShortBase58(),
      uniqueBedspaceRef = bedspaceOne.id.toShortBase58()
    )
  }

  @Test
  fun `Get bedspace occupancy report returns OK with correct body with pdu and local authority`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val (premises, bedspace) = setupPremisesWithABedspace(user)

        bedspace.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
        cas3BedspacesRepository.save(bedspace)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2023-04-05"))
          withDepartureDate(LocalDate.parse("2023-04-15"))
        }

        val expectedReportRows = listOf(
          BedspaceOccupancyReportRow(
            probationRegion = user.probationRegion.name,
            pdu = user.probationDeliveryUnit?.name,
            localAuthority = premises.localAuthorityArea?.name,
            propertyRef = premises.name,
            addressLine1 = premises.addressLine1,
            town = premises.town,
            postCode = premises.postcode,
            bedspaceRef = bedspace.reference,
            bookedDaysActiveAndClosed = 0,
            confirmedDays = 0,
            provisionalDays = 11,
            scheduledTurnaroundDays = 0,
            effectiveTurnaroundDays = 0,
            voidDays = 0,
            totalBookedDays = 0,
            bedspaceStartDate = bedspace.createdAt.toLocalDate(),
            bedspaceEndDate = bedspace.endDate,
            bedspaceOnlineDays = 30,
            occupancyRate = 0.0,
            uniquePropertyRef = premises.id.toShortBase58(),
            uniqueBedspaceRef = bedspace.id.toShortBase58(),
          ),
        )

        val expectedDataFrame = expectedReportRows.toDataFrame()

        webTestClient.get()
          .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedspaceOccupancyReportRow>(Remove)
            assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bedspace occupancy report returns OK and shows correctly bookedDaysActiveAndClosed the total number of days for Bookings that are marked as arrived`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val (premises, bedspace) = setupPremisesWithABedspace(user)

        bedspace.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
        cas3BedspacesRepository.save(bedspace)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2023-03-25"))
          withDepartureDate(LocalDate.parse("2023-04-17"))
        }

        cas3ArrivalEntityFactory.produceAndPersist {
          withBooking(booking)
          withArrivalDate(LocalDate.parse("2023-03-25"))
        }

        val expectedReportRows = listOf(
          BedspaceOccupancyReportRow(
            probationRegion = user.probationRegion.name,
            pdu = user.probationDeliveryUnit?.name,
            localAuthority = premises.localAuthorityArea?.name,
            propertyRef = premises.name,
            addressLine1 = premises.addressLine1,
            town = premises.town,
            postCode = premises.postcode,
            bedspaceRef = bedspace.reference,
            bookedDaysActiveAndClosed = 17,
            confirmedDays = 0,
            provisionalDays = 0,
            scheduledTurnaroundDays = 0,
            effectiveTurnaroundDays = 0,
            voidDays = 0,
            totalBookedDays = 17,
            bedspaceStartDate = bedspace.createdAt.toLocalDate(),
            bedspaceEndDate = bedspace.endDate,
            bedspaceOnlineDays = 30,
            occupancyRate = 0.5666666666666667,
            uniquePropertyRef = premises.id.toShortBase58(),
            uniqueBedspaceRef = bedspace.id.toShortBase58(),
          ),
        )

        val expectedDataFrame = expectedReportRows.toDataFrame()

        webTestClient.get()
          .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedspaceOccupancyReportRow>(Remove)
            assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bedspace occupancy report returns OK and shows correctly bookedDaysActiveAndClosed when there are multiple cancellations in the same period`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val (premises, bedspace) = setupPremisesWithABedspace(user)
        bedspace.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
        cas3BedspacesRepository.save(bedspace)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val booking1 = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2023-03-02"))
          withDepartureDate(LocalDate.parse("2023-05-25"))
        }

        val booking2 = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(randomStringLowerCase(10))
          withArrivalDate(LocalDate.parse("2023-02-18"))
          withDepartureDate(LocalDate.parse("2023-05-11"))
        }

        val booking3 = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(randomStringLowerCase(10))
          withArrivalDate(LocalDate.parse("2023-02-15"))
          withDepartureDate(LocalDate.parse("2023-05-08"))
        }

        cas3ArrivalEntityFactory.produceAndPersist {
          withBooking(booking1)
          withArrivalDate(LocalDate.parse("2023-03-02"))
          withExpectedDepartureDate(LocalDate.parse("2023-05-25"))
        }

        cas3CancellationEntityFactory.produceAndPersist {
          withBooking(booking2)
          withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
        }

        cas3CancellationEntityFactory.produceAndPersist {
          withBooking(booking3)
          withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking1)
          withWorkingDayCount(2)
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking1)
          withWorkingDayCount(5)
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking1)
          withWorkingDayCount(2)
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking2)
          withWorkingDayCount(2)
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking3)
          withWorkingDayCount(2)
        }

        val expectedReportRows = listOf(
          BedspaceOccupancyReportRow(
            probationRegion = user.probationRegion.name,
            pdu = user.probationDeliveryUnit?.name,
            localAuthority = premises.localAuthorityArea?.name,
            propertyRef = premises.name,
            addressLine1 = premises.addressLine1,
            town = premises.town,
            postCode = premises.postcode,
            bedspaceRef = bedspace.reference,
            bookedDaysActiveAndClosed = 30,
            confirmedDays = 0,
            provisionalDays = 0,
            scheduledTurnaroundDays = 0,
            effectiveTurnaroundDays = 0,
            voidDays = 0,
            totalBookedDays = 30,
            bedspaceStartDate = bedspace.createdAt.toLocalDate(),
            bedspaceEndDate = bedspace.endDate,
            bedspaceOnlineDays = 30,
            occupancyRate = 1.0,
            uniquePropertyRef = premises.id.toShortBase58(),
            uniqueBedspaceRef = bedspace.id.toShortBase58(),
          ),
        )

        val expectedDataFrame = expectedReportRows.toDataFrame()

        webTestClient.get()
          .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedspaceOccupancyReportRow>(Remove)
            assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bedspace occupancy report returns OK and shows correctly bookedDaysActiveAndClosed when there are multiple bookings in the same period`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val (premises, bedspace) = setupPremisesWithABedspace(user)
        bedspace.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
        cas3BedspacesRepository.save(bedspace)

        cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBedspace(bedspace)
          withStartDate(LocalDate.parse("2023-04-28"))
          withEndDate(LocalDate.parse("2023-05-04"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val booking1 = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2023-03-25"))
          withDepartureDate(LocalDate.parse("2023-04-05"))
        }

        val booking2 = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(randomStringLowerCase(10))
          withArrivalDate(LocalDate.parse("2023-04-07"))
          withDepartureDate(LocalDate.parse("2023-04-14"))
        }

        val booking3 = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(randomStringLowerCase(10))
          withArrivalDate(LocalDate.parse("2023-04-08"))
          withDepartureDate(LocalDate.parse("2023-04-22"))
        }

        cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(randomStringLowerCase(10))
          withArrivalDate(LocalDate.parse("2023-04-24"))
          withDepartureDate(LocalDate.parse("2023-05-20"))
        }

        cas3ArrivalEntityFactory.produceAndPersist {
          withBooking(booking1)
          withArrivalDate(LocalDate.parse("2023-03-25"))
          withExpectedDepartureDate(LocalDate.parse("2023-04-05"))
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking1)
          withCreatedAt(OffsetDateTime.parse("2023-02-25T16:00:00+01:00"))
          withWorkingDayCount(2)
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking1)
          withCreatedAt(OffsetDateTime.parse("2023-02-12T17:00:00+01:00"))
          withWorkingDayCount(5)
        }

        cas3CancellationEntityFactory.produceAndPersist {
          withBooking(booking2)
          withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
        }

        cas3ArrivalEntityFactory.produceAndPersist {
          withBooking(booking3)
          withArrivalDate(LocalDate.parse("2023-04-08"))
          withExpectedDepartureDate(LocalDate.parse("2023-04-22"))
        }

        val expectedReportRows = listOf(
          BedspaceOccupancyReportRow(
            probationRegion = user.probationRegion.name,
            pdu = user.probationDeliveryUnit?.name,
            localAuthority = premises.localAuthorityArea?.name,
            propertyRef = premises.name,
            addressLine1 = premises.addressLine1,
            town = premises.town,
            postCode = premises.postcode,
            bedspaceRef = bedspace.reference,
            bookedDaysActiveAndClosed = 20,
            confirmedDays = 0,
            provisionalDays = 7,
            scheduledTurnaroundDays = 2,
            effectiveTurnaroundDays = 2,
            voidDays = 3,
            totalBookedDays = 20,
            bedspaceStartDate = bedspace.createdAt.toLocalDate(),
            bedspaceEndDate = bedspace.endDate,
            bedspaceOnlineDays = 30,
            occupancyRate = 0.6666666666666666,
            uniquePropertyRef = premises.id.toShortBase58(),
            uniqueBedspaceRef = bedspace.id.toShortBase58(),
          ),
        )

        val expectedDataFrame = expectedReportRows.toDataFrame()

        webTestClient.get()
          .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedspaceOccupancyReportRow>(Remove)
            assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bedspace occupancy report returns OK and shows correctly bookedDaysActiveAndClosed when there are multiple booking arrivals in the same period`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val (premises, bedspace) = setupPremisesWithABedspace(user)
        bedspace.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
        cas3BedspacesRepository.save(bedspace)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2024-04-05"))
          withDepartureDate(LocalDate.parse("2024-06-04"))
        }

        cas3ArrivalEntityFactory.produceAndPersist {
          withBooking(booking)
          withArrivalDate(LocalDate.parse("2024-04-05"))
          withExpectedDepartureDate(LocalDate.parse("2024-06-28"))
          withCreatedAt(OffsetDateTime.parse("2024-04-06T08:34:56.789Z"))
        }

        cas3ArrivalEntityFactory.produceAndPersist {
          withBooking(booking)
          withArrivalDate(LocalDate.parse("2024-04-07"))
          withExpectedDepartureDate(LocalDate.parse("2024-06-30"))
          withCreatedAt(OffsetDateTime.parse("2024-04-06T09:57:21.789Z"))
        }

        cas3ArrivalEntityFactory.produceAndPersist {
          withBooking(booking)
          withArrivalDate(LocalDate.parse("2024-04-06"))
          withExpectedDepartureDate(LocalDate.parse("2024-06-27"))
          withCreatedAt(OffsetDateTime.parse("2024-04-06T09:53:17.789Z"))
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking)
          withCreatedAt(OffsetDateTime.parse("2024-03-28T17:00:00+01:00"))
          withWorkingDayCount(7)
        }

        val expectedReportRows = listOf(
          BedspaceOccupancyReportRow(
            probationRegion = user.probationRegion.name,
            pdu = user.probationDeliveryUnit?.name,
            localAuthority = premises.localAuthorityArea?.name,
            propertyRef = premises.name,
            addressLine1 = premises.addressLine1,
            town = premises.town,
            postCode = premises.postcode,
            bedspaceRef = bedspace.reference,
            bookedDaysActiveAndClosed = 26,
            confirmedDays = 0,
            provisionalDays = 0,
            scheduledTurnaroundDays = 0,
            effectiveTurnaroundDays = 0,
            voidDays = 0,
            totalBookedDays = 26,
            bedspaceStartDate = bedspace.createdAt.toLocalDate(),
            bedspaceEndDate = bedspace.endDate,
            bedspaceOnlineDays = 30,
            occupancyRate = 0.8666666666666667,
            uniquePropertyRef = premises.id.toShortBase58(),
            uniqueBedspaceRef = bedspace.id.toShortBase58(),
          ),
        )

        val expectedDataFrame = expectedReportRows.toDataFrame()

        webTestClient.get()
          .uri("/cas3/reports/bedOccupancy?startDate=2024-04-01&endDate=2024-04-30&probationRegionId=${user.probationRegion.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedspaceOccupancyReportRow>(Remove)
            assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bedspace occupancy report returns OK and shows confirmedDays with the total number of days for Bookings that are marked as confirmed but not arrived`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val (premises, bedspace) = setupPremisesWithABedspace(user)
        bedspace.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
        cas3BedspacesRepository.save(bedspace)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2023-03-25"))
          withDepartureDate(LocalDate.parse("2023-04-10"))
        }

        cas3v2ConfirmationEntityFactory.produceAndPersist {
          withBooking(booking)
        }

        val expectedReportRows = listOf(
          BedspaceOccupancyReportRow(
            probationRegion = user.probationRegion.name,
            pdu = user.probationDeliveryUnit?.name,
            localAuthority = premises.localAuthorityArea?.name,
            propertyRef = premises.name,
            addressLine1 = premises.addressLine1,
            town = premises.town,
            postCode = premises.postcode,
            bedspaceRef = bedspace.reference,
            bookedDaysActiveAndClosed = 0,
            confirmedDays = 10,
            provisionalDays = 0,
            scheduledTurnaroundDays = 0,
            effectiveTurnaroundDays = 0,
            voidDays = 0,
            totalBookedDays = 0,
            bedspaceStartDate = bedspace.createdAt.toLocalDate(),
            bedspaceEndDate = bedspace.endDate,
            bedspaceOnlineDays = 30,
            occupancyRate = 0.0,
            uniquePropertyRef = premises.id.toShortBase58(),
            uniqueBedspaceRef = bedspace.id.toShortBase58(),
          ),
        )

        val expectedDataFrame = expectedReportRows.toDataFrame()

        webTestClient.get()
          .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedspaceOccupancyReportRow>(Remove)
            assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bedspace occupancy report returns OK and shows correctly scheduledTurnaroundDays the number of working days in the report period for the turnaround`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val (premises, bedspace) = setupPremisesWithABedspace(user)
        bedspace.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
        cas3BedspacesRepository.save(bedspace)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2023-04-07"))
          withDepartureDate(LocalDate.parse("2023-04-21"))
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking)
          withWorkingDayCount(5)
        }

        val expectedReportRows = listOf(
          BedspaceOccupancyReportRow(
            probationRegion = user.probationRegion.name,
            pdu = user.probationDeliveryUnit?.name,
            localAuthority = premises.localAuthorityArea?.name,
            propertyRef = premises.name,
            addressLine1 = premises.addressLine1,
            town = premises.town,
            postCode = premises.postcode,
            bedspaceRef = bedspace.reference,
            bookedDaysActiveAndClosed = 0,
            confirmedDays = 0,
            provisionalDays = 15,
            scheduledTurnaroundDays = 5,
            effectiveTurnaroundDays = 7,
            voidDays = 0,
            totalBookedDays = 0,
            bedspaceStartDate = bedspace.createdAt.toLocalDate(),
            bedspaceEndDate = bedspace.endDate,
            bedspaceOnlineDays = 30,
            occupancyRate = 0.0,
            uniquePropertyRef = premises.id.toShortBase58(),
            uniqueBedspaceRef = bedspace.id.toShortBase58(),
          ),
        )

        val expectedDataFrame = expectedReportRows.toDataFrame()

        webTestClient.get()
          .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedspaceOccupancyReportRow>(Remove)
            assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bedspace occupancy report returns OK and shows correctly effectiveTurnaroundDays the total number of days in the report period for the turnaround`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val (premises, bedspace) = setupPremisesWithABedspace(user)
        bedspace.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
        cas3BedspacesRepository.save(bedspace)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2023-03-25"))
          withDepartureDate(LocalDate.parse("2023-04-17"))
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking)
          withWorkingDayCount(5)
        }

        val expectedReportRows = listOf(
          BedspaceOccupancyReportRow(
            probationRegion = user.probationRegion.name,
            pdu = user.probationDeliveryUnit?.name,
            localAuthority = premises.localAuthorityArea?.name,
            propertyRef = premises.name,
            addressLine1 = premises.addressLine1,
            town = premises.town,
            postCode = premises.postcode,
            bedspaceRef = bedspace.reference,
            bookedDaysActiveAndClosed = 0,
            confirmedDays = 0,
            provisionalDays = 17,
            scheduledTurnaroundDays = 5,
            effectiveTurnaroundDays = 7,
            voidDays = 0,
            totalBookedDays = 0,
            bedspaceStartDate = bedspace.createdAt.toLocalDate(),
            bedspaceEndDate = bedspace.endDate,
            bedspaceOnlineDays = 30,
            occupancyRate = 0.0,
            uniquePropertyRef = premises.id.toShortBase58(),
            uniqueBedspaceRef = bedspace.id.toShortBase58(),
          ),
        )

        val expectedDataFrame = expectedReportRows.toDataFrame()

        webTestClient.get()
          .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedspaceOccupancyReportRow>(Remove)
            assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bedspace occupancy report returns OK and shows correctly scheduledTurnaroundDays and effectiveTurnaroundDays when there are multiple bookings`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val (premises, bedspace) = setupPremisesWithABedspace(user)
        bedspace.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
        cas3BedspacesRepository.save(bedspace)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val booking1 = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2024-06-24"))
          withDepartureDate(LocalDate.parse("2024-09-16"))
        }

        val booking2 = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2024-03-21"))
          withDepartureDate(LocalDate.parse("2024-06-13"))
        }

        val booking3 = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2024-03-22"))
          withDepartureDate(LocalDate.parse("2024-06-12"))
        }

        val booking4 = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2024-03-15"))
          withDepartureDate(LocalDate.parse("2024-06-07"))
        }

        val booking5 = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2024-03-12"))
          withDepartureDate(LocalDate.parse("2024-06-04"))
        }

        cas3ArrivalEntityFactory.produceAndPersist {
          withBooking(booking3)
          withArrivalDate(LocalDate.parse("2024-03-22"))
          withExpectedDepartureDate(LocalDate.parse("2024-06-14"))
          withCreatedAt(OffsetDateTime.parse("2024-03-25T09:23:17.789Z"))
        }

        cas3ArrivalEntityFactory.produceAndPersist {
          withBooking(booking1)
          withArrivalDate(LocalDate.parse("2024-06-24"))
          withExpectedDepartureDate(LocalDate.parse("2024-09-16"))
          withCreatedAt(OffsetDateTime.parse("2024-06-28T08:31:17.789Z"))
        }

        cas3CancellationEntityFactory.produceAndPersist {
          withBooking(booking4)
          withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
        }

        cas3CancellationEntityFactory.produceAndPersist {
          withBooking(booking2)
          withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
        }

        cas3CancellationEntityFactory.produceAndPersist {
          withBooking(booking5)
          withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking5)
          withWorkingDayCount(7)
          withCreatedAt(OffsetDateTime.parse("2024-03-06T10:45:00+01:00"))
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking4)
          withWorkingDayCount(7)
          withCreatedAt(OffsetDateTime.parse("2024-03-13T09:34:00+01:00"))
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking2)
          withWorkingDayCount(7)
          withCreatedAt(OffsetDateTime.parse("2024-03-13T14:13:00+01:00"))
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking3)
          withWorkingDayCount(7)
          withCreatedAt(OffsetDateTime.parse("2024-03-13T16:43:00+01:00"))
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking3)
          withWorkingDayCount(8)
          withCreatedAt(OffsetDateTime.parse("2024-06-13T09:23:00+01:00"))
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking1)
          withWorkingDayCount(7)
          withCreatedAt(OffsetDateTime.parse("2024-06-17T13:55:00+01:00"))
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking3)
          withWorkingDayCount(5)
          withCreatedAt(OffsetDateTime.parse("2024-06-18T09:42:27+01:00"))
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking3)
          withWorkingDayCount(3)
          withCreatedAt(OffsetDateTime.parse("2024-06-18T09:42:37+01:00"))
        }

        val expectedReportRows = listOf(
          BedspaceOccupancyReportRow(
            probationRegion = user.probationRegion.name,
            pdu = user.probationDeliveryUnit?.name,
            localAuthority = premises.localAuthorityArea?.name,
            propertyRef = premises.name,
            addressLine1 = premises.addressLine1,
            town = premises.town,
            postCode = premises.postcode,
            bedspaceRef = bedspace.reference,
            bookedDaysActiveAndClosed = 19,
            confirmedDays = 0,
            provisionalDays = 0,
            scheduledTurnaroundDays = 3,
            effectiveTurnaroundDays = 5,
            voidDays = 0,
            totalBookedDays = 19,
            bedspaceStartDate = bedspace.createdAt.toLocalDate(),
            bedspaceEndDate = bedspace.endDate,
            bedspaceOnlineDays = 30,
            occupancyRate = 0.6333333333333333,
            uniquePropertyRef = premises.id.toShortBase58(),
            uniqueBedspaceRef = bedspace.id.toShortBase58(),
          ),
        )

        val expectedDataFrame = expectedReportRows.toDataFrame()

        webTestClient.get()
          .uri("/cas3/reports/bedOccupancy?startDate=2024-06-01&endDate=2024-06-30&probationRegionId=${user.probationRegion.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedspaceOccupancyReportRow>(Remove)
            assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bedspace occupancy report returns OK and shows correctly voidDays the total number of days in the month for voids`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val (premises, bedspace) = setupPremisesWithABedspace(user)
        bedspace.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
        cas3BedspacesRepository.save(bedspace)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBedspace(bedspace)
          withStartDate(LocalDate.parse("2023-03-28"))
          withEndDate(LocalDate.parse("2023-04-04"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        }

        cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBedspace(bedspace)
          withStartDate(LocalDate.parse("2023-04-25"))
          withEndDate(LocalDate.parse("2023-05-03"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        }

        val expectedReportRows = listOf(
          BedspaceOccupancyReportRow(
            probationRegion = user.probationRegion.name,
            pdu = user.probationDeliveryUnit?.name,
            localAuthority = premises.localAuthorityArea?.name,
            propertyRef = premises.name,
            addressLine1 = premises.addressLine1,
            town = premises.town,
            postCode = premises.postcode,
            bedspaceRef = bedspace.reference,
            bookedDaysActiveAndClosed = 0,
            confirmedDays = 0,
            provisionalDays = 0,
            scheduledTurnaroundDays = 0,
            effectiveTurnaroundDays = 0,
            voidDays = 10,
            totalBookedDays = 0,
            bedspaceStartDate = bedspace.createdAt.toLocalDate(),
            bedspaceEndDate = bedspace.endDate,
            bedspaceOnlineDays = 30,
            occupancyRate = 0.0,
            uniquePropertyRef = premises.id.toShortBase58(),
            uniqueBedspaceRef = bedspace.id.toShortBase58(),
          ),
        )

        val expectedDataFrame = expectedReportRows.toDataFrame()

        webTestClient.get()
          .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedspaceOccupancyReportRow>(Remove)
            assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bedspace occupancy report returns OK and shows correctly totalBookedDays`() {
    givenAUser(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val (premises, bedspace) = setupPremisesWithABedspace(user)
        bedspace.apply { createdAt = OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }
        cas3BedspacesRepository.save(bedspace)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.parse("2023-03-28"))
          withDepartureDate(LocalDate.parse("2023-04-04"))
        }

        cas3v2TurnaroundFactory.produceAndPersist {
          withBooking(booking)
          withWorkingDayCount(5)
        }

        cas3ArrivalEntityFactory.produceAndPersist {
          withBooking(booking)
          withArrivalDate(LocalDate.parse("2023-03-28"))
        }

        cas3DepartureEntityFactory.produceAndPersist {
          withBooking(booking)
          withDateTime(OffsetDateTime.parse("2023-04-04T12:00:00.000Z"))
          withReason(departureReasonEntityFactory.produceAndPersist())
          withMoveOnCategory(moveOnCategoryEntityFactory.produceAndPersist())
        }

        val expectedReportRows = listOf(
          BedspaceOccupancyReportRow(
            probationRegion = user.probationRegion.name,
            pdu = user.probationDeliveryUnit?.name,
            localAuthority = premises.localAuthorityArea?.name,
            propertyRef = premises.name,
            addressLine1 = premises.addressLine1,
            town = premises.town,
            postCode = premises.postcode,
            bedspaceRef = bedspace.reference,
            bookedDaysActiveAndClosed = 4,
            confirmedDays = 0,
            provisionalDays = 0,
            scheduledTurnaroundDays = 5,
            effectiveTurnaroundDays = 7,
            voidDays = 0,
            totalBookedDays = 4,
            bedspaceStartDate = bedspace.createdAt.toLocalDate(),
            bedspaceEndDate = bedspace.endDate,
            bedspaceOnlineDays = 30,
            occupancyRate = 0.13333333333333333,
            uniquePropertyRef = premises.id.toShortBase58(),
            uniqueBedspaceRef = bedspace.id.toShortBase58(),
          ),
        )

        val expectedDataFrame = expectedReportRows.toDataFrame()

        webTestClient.get()
          .uri("/cas3/reports/bedOccupancy?startDate=2023-04-01&endDate=2023-04-30&probationRegionId=${user.probationRegion.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedspaceOccupancyReportRow>(Remove)
            assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  private fun setupPremisesWithABedspace(
    user: UserEntity,
    localAuthorityArea: LocalAuthorityAreaEntity = localAuthorityEntityFactory.produceAndPersist(),
    startDate: LocalDate = LocalDate.now().randomDateBefore(6),
    bedspaceReference: String = randomStringUpperCase(6)
  ): Pair<Cas3PremisesEntity, Cas3BedspacesEntity> {
    val premises = givenACas3Premises(
      probationDeliveryUnit = user.probationDeliveryUnit!!,
      localAuthorityArea  = localAuthorityArea,
      status = Cas3PremisesStatus.online,
    )
    val bedspaceStartDate = startDate.minusDays(100)
    val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
      withPremises(premises)
      withReference(bedspaceReference)
      withStartDate(bedspaceStartDate)
      withCreatedDate(bedspaceStartDate)
      withEndDate(null)
    }
    return premises to bedspace
  }
}
