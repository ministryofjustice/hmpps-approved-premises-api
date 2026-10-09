package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1DataScienceReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.JdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2DataScienceReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3DataScienceReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CasDataScienceReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CasDocumentSubType
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.time.LocalDate

class CasDataScienceReportGeneratorTest {

  private val cas1Repo = mockk<Cas1DataScienceReportRepository>()
  private val cas2Repo = mockk<Cas2DataScienceReportRepository>()
  private val cas3Repo = mockk<Cas3DataScienceReportRepository>()

  private val generator = CasDataScienceReportGenerator(
    cas1DataScienceReportRepository = cas1Repo,
    cas2DataScienceReportRepository = cas2Repo,
    cas3DataScienceReportRepository = cas3Repo,
  )

  private val mockResultSet = mockk<ResultSet>(relaxed = true)
  private val mockMetaData = mockk<ResultSetMetaData>(relaxed = true)

  @BeforeEach
  fun setUp() {
    every { mockMetaData.columnCount } returns 2
    every { mockMetaData.getColumnName(1) } returns "header1"
    every { mockMetaData.getColumnName(2) } returns "header2"
    every { mockMetaData.getColumnLabel(1) } returns "header1"
    every { mockMetaData.getColumnLabel(2) } returns "header2"
    every { mockResultSet.metaData } returns mockMetaData
    every { mockResultSet.next() } returns false
  }

  @Test
  fun `generateReport for CAS1 invokes cas1 repository and returns GeneratedReport with temp file`() {
    val executionDate = LocalDate.of(2026, 10, 8)
    every { cas1Repo.generate(any()) } answers {
      firstArg<JdbcResultSetConsumer>().consume(mockResultSet)
    }

    val report = generator.generateReport(CasDocumentSubType.CAS1, executionDate)

    assertThat(report.documentSubType).isEqualTo(CasDocumentSubType.CAS1)
    assertThat(report.executionDate).isEqualTo(executionDate)
    assertThat(report.filename).isEqualTo("cas1_data_science_report_2026-10-08.csv")
    assertThat(report.documentUuid).isNotNull
    assertThat(report.tempFile).exists()
    assertThat(report.tempFile.readText()).contains("\"header1\",\"header2\"")

    report.tempFile.delete()
    verify(exactly = 1) { cas1Repo.generate(any()) }
  }

  @Test
  fun `generateReport for CAS2 invokes cas2 repository and returns GeneratedReport`() {
    val executionDate = LocalDate.of(2026, 10, 8)
    every { cas2Repo.generate(any()) } answers {
      firstArg<JdbcResultSetConsumer>().consume(mockResultSet)
    }

    val report = generator.generateReport(CasDocumentSubType.CAS2, executionDate)

    assertThat(report.documentSubType).isEqualTo(CasDocumentSubType.CAS2)
    assertThat(report.filename).isEqualTo("cas2_data_science_report_2026-10-08.csv")
    assertThat(report.tempFile).exists()

    report.tempFile.delete()
    verify(exactly = 1) { cas2Repo.generate(any()) }
  }

  @Test
  fun `generateReport for CAS3 invokes cas3 repository and returns GeneratedReport`() {
    val executionDate = LocalDate.of(2026, 10, 8)
    every { cas3Repo.generate(any()) } answers {
      firstArg<JdbcResultSetConsumer>().consume(mockResultSet)
    }

    val report = generator.generateReport(CasDocumentSubType.CAS3, executionDate)

    assertThat(report.documentSubType).isEqualTo(CasDocumentSubType.CAS3)
    assertThat(report.filename).isEqualTo("cas3_data_science_report_2026-10-08.csv")
    assertThat(report.tempFile).exists()

    report.tempFile.delete()
    verify(exactly = 1) { cas3Repo.generate(any()) }
  }
}
