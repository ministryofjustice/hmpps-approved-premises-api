package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.generator

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.count
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedRevisionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.Cas1OutOfServiceBedsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.VoidBedspaceReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService
import java.time.LocalDate

class Cas1OutOfServiceBedReportGeneratorTest {
  private val outOfServiceBedRepository = mockk<Cas1OutOfServiceBedRepository>()

  private val reportGenerator = Cas1OutOfServiceBedsReportGenerator(
    outOfServiceBedRepository,
  )

  @Test
  fun `Results for out-of-service beds from all probation regions are returned in the report`() {
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

    val premisesInProbationRegion = ApprovedPremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea1)
      .withProbationRegion(probationRegion1)
      .produce()

    val roomInProbationRegion = RoomEntityFactory()
      .withPremises(premisesInProbationRegion)
      .produce()

    val bedInProbationRegion = BedEntityFactory()
      .withRoom(roomInProbationRegion)
      .produce()

    val outOfServiceBedInProbationArea = Cas1OutOfServiceBedEntityFactory()
      .withBed(bedInProbationRegion)
      .produce()
      .apply {
        this.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
          .withOutOfServiceBed(this)
          .withStartDate(LocalDate.parse("2023-04-05"))
          .withEndDate(LocalDate.parse("2023-04-07"))
          .produce()
      }

    val premisesOutsideProbationRegion = ApprovedPremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea2)
      .withProbationRegion(probationRegion2)
      .produce()

    val roomOutsideProbationRegion = RoomEntityFactory()
      .withPremises(premisesOutsideProbationRegion)
      .produce()

    val bedOutsideProbationRegion = BedEntityFactory()
      .withRoom(roomOutsideProbationRegion)
      .produce()

    val outOfServiceBedOutsideProbationArea = Cas1OutOfServiceBedEntityFactory()
      .withBed(bedOutsideProbationRegion)
      .produce()
      .apply {
        this.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
          .withOutOfServiceBed(this)
          .withStartDate(LocalDate.parse("2023-04-05"))
          .withEndDate(LocalDate.parse("2023-04-07"))
          .produce()
      }

    every {
      outOfServiceBedRepository.findByBedIdAndOverlappingDate(
        bedInProbationRegion.id,
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        null,
      )
    } returns listOf(outOfServiceBedInProbationArea.id.toString())

    every {
      outOfServiceBedRepository.findAllById(listOf(outOfServiceBedInProbationArea.id))
    } returns listOf(outOfServiceBedInProbationArea)

    every {
      outOfServiceBedRepository.findByBedIdAndOverlappingDate(
        bedOutsideProbationRegion.id,
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        null,
      )
    } returns listOf(outOfServiceBedOutsideProbationArea.id.toString())

    every {
      outOfServiceBedRepository.findAllById(listOf(outOfServiceBedOutsideProbationArea.id))
    } returns listOf(outOfServiceBedOutsideProbationArea)

    val result = reportGenerator.createReport(
      listOf(bedInProbationRegion, bedOutsideProbationRegion),
      Cas1ReportService.MonthSpecificReportParams(2023, 4),
    )

    assertThat(result.count()).isEqualTo(2)
    assertThat(result[0][VoidBedspaceReportRow::ap]).isEqualTo(premisesInProbationRegion.name)
    assertThat(result[1][VoidBedspaceReportRow::ap]).isEqualTo(premisesOutsideProbationRegion.name)
  }

  @Test
  fun `Out-of-service bed rows are correctly generated`() {
    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
      .withBed(bed)
      .produce()
      .apply {
        this.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
          .withOutOfServiceBed(this)
          .withStartDate(LocalDate.parse("2023-04-05"))
          .withEndDate(LocalDate.parse("2023-04-07"))
          .produce()
      }

    every {
      outOfServiceBedRepository.findByBedIdAndOverlappingDate(
        bed.id,
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        null,
      )
    } returns listOf(outOfServiceBed.id.toString())

    every {
      outOfServiceBedRepository.findAllById(listOf(outOfServiceBed.id))
    } returns listOf(outOfServiceBed)

    val result = reportGenerator.createReport(
      listOf(bed),
      Cas1ReportService.MonthSpecificReportParams(2023, 4),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][VoidBedspaceReportRow::roomName]).isEqualTo(room.name)
    assertThat(result[0][VoidBedspaceReportRow::bedName]).isEqualTo(bed.name)
    assertThat(result[0][VoidBedspaceReportRow::id]).isEqualTo(outOfServiceBed.id.toString())
    assertThat(result[0][VoidBedspaceReportRow::workOrderId]).isEqualTo(outOfServiceBed.referenceNumber)
    assertThat(result[0][VoidBedspaceReportRow::region]).isEqualTo(premises.probationRegion.name)
    assertThat(result[0][VoidBedspaceReportRow::ap]).isEqualTo(premises.name)
    assertThat(result[0][VoidBedspaceReportRow::reason]).isEqualTo(outOfServiceBed.reason.name)
    assertThat(result[0][VoidBedspaceReportRow::startDate]).isEqualTo(outOfServiceBed.startDate)
    assertThat(result[0][VoidBedspaceReportRow::endDate]).isEqualTo(outOfServiceBed.endDate)
    assertThat(result[0][VoidBedspaceReportRow::lengthDays]).isEqualTo(3)
  }

  @Test
  fun `Length Days only includes days in month requested where out-of-service bed spans more than 1 calendar month`() {
    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
      .withBed(bed)
      .produce()
      .apply {
        this.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
          .withOutOfServiceBed(this)
          .withStartDate(LocalDate.parse("2023-03-28"))
          .withEndDate(LocalDate.parse("2023-04-07"))
          .produce()
      }

    every {
      outOfServiceBedRepository.findByBedIdAndOverlappingDate(
        bed.id,
        LocalDate.parse("2023-04-01"),
        LocalDate.parse("2023-04-30"),
        null,
      )
    } returns listOf(outOfServiceBed.id.toString())

    every {
      outOfServiceBedRepository.findAllById(listOf(outOfServiceBed.id))
    } returns listOf(outOfServiceBed)

    val result = reportGenerator.createReport(
      listOf(bed),
      Cas1ReportService.MonthSpecificReportParams(2023, 4),
    )

    assertThat(result.count()).isEqualTo(1)
    assertThat(result[0][VoidBedspaceReportRow::roomName]).isEqualTo(room.name)
    assertThat(result[0][VoidBedspaceReportRow::bedName]).isEqualTo(bed.name)
    assertThat(result[0][VoidBedspaceReportRow::id]).isEqualTo(outOfServiceBed.id.toString())
    assertThat(result[0][VoidBedspaceReportRow::workOrderId]).isEqualTo(outOfServiceBed.referenceNumber)
    assertThat(result[0][VoidBedspaceReportRow::region]).isEqualTo(premises.probationRegion.name)
    assertThat(result[0][VoidBedspaceReportRow::ap]).isEqualTo(premises.name)
    assertThat(result[0][VoidBedspaceReportRow::reason]).isEqualTo(outOfServiceBed.reason.name)
    assertThat(result[0][VoidBedspaceReportRow::startDate]).isEqualTo(outOfServiceBed.startDate)
    assertThat(result[0][VoidBedspaceReportRow::endDate]).isEqualTo(outOfServiceBed.endDate)
    assertThat(result[0][VoidBedspaceReportRow::lengthDays]).isEqualTo(7)
  }
}
