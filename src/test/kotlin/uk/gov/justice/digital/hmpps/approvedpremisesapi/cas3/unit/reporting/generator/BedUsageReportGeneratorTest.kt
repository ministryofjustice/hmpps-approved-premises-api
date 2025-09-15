package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.reporting.generator

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.count
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3TurnaroundEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BedUsageReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedUsageType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.time.LocalDate

class BedUsageReportGeneratorTest {
  private val mockBookingTransformer = mockk<BookingTransformer>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockLostBedsRepository = mockk<Cas3VoidBedspacesRepository>()
  private val mockWorkingDayService = mockk<WorkingDayService>()

  private val bedUsageReportGenerator = BedUsageReportGenerator(
    mockBookingTransformer,
    mockBookingRepository,
    mockLostBedsRepository,
    mockWorkingDayService,
  )

  @Test
  fun `Only results for beds from the specified service are returned in the report`() {
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 4, 30)
    val temporaryAccommodationPremises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val temporaryAccommodationRoom = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremises)
      .produce()

    val temporaryAccommodationBed = BedEntityFactory()
      .withRoom(temporaryAccommodationRoom)
      .produce()

    val temporaryAccommodationLostBed = Cas3VoidBedspaceEntityFactory()
      .withBed(temporaryAccommodationBed)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremises)
      .produce()

    val approvedPremises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val approvedPremisesRoom = RoomEntityFactory()
      .withPremises(approvedPremises)
      .produce()

    val approvedPremisesBed = BedEntityFactory()
      .withRoom(approvedPremisesRoom)
      .produce()

    val approvedPremisesLostBed = Cas3VoidBedspaceEntityFactory()
      .withBed(approvedPremisesBed)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(approvedPremises)
      .produce()

    every { mockBookingRepository.findAllByOverlappingDateForBed(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), temporaryAccommodationBed) } returns emptyList()
    every { mockBookingRepository.findAllByOverlappingDateForBed(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), approvedPremisesBed) } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBed,
      )
    } returns listOf(temporaryAccommodationLostBed)
    every { mockLostBedsRepository.findAllByOverlappingDateForBedspace(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), approvedPremisesBed) } returns listOf(approvedPremisesLostBed)

    val result = bedUsageReportGenerator.createReport(
      listOf(approvedPremisesBed, temporaryAccommodationBed),
      BedUsageReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUsageReportRow::propertyRef]).isEqualTo(temporaryAccommodationPremises.name)
    assertThat(result[0][BedUsageReportRow::uniquePropertyRef]).isEqualTo(temporaryAccommodationPremises.id.toShortBase58())
  }

  @Test
  fun `Only results for beds from the temporary accommodation service are returned in the report with 3 month duration`() {
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 7, 1)
    val temporaryAccommodationPremises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val temporaryAccommodationRoom = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremises)
      .produce()

    val temporaryAccommodationBed = BedEntityFactory()
      .withRoom(temporaryAccommodationRoom)
      .produce()

    val temporaryAccommodationLostBed = Cas3VoidBedspaceEntityFactory()
      .withBed(temporaryAccommodationBed)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremises)
      .produce()

    val approvedPremises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val approvedPremisesRoom = RoomEntityFactory()
      .withPremises(approvedPremises)
      .produce()

    val approvedPremisesBed = BedEntityFactory()
      .withRoom(approvedPremisesRoom)
      .produce()

    val approvedPremisesLostBed = Cas3VoidBedspaceEntityFactory()
      .withBed(approvedPremisesBed)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(approvedPremises)
      .produce()

    every {
      mockBookingRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-07-01"),
        temporaryAccommodationBed,
      )
    } returns emptyList()
    every {
      mockBookingRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-07-01"),
        approvedPremisesBed,
      )
    } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-07-01"),
        temporaryAccommodationBed,
      )
    } returns listOf(temporaryAccommodationLostBed)
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-07-01"),
        approvedPremisesBed,
      )
    } returns listOf(approvedPremisesLostBed)

    val bedUsageReportGeneratorWithThreeMonth = BedUsageReportGenerator(
      mockBookingTransformer,
      mockBookingRepository,
      mockLostBedsRepository,
      mockWorkingDayService,
    )

    val result = bedUsageReportGeneratorWithThreeMonth.createReport(
      listOf(approvedPremisesBed, temporaryAccommodationBed),
      BedUsageReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUsageReportRow::propertyRef]).isEqualTo(temporaryAccommodationPremises.name)
    assertThat(result[0][BedUsageReportRow::uniquePropertyRef]).isEqualTo(temporaryAccommodationPremises.id.toShortBase58())

    verify {
      mockBookingRepository.findAllByOverlappingDateForBed(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-07-01"), any<BedEntity>())
    }
  }

  @Test
  fun `Only results for beds from the approved premises service are returned in the report for 1 month for approved premises`() {
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 4, 30)
    val temporaryAccommodationPremises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val temporaryAccommodationRoom = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremises)
      .produce()

    val temporaryAccommodationBed = BedEntityFactory()
      .withRoom(temporaryAccommodationRoom)
      .produce()

    val temporaryAccommodationLostBed = Cas3VoidBedspaceEntityFactory()
      .withBed(temporaryAccommodationBed)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremises)
      .produce()

    val approvedPremises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val approvedPremisesRoom = RoomEntityFactory()
      .withPremises(approvedPremises)
      .produce()

    val approvedPremisesBed = BedEntityFactory()
      .withRoom(approvedPremisesRoom)
      .produce()

    val approvedPremisesLostBed = Cas3VoidBedspaceEntityFactory()
      .withBed(approvedPremisesBed)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(approvedPremises)
      .produce()

    every { mockBookingRepository.findAllByOverlappingDateForBed(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), temporaryAccommodationBed) } returns emptyList()
    every { mockBookingRepository.findAllByOverlappingDateForBed(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), approvedPremisesBed) } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBed,
      )
    } returns listOf(temporaryAccommodationLostBed)
    every { mockLostBedsRepository.findAllByOverlappingDateForBedspace(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), approvedPremisesBed) } returns listOf(approvedPremisesLostBed)

    val bedUsageReportGeneratorWithThreeMonth = BedUsageReportGenerator(
      mockBookingTransformer,
      mockBookingRepository,
      mockLostBedsRepository,
      mockWorkingDayService,
    )

    val result = bedUsageReportGeneratorWithThreeMonth.createReport(
      listOf(approvedPremisesBed, temporaryAccommodationBed),
      BedUsageReportProperties(ServiceName.approvedPremises, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUsageReportRow::propertyRef]).isEqualTo(approvedPremises.name)
    assertThat(result[0][BedUsageReportRow::uniquePropertyRef]).isEqualTo(approvedPremises.id.toShortBase58())

    verify {
      mockBookingRepository.findAllByOverlappingDateForBed(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), any<BedEntity>())
    }
  }

  @Test
  fun `Only results for beds from the specified probation region are returned in the report`() {
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

    val temporaryAccommodationPremisesInProbationRegion = TemporaryAccommodationPremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1)
      .produce()

    val temporaryAccommodationRoomInProbationRegion = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremisesInProbationRegion)
      .produce()

    val temporaryAccommodationBedInProbationRegion = BedEntityFactory()
      .withRoom(temporaryAccommodationRoomInProbationRegion)
      .produce()

    val temporaryAccommodationLostBedInProbationArea = Cas3VoidBedspaceEntityFactory()
      .withBed(temporaryAccommodationBedInProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremisesInProbationRegion)
      .produce()

    val temporaryAccommodationPremisesOutsideProbationRegion = TemporaryAccommodationPremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea2)
      .withProbationRegion(probationRegion2)
      .produce()

    val temporaryAccommodationRoomOutsideProbationRegion = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremisesOutsideProbationRegion)
      .produce()

    val temporaryAccommodationBedOutsideProbationRegion = BedEntityFactory()
      .withRoom(temporaryAccommodationRoomOutsideProbationRegion)
      .produce()

    val temporaryAccommodationLostBedOutsideProbationArea = Cas3VoidBedspaceEntityFactory()
      .withBed(temporaryAccommodationBedOutsideProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremisesOutsideProbationRegion)
      .produce()

    every { mockBookingRepository.findAllByOverlappingDateForBed(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), temporaryAccommodationBedInProbationRegion) } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedInProbationRegion,
      )
    } returns listOf(temporaryAccommodationLostBedInProbationArea)
    every { mockBookingRepository.findAllByOverlappingDateForBed(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), temporaryAccommodationBedOutsideProbationRegion) } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedOutsideProbationRegion,
      )
    } returns listOf(temporaryAccommodationLostBedOutsideProbationArea)

    val result = bedUsageReportGenerator.createReport(
      listOf(temporaryAccommodationBedInProbationRegion, temporaryAccommodationBedOutsideProbationRegion),
      BedUsageReportProperties(ServiceName.temporaryAccommodation, probationRegion1.id, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUsageReportRow::propertyRef]).isEqualTo(temporaryAccommodationPremisesInProbationRegion.name)
    assertThat(result[0][BedUsageReportRow::uniquePropertyRef]).isEqualTo(temporaryAccommodationPremisesInProbationRegion.id.toShortBase58())
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

    val temporaryAccommodationPremisesInProbationRegion = TemporaryAccommodationPremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1)
      .produce()

    val temporaryAccommodationRoomInProbationRegion = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremisesInProbationRegion)
      .produce()

    val temporaryAccommodationBedInProbationRegion = BedEntityFactory()
      .withRoom(temporaryAccommodationRoomInProbationRegion)
      .produce()

    val temporaryAccommodationLostBedInProbationArea = Cas3VoidBedspaceEntityFactory()
      .withBed(temporaryAccommodationBedInProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremisesInProbationRegion)
      .produce()

    val temporaryAccommodationPremisesOutsideProbationRegion = TemporaryAccommodationPremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea2)
      .withProbationRegion(probationRegion2)
      .produce()

    val temporaryAccommodationRoomOutsideProbationRegion = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremisesOutsideProbationRegion)
      .produce()

    val temporaryAccommodationBedOutsideProbationRegion = BedEntityFactory()
      .withRoom(temporaryAccommodationRoomOutsideProbationRegion)
      .produce()

    val temporaryAccommodationLostBedOutsideProbationArea = Cas3VoidBedspaceEntityFactory()
      .withBed(temporaryAccommodationBedOutsideProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremisesOutsideProbationRegion)
      .produce()

    every {
      mockBookingRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedInProbationRegion,
      )
    } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedInProbationRegion,
      )
    } returns listOf(temporaryAccommodationLostBedInProbationArea)
    every {
      mockBookingRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedOutsideProbationRegion,
      )
    } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedOutsideProbationRegion,
      )
    } returns listOf(temporaryAccommodationLostBedOutsideProbationArea)

    val result = bedUsageReportGenerator.createReport(
      listOf(temporaryAccommodationBedInProbationRegion, temporaryAccommodationBedOutsideProbationRegion),
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

    val temporaryAccommodationPremises = TemporaryAccommodationPremisesEntityFactory()
      .withProbationRegion(probationRegion)
      .withLocalAuthorityArea(localAuthority)
      .withProbationDeliveryUnit(probationDeliveryUnit)
      .produce()

    val temporaryAccommodationRoom = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremises)
      .produce()

    val temporaryAccommodationBed = BedEntityFactory()
      .withRoom(temporaryAccommodationRoom)
      .produce()

    val temporaryAccommodationBooking = BookingEntityFactory()
      .withBed(temporaryAccommodationBed)
      .withArrivalDate(LocalDate.parse("2023-04-05"))
      .withDepartureDate(LocalDate.parse("2023-04-07"))
      .withCrn("CRN321")
      .withPremises(temporaryAccommodationPremises)
      .produce()

    every { mockBookingRepository.findAllByOverlappingDateForBed(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), temporaryAccommodationBed) } returns listOf(temporaryAccommodationBooking)
    every { mockLostBedsRepository.findAllByOverlappingDateForBedspace(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), temporaryAccommodationBed) } returns emptyList()

    every { mockBookingTransformer.determineStatus(temporaryAccommodationBooking) } returns BookingStatus.closed

    val result = bedUsageReportGenerator.createReport(
      listOf(temporaryAccommodationBed),
      BedUsageReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUsageReportRow::pdu]).isEqualTo(temporaryAccommodationPremises.probationDeliveryUnit!!.name)
    assertThat(result[0][BedUsageReportRow::propertyRef]).isEqualTo(temporaryAccommodationPremises.name)
    assertThat(result[0][BedUsageReportRow::addressLine1]).isEqualTo(temporaryAccommodationPremises.addressLine1)
    assertThat(result[0][BedUsageReportRow::bedspaceRef]).isEqualTo(temporaryAccommodationRoom.name)
    assertThat(result[0][BedUsageReportRow::crn]).isEqualTo(temporaryAccommodationBooking.crn)
    assertThat(result[0][BedUsageReportRow::type]).isEqualTo(BedUsageType.Booking)
    assertThat(result[0][BedUsageReportRow::startDate]).isEqualTo(temporaryAccommodationBooking.arrivalDate)
    assertThat(result[0][BedUsageReportRow::endDate]).isEqualTo(temporaryAccommodationBooking.departureDate)
    assertThat(result[0][BedUsageReportRow::durationOfBookingDays]).isEqualTo(2)
    assertThat(result[0][BedUsageReportRow::bookingStatus]).isEqualTo(BookingStatus.closed)
    assertThat(result[0][BedUsageReportRow::voidCategory]).isEqualTo(null)
    assertThat(result[0][BedUsageReportRow::voidNotes]).isEqualTo(null)
    assertThat(result[0][BedUsageReportRow::uniquePropertyRef]).isEqualTo(temporaryAccommodationPremises.id.toShortBase58())
    assertThat(result[0][BedUsageReportRow::uniqueBedspaceRef]).isEqualTo(temporaryAccommodationRoom.id.toShortBase58())
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

    val temporaryAccommodationPremises = TemporaryAccommodationPremisesEntityFactory()
      .withProbationRegion(probationRegion)
      .withLocalAuthorityArea(localAuthority)
      .withProbationDeliveryUnit(probationDeliveryUnit)
      .produce()

    val temporaryAccommodationRoom = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremises)
      .produce()

    val temporaryAccommodationBed = BedEntityFactory()
      .withRoom(temporaryAccommodationRoom)
      .produce()

    val temporaryAccommodationBooking = BookingEntityFactory()
      .withBed(temporaryAccommodationBed)
      .withArrivalDate(LocalDate.parse("2023-04-05"))
      .withDepartureDate(LocalDate.parse("2023-04-07"))
      .withCrn("CRN321")
      .withPremises(temporaryAccommodationPremises)
      .produce()

    val turnaround = Cas3TurnaroundEntityFactory()
      .withBooking(temporaryAccommodationBooking)
      .withWorkingDayCount(2)
      .produce()

    temporaryAccommodationBooking.turnarounds += turnaround

    every { mockBookingRepository.findAllByOverlappingDateForBed(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), temporaryAccommodationBed) } returns listOf(temporaryAccommodationBooking)
    every { mockLostBedsRepository.findAllByOverlappingDateForBedspace(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), temporaryAccommodationBed) } returns emptyList()

    every { mockBookingTransformer.determineStatus(temporaryAccommodationBooking) } returns BookingStatus.closed

    every { mockWorkingDayService.addWorkingDays(LocalDate.parse("2023-04-07"), 2) } returns LocalDate.parse("2023-04-09")

    val result = bedUsageReportGenerator.createReport(
      listOf(temporaryAccommodationBed),
      BedUsageReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(2)
    assertThat(result[1][BedUsageReportRow::pdu]).isEqualTo(temporaryAccommodationPremises.probationDeliveryUnit!!.name)
    assertThat(result[1][BedUsageReportRow::propertyRef]).isEqualTo(temporaryAccommodationPremises.name)
    assertThat(result[1][BedUsageReportRow::addressLine1]).isEqualTo(temporaryAccommodationPremises.addressLine1)
    assertThat(result[1][BedUsageReportRow::bedspaceRef]).isEqualTo(temporaryAccommodationRoom.name)
    assertThat(result[1][BedUsageReportRow::crn]).isEqualTo(null)
    assertThat(result[1][BedUsageReportRow::type]).isEqualTo(BedUsageType.Turnaround)
    assertThat(result[1][BedUsageReportRow::startDate]).isEqualTo(LocalDate.parse("2023-04-08"))
    assertThat(result[1][BedUsageReportRow::endDate]).isEqualTo(LocalDate.parse("2023-04-09"))
    assertThat(result[1][BedUsageReportRow::durationOfBookingDays]).isEqualTo(1)
    assertThat(result[1][BedUsageReportRow::bookingStatus]).isEqualTo(null)
    assertThat(result[1][BedUsageReportRow::voidCategory]).isEqualTo(null)
    assertThat(result[1][BedUsageReportRow::voidNotes]).isEqualTo(null)
    assertThat(result[1][BedUsageReportRow::uniquePropertyRef]).isEqualTo(temporaryAccommodationPremises.id.toShortBase58())
    assertThat(result[1][BedUsageReportRow::uniqueBedspaceRef]).isEqualTo(temporaryAccommodationRoom.id.toShortBase58())
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

    val temporaryAccommodationPremises = TemporaryAccommodationPremisesEntityFactory()
      .withProbationRegion(probationRegion)
      .withLocalAuthorityArea(localAuthority)
      .withProbationDeliveryUnit(probationDeliveryUnit)
      .produce()

    val temporaryAccommodationRoom = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremises)
      .produce()

    val temporaryAccommodationBed = BedEntityFactory()
      .withRoom(temporaryAccommodationRoom)
      .produce()

    val temporaryAccommodationLostBed = Cas3VoidBedspaceEntityFactory()
      .withBed(temporaryAccommodationBed)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremises)
      .withCostCentre(Cas3CostCentre.SUPPLIER)
      .produce()

    every { mockBookingRepository.findAllByOverlappingDateForBed(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), temporaryAccommodationBed) } returns emptyList()
    every {
      mockLostBedsRepository.findAllByOverlappingDateForBedspace(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBed,
      )
    } returns listOf(temporaryAccommodationLostBed)

    val result = bedUsageReportGenerator.createReport(
      listOf(temporaryAccommodationBed),
      BedUsageReportProperties(ServiceName.temporaryAccommodation, null, startDate, endDate),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][BedUsageReportRow::pdu]).isEqualTo(temporaryAccommodationPremises.probationDeliveryUnit!!.name)
    assertThat(result[0][BedUsageReportRow::propertyRef]).isEqualTo(temporaryAccommodationPremises.name)
    assertThat(result[0][BedUsageReportRow::addressLine1]).isEqualTo(temporaryAccommodationPremises.addressLine1)
    assertThat(result[0][BedUsageReportRow::bedspaceRef]).isEqualTo(temporaryAccommodationRoom.name)
    assertThat(result[0][BedUsageReportRow::crn]).isEqualTo(null)
    assertThat(result[0][BedUsageReportRow::type]).isEqualTo(BedUsageType.Void)
    assertThat(result[0][BedUsageReportRow::startDate]).isEqualTo(temporaryAccommodationLostBed.startDate)
    assertThat(result[0][BedUsageReportRow::endDate]).isEqualTo(temporaryAccommodationLostBed.endDate)
    assertThat(result[0][BedUsageReportRow::durationOfBookingDays]).isEqualTo(2)
    assertThat(result[0][BedUsageReportRow::bookingStatus]).isEqualTo(null)
    assertThat(result[0][BedUsageReportRow::voidCategory]).isEqualTo(temporaryAccommodationLostBed.reason.name)
    assertThat(result[0][BedUsageReportRow::voidNotes]).isEqualTo(temporaryAccommodationLostBed.notes)
    assertThat(result[0][BedUsageReportRow::costCentre]).isEqualTo(Cas3CostCentre.SUPPLIER)
    assertThat(result[0][BedUsageReportRow::uniquePropertyRef]).isEqualTo(temporaryAccommodationPremises.id.toShortBase58())
    assertThat(result[0][BedUsageReportRow::uniqueBedspaceRef]).isEqualTo(temporaryAccommodationRoom.id.toShortBase58())
  }
}
