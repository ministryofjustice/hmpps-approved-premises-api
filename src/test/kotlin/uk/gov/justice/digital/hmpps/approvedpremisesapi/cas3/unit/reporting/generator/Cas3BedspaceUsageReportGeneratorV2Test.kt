package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.reporting.generator

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.count
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.v2.Cas3v2TurnaroundEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BedspaceUsageReportGeneratorV2
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceUsageReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.Cas3BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.Cas3BedUsageType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import java.time.LocalDate

class Cas3BedspaceUsageReportGeneratorV2Test {
  private val mockBookingTransformer = mockk<Cas3BookingTransformer>()
  private val mockBookingRepository = mockk<Cas3v2BookingRepository>()
  private val mockLostBedsRepository = mockk<Cas3VoidBedspacesRepository>()
  private val mockWorkingDayService = mockk<WorkingDayService>()

  private val bedUsageReportGenerator = BedspaceUsageReportGeneratorV2(
    mockBookingTransformer,
    mockWorkingDayService,
  )

  @Test
  fun `Results for bedspaces are returned in the report`() {
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 4, 30)
    val premises = Cas3PremisesEntityFactory()
      .withDefaults()
      .produce()

    val bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .produce()

    val voidBedspace = Cas3VoidBedspaceEntityFactory()
      .withBedspace(bedspace)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .produce()

