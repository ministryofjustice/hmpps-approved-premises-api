package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener

import java.net.URI
import java.util.UUID

/**
 * Handles processing of a specific inbox event type. Each handler is responsible for a single event
 * type and manages its own transaction boundary. Add new handlers by implementing this interface
 * and registering as a Spring bean.
 *
 * Events with the same [getPartitionKey] are processed sequentially to avoid concurrent updates to
 * the same resource (e.g. case per CRN). Events with different keys are processed in parallel.
 */
interface InboxEventHandler {

  /**
   * The domain event type this handler supports
   *
   * We use a non-bounded type here so we can use custom handlers during integration testing
   **/
  fun supportedEventType(): String

  /**
   * Partition key for serialising processing. Events with the same key are never processed
   * concurrently. Return null to process independently (each event in its own partition).
   */
  fun getPartitionKey(inboxEvent: InboxEvent): String? = null

  /**
   * Process the inbox event. Should run in its own transaction to make success or failure isolated per
   * event. This function should be idempotent.
   *
   * Exceptions should be thrown to indicate failures that require manual intervention (in this case
   * the event will be marked as `FAILED` and an alert will be raised)
   */
  fun handle(inboxEvent: InboxEvent): Result

  enum class Result {
    PROCESSED,
    IGNORED,
  }

  data class InboxEvent(
    val id: UUID,
    val eventDetailUrl: String?,
    val payload: String,
  ) {
    fun uri(): URI = URI.create(requireNotNull(eventDetailUrl) { "Missing detail url" })
  }
}
