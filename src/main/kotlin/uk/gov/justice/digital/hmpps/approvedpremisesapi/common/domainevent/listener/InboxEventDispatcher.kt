package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.InboxEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.ProcessedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.partition

/**
 * Dispatches inbox events to registered handlers. Each event is processed in isolation - handlers
 * manage their own transactions. Add new event types by implementing [InboxEventHandler] and
 * registering as a Spring bean.
 *
 * Partitions events by [InboxEventHandler.getPartitionKey] so that events for the same key are
 * never processed concurrently. This avoids race conditions when updating the same resource. Events
 * with different keys run in parallel using coroutines. Events are fetched ordered by
 * [eventOccurredAt] ascending; within each partition, this order is preserved.
 *
 * If processing of an event fails (either with a FAILED response or from it throwing an exception)
 * it will be recorded as FAILED and a sentry alert raised
 */

@Component
@ConfigurationProperties(prefix = "domain-events.inbox.dispatcher")
class DispatcherConfig(
  var maxEventsPerBatch: Int = 10,
  var maxConcurrentEvents: Int = 4,
)

@Component
class InboxEventDispatcher(
  handlers: List<InboxEventHandler>,
  private val dispatcherConfig: DispatcherConfig,
  private val inboxEventService: InboxEventService,
  private val sentryService: SentryService,
) {
  private val log = LoggerFactory.getLogger(javaClass)

  private val eventTypeToHandlers: Map<String, InboxEventHandler> =
    handlers.associateBy { it.supportedEventType() }

  @Scheduled(cron = $$"${domain-events.inbox.dispatcher.cron}")
  @SchedulerLock(
    name = "InboxEventDispatcher",
    lockAtMostFor = $$"${domain-events.inbox.dispatcher.shedlock.lock-at-most-for}",
    lockAtLeastFor = $$"${domain-events.inbox.dispatcher.shedlock.lock-at-least-for}",
  )
  fun processScheduled() = process()

  fun process() = runBlocking {
    val progressTracker = ProgressTracker()
    val batchSize = dispatcherConfig.maxEventsPerBatch

    val inboxEvents = inboxEventService.findPendingOldestFirst(dispatcherConfig.maxEventsPerBatch)
    if (inboxEvents.isEmpty()) {
      return@runBlocking progressTracker.toStats()
    }

    val concurrencyLimit = Semaphore(dispatcherConfig.maxConcurrentEvents)

    log.info("Processing inbox batch [count={}, eventIds={}, batchSize={}]", inboxEvents.size, inboxEvents.map { it.id }, batchSize)

    val (partitions, eventsWithoutHandlers) = partitionByKey(inboxEvents)
    eventsWithoutHandlers.forEach {
      sentryService.captureErrorMessage("No handler registered for event type [inboxEventId=${it.id}, eventType=${it.eventType}]")
      progressTracker.eventSkipped()
    }
    log.debug("Partitioned into {} groups", partitions.size)

    coroutineScope {
      partitions.map { (_, events) ->
        async(Dispatchers.IO) {
          concurrencyLimit.withPermit {
            events.forEach { dispatchEvent(it, progressTracker) }
          }
        }
      }.awaitAll()
    }

    log.info(
      "Inbox batch complete [total={}, processed={}, notProcessed={}, failed={}, skipped={}]",
      inboxEvents.size,
      progressTracker.processedCount.get(),
      progressTracker.notProcessedCount.get(),
      progressTracker.failedCount.get(),
      progressTracker.skippedCount.get(),
    )

    progressTracker.toStats()
  }

  /**
   * Groups events by partition key. The given event order is retained with-in the partition
   * (i.e. by eventOccurredAt asc)
   */
  private fun partitionByKey(
    inboxEvents: List<InboxEventEntity>,
  ): PartitioningResult {
    val (withHandler, withoutHandler) = inboxEvents.partition { it.resolveHandler() != null }
    val partitions: Map<String, List<InboxEventEntity>> = withHandler.groupBy { event ->
      event.resolveHandler()!!.getPartitionKey(event.toInboxEvent()) ?: event.id.toString()
    }
    return PartitioningResult(partitions, withoutHandler)
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun dispatchEvent(
    inboxEvent: InboxEventEntity,
    progressTracker: ProgressTracker,
  ) {
    val handler = inboxEvent.resolveHandler()!!

    try {
      when (handler.handle(inboxEvent.toInboxEvent())) {
        InboxEventHandler.Result.PROCESSED -> {
          inboxEventService.updateInboxEventStatusAndSave(inboxEvent, ProcessedStatus.PROCESSED)
          progressTracker.eventProcessed()
        }
        InboxEventHandler.Result.NOT_PROCESSED -> {
          inboxEventService.updateInboxEventStatusAndSave(inboxEvent, ProcessedStatus.NOT_PROCESSED)
          progressTracker.eventNotProcessed()
        }
        InboxEventHandler.Result.FAILED -> {
          sentryService.captureErrorMessage("Unexpected error dispatching to handler [inboxEventId=${inboxEvent.id}, eventType=${inboxEvent.eventType}]")
          inboxEventService.updateInboxEventStatusAndSave(inboxEvent, ProcessedStatus.FAILED)
          progressTracker.eventFailed()
        }
      }
    } catch (e: Throwable) {
      sentryService.captureException(
        InboxEventDispatcherFailureException("Unexpected error dispatching to handler [inboxEventId=${inboxEvent.id}, eventType=${inboxEvent.eventType}]", e),
      )
      inboxEventService.updateInboxEventStatusAndSave(inboxEvent, ProcessedStatus.FAILED)
      progressTracker.eventFailed()
    }
  }

  class InboxEventDispatcherFailureException(
    override val message: String,
    override val cause: Throwable,
  ) : Exception()

  private data class ProgressTracker(
    val processedCount: AtomicInteger = AtomicInteger(0),
    val notProcessedCount: AtomicInteger = AtomicInteger(0),
    val failedCount: AtomicInteger = AtomicInteger(0),
    val skippedCount: AtomicInteger = AtomicInteger(0),
  ) {
    fun eventSkipped() = skippedCount.incrementAndGet()
    fun eventFailed() = failedCount.incrementAndGet()
    fun eventProcessed() = processedCount.incrementAndGet()
    fun eventNotProcessed() = notProcessedCount.incrementAndGet()
    fun toStats() = EventDispatcherStats(processedCount.get(), notProcessedCount.get(), failedCount.get(), skippedCount.get())
  }

  data class EventDispatcherStats(
    val processedCount: Int,
    val notProcessedCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
  )

  private data class PartitioningResult(
    val partitions: Map<String, List<InboxEventEntity>>,
    val withoutHandlers: List<InboxEventEntity>,
  )

  private fun InboxEventEntity.resolveHandler() = eventTypeToHandlers[this.eventType]

  private fun InboxEventEntity.toInboxEvent() = InboxEventHandler.InboxEvent(
    id = this.id,
    eventDetailUrl = this.eventDetailUrl,
    payload = this.payload,
  )
}
