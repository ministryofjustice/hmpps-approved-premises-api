package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.generator

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.count
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ConfirmationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TurnaroundEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BedUtilisationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUtilisationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUtilisationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.util.convertToCas3BedUtilisationBedspaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.util.convertToCas3BedUtilisationBookingReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.util.convertToCas3BedUtilisationBookingTurnaroundReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.util.convertToCas3BedUtilisationLostBedReportData
import java.time.LocalDate
import java.time.OffsetDateTime

@SuppressWarnings("LargeClass")
class BedUtilisationReportGeneratorTest {
  private val startDate = LocalDate.of(2023, 4, 1)
  private val endDate = LocalDate.of(2023, 4, 30)
  private val mockWorkingDayService = mockk<WorkingDayService>()

  private val bedUtilisationReportGenerator = BedUtilisationReportGenerator(mockWorkingDayService)

  @Test
  fun `Only results for beds from the specified service are returned in the report`() {
    val temporaryAccommodationPremises =
      TemporaryAccommodationPremisesEntityFactory().withUnitTestControlTestProbationAreaAndLocalAuthority().produce()

    val temporaryAccommodationRoom = RoomEntityFactory().withPremises(temporaryAccommodationPremises).produce()

    val temporaryAccommodationBed = BedEntityFactory().withRoom(temporaryAccommodationRoom).produce()

    val temporaryAccommodationLostBed =
      Cas3VoidBedspaceEntityFactory().withBed(temporaryAccommodationBed).withStartDate(LocalDate.parse("2023-04-05"))
        .withEndDate(LocalDate.parse("2023-04-07")).withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withPremises(temporaryAccommodationPremises).produce()

    val approvedPremises =
      ApprovedPremisesEntityFactory().withUnitTestControlTestProbationAreaAndLocalAuthority().produce()

    val approvedPremisesRoom = RoomEntityFactory().withPremises(approvedPremises).produce()

    val approvedPremisesBed = BedEntityFactory().withRoom(approvedPremisesRoom).produce()

    Cas3VoidBedspaceEntityFactory().withBed(approvedPremisesBed).withStartDate(LocalDate.parse("2023-04-10"))
      .withEndDate(LocalDate.parse("2023-04-17")).withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(approvedPremises).produce()

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(temporaryAccommodationBed)
    val bedUtilisationLostBedReportData = convertToCas3BedUtilisationLostBedReportData(temporaryAccommodationLostBed)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(bedUtilisationLostBedReportData),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::propertyRef]).isEqualTo(temporaryAccommodationPremises.name)
    assertThat(result[0][BedUtilisationReportRow::uniquePropertyRef]).isEqualTo(temporaryAccommodationPremises.id.toShortBase58())
  }

  @Test
  fun `Only results for beds from the temporary accommodation service are returned in the report for 3 months`() {
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 7, 1)
    val temporaryAccommodationPremises =
      TemporaryAccommodationPremisesEntityFactory().withUnitTestControlTestProbationAreaAndLocalAuthority().produce()

    val temporaryAccommodationRoom = RoomEntityFactory().withPremises(temporaryAccommodationPremises).produce()

    val temporaryAccommodationBed = BedEntityFactory().withRoom(temporaryAccommodationRoom).produce()

    val temporaryAccommodationLostBed =
      Cas3VoidBedspaceEntityFactory().withBed(temporaryAccommodationBed).withStartDate(LocalDate.parse("2023-04-05"))
        .withEndDate(LocalDate.parse("2023-04-07")).withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withPremises(temporaryAccommodationPremises).produce()

    val approvedPremises =
      ApprovedPremisesEntityFactory().withUnitTestControlTestProbationAreaAndLocalAuthority().produce()

    val approvedPremisesRoom = RoomEntityFactory().withPremises(approvedPremises).produce()

    val approvedPremisesBed = BedEntityFactory().withRoom(approvedPremisesRoom).produce()

    Cas3VoidBedspaceEntityFactory().withBed(approvedPremisesBed).withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07")).withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(approvedPremises).produce()

    val bedUtilisationReportGeneratorForThreeMonths = BedUtilisationReportGenerator(mockWorkingDayService)

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(temporaryAccommodationBed)
    val bedUtilisationLostBedReportData = convertToCas3BedUtilisationLostBedReportData(temporaryAccommodationLostBed)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(bedUtilisationLostBedReportData),
    )

    val result = bedUtilisationReportGeneratorForThreeMonths.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::propertyRef]).isEqualTo(temporaryAccommodationPremises.name)
    assertThat(result[0][BedUtilisationReportRow::uniquePropertyRef]).isEqualTo(temporaryAccommodationPremises.id.toShortBase58())
  }

  @Test
  fun `Only results for beds from the approved premises service are returned in the report for 1 months`() {
    val temporaryAccommodationPremises =
      TemporaryAccommodationPremisesEntityFactory().withUnitTestControlTestProbationAreaAndLocalAuthority().produce()

    val temporaryAccommodationRoom = RoomEntityFactory().withPremises(temporaryAccommodationPremises).produce()

    val temporaryAccommodationBed = BedEntityFactory().withRoom(temporaryAccommodationRoom).produce()

    Cas3VoidBedspaceEntityFactory().withBed(temporaryAccommodationBed).withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07")).withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremises).produce()

    val approvedPremises =
      ApprovedPremisesEntityFactory().withUnitTestControlTestProbationAreaAndLocalAuthority().produce()

    val approvedPremisesRoom = RoomEntityFactory().withPremises(approvedPremises).produce()

    val approvedPremisesBed = BedEntityFactory().withRoom(approvedPremisesRoom).produce()

    val approvedPremisesLostBed =
      Cas3VoidBedspaceEntityFactory().withBed(approvedPremisesBed).withStartDate(LocalDate.parse("2023-04-05"))
        .withEndDate(LocalDate.parse("2023-04-07")).withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withPremises(approvedPremises).produce()

    val bedUtilisationReportGeneratorForThreeMonths = BedUtilisationReportGenerator(mockWorkingDayService)

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(approvedPremisesBed)
    val bedUtilisationLostBedReportData = convertToCas3BedUtilisationLostBedReportData(approvedPremisesLostBed)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(bedUtilisationLostBedReportData),
    )

    val result = bedUtilisationReportGeneratorForThreeMonths.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.approvedPremises, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::propertyRef]).isEqualTo(approvedPremises.name)
    assertThat(result[0][BedUtilisationReportRow::uniquePropertyRef]).isEqualTo(approvedPremises.id.toShortBase58())
  }

  @Test
  fun `Only results for beds from the specified probation region are returned in the report`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val probationRegion2 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea2 = LocalAuthorityEntityFactory().produce()

    val temporaryAccommodationPremisesInProbationRegion =
      TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
        .withProbationRegion(probationRegion1).produce()

    val temporaryAccommodationRoomInProbationRegion =
      RoomEntityFactory().withPremises(temporaryAccommodationPremisesInProbationRegion).produce()

    val temporaryAccommodationBedInProbationRegion =
      BedEntityFactory().withRoom(temporaryAccommodationRoomInProbationRegion).produce()

    val temporaryAccommodationLostBedInProbationArea =
      Cas3VoidBedspaceEntityFactory().withBed(temporaryAccommodationBedInProbationRegion)
        .withStartDate(LocalDate.parse("2023-04-05")).withEndDate(LocalDate.parse("2023-04-07"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withPremises(temporaryAccommodationPremisesInProbationRegion).produce()

    val temporaryAccommodationPremisesOutsideProbationRegion =
      TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea2)
        .withProbationRegion(probationRegion2).produce()

    val temporaryAccommodationRoomOutsideProbationRegion =
      RoomEntityFactory().withPremises(temporaryAccommodationPremisesOutsideProbationRegion).produce()

    val temporaryAccommodationBedOutsideProbationRegion =
      BedEntityFactory().withRoom(temporaryAccommodationRoomOutsideProbationRegion).produce()

    val temporaryAccommodationLostBedOutsideProbationArea =
      Cas3VoidBedspaceEntityFactory().withBed(temporaryAccommodationBedOutsideProbationRegion)
        .withStartDate(LocalDate.parse("2023-04-05")).withEndDate(LocalDate.parse("2023-04-07"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withPremises(temporaryAccommodationPremisesOutsideProbationRegion).produce()

    val bedUtilisationBedspaceReportData =
      convertToCas3BedUtilisationBedspaceReportData(temporaryAccommodationBedInProbationRegion)
    val bedUtilisationLostBedReportData =
      convertToCas3BedUtilisationLostBedReportData(temporaryAccommodationLostBedInProbationArea)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(bedUtilisationLostBedReportData),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, probationRegion1.id, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::propertyRef]).isEqualTo(
      temporaryAccommodationPremisesInProbationRegion.name,
    )
    assertThat(result[0][BedUtilisationReportRow::uniquePropertyRef]).isEqualTo(
      temporaryAccommodationPremisesInProbationRegion.id.toShortBase58(),
    )
  }

  @Test
  fun `Results for beds from all probation regions are returned in the report if no probation region ID is provided`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val probationRegion2 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea2 = LocalAuthorityEntityFactory().produce()

    val temporaryAccommodationPremisesInProbationRegion =
      TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
        .withProbationRegion(probationRegion1).produce()

    val temporaryAccommodationRoomInProbationRegion =
      RoomEntityFactory().withPremises(temporaryAccommodationPremisesInProbationRegion).produce()

    val temporaryAccommodationBedInProbationRegion =
      BedEntityFactory().withRoom(temporaryAccommodationRoomInProbationRegion).produce()

    val temporaryAccommodationLostBedInProbationArea =
      Cas3VoidBedspaceEntityFactory().withBed(temporaryAccommodationBedInProbationRegion)
        .withStartDate(LocalDate.parse("2023-04-05")).withEndDate(LocalDate.parse("2023-04-07"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withPremises(temporaryAccommodationPremisesInProbationRegion).produce()

    val temporaryAccommodationPremisesOutsideProbationRegion =
      TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea2)
        .withProbationRegion(probationRegion2).produce()

    val temporaryAccommodationRoomOutsideProbationRegion =
      RoomEntityFactory().withPremises(temporaryAccommodationPremisesOutsideProbationRegion).produce()

    val temporaryAccommodationBedOutsideProbationRegion =
      BedEntityFactory().withRoom(temporaryAccommodationRoomOutsideProbationRegion).produce()

    val temporaryAccommodationLostBedOutsideProbationArea =
      Cas3VoidBedspaceEntityFactory().withBed(temporaryAccommodationBedOutsideProbationRegion)
        .withStartDate(LocalDate.parse("2023-04-05")).withEndDate(LocalDate.parse("2023-04-07"))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withPremises(temporaryAccommodationPremisesOutsideProbationRegion).produce()

    val bedUtilisationBedInProbationRegionReportData =
      convertToCas3BedUtilisationBedspaceReportData(temporaryAccommodationBedInProbationRegion)
    val bedUtilisationLostBedInProbationRegionReportData =
      convertToCas3BedUtilisationLostBedReportData(temporaryAccommodationLostBedInProbationArea)
    val bedUtilisationInProbationRegionReportData = BedUtilisationReportData(
      bedUtilisationBedInProbationRegionReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(bedUtilisationLostBedInProbationRegionReportData),
    )
    val bedUtilisationBedOutsideProbationRegionReportData =
      convertToCas3BedUtilisationBedspaceReportData(temporaryAccommodationBedOutsideProbationRegion)
    val bedUtilisationLostBedOutsideProbationRegionReportData =
      convertToCas3BedUtilisationLostBedReportData(temporaryAccommodationLostBedOutsideProbationArea)
    val bedUtilisationOutsideProbationRegionReportData = BedUtilisationReportData(
      bedUtilisationBedOutsideProbationRegionReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(bedUtilisationLostBedOutsideProbationRegionReportData),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationInProbationRegionReportData, bedUtilisationOutsideProbationRegionReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(2)
    assertThat(result[0][BedUtilisationReportRow::propertyRef]).isEqualTo(
      temporaryAccommodationPremisesInProbationRegion.name,
    )
    assertThat(result[0][BedUtilisationReportRow::uniquePropertyRef]).isEqualTo(
      temporaryAccommodationPremisesInProbationRegion.id.toShortBase58(),
    )
    assertThat(result[1][BedUtilisationReportRow::propertyRef]).isEqualTo(
      temporaryAccommodationPremisesOutsideProbationRegion.name,
    )
    assertThat(result[1][BedUtilisationReportRow::uniquePropertyRef]).isEqualTo(
      temporaryAccommodationPremisesOutsideProbationRegion.id.toShortBase58(),
    )
  }

  @Test
  fun `bookedDaysActiveAndClosed shows the total number of days within the month for Bookings that are marked as arrived`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed = BedEntityFactory().withRoom(room).produce()

    // An irrelevant Booking - does not have an Arrival set
    BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2023-04-05"))
      .withDepartureDate(LocalDate.parse("2023-04-10")).produce()

    val relevantBookingStraddlingStartOfMonth =
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2023-03-28"))
        .withDepartureDate(LocalDate.parse("2023-04-04")).produce().apply {
          arrivals += ArrivalEntityFactory().withBooking(this).produce()
        }

    val relevantBookingStraddlingEndOfMonth =
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2023-04-28"))
        .withDepartureDate(LocalDate.parse("2023-05-04")).produce().apply {
          arrivals += ArrivalEntityFactory().withBooking(this).produce()
        }

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val relevantBookingStraddlingStartOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingEndOfMonth)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::bookedDaysActiveAndClosed]).isEqualTo(7)
  }

  @Test
  fun `confirmedDays shows the total number of days within the month for Bookings that are marked as confirmed but not arrived`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed = BedEntityFactory().withRoom(room).produce()

    // An irrelevant Booking - does not have a Confirmation set
    BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2023-04-05"))
      .withDepartureDate(LocalDate.parse("2023-04-10")).produce()

    val relevantBookingStraddlingStartOfMonth =
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2023-03-28"))
        .withDepartureDate(LocalDate.parse("2023-04-04")).produce().apply {
          confirmation = ConfirmationEntityFactory().withBooking(this).produce()
        }

    val relevantBookingStraddlingEndOfMonth =
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2023-04-28"))
        .withDepartureDate(LocalDate.parse("2023-05-04")).produce().apply {
          confirmation = ConfirmationEntityFactory().withBooking(this).produce()
        }

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val relevantBookingStraddlingStartOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingEndOfMonth)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::confirmedDays]).isEqualTo(7)
  }

  @Test
  fun `scheduledTurnaroundDays shows the number of working days in the month for the turnaround`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed = BedEntityFactory().withRoom(room).produce()

    val relevantBookingStraddlingStartOfMonth =
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2023-03-28"))
        .withDepartureDate(LocalDate.parse("2023-04-04")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(5).produce()
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
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2023-04-25"))
        .withDepartureDate(LocalDate.parse("2023-04-27")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(3).produce()
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

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val relevantBookingStraddlingStartOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingTurnaroundStraddlingStartOfMonthReportData =
      convertToCas3BedUtilisationBookingTurnaroundReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingEndOfMonth)
    val relevantBookingTurnaroundStraddlingEndOfMonthReportData =
      convertToCas3BedUtilisationBookingTurnaroundReportData(relevantBookingStraddlingEndOfMonth)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(relevantBookingTurnaroundStraddlingStartOfMonthReportData, relevantBookingTurnaroundStraddlingEndOfMonthReportData),
      listOf(),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::scheduledTurnaroundDays]).isEqualTo(6)
  }

  @Test
  fun `effectiveTurnaroundDays shows the total number of days (regardless of whether working days) in the month for the turnaround`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed = BedEntityFactory().withRoom(room).produce()

    val relevantBookingStraddlingStartOfMonth =
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2023-03-28"))
        .withDepartureDate(LocalDate.parse("2023-04-04")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(5).produce()
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
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2023-04-25"))
        .withDepartureDate(LocalDate.parse("2023-04-27")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(3).produce()
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

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val relevantBookingStraddlingStartOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingTurnaroundStraddlingStartOfMonthReportData =
      convertToCas3BedUtilisationBookingTurnaroundReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingEndOfMonth)
    val relevantBookingTurnaroundStraddlingEndOfMonthReportData =
      convertToCas3BedUtilisationBookingTurnaroundReportData(relevantBookingStraddlingEndOfMonth)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(relevantBookingTurnaroundStraddlingStartOfMonthReportData, relevantBookingTurnaroundStraddlingEndOfMonthReportData),
      listOf(),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::effectiveTurnaroundDays]).isEqualTo(9)
  }

  @Test
  fun `voidDays shows the total number of days in the month for voids`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed = BedEntityFactory().withRoom(room).produce()

    val relevantVoidStraddlingStartOfMonth =
      Cas3VoidBedspaceEntityFactory().withBed(bed).withPremises(premises).withStartDate(LocalDate.parse("2023-03-28"))
        .withEndDate(LocalDate.parse("2023-04-04")).withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

    val relevantVoidStraddlingEndOfMonth =
      Cas3VoidBedspaceEntityFactory().withBed(bed).withPremises(premises).withStartDate(LocalDate.parse("2023-04-25"))
        .withEndDate(LocalDate.parse("2023-05-03")).withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val relevantVoidStraddlingStartOfMonthReportData =
      convertToCas3BedUtilisationLostBedReportData(relevantVoidStraddlingStartOfMonth)
    val relevantVoidStraddlingEndOfMonthReportData =
      convertToCas3BedUtilisationLostBedReportData(relevantVoidStraddlingEndOfMonth)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(relevantVoidStraddlingStartOfMonthReportData, relevantVoidStraddlingEndOfMonthReportData),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::voidDays]).isEqualTo(10)
  }

  @Test
  fun `totalBookedDays shows the combined total days in the month of non-cancelled bookings, not non-cancelled voids or turnarounds - bedspaceOnlineDays, occupancyRate show correctly`() {
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed =
      BedEntityFactory().withRoom(room).withCreatedAt { OffsetDateTime.parse("2023-02-16T14:03:00+00:00") }.produce()

    val departureReason = DepartureReasonEntityFactory().produce()
    val moveOnCategory = MoveOnCategoryEntityFactory().produce()

    val relevantBookingStraddlingStartOfMonth =
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2023-03-28"))
        .withDepartureDate(LocalDate.parse("2023-04-04")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(5).produce()
          arrivals += ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2023-03-28")).produce()
          departures += DepartureEntityFactory().withBooking(this)
            .withDateTime(OffsetDateTime.parse("2023-04-04T12:00:00.000Z")).withReason(departureReason)
            .withMoveOnCategory(moveOnCategory).produce()
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
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2023-04-25"))
        .withDepartureDate(LocalDate.parse("2023-04-27")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(4).produce()
          arrivals += ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2023-04-25")).produce()
          departures += DepartureEntityFactory().withBooking(this)
            .withDateTime(OffsetDateTime.parse("2023-04-27T12:00:00.000Z")).withReason(departureReason)
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
      Cas3VoidBedspaceEntityFactory().withBed(bed).withPremises(premises).withStartDate(LocalDate.parse("2023-03-28"))
        .withEndDate(LocalDate.parse("2023-04-04")).withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

    val relevantVoidStraddlingEndOfMonth =
      Cas3VoidBedspaceEntityFactory().withBed(bed).withPremises(premises).withStartDate(LocalDate.parse("2023-04-25"))
        .withEndDate(LocalDate.parse("2023-05-03")).withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .produce()

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val relevantBookingStraddlingStartOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingEndOfMonth)
    val relevantVoidStraddlingStartOfMonthReportData =
      convertToCas3BedUtilisationLostBedReportData(relevantVoidStraddlingStartOfMonth)
    val relevantVoidStraddlingEndOfMonthReportData =
      convertToCas3BedUtilisationLostBedReportData(relevantVoidStraddlingEndOfMonth)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(relevantVoidStraddlingStartOfMonthReportData, relevantVoidStraddlingEndOfMonthReportData),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
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
    assertThat(result[0][BedUtilisationReportRow::totalBookedDays]).isEqualTo(7)
    assertThat(result[0][BedUtilisationReportRow::bedspaceOnlineDays]).isEqualTo(30)
    assertThat(result[0][BedUtilisationReportRow::occupancyRate]).isEqualTo(7.toDouble() / 30)
  }

  @Test
  fun `bedspaceStartDate show bedspace created date`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed =
      BedEntityFactory().withRoom(room).withCreatedAt { OffsetDateTime.parse("2024-01-16T14:03:00+00:00") }.produce()

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::bedspaceStartDate]).isEqualTo(LocalDate.parse("2024-01-16"))
  }

  @Test
  fun `bedspaceEndDate show bedspace end date when it is not null`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed = BedEntityFactory().withRoom(room).withCreatedAt { OffsetDateTime.parse("2024-02-16T14:03:00+00:00") }
      .withEndDate { LocalDate.parse("2024-05-12") }.produce()

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::bedspaceEndDate]).isEqualTo(LocalDate.parse("2024-05-12"))
  }

  @Test
  fun `bedspaceStartDate show nothing when bedspace start date is null`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed = BedEntityFactory().withRoom(room).withCreatedAt(null).produce()

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::bedspaceStartDate]).isNull()
  }

  @Test
  fun `bedspaceEndDate show nothing when bedspace end date is null`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed =
      BedEntityFactory().withRoom(room).withCreatedAt { OffsetDateTime.parse("2024-02-16T14:03:00+00:00") }.produce()

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::bedspaceEndDate]).isNull()
  }

  @Test
  fun `bedspaceOnlineDays shows number of days between bedspaceStartDate and report end date when bedspaceStartDate is later than report start date and bedspaceEndDate is null`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed =
      BedEntityFactory().withRoom(room).withCreatedAt { OffsetDateTime.parse("2024-02-05T14:03:00+00:00") }.produce()

    val departureReason = DepartureReasonEntityFactory().produce()
    val moveOnCategory = MoveOnCategoryEntityFactory().produce()

    val relevantBookingStraddlingStartOfMonth =
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2024-02-07"))
        .withDepartureDate(LocalDate.parse("2024-02-12")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(2).produce()
          arrivals += ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-07")).produce()
          departures += DepartureEntityFactory().withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-12T12:00:00.000Z")).withReason(departureReason)
            .withMoveOnCategory(moveOnCategory).produce()
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
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2024-02-16"))
        .withDepartureDate(LocalDate.parse("2024-02-22")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(3).produce()
          arrivals += ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-16")).produce()
          departures += DepartureEntityFactory().withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-22T12:00:00.000Z")).withReason(departureReason)
            .withMoveOnCategory(moveOnCategory).produce()
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

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val relevantBookingStraddlingStartOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingEndOfMonth)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::totalBookedDays]).isEqualTo(13)
    assertThat(result[0][BedUtilisationReportRow::bedspaceStartDate]).isEqualTo(LocalDate.parse("2024-02-05"))
    assertThat(result[0][BedUtilisationReportRow::bedspaceEndDate]).isNull()
    assertThat(result[0][BedUtilisationReportRow::bedspaceOnlineDays]).isEqualTo(25)
    assertThat(result[0][BedUtilisationReportRow::occupancyRate]).isEqualTo(13.toDouble() / 25)
  }

  @Test
  fun `bedspaceOnlineDays shows number of days between bedspaceStartDate and bedspaceEndtDate when bedspace dates are in report dates range`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed = BedEntityFactory().withRoom(room).withCreatedAt { OffsetDateTime.parse("2024-02-05T14:03:00+00:00") }
      .withEndDate { LocalDate.parse("2024-02-27") }.produce()

    val departureReason = DepartureReasonEntityFactory().produce()
    val moveOnCategory = MoveOnCategoryEntityFactory().produce()

    val relevantBookingStraddlingStartOfMonth =
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2024-02-07"))
        .withDepartureDate(LocalDate.parse("2024-02-12")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(2).produce()
          arrivals += ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-07")).produce()
          departures += DepartureEntityFactory().withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-12T12:00:00.000Z")).withReason(departureReason)
            .withMoveOnCategory(moveOnCategory).produce()
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
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2024-02-16"))
        .withDepartureDate(LocalDate.parse("2024-02-22")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(3).produce()
          arrivals += ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-16")).produce()
          departures += DepartureEntityFactory().withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-22T12:00:00.000Z")).withReason(departureReason)
            .withMoveOnCategory(moveOnCategory).produce()
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

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val relevantBookingStraddlingStartOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingEndOfMonth)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::totalBookedDays]).isEqualTo(13)
    assertThat(result[0][BedUtilisationReportRow::bedspaceStartDate]).isEqualTo(LocalDate.parse("2024-02-05"))
    assertThat(result[0][BedUtilisationReportRow::bedspaceEndDate]).isEqualTo(LocalDate.parse("2024-02-27"))
    assertThat(result[0][BedUtilisationReportRow::bedspaceOnlineDays]).isEqualTo(23)
    assertThat(result[0][BedUtilisationReportRow::occupancyRate]).isEqualTo(13.toDouble() / 23)
  }

  @Test
  fun `bedspaceOnlineDays shows number of days between report start date and bedspaceEndtDate when bedspace star and end dates are earlier than report start and end dates`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed = BedEntityFactory().withRoom(room).withCreatedAt { OffsetDateTime.parse("2024-01-17T14:03:00+00:00") }
      .withEndDate { LocalDate.parse("2024-02-25") }.produce()

    val departureReason = DepartureReasonEntityFactory().produce()
    val moveOnCategory = MoveOnCategoryEntityFactory().produce()

    val relevantBookingStraddlingStartOfMonth =
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2024-02-02"))
        .withDepartureDate(LocalDate.parse("2024-02-07")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(2).produce()
          arrivals += ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-02")).produce()
          departures += DepartureEntityFactory().withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-07T12:00:00.000Z")).withReason(departureReason)
            .withMoveOnCategory(moveOnCategory).produce()
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
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2024-02-10"))
        .withDepartureDate(LocalDate.parse("2024-02-15")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(3).produce()
          arrivals += ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-10")).produce()
          departures += DepartureEntityFactory().withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-15T12:00:00.000Z")).withReason(departureReason)
            .withMoveOnCategory(moveOnCategory).produce()
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

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val relevantBookingStraddlingStartOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingEndOfMonth)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::totalBookedDays]).isEqualTo(12)
    assertThat(result[0][BedUtilisationReportRow::bedspaceStartDate]).isEqualTo(LocalDate.parse("2024-01-17"))
    assertThat(result[0][BedUtilisationReportRow::bedspaceEndDate]).isEqualTo(LocalDate.parse("2024-02-25"))
    assertThat(result[0][BedUtilisationReportRow::bedspaceOnlineDays]).isEqualTo(25)
    assertThat(result[0][BedUtilisationReportRow::occupancyRate]).isEqualTo(12.toDouble() / 25)
  }

  @Test
  fun `bedspaceOnlineDays shows number of days between report start and end date when bedspace start and end dates are out of the report dates range`() {
    val startDate = LocalDate.of(2024, 2, 1)
    val endDate = LocalDate.of(2024, 2, 29)
    val apArea = ApAreaEntityFactory().produce()

    val probationRegion1 = ProbationRegionEntityFactory().withApArea(apArea).produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val premises = TemporaryAccommodationPremisesEntityFactory().withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1).produce()

    val room = RoomEntityFactory().withPremises(premises).produce()

    val bed = BedEntityFactory().withRoom(room).withCreatedAt { OffsetDateTime.parse("2024-01-17T14:03:00+00:00") }
      .withEndDate { LocalDate.parse("2024-03-15") }.produce()

    val departureReason = DepartureReasonEntityFactory().produce()
    val moveOnCategory = MoveOnCategoryEntityFactory().produce()

    val relevantBookingStraddlingStartOfMonth =
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2024-02-02"))
        .withDepartureDate(LocalDate.parse("2024-02-07")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(2).produce()
          arrivals += ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-02")).produce()
          departures += DepartureEntityFactory().withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-07T12:00:00.000Z")).withReason(departureReason)
            .withMoveOnCategory(moveOnCategory).produce()
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
      BookingEntityFactory().withBed(bed).withPremises(premises).withArrivalDate(LocalDate.parse("2024-02-10"))
        .withDepartureDate(LocalDate.parse("2024-02-15")).produce().apply {
          turnarounds += TurnaroundEntityFactory().withBooking(this).withWorkingDayCount(3).produce()
          arrivals += ArrivalEntityFactory().withBooking(this).withArrivalDate(LocalDate.parse("2024-02-10")).produce()
          departures += DepartureEntityFactory().withBooking(this)
            .withDateTime(OffsetDateTime.parse("2024-02-15T12:00:00.000Z")).withReason(departureReason)
            .withMoveOnCategory(moveOnCategory).produce()
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

    val bedUtilisationBedspaceReportData = convertToCas3BedUtilisationBedspaceReportData(bed)
    val relevantBookingStraddlingStartOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingStartOfMonth)
    val relevantBookingStraddlingEndOfMonthReportData =
      convertToCas3BedUtilisationBookingReportData(relevantBookingStraddlingEndOfMonth)
    val bedUtilisationReportData = BedUtilisationReportData(
      bedUtilisationBedspaceReportData,
      listOf(relevantBookingStraddlingStartOfMonthReportData, relevantBookingStraddlingEndOfMonthReportData),
      listOf(),
      listOf(),
      listOf(),
    )

    val result = bedUtilisationReportGenerator.createReport(
      listOf(bedUtilisationReportData),
      BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUtilisationReportRow::totalBookedDays]).isEqualTo(12)
    assertThat(result[0][BedUtilisationReportRow::bedspaceStartDate]).isEqualTo(LocalDate.parse("2024-01-17"))
    assertThat(result[0][BedUtilisationReportRow::bedspaceEndDate]).isEqualTo(LocalDate.parse("2024-03-15"))
    assertThat(result[0][BedUtilisationReportRow::bedspaceOnlineDays]).isEqualTo(29)
    assertThat(result[0][BedUtilisationReportRow::occupancyRate]).isEqualTo(12.toDouble() / 29)
  }
}
