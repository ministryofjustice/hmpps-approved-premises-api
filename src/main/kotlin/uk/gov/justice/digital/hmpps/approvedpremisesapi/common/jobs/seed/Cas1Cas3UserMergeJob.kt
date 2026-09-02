package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.Cas1Cas3UserMergeJob.Cas1Cas3UserMergeRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserMergeService

@Component
class Cas1Cas3UserMergeJob(
  val userMergeService: UserMergeService,
) : SeedJob<Cas1Cas3UserMergeRow>(
  requiredHeaders = setOf(
    "old_delius_username",
    "new_delius_username",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun processRow(row: Cas1Cas3UserMergeRow) {
    log.info("Merging user ${row.oldDeliusUsername} into ${row.newDeliusUsername}")
    userMergeService.mergeUser(
      oldDeliusUsername = row.oldDeliusUsername,
      newDeliusUsername = row.newDeliusUsername,
    )
  }

  override fun deserializeRow(columns: Map<String, String>): Cas1Cas3UserMergeRow {
    val seedColumns = SeedColumns(columns)

    return Cas1Cas3UserMergeRow(
      oldDeliusUsername = seedColumns.getStringOrNull("old_delius_username")!!.uppercase(),
      newDeliusUsername = seedColumns.getStringOrNull("new_delius_username")!!.uppercase(),
    )
  }

  data class Cas1Cas3UserMergeRow(
    val oldDeliusUsername: String,
    val newDeliusUsername: String,
  )
}
