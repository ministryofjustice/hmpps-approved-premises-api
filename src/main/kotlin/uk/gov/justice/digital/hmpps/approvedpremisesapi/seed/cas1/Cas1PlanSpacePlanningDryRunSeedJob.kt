package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanner.PlanCriteria
import java.time.LocalDate
import java.util.UUID

class Cas1PlanSpacePlanningDryRunSeedJob(
  fileName: String,
  private val spacePlanner: SpacePlanner,
  private val cas1PremisesService: Cas1PremisesService,
) : SeedJob<Cas1SpacePlanningDryRunCsvRow>(
  id = UUID.randomUUID(),
  fileName = fileName,
  requiredHeaders = setOf(
    "premises_id",
    "start_date",
    "end_date",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1SpacePlanningDryRunCsvRow(
    premisesId = UUID.fromString(columns["premises_id"]!!.trim()),
    startDate = LocalDate.parse(columns["start_date"]!!.trim()),
    endDate = LocalDate.parse(columns["end_date"]!!.trim()),
  )

  override fun processRow(row: Cas1SpacePlanningDryRunCsvRow) {
    val premiseId = row.premisesId
    val startDate = row.startDate
    val endDate = row.endDate

    log.info("Planning dry run for premise $premiseId between $startDate and $endDate")

    val premise = cas1PremisesService.findPremiseById(premiseId) ?: error("Could not find premise with id $premiseId")

    val planCriteria = PlanCriteria(premise, startDate, endDate)
    spacePlanner.plan(planCriteria)
  }
}

data class Cas1SpacePlanningDryRunCsvRow(
  val premisesId: UUID,
  val startDate: LocalDate,
  val endDate: LocalDate,
)