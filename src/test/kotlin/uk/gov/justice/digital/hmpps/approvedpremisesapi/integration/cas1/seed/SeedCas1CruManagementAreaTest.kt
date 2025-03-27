package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AutoAllocationDay
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
        .withAssessmentAutoAllocations(
          mutableMapOf(
            AutoAllocationDay.MONDAY to "old monday",
            AutoAllocationDay.TUESDAY to "old tuesday",
            AutoAllocationDay.WEDNESDAY to "old wednesday",
            AutoAllocationDay.THURSDAY to "old thursday",
            AutoAllocationDay.FRIDAY to "old friday",
            AutoAllocationDay.SATURDAY to "old saturday",
            AutoAllocationDay.SUNDAY to "old sunday",
          ),
        )
        .produce(),
    ).id
  }

  @Test
  fun `Attempting to seed using an invalid id logs an error`() {
    val invalidId = UUID.randomUUID()

    seed(
      SeedFileType.approvedPremisesCruManagementAreas,
      contents = listOf(
        CruManagementAreaSeedCsvRow(
          id = invalidId,
          currentName = "doesnt matter",
          emailAddress = "doesnt matter",
          assessmentAutoAllocationUsername = "doesnt matter",
          assessmentAutoAllocationMonday = "doesnt matter",
          assessmentAutoAllocationTuesday = "doesnt matter",
          assessmentAutoAllocationWednesday = "doesnt matter",
          assessmentAutoAllocationThursday = "doesnt matter",
          assessmentAutoAllocationFriday = "doesnt matter",
          assessmentAutoAllocationSaturday = "doesnt matter",
          assessmentAutoAllocationSunday = "doesnt matter",
        ),
      ).toCsv(),
    )

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
    seed(
      SeedFileType.approvedPremisesCruManagementAreas,
      contents = listOf(
        CruManagementAreaSeedCsvRow(
          id = areaId,
          currentName = "Wrong Name",
          emailAddress = "doesnt matter",
          assessmentAutoAllocationUsername = "doesnt matter",
          assessmentAutoAllocationMonday = "doesnt matter",
          assessmentAutoAllocationTuesday = "doesnt matter",
          assessmentAutoAllocationWednesday = "doesnt matter",
          assessmentAutoAllocationThursday = "doesnt matter",
          assessmentAutoAllocationFriday = "doesnt matter",
          assessmentAutoAllocationSaturday = "doesnt matter",
          assessmentAutoAllocationSunday = "doesnt matter",
        ),
      ).toCsv(),
    )

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
    seed(
      SeedFileType.approvedPremisesCruManagementAreas,
      contents = listOf(
        CruManagementAreaSeedCsvRow(
          id = areaId,
          currentName = "Existing Name",
          emailAddress = "updated@test.com",
          assessmentAutoAllocationUsername = "updated delius username",
          assessmentAutoAllocationMonday = "username mon",
          assessmentAutoAllocationTuesday = "username tue",
          assessmentAutoAllocationWednesday = "username wed",
          assessmentAutoAllocationThursday = "username thu",
          assessmentAutoAllocationFriday = "username fri",
          assessmentAutoAllocationSaturday = "username sat",
          assessmentAutoAllocationSunday = "username sun",
        ),
      ).toCsv(),
    )

    val updatedArea = cas1CruManagementAreaRepository.findByIdOrNull(areaId)!!

    assertThat(updatedArea.emailAddress).isEqualTo("updated@test.com")
    assertThat(updatedArea.assessmentAutoAllocationUsername).isEqualTo("updated delius username")

    assertThat(updatedArea.assessmentAutoAllocations).isEqualTo(
      mapOf(
        AutoAllocationDay.MONDAY to "username mon",
        AutoAllocationDay.TUESDAY to "username tue",
        AutoAllocationDay.WEDNESDAY to "username wed",
        AutoAllocationDay.THURSDAY to "username thu",
        AutoAllocationDay.FRIDAY to "username fri",
        AutoAllocationDay.SATURDAY to "username sat",
        AutoAllocationDay.SUNDAY to "username sun",
      ),
    )
  }

  @Test
  fun `Updating cru management area with null values removes configuration`() {
    seed(
      SeedFileType.approvedPremisesCruManagementAreas,
      contents = listOf(
        CruManagementAreaSeedCsvRow(
          id = areaId,
          currentName = "Existing Name",
          emailAddress = "   ",
          assessmentAutoAllocationUsername = "  ",
          assessmentAutoAllocationMonday = "",
          assessmentAutoAllocationTuesday = "still something here",
          assessmentAutoAllocationWednesday = " ",
          assessmentAutoAllocationThursday = "",
          assessmentAutoAllocationFriday = "and here",
          assessmentAutoAllocationSaturday = "",
          assessmentAutoAllocationSunday = "",
        ),
      ).toCsv(),
    )

    val updatedArea = cas1CruManagementAreaRepository.findByIdOrNull(areaId)!!

    assertThat(updatedArea.emailAddress).isNull()
    assertThat(updatedArea.assessmentAutoAllocationUsername).isNull()
    assertThat(updatedArea.assessmentAutoAllocations).isEqualTo(
      mapOf(
        AutoAllocationDay.TUESDAY to "still something here",
        AutoAllocationDay.FRIDAY to "and here",
      ),
    )
  }

  private fun List<CruManagementAreaSeedCsvRow>.toCsv(): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "id",
        "current_name",
        "email_address",
        "assessment_auto_allocation_username",
        "assessment_auto_allocation_monday",
        "assessment_auto_allocation_tuesday",
        "assessment_auto_allocation_wednesday",
        "assessment_auto_allocation_thursday",
        "assessment_auto_allocation_friday",
        "assessment_auto_allocation_saturday",
        "assessment_auto_allocation_sunday",
      )
      .newRow()

    this.forEach {
      builder
        .withQuotedField(it.id)
        .withQuotedField(it.currentName)
        .withQuotedField(it.emailAddress ?: "")
        .withQuotedField(it.assessmentAutoAllocationUsername ?: "")
        .withQuotedField(it.assessmentAutoAllocationMonday ?: "")
        .withQuotedField(it.assessmentAutoAllocationTuesday ?: "")
        .withQuotedField(it.assessmentAutoAllocationWednesday ?: "")
        .withQuotedField(it.assessmentAutoAllocationThursday ?: "")
        .withQuotedField(it.assessmentAutoAllocationFriday ?: "")
        .withQuotedField(it.assessmentAutoAllocationSaturday ?: "")
        .withQuotedField(it.assessmentAutoAllocationSunday ?: "")
        .newRow()
    }

    return builder.build()
  }
}
