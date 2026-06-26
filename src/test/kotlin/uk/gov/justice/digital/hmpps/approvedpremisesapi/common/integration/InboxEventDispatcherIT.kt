package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener.DispatcherConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener.InboxEventDispatcher
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.factory.HmppsDomainEventFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.InboxEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.ProcessedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonReference
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Integration tests for [InboxEventDispatcher] proving:
 * - Virtual threads enable concurrent processing
 * - Events are processed in eventOccurredAt order (oldest first)
 * - maxEventsPerBatch limits batch size
 */

class InboxEventDispatcherIT : IntegrationTestBase() {
  @Autowired
  lateinit var inboxEventDispatcher: InboxEventDispatcher

  @Autowired
  lateinit var dispatcherConfig: DispatcherConfig

  @Autowired
  lateinit var mockEventHandler: MockInboxEventHandler

  @Autowired
  lateinit var inboxAsserter: InboxAsserter

  private val crn = UUID.randomUUID().toString()

  @BeforeEach
  fun setup() {
    dispatcherConfig.maxConcurrentEvents = 4
    dispatcherConfig.maxEventsPerBatch = 10
  }

  @Test
  fun `processes only maxEventsPerBatch events per invocation`() {
    dispatcherConfig.maxEventsPerBatch = 2

    val baseTime = OffsetDateTime.now(ZoneOffset.UTC)
    (1..5).forEach { i ->
      createInboxEvent(eventOccurredAt = baseTime.plusSeconds(i.toLong()), crn = UUID.randomUUID().toString())
    }

    inboxEventDispatcher.process()

    val processed =
      inboxEventRepository.findAllByProcessedStatus(
        ProcessedStatus.PROCESSED,
        Pageable.unpaged(Sort.by("eventOccurredAt").ascending()),
      )
    val pending =
      inboxEventRepository.findAllByProcessedStatus(
        ProcessedStatus.PENDING,
        Pageable.unpaged(Sort.by("eventOccurredAt").ascending()),
      )

    assertThat(processed).hasSize(2)
    assertThat(pending).hasSize(3)

    mockEventHandler.assertThatHasProcessedEvents(processed)
  }

  @Test
  fun `processes events in eventOccurredAt ascending order`() {
    dispatcherConfig.maxEventsPerBatch = 1

    val t1 = OffsetDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
    val t2 = OffsetDateTime.of(2025, 1, 1, 11, 0, 0, 0, ZoneOffset.UTC)
    val t3 = OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)

    createInboxEvent(eventOccurredAt = t3, crn = UUID.randomUUID().toString())
    createInboxEvent(eventOccurredAt = t1, crn = UUID.randomUUID().toString())
    createInboxEvent(eventOccurredAt = t2, crn = UUID.randomUUID().toString())

    inboxEventDispatcher.process()

    val firstBatch =
      inboxEventRepository
        .findAllByProcessedStatus(
          ProcessedStatus.PROCESSED,
          PageRequest.of(0, 10, Sort.by("processedAt").ascending()),
        )

    val firstProcessed = firstBatch.single()

    inboxEventDispatcher.process()

    val secondBatch = inboxEventRepository
      .findAllByProcessedStatus(
        ProcessedStatus.PROCESSED,
        PageRequest.of(0, 10, Sort.by("processedAt").ascending()),
      )
    assertThat(secondBatch).hasSize(2)
    val secondProcessed = secondBatch.last()

    inboxEventDispatcher.process()
    val thirdBatch =
      inboxEventRepository
        .findAllByProcessedStatus(
          ProcessedStatus.PROCESSED,
          PageRequest.of(0, 10, Sort.by("processedAt").ascending()),
        )
    assertThat(thirdBatch).hasSize(3)
    val thirdProcessed = thirdBatch.last()

    assertThat(firstProcessed.eventOccurredAt).isEqualTo(t1)
    assertThat(secondProcessed.eventOccurredAt).isEqualTo(t2)
    assertThat(thirdProcessed.eventOccurredAt).isEqualTo(t3)

    mockEventHandler.assertThatHasProcessedEvents(listOf(firstProcessed, secondProcessed, thirdProcessed))
  }

  @Test
  fun `processes at most 4 events concurrently due to semaphore limit`() {
    dispatcherConfig.maxEventsPerBatch = 5
    dispatcherConfig.maxConcurrentEvents = 4
    val crns = (1..5).map { "X12345$it" }

    val baseTime = OffsetDateTime.now(ZoneOffset.UTC)
    crns.mapIndexed { i, c ->
      createInboxEvent(eventOccurredAt = baseTime.plusSeconds(i.toLong()), crn = c)
    }

    inboxEventDispatcher.process()

    inboxAsserter.assertProcessedCount(5)

    assertThat(mockEventHandler.maxConcurrent.get()).isEqualTo(4)
  }

  @Test
  fun `processes multiple events concurrently using coroutines`() {
    val delayMs = 200
    val crns = listOf("X123451", "X123452", "X123453", "X123454")

    val baseTime = OffsetDateTime.now(ZoneOffset.UTC)
    crns.mapIndexed { i, c ->
      createInboxEvent(eventOccurredAt = baseTime.plusSeconds(i.toLong()), crn = c)
    }

    val start = System.currentTimeMillis()
    inboxEventDispatcher.process()
    val elapsed = System.currentTimeMillis() - start

    val processed =
      inboxEventRepository.findAllByProcessedStatus(
        ProcessedStatus.PROCESSED,
        PageRequest.of(0, 10, Sort.by("eventOccurredAt").ascending()),
      )

    inboxAsserter.assertProcessedCount(4)

    // With 4 events and semaphore(4), parallel execution: ~delayMs. Sequential would be
    // 4*delayMs.
    assertThat(elapsed).isLessThan((delayMs * 3).toLong())
  }

  private fun createInboxEvent(
    eventOccurredAt: OffsetDateTime,
    crn: String = this.crn,
  ): InboxEventEntity {
    val payload =
      HmppsDomainEventFactory()
        .withEventType(MockInboxEventHandler.EVENT_TYPE)
        .withPersonReference(
          PersonReference(
            listOf(
              PersonIdentifier("CRN", crn),
            ),
          ),
        ).produce()

    return buildPendingInboxEventEntity(
      eventType = MockInboxEventHandler.EVENT_TYPE,
      eventOccurredAt = eventOccurredAt,
      payload = jsonMapper.writeValueAsString(payload),
    )
  }

  private fun buildPendingInboxEventEntity(
    eventType: String,
    eventOccurredAt: OffsetDateTime,
    payload: String,
  ) = inboxEventEntityFactory.produceAndPersist {
    withEventType(eventType)
    withEventOccurredAt(eventOccurredAt)
    withPayload(payload)
    withProcessedStatus(ProcessedStatus.PENDING)
    withProcessedAt(null)
  }
}
