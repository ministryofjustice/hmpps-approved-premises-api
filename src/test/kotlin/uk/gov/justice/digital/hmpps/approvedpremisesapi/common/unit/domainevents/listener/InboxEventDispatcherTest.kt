package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.unit.domainevents.listener

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener.DispatcherConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener.InboxEventDispatcher
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener.InboxEventHandler
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener.InboxEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.factory.InboxEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.InboxEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.ProcessedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService

/**
 * Concurrency and batching is tested by [InboxEventDispatcherIT]
 */
@ExtendWith(MockKExtension::class)
class InboxEventDispatcherTest {

  @RelaxedMockK
  lateinit var inboxEventService: InboxEventService

  @RelaxedMockK
  lateinit var sentryService: SentryService

  @Test
  fun `no events, do nothing`() {
    every { inboxEventService.findPendingOldestFirst(10) } returns emptyList()

    val stats = inboxEventDispatcher(
      maxEventsPerBatch = 10,
    ).process()

    assertThat(stats.processedCount).isEqualTo(0)
    assertThat(stats.ignoredCount).isEqualTo(0)
    assertThat(stats.skippedCount).isEqualTo(0)
    assertThat(stats.failedCount).isEqualTo(0)

    verifyNoEventUpdatesMade()
  }

  @Test
  fun `single event, no handler, skip and alert`() {
    val event = buildPendingInboxEventEntity(eventType = "test.event")

    every { inboxEventService.findPendingOldestFirst(10) } returns listOf(event)

    val stats = inboxEventDispatcher(
      handlers = emptyList(),
      maxEventsPerBatch = 10,
    ).process()

    assertThat(stats.processedCount).isEqualTo(0)
    assertThat(stats.ignoredCount).isEqualTo(0)
    assertThat(stats.skippedCount).isEqualTo(1)
    assertThat(stats.failedCount).isEqualTo(0)

    verifyNoEventUpdatesMade()
    verify { sentryService.captureErrorMessage("No handler registered for event type [inboxEventId=${event.id}, eventType=test.event]") }
  }

  @Test
  fun `handler returns PROCESSED, update event processed state to PROCESSED`() {
    val event = buildPendingInboxEventEntity(eventType = "test.event")

    every { inboxEventService.findPendingOldestFirst(10) } returns listOf(event)

    val handler = MockEventHandler(
      supportedEventType = "test.event",
      result = InboxEventHandler.Result.PROCESSED,
    )

    val stats = inboxEventDispatcher(
      handlers = listOf(handler),
      maxEventsPerBatch = 10,
    ).process()

    handler.assertThatHasProcessedEvent(event)

    assertThat(stats.processedCount).isEqualTo(1)
    assertThat(stats.ignoredCount).isEqualTo(0)
    assertThat(stats.skippedCount).isEqualTo(0)
    assertThat(stats.failedCount).isEqualTo(0)

    verify { inboxEventService.updateInboxEventStatusAndSave(event, ProcessedStatus.PROCESSED) }
  }

  @Test
  fun `handler returns IGNORED, update event processed state to IGNORED`() {
    val event = buildPendingInboxEventEntity(eventType = "test.event")

    every { inboxEventService.findPendingOldestFirst(10) } returns listOf(event)

    val handler = MockEventHandler(
      supportedEventType = "test.event",
      result = InboxEventHandler.Result.IGNORED,
    )

    val stats = inboxEventDispatcher(
      handlers = listOf(handler),
      maxEventsPerBatch = 10,
    ).process()

    handler.assertThatHasProcessedEvent(event)

    assertThat(stats.processedCount).isEqualTo(0)
    assertThat(stats.ignoredCount).isEqualTo(1)
    assertThat(stats.skippedCount).isEqualTo(0)
    assertThat(stats.failedCount).isEqualTo(0)

    verify { inboxEventService.updateInboxEventStatusAndSave(event, ProcessedStatus.IGNORED) }
  }

