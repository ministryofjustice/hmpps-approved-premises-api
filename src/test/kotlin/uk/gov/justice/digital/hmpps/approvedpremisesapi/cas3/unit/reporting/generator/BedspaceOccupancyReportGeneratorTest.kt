package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.reporting.generator

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.count
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.v2.Cas3v2ConfirmationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.v2.Cas3v2TurnaroundEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BedspaceOccupancyReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedspaceOccupancyReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.reporting.util.convertToCas3BedspaceOccupancyBedspaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.reporting.util.convertToCas3BedspaceOccupancyBookingReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.reporting.util.convertToCas3BedspaceOccupancyBookingTurnaroundReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.reporting.util.convertToCas3BedspaceOccupancyVoidBedpaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.reporting.util.convertToCas3BedspaceOccupancyVoidBedspaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import java.time.LocalDate
import java.time.OffsetDateTime

@SuppressWarnings("LargeClass")
class BedspaceOccupancyReportGeneratorTest {
  private val startDate = LocalDate.of(2023, 4, 1)
  private val endDate = LocalDate.of(2023, 4, 30)
  private val mockWorkingDayService = mockk<WorkingDayService>()

  private val bedspaceOccupancyReportGenerator = BedspaceOccupancyReportGenerator(mockWorkingDayService)

  @Test
  fun `Results for bedspaces are returned in the report`() {
    val cas3Premises = Cas3PremisesEntityFactory()
      .withDefaults()
      .produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(cas3Premises).produce()

    val cas3VoidBedspaces =
      Cas3VoidBedspaceEntityFactory().withBedspace(cas3Bedspace)
        .withStartDate(LocalDate.parse("2023-04-05"))
        .withEndDate(LocalDate.parse("2023-04-07"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val bedspaceOccupancyVoidBedspaceReportData = convertToCas3BedspaceOccupancyVoidBedpaceReportData(cas3VoidBedspaces)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(bedspaceOccupancyVoidBedspaceReportData),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::propertyRef]).isEqualTo(cas3Premises.name)
    assertThat(result[0][BedspaceOccupancyReportRow::uniquePropertyRef]).isEqualTo(cas3Premises.id.toShortBase58())
  }

  @Test
  fun `Results for bedspaces are returned in the report for 3 months`() {
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 7, 1)

    val cas3Premises = Cas3PremisesEntityFactory()
      .withDefaults()
      .produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(cas3Premises).produce()

