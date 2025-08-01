package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob

@Service
class Cas1CreateTestApplicationsSeedJob(
  val cas1ApplicationSeedService: Cas1ApplicationSeedService,
  val transactionTemplate: TransactionTemplate,
) : SeedJob<Cas1CreateTestApplicationsSeedCsvRow>(
  requiredHeaders = setOf(
    "creator_username",
    "crn",
    "count",
    "state",
    "premises_qcode",
  ),
  runInTransaction = false,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): Cas1CreateTestApplicationsSeedCsvRow {
    val seedColumns = SeedColumns(columns)

    return Cas1CreateTestApplicationsSeedCsvRow(
      creatorUsername = seedColumns.getStringOrNull("creator_username")!!,
      crn = seedColumns.getStringOrNull("crn")!!,
      count = seedColumns.getIntOrNull("count")!!,
      state = Cas1ApplicationSeedService.ApplicationState.valueOf(seedColumns.getStringOrNull("state")!!),
      premisesQCode = seedColumns.getStringOrNull("premises_qcode"),
    )
  }

  @SuppressWarnings("MagicNumber")
  @OptIn(ExperimentalCoroutinesApi::class)
  override fun processRow(row: Cas1CreateTestApplicationsSeedCsvRow) {
    val deliusUsername = row.creatorUsername
    val crn = row.crn
    val count = row.count
    val state = row.state

    log.info("Creating $count applications for CRN $crn in state $state using creator username $deliusUsername")

    runBlocking(Dispatchers.IO.limitedParallelism(10)) {
      repeat(row.count) {
        launch {
          transactionTemplate.execute {
            cas1ApplicationSeedService.createApplication(
              deliusUserName = deliusUsername,
              crn = crn,
              createIfExistingApplicationForCrn = true,
              state = row.state,
              premisesQCode = row.premisesQCode,
            )
          }
        }
      }
    }

    log.info("Have created $count applications for CRN $crn in state $state using creator username $deliusUsername")
  }
}

data class Cas1CreateTestApplicationsSeedCsvRow(
  val creatorUsername: String,
  val crn: String,
  val count: Int,
  val state: Cas1ApplicationSeedService.ApplicationState,
  val premisesQCode: String?,
)
