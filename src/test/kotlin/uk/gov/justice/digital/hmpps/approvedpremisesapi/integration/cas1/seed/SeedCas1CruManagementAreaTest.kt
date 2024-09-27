package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.CruManagementAreaSeedCsvRow
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCas1CruManagementAreaTest : SeedTestBase() {

  lateinit var areaId: UUID

  @BeforeEach
  fun setupArea() {
    areaId = cas1CruManagementAreaRepository.save(
      Cas1CruManagementAreaEntityFactory()
        .withId(UUID.randomUUID())
        .withName("Existing Name")
        .withEmailAddress("exisdtingEmail@here.com")
        .withAssessmentAutoAllocationUsername("existingUserName")
        .produce(),
    ).id
  }

  @Test
  fun `Attempting to seed using an invalid id logs an error`() {
    val invalidId = UUID.randomUUID()
    withCsv(
      csvName = "invalid-id",
      contents = listOf(
        CruManagementAreaSeedCsvRow(
          id = invalidId,
          currentName = "doesnt matter",
          emailAddress = "doesnt matter",
          assessmentAutoAllocationUsername = "doesnt matter",
        ),
      ).toCsv(),
    )

    seedService.seedData(SeedFileType.approvedPremisesCruManagementAreas, "invalid-id.csv")

    assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Error on row 1:" &&
          it.throwable != null &&
          it.throwable.message == "CRU Management Area with id '$invalidId' does not exist"
      }
  }

  @Test
  fun `Attempting to seed using an invalid name logs an error`() {
    withCsv(
      csvName = "invalid-name",
      contents = listOf(
        CruManagementAreaSeedCsvRow(
          id = areaId,
          currentName = "Wrong Name",
          emailAddress = "doesnt matter",
          assessmentAutoAllocationUsername = "doesnt matter",
        ),
      ).toCsv(),
    )

    seedService.seedData(SeedFileType.approvedPremisesCruManagementAreas, "invalid-name.csv")

    assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Error on row 1:" &&
          it.throwable != null &&
          it.throwable.message == "Not updating entry for '$areaId' as current name 'Wrong Name' in seed file doesn't match actual name 'Existing Name'"
      }
  }

  @Test
  fun `Updating cru management area persists correctly`() {
    withCsv(
      csvName = "valid-csv",
      contents = listOf(
        CruManagementAreaSeedCsvRow(
          id = areaId,
          currentName = "Existing Name",
          emailAddress = "updated@test.com",
          assessmentAutoAllocationUsername = "updated delius username",
        ),
      ).toCsv(),
    )

    seedService.seedData(SeedFileType.approvedPremisesCruManagementAreas, "valid-csv.csv")

    val updatedArea = cas1CruManagementAreaRepository.findByIdOrNull(areaId)!!

    assertThat(updatedArea.emailAddress).isEqualTo("updated@test.com")
    assertThat(updatedArea.assessmentAutoAllocationUsername).isEqualTo("updated delius username")
  }

  @Test
  fun `Updating cru management area with null values correctly`() {
    withCsv(
      csvName = "valid-csv",
      contents = listOf(
        CruManagementAreaSeedCsvRow(
          id = areaId,
          currentName = "Existing Name",
          emailAddress = "   ",
          assessmentAutoAllocationUsername = "  ",
        ),
      ).toCsv(),
    )

    seedService.seedData(SeedFileType.approvedPremisesCruManagementAreas, "valid-csv.csv")

    val updatedArea = cas1CruManagementAreaRepository.findByIdOrNull(areaId)!!

    assertThat(updatedArea.emailAddress).isNull()
    assertThat(updatedArea.assessmentAutoAllocationUsername).isNull()
  }

  private fun List<CruManagementAreaSeedCsvRow>.toCsv(): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "id",
        "current_name",
        "email_address",
        "assessment_auto_allocation_username",
      )
      .newRow()

    this.forEach {
      builder
        .withQuotedField(it.id)
        .withQuotedField(it.currentName)
        .withQuotedField(it.emailAddress)
        .withQuotedField(it.assessmentAutoAllocationUsername)
        .newRow()
    }

    return builder.build()
  }
}
