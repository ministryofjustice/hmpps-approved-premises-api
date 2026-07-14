package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration

import org.springframework.transaction.support.TransactionTemplate

abstract class MigrationInBatchesJob(
  private val migrationLogger: MigrationLogger,
  private val transactionTemplate: TransactionTemplate,
) : MigrationJob() {

  @SuppressWarnings("MagicNumber")
  protected fun <T> processInBatches(
    items: List<T>,
    batchSize: Int,
    processBatch: (List<T>) -> Unit,
  ) {
    val chunkedItems = items.chunked(batchSize)
    chunkedItems.forEachIndexed { index, batch ->
      migrationLogger.info("Processing batch ${index + 1} of ${chunkedItems.size}...")
      transactionTemplate.executeWithoutResult {
        processBatch(batch)
      }
      if (index < chunkedItems.lastIndex) {
        Thread.sleep(1000L)
      }
    }
  }
}