    val temporaryAccommodationLostBed =
      Cas3VoidBedspaceEntityFactory()
        .withBedspace(cas3Bedspace)
        .withStartDate(LocalDate.parse("2023-04-05"))
        .withEndDate(LocalDate.parse("2023-04-07"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

    val bedspaceOccupancyReportGeneratorForThreeMonths = BedspaceOccupancyReportGenerator(mockWorkingDayService)

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val bedspaceOccupancyVoidBedspaceReportData = convertToCas3BedspaceOccupancyVoidBedpaceReportData(temporaryAccommodationLostBed)

    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(bedspaceOccupancyVoidBedspaceReportData),
    )

    val result = bedspaceOccupancyReportGeneratorForThreeMonths.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::propertyRef]).isEqualTo(cas3Premises.name)
    assertThat(result[0][BedspaceOccupancyReportRow::uniquePropertyRef]).isEqualTo(cas3Premises.id.toShortBase58())
  }

  @Test
  fun `Results for bedspaces from the specified probation region are returned in the report`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val probationRegion2 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea2 = LocalAuthorityEntityFactory().produce()

    val cas3PremisesInProbationRegion =
      Cas3PremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
        .withProbationDeliveryUnit(
          ProbationDeliveryUnitEntityFactory().withProbationRegion(probationRegion1).produce(),
        ).produce()

    val cas3BedspaceInProbationRegion = Cas3BedspaceEntityFactory()
      .withPremises(cas3PremisesInProbationRegion).produce()

    val cas3VoidBedspaceInProbationArea =
      Cas3VoidBedspaceEntityFactory()
        .withBedspace(cas3BedspaceInProbationRegion)
        .withStartDate(LocalDate.parse("2023-04-05")).withEndDate(LocalDate.parse("2023-04-07"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

    val cas3PremisesOutsideProbationRegion =
      Cas3PremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea2)
        .withProbationDeliveryUnit(
          ProbationDeliveryUnitEntityFactory().withProbationRegion(probationRegion2).produce(),
        ).produce()

    val cas3BedspaceOutsideProbationRegion = Cas3BedspaceEntityFactory()
      .withPremises(cas3PremisesOutsideProbationRegion).produce()

    Cas3VoidBedspaceEntityFactory()
      .withBedspace(cas3BedspaceOutsideProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .produce()

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3BedspaceInProbationRegion)
    val bedspaceOccupancyVoidBedspaceReportData = convertToCas3BedspaceOccupancyVoidBedpaceReportData(cas3VoidBedspaceInProbationArea)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(bedspaceOccupancyVoidBedspaceReportData),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(probationRegion1.id, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::propertyRef]).isEqualTo(
      cas3PremisesInProbationRegion.name,
    )
    assertThat(result[0][BedspaceOccupancyReportRow::uniquePropertyRef]).isEqualTo(
      cas3PremisesInProbationRegion.id.toShortBase58(),
    )
  }

  @Test
  fun `Results for beds from all probation regions are returned in the report if no probation region ID is provided`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val probationRegion2 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea2 = LocalAuthorityEntityFactory().produce()

    val cas3PremisesInProbationRegion =
      Cas3PremisesEntityFactory()
        .withLocalAuthorityArea(localAuthorityArea1)
        .withProbationDeliveryUnit(
          ProbationDeliveryUnitEntityFactory().withProbationRegion(probationRegion1).produce(),
        ).produce()

    val cas3BedspaceInProbationRegion = Cas3BedspaceEntityFactory()
      .withPremises(cas3PremisesInProbationRegion).produce()

    val cas3VoidBedspaceInProbationArea =
      Cas3VoidBedspaceEntityFactory()
        .withBedspace(cas3BedspaceInProbationRegion)
        .withStartDate(LocalDate.parse("2023-04-05")).withEndDate(LocalDate.parse("2023-04-07"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

    val cas3PremisesOutsideProbationRegion =
      Cas3PremisesEntityFactory()
        .withLocalAuthorityArea(localAuthorityArea2)
        .withProbationDeliveryUnit(
          ProbationDeliveryUnitEntityFactory().withProbationRegion(probationRegion2).produce(),
        ).produce()

    val cas3BedspaceOutsideProbationRegion =
      Cas3BedspaceEntityFactory()
        .withPremises(cas3PremisesOutsideProbationRegion)
        .produce()

    val cas3VoidBedspaceOutsideProbationArea =
      Cas3VoidBedspaceEntityFactory()
        .withBedspace(cas3BedspaceOutsideProbationRegion)
        .withStartDate(LocalDate.parse("2023-04-05")).withEndDate(LocalDate.parse("2023-04-07"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

    val bedspaceOccupancyBedspaceInProbationRegionReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3BedspaceInProbationRegion)
    val bedspaceOccupancyVoidBedspaceInProbationRegionReportData = convertToCas3BedspaceOccupancyVoidBedpaceReportData(cas3VoidBedspaceInProbationArea)
    val bedspaceOccupancyInProbationRegionReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceInProbationRegionReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(bedspaceOccupancyVoidBedspaceInProbationRegionReportData),
    )
    val bedspaceOccupancyBedspaceOutsideProbationRegionReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3BedspaceOutsideProbationRegion)
    val bedspaceOccupancyVoidBedspaceOutsideProbationRegionReportData = convertToCas3BedspaceOccupancyVoidBedpaceReportData(cas3VoidBedspaceOutsideProbationArea)
    val bedspaceOccupancyOutsideProbationRegionReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceOutsideProbationRegionReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(bedspaceOccupancyVoidBedspaceOutsideProbationRegionReportData),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyInProbationRegionReportData, bedspaceOccupancyOutsideProbationRegionReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(2)
    assertThat(result[0][BedspaceOccupancyReportRow::propertyRef]).isEqualTo(
      cas3PremisesInProbationRegion.name,
    )
    assertThat(result[0][BedspaceOccupancyReportRow::uniquePropertyRef]).isEqualTo(
      cas3PremisesInProbationRegion.id.toShortBase58(),
    )
    assertThat(result[1][BedspaceOccupancyReportRow::propertyRef]).isEqualTo(
      cas3PremisesOutsideProbationRegion.name,
    )
    assertThat(result[1][BedspaceOccupancyReportRow::uniquePropertyRef]).isEqualTo(
      cas3PremisesOutsideProbationRegion.id.toShortBase58(),
    )
  }

  @Test
  fun `bookedDaysActiveAndClosed shows the total number of days within the month for Bookings that are marked as arrived`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = Cas3PremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory().withProbationRegion(probationRegion1).produce(),
      ).produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .produce()

    // An irrelevant Booking - does not have an Arrival set
    Cas3BookingEntityFactory()
      .withBedspace(cas3Bedspace).withPremises(premises)
      .withArrivalDate(LocalDate.parse("2023-04-05"))
      .withDepartureDate(LocalDate.parse("2023-04-10"))
      .produce()

    val relevantBookingStraddlingStartOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2023-03-28"))
        .withDepartureDate(LocalDate.parse("2023-04-04"))
        .produce().apply {
          arrivals += Cas3ArrivalEntityFactory().withBooking(this).produce()
        }

    val relevantBookingStraddlingEndOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2023-04-28"))
        .withDepartureDate(LocalDate.parse("2023-05-04"))
        .produce().apply {
          arrivals += Cas3ArrivalEntityFactory().withBooking(this).produce()
        }

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val relevantBookingStraddlingStartOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingEndOfMonth)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::bookedDaysActiveAndClosed]).isEqualTo(7)
  }

  @Test
  fun `confirmedDays shows the total number of days within the month for Bookings that are marked as confirmed but not arrived`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      ).produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .produce()

    // An irrelevant Booking - does not have a Confirmation set
    Cas3BookingEntityFactory()
      .withBedspace(cas3Bedspace)
      .withPremises(premises)
      .withArrivalDate(LocalDate.parse("2023-04-05"))
      .withDepartureDate(LocalDate.parse("2023-04-10"))
      .produce()

    val relevantBookingStraddlingStartOfMonth =
      Cas3BookingEntityFactory().withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2023-03-28"))
        .withDepartureDate(LocalDate.parse("2023-04-04"))
        .produce().apply {
          confirmation = Cas3v2ConfirmationEntityFactory().withBooking(this).produce()
        }

    val relevantBookingStraddlingEndOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2023-04-28"))
        .withDepartureDate(LocalDate.parse("2023-05-04"))
        .produce().apply {
          confirmation = Cas3v2ConfirmationEntityFactory().withBooking(this).produce()
        }

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val relevantBookingStraddlingStartOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingEndOfMonth)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::confirmedDays]).isEqualTo(7)
  }

  @Test
  fun `scheduledTurnaroundDays shows the number of working days in the month for the turnaround`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      ).produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .produce()

    val relevantBookingStraddlingStartOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2023-03-28"))
        .withDepartureDate(LocalDate.parse("2023-04-04")).produce().apply {
          turnarounds += Cas3v2TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(5).produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingStartOfMonth.departureDate,
        relevantBookingStraddlingStartOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2023-04-10")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2023-04-05"),
        LocalDate.parse("2023-04-10"),
      )
    } returns 5

    val relevantBookingStraddlingEndOfMonth =
      Cas3BookingEntityFactory().withBedspace(cas3Bedspace).withPremises(premises).withArrivalDate(LocalDate.parse("2023-04-25"))
        .withDepartureDate(LocalDate.parse("2023-04-27")).produce().apply {
          turnarounds += Cas3v2TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(3).produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingEndOfMonth.departureDate,
        relevantBookingStraddlingEndOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2023-05-01")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2023-04-28"),
        LocalDate.parse("2023-04-30"),
      )
    } returns 1

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val relevantBookingStraddlingStartOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingTurnaroundStraddlingStartOfMonthReportData = convertToCas3BedspaceOccupancyBookingTurnaroundReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingEndOfMonth)
    val relevantBookingTurnaroundStraddlingEndOfMonthReportData = convertToCas3BedspaceOccupancyBookingTurnaroundReportData(relevantBookingStraddlingEndOfMonth)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(relevantBookingTurnaroundStraddlingStartOfMonthReportData, relevantBookingTurnaroundStraddlingEndOfMonthReportData),
      listOf(),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::scheduledTurnaroundDays]).isEqualTo(6)
  }

  @Test
  fun `effectiveTurnaroundDays shows the total number of days (regardless of whether working days) in the month for the turnaround`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      ).produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .produce()

    val relevantBookingStraddlingStartOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2023-03-28"))
        .withDepartureDate(LocalDate.parse("2023-04-04"))
        .produce().apply {
          turnarounds += Cas3v2TurnaroundEntityFactory()
            .withBooking(this)
            .withWorkingDayCount(5)
            .produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingStartOfMonth.departureDate,
        relevantBookingStraddlingStartOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2023-04-10")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2023-04-05"),
        LocalDate.parse("2023-04-10"),
      )
    } returns 4

    val relevantBookingStraddlingEndOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2023-04-25"))
        .withDepartureDate(LocalDate.parse("2023-04-27"))
        .produce().apply {
          turnarounds += Cas3v2TurnaroundEntityFactory()
            .withBooking(this)
            .withWorkingDayCount(3)
            .produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingEndOfMonth.departureDate,
        relevantBookingStraddlingEndOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2023-05-01")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2023-04-28"),
        LocalDate.parse("2023-04-30"),
      )
    } returns 1

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val relevantBookingStraddlingStartOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingTurnaroundStraddlingStartOfMonthReportData = convertToCas3BedspaceOccupancyBookingTurnaroundReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingEndOfMonth)
    val relevantBookingTurnaroundStraddlingEndOfMonthReportData = convertToCas3BedspaceOccupancyBookingTurnaroundReportData(relevantBookingStraddlingEndOfMonth)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(relevantBookingTurnaroundStraddlingStartOfMonthReportData, relevantBookingTurnaroundStraddlingEndOfMonthReportData),
      listOf(),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::effectiveTurnaroundDays]).isEqualTo(9)
  }

  @Test
  fun `voidDays shows the total number of days in the month for voids`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      ).produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .produce()

    val relevantVoidStraddlingStartOfMonth =
      Cas3VoidBedspaceEntityFactory().withBedspace(cas3Bedspace)
        .withStartDate(LocalDate.parse("2023-03-28"))
        .withEndDate(LocalDate.parse("2023-04-04"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

    val relevantVoidStraddlingEndOfMonth =
      Cas3VoidBedspaceEntityFactory().withBedspace(cas3Bedspace)
        .withStartDate(LocalDate.parse("2023-04-25"))
        .withEndDate(LocalDate.parse("2023-05-03"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val relevantVoidStraddlingStartOfMonthReportData = convertToCas3BedspaceOccupancyVoidBedspaceReportData(relevantVoidStraddlingStartOfMonth)
    val relevantVoidStraddlingEndOfMonthReportData = convertToCas3BedspaceOccupancyVoidBedspaceReportData(relevantVoidStraddlingEndOfMonth)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(relevantVoidStraddlingStartOfMonthReportData, relevantVoidStraddlingEndOfMonthReportData),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::voidDays]).isEqualTo(10)
  }

  @Test
  fun `totalBookedDays shows the combined total days in the month of non-cancelled bookings, not non-cancelled voids or turnarounds - bedspaceOnlineDays, occupancyRate show correctly`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      ).produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .withCreatedAt(OffsetDateTime.parse("2023-02-16T14:03:00+00:00"))
      .produce()

    val departureReason = DepartureReasonEntityFactory().produce()
    val moveOnCategory = MoveOnCategoryEntityFactory().produce()

    val relevantBookingStraddlingStartOfMonth =
      Cas3BookingEntityFactory().withBedspace(cas3Bedspace).withPremises(premises).withArrivalDate(LocalDate.parse("2023-03-28"))
        .withDepartureDate(LocalDate.parse("2023-04-04")).produce().apply {
          turnarounds += Cas3v2TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(5).produce()
          arrivals += Cas3ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2023-03-28")).produce()
          departures += Cas3DepartureEntityFactory().withBooking(this)
            .withDateTime(OffsetDateTime.parse("2023-04-04T12:00:00.000Z"))
            .withReason(departureReason)
            .withMoveOnCategory(moveOnCategory)
            .produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingStartOfMonth.departureDate,
        relevantBookingStraddlingStartOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2023-04-09")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2023-04-05"),
        LocalDate.parse("2023-04-09"),
      )
    } returns 4

    val relevantBookingStraddlingEndOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2023-04-25"))
        .withDepartureDate(LocalDate.parse("2023-04-27"))
        .produce()
        .apply {
          turnarounds += Cas3v2TurnaroundEntityFactory()
            .withBooking(this)
            .withWorkingDayCount(4)
            .produce()
          arrivals += Cas3ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2023-04-25")).produce()
          departures += Cas3DepartureEntityFactory()
            .withBooking(this)
            .withDateTime(OffsetDateTime.parse("2023-04-27T12:00:00.000Z"))
            .withReason(departureReason)
            .withMoveOnCategory(moveOnCategory).produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingEndOfMonth.departureDate,
        relevantBookingStraddlingEndOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2023-05-01")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2023-04-28"),
        LocalDate.parse("2023-04-30"),
      )
    } returns 1

    val relevantVoidStraddlingStartOfMonth =
      Cas3VoidBedspaceEntityFactory()
        .withBedspace(cas3Bedspace)
        .withStartDate(LocalDate.parse("2023-03-28"))
        .withEndDate(LocalDate.parse("2023-04-04"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

    val relevantVoidStraddlingEndOfMonth =
      Cas3VoidBedspaceEntityFactory()
        .withBedspace(cas3Bedspace)
        .withStartDate(LocalDate.parse("2023-04-25"))
        .withEndDate(LocalDate.parse("2023-05-03"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val relevantBookingStraddlingStartOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingEndOfMonth)
    val relevantVoidStraddlingStartOfMonthReportData = convertToCas3BedspaceOccupancyVoidBedspaceReportData(relevantVoidStraddlingStartOfMonth)
    val relevantVoidStraddlingEndOfMonthReportData = convertToCas3BedspaceOccupancyVoidBedspaceReportData(relevantVoidStraddlingEndOfMonth)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(relevantVoidStraddlingStartOfMonthReportData, relevantVoidStraddlingEndOfMonthReportData),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    // The following should be counted for a total of 7 booked days:
    // 4 days for relevantBookingStraddlingStartOfMonth
    // 3 days for relevantBookingStraddlingEndOfMonth
    //
    // These should not be counted:
    // 5 days for turnaround of relevantBookingStraddlingStartOfMonth
    // 3 days for turnaround of relevantBookingStraddlingEndOfMonth
    // 4 days for relevantVoidStraddlingStartOfMonth
    // 6 days for relevantVoidStraddlingEndOfMonth
    assertThat(result[0][BedspaceOccupancyReportRow::totalBookedDays]).isEqualTo(7)
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceOnlineDays]).isEqualTo(30)
    assertThat(result[0][BedspaceOccupancyReportRow::occupancyRate]).isEqualTo(7.toDouble() / 30)
  }

  @Test
  fun `bedspaceStartDate show bedspace created date`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      )
      .produce()

    val bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .withCreatedAt(OffsetDateTime.parse("2024-01-16T14:03:00+00:00"))
      .produce()

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(bedspace)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceStartDate]).isEqualTo(LocalDate.parse("2024-01-16"))
  }

  @Test
  fun `bedspaceEndDate show bedspace end date when it is not null`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()
    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      ).produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .withCreatedAt(OffsetDateTime.parse("2024-02-16T14:03:00+00:00"))
      .withEndDate(LocalDate.parse("2024-05-12"))
      .produce()

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceEndDate]).isEqualTo(LocalDate.parse("2024-05-12"))
  }

  @Test
  fun `bedspaceEndDate show nothing when bedspace end date is null`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      ).produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .withEndDate(null)
      .withCreatedAt(OffsetDateTime.parse("2024-02-16T14:03:00+00:00"))
      .produce()

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceEndDate]).isNull()
  }

  @Test
  fun `bedspaceOnlineDays shows number of days between bedspaceStartDate and report end date when bedspaceStartDate is later than report start date and bedspaceEndDate is null`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      ).produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .withEndDate(null)
      .withCreatedAt(OffsetDateTime.parse("2024-02-05T14:03:00+00:00"))
      .produce()

    val departureReason = DepartureReasonEntityFactory().produce()
    val moveOnCategory = MoveOnCategoryEntityFactory().produce()

    val relevantBookingStraddlingStartOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2024-02-07"))
        .withDepartureDate(LocalDate.parse("2024-02-12"))
        .produce().apply {
          turnarounds += Cas3v2TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(2).produce()
          arrivals += Cas3ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-07")).produce()
          departures += Cas3DepartureEntityFactory()
            .withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-12T12:00:00.000Z"))
            .withReason(departureReason)
            .withMoveOnCategory(moveOnCategory)
            .produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingStartOfMonth.departureDate,
        relevantBookingStraddlingStartOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2024-02-14")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2024-02-13"),
        LocalDate.parse("2024-02-14"),
      )
    } returns 2

    val relevantBookingStraddlingEndOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2024-02-16"))
        .withDepartureDate(LocalDate.parse("2024-02-22"))
        .produce().apply {
          turnarounds += Cas3v2TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(3).produce()
          arrivals += Cas3ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-16")).produce()
          departures += Cas3DepartureEntityFactory()
            .withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-22T12:00:00.000Z"))
            .withReason(departureReason)
            .withMoveOnCategory(moveOnCategory)
            .produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingEndOfMonth.departureDate,
        relevantBookingStraddlingEndOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2024-02-27")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2024-02-23"),
        LocalDate.parse("2024-02-27"),
      )
    } returns 3

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val relevantBookingStraddlingStartOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingEndOfMonth)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::totalBookedDays]).isEqualTo(13)
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceStartDate]).isEqualTo(LocalDate.parse("2024-02-05"))
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceEndDate]).isNull()
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceOnlineDays]).isEqualTo(25)
    assertThat(result[0][BedspaceOccupancyReportRow::occupancyRate]).isEqualTo(13.toDouble() / 25)
  }

  @Test
  fun `bedspaceOnlineDays shows number of days between bedspaceStartDate and bedspaceEndDate when bedspace dates are in report dates range`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      ).produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .withCreatedAt(OffsetDateTime.parse("2024-02-05T14:03:00+00:00"))
      .withEndDate(LocalDate.parse("2024-02-27"))
      .produce()

    val departureReason = DepartureReasonEntityFactory().produce()
    val moveOnCategory = MoveOnCategoryEntityFactory().produce()

    val relevantBookingStraddlingStartOfMonth =
      Cas3BookingEntityFactory().withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2024-02-07"))
        .withDepartureDate(LocalDate.parse("2024-02-12"))
        .produce().apply {
          turnarounds += Cas3v2TurnaroundEntityFactory()
            .withBooking(this)
            .withWorkingDayCount(2)
            .produce()
          arrivals += Cas3ArrivalEntityFactory()
            .withBooking(this)
            .withArrivalDate(LocalDate.parse("2024-02-07"))
            .produce()
          departures += Cas3DepartureEntityFactory()
            .withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-12T12:00:00.000Z"))
            .withReason(departureReason)
            .withMoveOnCategory(moveOnCategory)
            .produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingStartOfMonth.departureDate,
        relevantBookingStraddlingStartOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2024-02-14")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2024-02-13"),
        LocalDate.parse("2024-02-14"),
      )
    } returns 2

    val relevantBookingStraddlingEndOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2024-02-16"))
        .withDepartureDate(LocalDate.parse("2024-02-22"))
        .produce().apply {
          turnarounds += Cas3v2TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(3).produce()
          arrivals += Cas3ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-16")).produce()
          departures += Cas3DepartureEntityFactory()
            .withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-22T12:00:00.000Z"))
            .withReason(departureReason)
            .withMoveOnCategory(moveOnCategory)
            .produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingEndOfMonth.departureDate,
        relevantBookingStraddlingEndOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2024-02-27")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2024-02-23"),
        LocalDate.parse("2024-02-27"),
      )
    } returns 3

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val relevantBookingStraddlingStartOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData = convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingEndOfMonth)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::totalBookedDays]).isEqualTo(13)
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceStartDate]).isEqualTo(LocalDate.parse("2024-02-05"))
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceEndDate]).isEqualTo(LocalDate.parse("2024-02-27"))
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceOnlineDays]).isEqualTo(23)
    assertThat(result[0][BedspaceOccupancyReportRow::occupancyRate]).isEqualTo(13.toDouble() / 23)
  }

  @Test
  fun `bedspaceOnlineDays shows number of days between report start date and bedspaceEndtDate when bedspace star and end dates are earlier than report start and end dates`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      ).produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .withCreatedAt(OffsetDateTime.parse("2024-01-17T14:03:00+00:00"))
      .withEndDate(LocalDate.parse("2024-02-25"))
      .produce()
    val departureReason = DepartureReasonEntityFactory().produce()
    val moveOnCategory = MoveOnCategoryEntityFactory().produce()

    val relevantBookingStraddlingStartOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2024-02-02"))
        .withDepartureDate(LocalDate.parse("2024-02-07"))
        .produce().apply {
          turnarounds += Cas3v2TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(2).produce()
          arrivals += Cas3ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-02")).produce()
          departures += Cas3DepartureEntityFactory()
            .withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-07T12:00:00.000Z"))
            .withReason(departureReason)
            .withMoveOnCategory(moveOnCategory)
            .produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingStartOfMonth.departureDate,
        relevantBookingStraddlingStartOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2024-02-09")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2024-02-08"),
        LocalDate.parse("2024-02-09"),
      )
    } returns 2

    val relevantBookingStraddlingEndOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2024-02-10"))
        .withDepartureDate(LocalDate.parse("2024-02-15"))
        .produce().apply {
          turnarounds += Cas3v2TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(3).produce()
          arrivals += Cas3ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-10")).produce()
          departures += Cas3DepartureEntityFactory().withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-15T12:00:00.000Z"))
            .withReason(departureReason)
            .withMoveOnCategory(moveOnCategory)
            .produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingEndOfMonth.departureDate,
        relevantBookingStraddlingEndOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2024-02-20")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2024-02-16"),
        LocalDate.parse("2024-02-20"),
      )
    } returns 3

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val relevantBookingStraddlingStartOfMonthReportData =
      convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData =
      convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingEndOfMonth)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::totalBookedDays]).isEqualTo(12)
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceStartDate]).isEqualTo(LocalDate.parse("2024-01-17"))
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceEndDate]).isEqualTo(LocalDate.parse("2024-02-25"))
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceOnlineDays]).isEqualTo(25)
    assertThat(result[0][BedspaceOccupancyReportRow::occupancyRate]).isEqualTo(12.toDouble() / 25)
  }

  @Test
  fun `bedspaceOnlineDays shows number of days between report start and end date when bedspace start and end dates are out of the report dates range`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      ).produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .withCreatedAt(OffsetDateTime.parse("2024-01-17T14:03:00+00:00"))
      .withEndDate(LocalDate.parse("2024-03-15"))
      .produce()

    val departureReason = DepartureReasonEntityFactory().produce()
    val moveOnCategory = MoveOnCategoryEntityFactory().produce()

    val relevantBookingStraddlingStartOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2024-02-02"))
        .withDepartureDate(LocalDate.parse("2024-02-07"))
        .produce().apply {
          turnarounds += Cas3v2TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(2).produce()
          arrivals += Cas3ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-02")).produce()
          departures += Cas3DepartureEntityFactory()
            .withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-07T12:00:00.000Z"))
            .withReason(departureReason)
            .withMoveOnCategory(moveOnCategory)
            .produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingStartOfMonth.departureDate,
        relevantBookingStraddlingStartOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2024-02-09")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2024-02-08"),
        LocalDate.parse("2024-02-09"),
      )
    } returns 2

    val relevantBookingStraddlingEndOfMonth =
      Cas3BookingEntityFactory()
        .withBedspace(cas3Bedspace)
        .withPremises(premises)
        .withArrivalDate(LocalDate.parse("2024-02-10"))
        .withDepartureDate(LocalDate.parse("2024-02-15"))
        .produce().apply {
          turnarounds += Cas3v2TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(3).produce()
          arrivals += Cas3ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-10")).produce()
          departures += Cas3DepartureEntityFactory()
            .withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-15T12:00:00.000Z"))
            .withReason(departureReason)
            .withMoveOnCategory(moveOnCategory)
            .produce()
        }

    every {
      mockWorkingDayService.addWorkingDays(
        relevantBookingStraddlingEndOfMonth.departureDate,
        relevantBookingStraddlingEndOfMonth.turnaround!!.workingDayCount,
      )
    } returns LocalDate.parse("2024-02-20")

    every {
      mockWorkingDayService.getWorkingDaysCount(
        LocalDate.parse("2024-02-16"),
        LocalDate.parse("2024-02-20"),
      )
    } returns 3

    val bedspaceOccupancyBedspaceReportData = convertToCas3BedspaceOccupancyBedspaceReportData(cas3Bedspace)
    val relevantBookingStraddlingStartOfMonthReportData =
      convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData =
      convertToCas3BedspaceOccupancyBookingReportData(relevantBookingStraddlingEndOfMonth)
    val bedspaceOccupancyReportData = BedspaceOccupancyReportData(
      bedspaceOccupancyBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedspaceOccupancyReportGenerator.createReport(
      listOf(bedspaceOccupancyReportData),
      BedspaceOccupancyReportProperties(null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedspaceOccupancyReportRow::totalBookedDays]).isEqualTo(12)
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceStartDate]).isEqualTo(LocalDate.parse("2024-01-17"))
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceEndDate]).isEqualTo(LocalDate.parse("2024-03-15"))
    assertThat(result[0][BedspaceOccupancyReportRow::bedspaceOnlineDays]).isEqualTo(29)
    assertThat(result[0][BedspaceOccupancyReportRow::occupancyRate]).isEqualTo(12.toDouble() / 29)
  }
}
