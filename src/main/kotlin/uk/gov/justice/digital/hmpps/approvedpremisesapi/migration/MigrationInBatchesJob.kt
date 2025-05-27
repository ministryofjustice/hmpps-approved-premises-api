package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

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
    items.chunked(batchSize).forEachIndexed { index, batch ->
      migrationLogger.info("Processing batch ${index + 1} of ${items.size / batchSize}...")
      transactionTemplate.executeWithoutResult {
        processBatch(batch)
      }
      if (index < items.chunked(batchSize).lastIndex) {
        Thread.sleep(1000L)
      }
    }
  }
}