  @Test
  fun `handler returns FAILED, update event processed state to FAILED and raise alert`() {
    val event = buildPendingInboxEventEntity(eventType = "test.event")

    every { inboxEventService.findPendingOldestFirst(10) } returns listOf(event)

    val handler = MockEventHandler(
      supportedEventType = "test.event",
      result = InboxEventHandler.Result.FAILED,
    )

    val stats = inboxEventDispatcher(
      handlers = listOf(handler),
      maxEventsPerBatch = 10,
    ).process()

    handler.assertThatHasProcessedEvent(event)

    assertThat(stats.processedCount).isEqualTo(0)
    assertThat(stats.ignoredCount).isEqualTo(0)
    assertThat(stats.skippedCount).isEqualTo(0)
    assertThat(stats.failedCount).isEqualTo(1)

    verify { inboxEventService.updateInboxEventStatusAndSave(event, ProcessedStatus.FAILED) }
    verify { sentryService.captureErrorMessage("Unexpected error dispatching to handler [inboxEventId=${event.id}, eventType=${event.eventType}]") }
  }

  @Test
  fun `handler throws Exception, ,update event processed state to FAILED and raise alert`() {
    val event = buildPendingInboxEventEntity(eventType = "test.event")

    every { inboxEventService.findPendingOldestFirst(10) } returns listOf(event)

    val exception = Exception("error message")

    val handler = MockEventHandler(
      supportedEventType = "test.event",
      responseException = exception,
      result = InboxEventHandler.Result.FAILED,
    )

    val stats = inboxEventDispatcher(
      handlers = listOf(handler),
      maxEventsPerBatch = 10,
    ).process()

    handler.assertThatHasProcessedEvent(event)

    assertThat(stats.processedCount).isEqualTo(0)
    assertThat(stats.ignoredCount).isEqualTo(0)
    assertThat(stats.skippedCount).isEqualTo(0)
    assertThat(stats.failedCount).isEqualTo(1)

    verify { inboxEventService.updateInboxEventStatusAndSave(event, ProcessedStatus.FAILED) }

    val raisedExceptionSlot = slot<InboxEventDispatcher.InboxEventDispatcherFailureException>()
    verify { sentryService.captureException(capture(raisedExceptionSlot)) }

    assertThat(raisedExceptionSlot.captured.message).isEqualTo("Unexpected error dispatching to handler [inboxEventId=${event.id}, eventType=${event.eventType}]")
    assertThat(raisedExceptionSlot.captured.cause).isEqualTo(exception)
  }

  private fun inboxEventDispatcher(
    handlers: List<InboxEventHandler> = emptyList(),
    maxEventsPerBatch: Int = 1,
    maxConcurrentEvents: Int = 1,
  ) = InboxEventDispatcher(
    handlers = handlers,
    dispatcherConfig = DispatcherConfig(maxEventsPerBatch, maxConcurrentEvents),
    inboxEventService = inboxEventService,
    sentryService = sentryService,
  )

  private data class MockEventHandler(
    val supportedEventType: String,
    val result: InboxEventHandler.Result,
    val responseException: Throwable? = null,
    val processedEvents: MutableList<InboxEventHandler.InboxEvent> = mutableListOf(),
  ) : InboxEventHandler {
    override fun supportedEventType() = supportedEventType
    override fun handle(inboxEvent: InboxEventHandler.InboxEvent): InboxEventHandler.Result {
      processedEvents.add(inboxEvent)

      if (responseException != null) {
        throw responseException
      }

      return result
    }

    fun assertThatHasProcessedEvent(event: InboxEventEntity) {
      assertThat(processedEvents.map { it.id }).contains(event.id)
    }
  }

  private fun verifyNoEventUpdatesMade() {
    verify(exactly = 0) { inboxEventService.updateInboxEventStatusAndSave(any(), any()) }
  }

  private fun buildPendingInboxEventEntity(eventType: String) = InboxEventEntityFactory()
    .withEventType(eventType)
    .produce()
}
