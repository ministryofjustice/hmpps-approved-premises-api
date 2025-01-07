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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspacesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.VoidBedspacesReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.VoidBedspaceReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.VoidBedspaceReportProperties
import java.time.LocalDate

class VoidBedspaceReportGeneratorTest {
  private val mockCas3VoidBedspacesRepository = mockk<Cas3VoidBedspacesRepository>()

  private val voidBedspacesReportGenerator = VoidBedspacesReportGenerator(
    mockCas3VoidBedspacesRepository,
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

    val temporaryAccommodationVoidBedspace = Cas3VoidBedspacesEntityFactory()
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

    val approvedPremisesLostBed = Cas3VoidBedspacesEntityFactory()
      .withBed(approvedPremisesBed)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(approvedPremises)
      .produce()

    every {
      mockCas3VoidBedspacesRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBed,
      )
    } returns listOf(temporaryAccommodationVoidBedspace)
    every {
      mockCas3VoidBedspacesRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        approvedPremisesBed,
      )
    } returns listOf(approvedPremisesLostBed)

    val result = voidBedspacesReportGenerator.createReport(
      listOf(approvedPremisesBed, temporaryAccommodationBed),
      VoidBedspaceReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][VoidBedspaceReportRow::ap]).isEqualTo(temporaryAccommodationPremises.name)
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

    val temporaryAccommodationVoidBedspaceInProbationArea = Cas3VoidBedspacesEntityFactory()
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

    val temporaryAccommodationVoidBedspaceOutsideProbationArea = Cas3VoidBedspacesEntityFactory()
      .withBed(temporaryAccommodationBedOutsideProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremisesOutsideProbationRegion)
      .produce()

    every {
      mockCas3VoidBedspacesRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedInProbationRegion,
      )
    } returns listOf(temporaryAccommodationVoidBedspaceInProbationArea)
    every {
      mockCas3VoidBedspacesRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedOutsideProbationRegion,
      )
    } returns listOf(temporaryAccommodationVoidBedspaceOutsideProbationArea)

    val result = voidBedspacesReportGenerator.createReport(
      listOf(temporaryAccommodationBedInProbationRegion, temporaryAccommodationBedOutsideProbationRegion),
      VoidBedspaceReportProperties(ServiceName.temporaryAccommodation, probationRegion1.id, 2023, 4),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][VoidBedspaceReportRow::ap]).isEqualTo(temporaryAccommodationPremisesInProbationRegion.name)
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

    val temporaryAccommodationVoidBedspaceInProbationArea = Cas3VoidBedspacesEntityFactory()
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

    val temporaryAccommodationVoidBedspaceOutsideProbationArea = Cas3VoidBedspacesEntityFactory()
      .withBed(temporaryAccommodationBedOutsideProbationRegion)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremisesOutsideProbationRegion)
      .produce()

    every {
      mockCas3VoidBedspacesRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedInProbationRegion,
      )
    } returns listOf(temporaryAccommodationVoidBedspaceInProbationArea)
    every {
      mockCas3VoidBedspacesRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBedOutsideProbationRegion,
      )
    } returns listOf(temporaryAccommodationVoidBedspaceOutsideProbationArea)

    val result = voidBedspacesReportGenerator.createReport(
      listOf(temporaryAccommodationBedInProbationRegion, temporaryAccommodationBedOutsideProbationRegion),
      VoidBedspaceReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )

    assertThat(result.count()).isEqualTo(2)
    assertThat(result[0][VoidBedspaceReportRow::ap]).isEqualTo(temporaryAccommodationPremisesInProbationRegion.name)
    assertThat(result[1][VoidBedspaceReportRow::ap]).isEqualTo(temporaryAccommodationPremisesOutsideProbationRegion.name)
  }

  @Test
  fun `Void Bedspace rows are correctly generated`() {
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

    val temporaryAccommodationVoidBedspace = Cas3VoidBedspacesEntityFactory()
      .withBed(temporaryAccommodationBed)
      .withStartDate(LocalDate.parse("2023-04-05"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremises)
      .produce()

    every {
      mockCas3VoidBedspacesRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBed,
      )
    } returns listOf(temporaryAccommodationVoidBedspace)

    val result = voidBedspacesReportGenerator.createReport(
      listOf(temporaryAccommodationBed),
      VoidBedspaceReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][VoidBedspaceReportRow::roomName]).isEqualTo(temporaryAccommodationRoom.name)
    assertThat(result[0][VoidBedspaceReportRow::bedName]).isEqualTo(temporaryAccommodationBed.name)
    assertThat(result[0][VoidBedspaceReportRow::id]).isEqualTo(temporaryAccommodationVoidBedspace.id.toString())
    assertThat(result[0][VoidBedspaceReportRow::workOrderId]).isEqualTo(temporaryAccommodationVoidBedspace.referenceNumber)
    assertThat(result[0][VoidBedspaceReportRow::region]).isEqualTo(temporaryAccommodationPremises.probationRegion.name)
    assertThat(result[0][VoidBedspaceReportRow::ap]).isEqualTo(temporaryAccommodationPremises.name)
    assertThat(result[0][VoidBedspaceReportRow::reason]).isEqualTo(temporaryAccommodationVoidBedspace.reason.name)
    assertThat(result[0][VoidBedspaceReportRow::startDate]).isEqualTo(temporaryAccommodationVoidBedspace.startDate)
    assertThat(result[0][VoidBedspaceReportRow::endDate]).isEqualTo(temporaryAccommodationVoidBedspace.endDate)
    assertThat(result[0][VoidBedspaceReportRow::lengthDays]).isEqualTo(3)
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

    val temporaryAccommodationVoidBedspace = Cas3VoidBedspacesEntityFactory()
      .withBed(temporaryAccommodationBed)
      .withStartDate(LocalDate.parse("2023-03-28"))
      .withEndDate(LocalDate.parse("2023-04-07"))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withPremises(temporaryAccommodationPremises)
      .produce()

    every {
      mockCas3VoidBedspacesRepository.findAllByOverlappingDateForBed(
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        temporaryAccommodationBed,
      )
    } returns listOf(temporaryAccommodationVoidBedspace)

    val result = voidBedspacesReportGenerator.createReport(
      listOf(temporaryAccommodationBed),
      VoidBedspaceReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][VoidBedspaceReportRow::roomName]).isEqualTo(temporaryAccommodationRoom.name)
    assertThat(result[0][VoidBedspaceReportRow::bedName]).isEqualTo(temporaryAccommodationBed.name)
    assertThat(result[0][VoidBedspaceReportRow::id]).isEqualTo(temporaryAccommodationVoidBedspace.id.toString())
    assertThat(result[0][VoidBedspaceReportRow::workOrderId]).isEqualTo(temporaryAccommodationVoidBedspace.referenceNumber)
    assertThat(result[0][VoidBedspaceReportRow::region]).isEqualTo(temporaryAccommodationPremises.probationRegion.name)
    assertThat(result[0][VoidBedspaceReportRow::ap]).isEqualTo(temporaryAccommodationPremises.name)
    assertThat(result[0][VoidBedspaceReportRow::reason]).isEqualTo(temporaryAccommodationVoidBedspace.reason.name)
    assertThat(result[0][VoidBedspaceReportRow::startDate]).isEqualTo(temporaryAccommodationVoidBedspace.startDate)
    assertThat(result[0][VoidBedspaceReportRow::endDate]).isEqualTo(temporaryAccommodationVoidBedspace.endDate)
    assertThat(result[0][VoidBedspaceReportRow::lengthDays]).isEqualTo(7)
  }
}
