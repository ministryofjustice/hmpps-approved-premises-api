package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.integration

import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.test.context.event.annotation.BeforeTestMethod
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener.InboxEventHandler
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.InboxEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.map
import kotlin.jvm.java

/**
 * A mock event handler installed as a spring bean so it can be used when testing
 * the inbox event dispatcher in [InboxEventDispatcherIT]. Partitions on CRN
 *
 * Will always return 'PROCESSED'
 */
@Service
class MockInboxEventHandler(
  private val jsonMapper: JsonMapper,
  val currentCount: AtomicInteger = AtomicInteger(0),
  val maxConcurrent: AtomicInteger = AtomicInteger(0),
) : InboxEventHandler {

  private val log = LoggerFactory.getLogger(javaClass)

  companion object {
    const val EVENT_TYPE: String = "mock.event.handler"
  }

  val processedEvents: MutableList<InboxEventHandler.InboxEvent> = Collections.synchronizedList(mutableListOf())

  override fun getPartitionKey(inboxEvent: InboxEventHandler.InboxEvent): String? {
    val snsDomainEvent = jsonMapper.readValue(inboxEvent.payload, SnsEvent::class.java)
    return snsDomainEvent.personReference.findCrn()
  }

  override fun supportedEventType() = EVENT_TYPE

  override fun handle(inboxEvent: InboxEventHandler.InboxEvent): InboxEventHandler.Result {
    val c = currentCount.incrementAndGet()
    maxConcurrent.updateAndGet { maxOf(it, c) }

    try {
      Thread.sleep(150)

      processedEvents.add(inboxEvent)
      log.info("Have handled event. Processed events size is now ${processedEvents.size}")

      return InboxEventHandler.Result.PROCESSED
    } finally {
      currentCount.decrementAndGet()
    }
  }

  fun assertThatHasProcessedEvents(events: List<InboxEventEntity>) {
    assertThat(processedEvents.map { it.id }).containsExactlyInAnyOrderElementsOf(events.map { it.id })
  }

  @BeforeTestMethod
  fun reset() {
    processedEvents.clear()
  }
}
