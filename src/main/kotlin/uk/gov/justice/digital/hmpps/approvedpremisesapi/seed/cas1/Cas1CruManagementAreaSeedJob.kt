package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AutoAllocationDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.mapOfNonNullValues
import java.util.UUID

/*
To generate a baseline CSV for this seed job, use the following and export it as CSV

select
cru.id as id,
cru.name as current_name,
cru.email_address as email_address,
cru.assessment_auto_allocation_username   as assessment_auto_allocation_username,
mon.delius_username as assessment_auto_allocation_monday,
tue.delius_username as assessment_auto_allocation_tuesday,
wed.delius_username as assessment_auto_allocation_wednesday,
thu.delius_username as assessment_auto_allocation_thursday,
fri.delius_username as assessment_auto_allocation_friday,
sat.delius_username as assessment_auto_allocation_saturday,
sun.delius_username as assessment_auto_allocation_sunday
from cas1_cru_management_areas cru
left outer join cas1_cru_management_area_auto_allocations mon on mon.cas1_cru_management_area_id = cru.id and mon."day" = 'MONDAY'
left outer join cas1_cru_management_area_auto_allocations tue on tue.cas1_cru_management_area_id = cru.id and tue."day" = 'TUESDAY'
left outer join cas1_cru_management_area_auto_allocations wed on wed.cas1_cru_management_area_id = cru.id and wed."day" = 'WEDNESDAY'
left outer join cas1_cru_management_area_auto_allocations thu on thu.cas1_cru_management_area_id = cru.id and thu."day" = 'THURSDAY'
left outer join cas1_cru_management_area_auto_allocations fri on fri.cas1_cru_management_area_id = cru.id and fri."day" = 'FRIDAY'
left outer join cas1_cru_management_area_auto_allocations sat on sat.cas1_cru_management_area_id = cru.id and sat."day" = 'SATURDAY'
left outer join cas1_cru_management_area_auto_allocations sun on sun.cas1_cru_management_area_id = cru.id and sun."day" = 'SUNDAY'
 */
@Component
class Cas1CruManagementAreaSeedJob(
  private val cas1CruManagementAreaRepository: Cas1CruManagementAreaRepository,
) : SeedJob<CruManagementAreaSeedCsvRow>(
  requiredHeaders = setOf(
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
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): CruManagementAreaSeedCsvRow {
    val seedColumns = SeedColumns(columns)

    return CruManagementAreaSeedCsvRow(
      id = seedColumns.getUuidOrNull("id")!!,
      currentName = seedColumns.getStringOrNull("current_name")!!,
      emailAddress = seedColumns.getStringOrNull("email_address"),
      assessmentAutoAllocationUsername = seedColumns.getStringOrNull("assessment_auto_allocation_username"),
      assessmentAutoAllocationMonday = seedColumns.getStringOrNull("assessment_auto_allocation_monday"),
      assessmentAutoAllocationTuesday = seedColumns.getStringOrNull("assessment_auto_allocation_tuesday"),
      assessmentAutoAllocationWednesday = seedColumns.getStringOrNull("assessment_auto_allocation_wednesday"),
      assessmentAutoAllocationThursday = seedColumns.getStringOrNull("assessment_auto_allocation_thursday"),
      assessmentAutoAllocationFriday = seedColumns.getStringOrNull("assessment_auto_allocation_friday"),
      assessmentAutoAllocationSaturday = seedColumns.getStringOrNull("assessment_auto_allocation_saturday"),
      assessmentAutoAllocationSunday = seedColumns.getStringOrNull("assessment_auto_allocation_sunday"),
    )
  }

  override fun processRow(row: CruManagementAreaSeedCsvRow) {
    val id = row.id
    val managementArea = cas1CruManagementAreaRepository.findByIdOrNull(id)
      ?: error("CRU Management Area with id '$id' does not exist")

    val currentName = row.currentName
    if (managementArea.name != currentName) {
      error("Not updating entry for '$id' as current name '$currentName' in seed file doesn't match actual name '${managementArea.name}'")
    }

    val emailAddress = row.emailAddress?.ifBlank { null }
    val autoAlloc = row.assessmentAutoAllocationUsername?.ifBlank { null }

    val before = managementArea.copy()

    managementArea.emailAddress = emailAddress
    managementArea.assessmentAutoAllocationUsername = autoAlloc

    managementArea.assessmentAutoAllocations.clear()
    managementArea.assessmentAutoAllocations.putAll(
      mapOfNonNullValues(
        AutoAllocationDay.MONDAY to row.assessmentAutoAllocationMonday,
        AutoAllocationDay.TUESDAY to row.assessmentAutoAllocationTuesday,
        AutoAllocationDay.WEDNESDAY to row.assessmentAutoAllocationWednesday,
        AutoAllocationDay.THURSDAY to row.assessmentAutoAllocationThursday,
        AutoAllocationDay.FRIDAY to row.assessmentAutoAllocationFriday,
        AutoAllocationDay.SATURDAY to row.assessmentAutoAllocationSaturday,
        AutoAllocationDay.SUNDAY to row.assessmentAutoAllocationSunday,
      ) as Map<AutoAllocationDay, String>,
    )

    cas1CruManagementAreaRepository.save(managementArea)

    log.info("Management are was $before, is now $managementArea")
  }
}

data class CruManagementAreaSeedCsvRow(
  val id: UUID,
  val currentName: String,
  val emailAddress: String?,
  val assessmentAutoAllocationUsername: String?,
  val assessmentAutoAllocationMonday: String?,
  val assessmentAutoAllocationTuesday: String?,
  val assessmentAutoAllocationWednesday: String?,
  val assessmentAutoAllocationThursday: String?,
  val assessmentAutoAllocationFriday: String?,
  val assessmentAutoAllocationSaturday: String?,
  val assessmentAutoAllocationSunday: String?,
)
