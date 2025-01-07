package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.generator

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.count
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3LostBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3LostBedsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.LostBedsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.LostBedReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.LostBedReportProperties
import java.time.LocalDate

class LostBedReportGeneratorTest {
  private val mockCas3LostBedsRepository = mockk<Cas3LostBedsRepository>()

  private val lostBedReportGenerator = LostBedsReportGenerator(
    mockCas3LostBedsRepository,
  )

  @Test
  fun `Only results for beds from the specified service are returned in the report`() {
    val temporaryAccommodationPremises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val temporaryAccommodationRoom = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremises)
      .produce()

    val temporaryAccommodationBed = BedEntityFactory()
      .withRoom(temporaryAccommodationRoom)
      .produce()

    val temporaryAccommodationLostBed = Cas3LostBedsEntityFactory()
      .withBed(temporaryAccommodationBed)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3LostBedReasonEntityFactory().produce() }
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

    val approvedPremisesLostBed = Cas3LostBedsEntityFactory()
      .withBed(approvedPremisesBed)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3LostBedReasonEntityFactory().produce() }
      .withPremises(approvedPremises)
      .produce()

    every {
      mockCas3LostBedsRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBed,
      )
    } returns listOf(temporaryAccommodationLostBed)
    every {
      mockCas3LostBedsRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        approvedPremisesBed,
      )
    } returns listOf(approvedPremisesLostBed)

    val result = lostBedReportGenerator.createReport(
      listOf(approvedPremisesBed, temporaryAccommodationBed),
      LostBedReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][LostBedReportRow::ap]).isEqualTo(temporaryAccommodationPremises.name)
  }

  @Test
  fun `Only results for beds from the specified probation region are returned in the report`() {
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

    val temporaryAccommodationLostBedInProbationArea = Cas3LostBedsEntityFactory()
      .withBed(temporaryAccommodationBedInProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3LostBedReasonEntityFactory().produce() }
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

    val temporaryAccommodationLostBedOutsideProbationArea = Cas3LostBedsEntityFactory()
      .withBed(temporaryAccommodationBedOutsideProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3LostBedReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremisesOutsideProbationRegion)
      .produce()

    every {
      mockCas3LostBedsRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedInProbationRegion,
      )
    } returns listOf(temporaryAccommodationLostBedInProbationArea)
    every {
      mockCas3LostBedsRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedOutsideProbationRegion,
      )
    } returns listOf(temporaryAccommodationLostBedOutsideProbationArea)

    val result = lostBedReportGenerator.createReport(
      listOf(temporaryAccommodationBedInProbationRegion, temporaryAccommodationBedOutsideProbationRegion),
      LostBedReportProperties(ServiceName.temporaryAccommodation, probationRegion1.id, 2023, 4),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][LostBedReportRow::ap]).isEqualTo(temporaryAccommodationPremisesInProbationRegion.name)
  }

  @Test
  fun `Results for beds from all probation regions are returned in the report if no probation region ID is provided`() {
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

    val temporaryAccommodationLostBedInProbationArea = Cas3LostBedsEntityFactory()
      .withBed(temporaryAccommodationBedInProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3LostBedReasonEntityFactory().produce() }
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

    val temporaryAccommodationLostBedOutsideProbationArea = Cas3LostBedsEntityFactory()
      .withBed(temporaryAccommodationBedOutsideProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3LostBedReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremisesOutsideProbationRegion)
      .produce()

    every {
      mockCas3LostBedsRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedInProbationRegion,
      )
    } returns listOf(temporaryAccommodationLostBedInProbationArea)
    every {
      mockCas3LostBedsRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedOutsideProbationRegion,
      )
    } returns listOf(temporaryAccommodationLostBedOutsideProbationArea)

    val result = lostBedReportGenerator.createReport(
      listOf(temporaryAccommodationBedInProbationRegion, temporaryAccommodationBedOutsideProbationRegion),
      LostBedReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )

    assertThat(result.count()).isEqualTo(2)
    assertThat(result[0][LostBedReportRow::ap]).isEqualTo(temporaryAccommodationPremisesInProbationRegion.name)
    assertThat(result[1][LostBedReportRow::ap]).isEqualTo(temporaryAccommodationPremisesOutsideProbationRegion.name)
  }

  @Test
  fun `Lost Bed rows are correctly generated`() {
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

    val temporaryAccommodationLostBed = Cas3LostBedsEntityFactory()
      .withBed(temporaryAccommodationBed)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3LostBedReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremises)
      .produce()

    every {
      mockCas3LostBedsRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBed,
      )
    } returns listOf(temporaryAccommodationLostBed)

    val result = lostBedReportGenerator.createReport(
      listOf(temporaryAccommodationBed),
      LostBedReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][LostBedReportRow::roomName]).isEqualTo(temporaryAccommodationRoom.name)
    assertThat(result[0][LostBedReportRow::bedName]).isEqualTo(temporaryAccommodationBed.name)
    assertThat(result[0][LostBedReportRow::id]).isEqualTo(temporaryAccommodationLostBed.id.toString())
    assertThat(result[0][LostBedReportRow::workOrderId]).isEqualTo(temporaryAccommodationLostBed.referenceNumber)
    assertThat(result[0][LostBedReportRow::region]).isEqualTo(temporaryAccommodationPremises.probationRegion.name)
    assertThat(result[0][LostBedReportRow::ap]).isEqualTo(temporaryAccommodationPremises.name)
    assertThat(result[0][LostBedReportRow::reason]).isEqualTo(temporaryAccommodationLostBed.reason.name)
    assertThat(result[0][LostBedReportRow::startDate]).isEqualTo(temporaryAccommodationLostBed.startDate)
    assertThat(result[0][LostBedReportRow::endDate]).isEqualTo(temporaryAccommodationLostBed.endDate)
    assertThat(result[0][LostBedReportRow::lengthDays]).isEqualTo(3)
  }

  @Test
  fun `Length Days only includes days in month requested where Booking spans more than 1 month`() {
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

    val temporaryAccommodationLostBed = Cas3LostBedsEntityFactory()
      .withBed(temporaryAccommodationBed)
      .withStartDate(LocalDate.parse("2023-03-28"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3LostBedReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremises)
      .produce()

    every {
      mockCas3LostBedsRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBed,
      )
    } returns listOf(temporaryAccommodationLostBed)

    val result = lostBedReportGenerator.createReport(
      listOf(temporaryAccommodationBed),
      LostBedReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][LostBedReportRow::roomName]).isEqualTo(temporaryAccommodationRoom.name)
    assertThat(result[0][LostBedReportRow::bedName]).isEqualTo(temporaryAccommodationBed.name)
    assertThat(result[0][LostBedReportRow::id]).isEqualTo(temporaryAccommodationLostBed.id.toString())
    assertThat(result[0][LostBedReportRow::workOrderId]).isEqualTo(temporaryAccommodationLostBed.referenceNumber)
    assertThat(result[0][LostBedReportRow::region]).isEqualTo(temporaryAccommodationPremises.probationRegion.name)
    assertThat(result[0][LostBedReportRow::ap]).isEqualTo(temporaryAccommodationPremises.name)
    assertThat(result[0][LostBedReportRow::reason]).isEqualTo(temporaryAccommodationLostBed.reason.name)
    assertThat(result[0][LostBedReportRow::startDate]).isEqualTo(temporaryAccommodationLostBed.startDate)
    assertThat(result[0][LostBedReportRow::endDate]).isEqualTo(temporaryAccommodationLostBed.endDate)
    assertThat(result[0][LostBedReportRow::lengthDays]).isEqualTo(7)
  }
}