    every { mockBookingRepository.findAllByOverlappingDateForBedspace(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), bedspace) } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        bedspace,
      )
    } returns listOf(voidBedspace)

    val reportData = BedspaceUsageReportData(
      bedspace = bedspace,
      bookings = emptyList(),
      voids = listOf(voidBedspace),
    )

    val result = bedUsageReportGenerator
      .createReport(
        listOf(reportData),
        BedUsageReportProperties(
          ServiceName.temporaryAccommodation,
          null,
          startDate,
          endDate,
        ),
      )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUsageReportRow::propertyRef]).isEqualTo(premises.name)
    assertThat(result[0][BedUsageReportRow::uniquePropertyRef]).isEqualTo(premises.id.toShortBase58())
  }

  @Test
  fun `Results for bedspaces are returned in the report with 3 month duration`() {
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 7, 1)
    val premises = Cas3PremisesEntityFactory()
      .withDefaults()
      .produce()

    val bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .produce()

    val voidBedspace = Cas3VoidBedspaceEntityFactory()
      .withBedspace(bedspace)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .produce()

    every {
      mockBookingRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-07-01"),
        bedspace,
      )
    } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-07-01"),
        bedspace,
      )
    } returns listOf(voidBedspace)

    val reportData = BedspaceUsageReportData(
      bedspace = bedspace,
      bookings = emptyList(),
      voids = listOf(voidBedspace),
    )

    val result = bedUsageReportGenerator
      .createReport(
        listOf(reportData),
        BedUsageReportProperties(
          ServiceName.temporaryAccommodation,
          null,
          startDate,
          endDate,
        ),
      )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUsageReportRow::propertyRef]).isEqualTo(premises.name)
    assertThat(result[0][BedUsageReportRow::uniquePropertyRef]).isEqualTo(premises.id.toShortBase58())
  }

  @Test
  fun `Results for bedspaces from the specified probation region are returned in the report`() {
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 4, 30)
    val apArea = ApAreaEntityFactory()
      .produce()

    val probationRegion1 = ProbationRegionEntityFactory()
      .withApArea(apArea)
      .produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val probationRegion2 = ProbationRegionEntityFactory()
      .withApArea(apArea)
      .produce()

    val localAuthorityArea2 = LocalAuthorityEntityFactory().produce()

    val cas3PremisesInProbationRegion = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      )
      .produce()

    val cas3BedspaceInProbationRegion = Cas3BedspaceEntityFactory()
      .withPremises(cas3PremisesInProbationRegion)
      .produce()

    val temporaryAccommodationLostBedInProbationArea = Cas3VoidBedspaceEntityFactory()
      .withBedspace(cas3BedspaceInProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .produce()

    val cas3PremisesOutsideProbationRegion = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea2)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion2)
          .produce(),
      )
      .produce()

    val cas3BedspaceOutsideProbationRegion = Cas3BedspaceEntityFactory()
      .withPremises(cas3PremisesOutsideProbationRegion)
      .produce()

    val temporaryAccommodationLostBedOutsideProbationArea = Cas3VoidBedspaceEntityFactory()
      .withBedspace(cas3BedspaceOutsideProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .produce()

    every { mockBookingRepository.findAllByOverlappingDateForBedspace(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), cas3BedspaceInProbationRegion) } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        cas3BedspaceInProbationRegion,
      )
    } returns listOf(temporaryAccommodationLostBedInProbationArea)
    every { mockBookingRepository.findAllByOverlappingDateForBedspace(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), cas3BedspaceOutsideProbationRegion) } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        cas3BedspaceOutsideProbationRegion,
      )
    } returns listOf(temporaryAccommodationLostBedOutsideProbationArea)

    val reportData1 = BedspaceUsageReportData(
      bedspace = cas3BedspaceInProbationRegion,
      bookings = emptyList(),
      voids = listOf(temporaryAccommodationLostBedInProbationArea),
    )

    val reportData2 = BedspaceUsageReportData(
      bedspace = cas3BedspaceOutsideProbationRegion,
      bookings = emptyList(),
      voids = listOf(temporaryAccommodationLostBedOutsideProbationArea),
    )

    val result = bedUsageReportGenerator.createReport(
      listOf(reportData1, reportData2),
      BedUsageReportProperties(ServiceName.temporaryAccommodation, probationRegion1.id, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUsageReportRow::propertyRef]).isEqualTo(cas3PremisesInProbationRegion.name)
    assertThat(result[0][BedUsageReportRow::uniquePropertyRef]).isEqualTo(cas3PremisesInProbationRegion.id.toShortBase58())
  }

  @Test
  fun `Results for beds from all probation regions are returned in the report if no probation region ID is provided`() {
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 4, 30)
    val apArea = ApAreaEntityFactory()
      .produce()

    val probationRegion1 = ProbationRegionEntityFactory()
      .withApArea(apArea)
      .produce()

    val localAuthorityArea1 = LocalAuthorityEntityFactory().produce()

    val probationRegion2 = ProbationRegionEntityFactory()
      .withApArea(apArea)
      .produce()

    val localAuthorityArea2 = LocalAuthorityEntityFactory().produce()

    val temporaryAccommodationPremisesInProbationRegion = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion1)
          .produce(),
      )
      .produce()

    val cas3BedspaceInProbationRegion = Cas3BedspaceEntityFactory()
      .withPremises(temporaryAccommodationPremisesInProbationRegion)
      .produce()

    val lostBedInProbationArea = Cas3VoidBedspaceEntityFactory()
      .withBedspace(cas3BedspaceInProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .produce()

    val temporaryAccommodationPremisesOutsideProbationRegion = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea2)
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion2)
          .produce(),
      )
      .produce()

    val cas3BedspaceOutsideProbationRegion = Cas3BedspaceEntityFactory()
      .withPremises(temporaryAccommodationPremisesOutsideProbationRegion)
      .produce()

    val temporaryAccommodationLostBedOutsideProbationArea = Cas3VoidBedspaceEntityFactory()
      .withBedspace(cas3BedspaceOutsideProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .produce()

    every {
      mockBookingRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        cas3BedspaceInProbationRegion,
      )
    } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        cas3BedspaceInProbationRegion,
      )
    } returns listOf(lostBedInProbationArea)
    every {
      mockBookingRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        cas3BedspaceOutsideProbationRegion,
      )
    } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        cas3BedspaceOutsideProbationRegion,
      )
    } returns listOf(temporaryAccommodationLostBedOutsideProbationArea)

    val reportData1 = BedspaceUsageReportData(
      bedspace = cas3BedspaceInProbationRegion,
      bookings = emptyList(),
      voids = listOf(lostBedInProbationArea),
    )

    val reportData2 = BedspaceUsageReportData(
      bedspace = cas3BedspaceOutsideProbationRegion,
      bookings = emptyList(),
      voids = listOf(temporaryAccommodationLostBedOutsideProbationArea),
    )

    val result = bedUsageReportGenerator.createReport(
      listOf(reportData1, reportData2),
      BedUsageReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(2)
    assertThat(result[0][BedUsageReportRow::propertyRef]).isEqualTo(temporaryAccommodationPremisesInProbationRegion.name)
    assertThat(result[0][BedUsageReportRow::uniquePropertyRef]).isEqualTo(temporaryAccommodationPremisesInProbationRegion.id.toShortBase58())
    assertThat(result[1][BedUsageReportRow::propertyRef]).isEqualTo(temporaryAccommodationPremisesOutsideProbationRegion.name)
    assertThat(result[1][BedUsageReportRow::uniquePropertyRef]).isEqualTo(temporaryAccommodationPremisesOutsideProbationRegion.id.toShortBase58())
  }

  @Test
  fun `Booking rows are correctly generated`() {
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 4, 30)
    val probationRegion = ProbationRegionEntityFactory()
      .withApArea(ApAreaEntityFactory().produce())
      .produce()

    val localAuthority = LocalAuthorityEntityFactory()
      .produce()

    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val cas3Premises = Cas3PremisesEntityFactory()
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withLocalAuthorityArea(localAuthority)
      .withProbationDeliveryUnit(probationDeliveryUnit)
      .produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(cas3Premises)
      .produce()

    val booking = Cas3BookingEntityFactory()
      .withBedspace(cas3Bedspace)
      .withArrivalDate(LocalDate.parse("2023-04-05"))
      .withDepartureDate(LocalDate.parse("2023-04-07"))
      .withCrn("CRN321")
      .withPremises(cas3Premises)
      .produce()

    every { mockBookingRepository.findAllByOverlappingDateForBedspace(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), cas3Bedspace) } returns listOf(booking)
    every { mockLostBedsRepository.findAllByOverlappingDateForBedspace(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), cas3Bedspace) } returns emptyList()

    every { mockBookingTransformer.determineStatus(booking) } returns Cas3BookingStatus.closed

    val reportData = BedspaceUsageReportData(
      bedspace = cas3Bedspace,
      bookings = listOf(booking),
      voids = emptyList(),
    )

    val result = bedUsageReportGenerator.createReport(
      listOf(reportData),
      BedUsageReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][Cas3BedUsageReportRow::pdu]).isEqualTo(cas3Premises.probationDeliveryUnit!!.name)
    assertThat(result[0][Cas3BedUsageReportRow::propertyRef]).isEqualTo(cas3Premises.name)
    assertThat(result[0][Cas3BedUsageReportRow::addressLine1]).isEqualTo(cas3Premises.addressLine1)
    assertThat(result[0][Cas3BedUsageReportRow::bedspaceRef]).isEqualTo(cas3Bedspace.reference)
    assertThat(result[0][Cas3BedUsageReportRow::crn]).isEqualTo(booking.crn)
    assertThat(result[0][Cas3BedUsageReportRow::type]).isEqualTo(Cas3BedUsageType.Booking)
    assertThat(result[0][Cas3BedUsageReportRow::startDate]).isEqualTo(booking.arrivalDate)
    assertThat(result[0][Cas3BedUsageReportRow::endDate]).isEqualTo(booking.departureDate)
    assertThat(result[0][Cas3BedUsageReportRow::durationOfBookingDays]).isEqualTo(2)
    assertThat(result[0][Cas3BedUsageReportRow::bookingStatus]).isEqualTo(Cas3BookingStatus.closed)
    assertThat(result[0][Cas3BedUsageReportRow::voidCategory]).isEqualTo(null)
    assertThat(result[0][Cas3BedUsageReportRow::voidNotes]).isEqualTo(null)
    assertThat(result[0][Cas3BedUsageReportRow::uniquePropertyRef]).isEqualTo(cas3Premises.id.toShortBase58())
    assertThat(result[0][Cas3BedUsageReportRow::uniqueBedspaceRef]).isEqualTo(cas3Bedspace.id.toShortBase58())
  }

  @Test
  fun `Turnaround rows are correctly generated`() {
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 4, 30)
    val probationRegion = ProbationRegionEntityFactory()
      .withApArea(ApAreaEntityFactory().produce())
      .produce()

    val localAuthority = LocalAuthorityEntityFactory()
      .produce()

    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val cas3Premises = Cas3PremisesEntityFactory()
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withLocalAuthorityArea(localAuthority)
      .withProbationDeliveryUnit(probationDeliveryUnit)
      .produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(cas3Premises)
      .produce()

    val booking = Cas3BookingEntityFactory()
      .withBedspace(cas3Bedspace)
      .withArrivalDate(LocalDate.parse("2023-04-05"))
      .withDepartureDate(LocalDate.parse("2023-04-07"))
      .withCrn("CRN321")
      .withPremises(cas3Premises)
      .produce()

    val turnaround = Cas3v2TurnaroundEntityFactory()
      .withBooking(booking)
      .withWorkingDayCount(2)
      .produce()

    booking.turnarounds += turnaround

    every { mockBookingRepository.findAllByOverlappingDateForBedspace(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), cas3Bedspace) } returns listOf(booking)
    every { mockLostBedsRepository.findAllByOverlappingDateForBedspace(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), cas3Bedspace) } returns emptyList()

    every { mockBookingTransformer.determineStatus(booking) } returns Cas3BookingStatus.closed

    every { mockWorkingDayService.addWorkingDays(LocalDate.parse("2023-04-07"), 2) } returns LocalDate.parse("2023-04-09")

    val reportData = BedspaceUsageReportData(
      bedspace = cas3Bedspace,
      bookings = listOf(booking),
      voids = emptyList(),
    )

    val result = bedUsageReportGenerator.createReport(
      listOf(reportData),
      BedUsageReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(2)
    assertThat(result[1][Cas3BedUsageReportRow::pdu]).isEqualTo(cas3Premises.probationDeliveryUnit!!.name)
    assertThat(result[1][Cas3BedUsageReportRow::propertyRef]).isEqualTo(cas3Premises.name)
    assertThat(result[1][Cas3BedUsageReportRow::addressLine1]).isEqualTo(cas3Premises.addressLine1)
    assertThat(result[1][Cas3BedUsageReportRow::bedspaceRef]).isEqualTo(cas3Bedspace.reference)
    assertThat(result[1][Cas3BedUsageReportRow::crn]).isEqualTo(null)
    assertThat(result[1][Cas3BedUsageReportRow::type]).isEqualTo(Cas3BedUsageType.Turnaround)
    assertThat(result[1][Cas3BedUsageReportRow::startDate]).isEqualTo(LocalDate.parse("2023-04-08"))
    assertThat(result[1][Cas3BedUsageReportRow::endDate]).isEqualTo(LocalDate.parse("2023-04-09"))
    assertThat(result[1][Cas3BedUsageReportRow::durationOfBookingDays]).isEqualTo(1)
    assertThat(result[1][Cas3BedUsageReportRow::bookingStatus]).isEqualTo(null)
    assertThat(result[1][Cas3BedUsageReportRow::voidCategory]).isEqualTo(null)
    assertThat(result[1][Cas3BedUsageReportRow::voidNotes]).isEqualTo(null)
    assertThat(result[1][Cas3BedUsageReportRow::uniquePropertyRef]).isEqualTo(cas3Premises.id.toShortBase58())
    assertThat(result[1][Cas3BedUsageReportRow::uniqueBedspaceRef]).isEqualTo(cas3Bedspace.id.toShortBase58())
  }

  @Test
  fun `Void rows are correctly generated`() {
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 4, 30)
    val probationRegion = ProbationRegionEntityFactory()
      .withApArea(ApAreaEntityFactory().produce())
      .produce()

    val localAuthority = LocalAuthorityEntityFactory()
      .produce()

    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val cas3Premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthority)
      .withProbationDeliveryUnit(probationDeliveryUnit)
      .produce()

    val cas3Bedspace = Cas3BedspaceEntityFactory()
      .withPremises(cas3Premises)
      .produce()

    val temporaryAccommodationLostBed = Cas3VoidBedspaceEntityFactory()
      .withBedspace(cas3Bedspace)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withCostCentre(Cas3CostCentre.SUPPLIER)
      .produce()

    every { mockBookingRepository.findAllByOverlappingDateForBedspace(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), cas3Bedspace) } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        cas3Bedspace,
      )
    } returns listOf(temporaryAccommodationLostBed)

    val reportData = BedspaceUsageReportData(
      bedspace = cas3Bedspace,
      bookings = emptyList(),
      voids = listOf(temporaryAccommodationLostBed),
    )

    val result = bedUsageReportGenerator.createReport(
      listOf(reportData),
      BedUsageReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][Cas3BedUsageReportRow::pdu]).isEqualTo(cas3Premises.probationDeliveryUnit!!.name)
    assertThat(result[0][Cas3BedUsageReportRow::propertyRef]).isEqualTo(cas3Premises.name)
    assertThat(result[0][Cas3BedUsageReportRow::addressLine1]).isEqualTo(cas3Premises.addressLine1)
    assertThat(result[0][Cas3BedUsageReportRow::bedspaceRef]).isEqualTo(cas3Bedspace.reference)
    assertThat(result[0][Cas3BedUsageReportRow::crn]).isEqualTo(null)
    assertThat(result[0][Cas3BedUsageReportRow::type]).isEqualTo(Cas3BedUsageType.Void)
    assertThat(result[0][Cas3BedUsageReportRow::startDate]).isEqualTo(temporaryAccommodationLostBed.startDate)
    assertThat(result[0][Cas3BedUsageReportRow::endDate]).isEqualTo(temporaryAccommodationLostBed.endDate)
    assertThat(result[0][Cas3BedUsageReportRow::durationOfBookingDays]).isEqualTo(2)
    assertThat(result[0][Cas3BedUsageReportRow::bookingStatus]).isEqualTo(null)
    assertThat(result[0][Cas3BedUsageReportRow::voidCategory]).isEqualTo(temporaryAccommodationLostBed.reason.name)
    assertThat(result[0][Cas3BedUsageReportRow::voidNotes]).isEqualTo(temporaryAccommodationLostBed.notes)
    assertThat(result[0][Cas3BedUsageReportRow::costCentre]).isEqualTo(Cas3CostCentre.SUPPLIER)
    assertThat(result[0][Cas3BedUsageReportRow::uniquePropertyRef]).isEqualTo(cas3Premises.id.toShortBase58())
    assertThat(result[0][Cas3BedUsageReportRow::uniqueBedspaceRef]).isEqualTo(cas3Bedspace.id.toShortBase58())
  }
}
