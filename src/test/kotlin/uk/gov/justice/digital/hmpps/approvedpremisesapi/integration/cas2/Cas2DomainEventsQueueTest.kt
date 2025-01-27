package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.setup.putUnwantedMessageOnQueue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.setup.putWantedMessageOnQueue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.MessageListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.MessageService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.util.concurrent.TimeUnit

class Cas2DomainEventsQueueTest : IntegrationTestBase() {

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  @SpykBean
  private lateinit var mockMessageService: MessageService

  @SpykBean
  private lateinit var mockMessageListener: MessageListener

  private val cas2DomainEventsQueue by lazy {
    hmppsQueueService.findByQueueId("castwodomaineventsqueue") ?: throw MissingQueueException("HmppsQueue castwodomaineventsqueue not found")
  }

  private val cas2DomainEventsClient by lazy { cas2DomainEventsQueue.sqsClient }

  fun putMessageOnCas2DomainEventsQueue() = putWantedMessageOnQueue(
    cas2DomainEventsClient,
    cas2DomainEventsQueue.queueUrl,
  )

  fun putUnwantedMessageOnCas2DomainEventsQueue() = putUnwantedMessageOnQueue(
    cas2DomainEventsClient,
    cas2DomainEventsQueue.queueUrl,
  )

  @Test
  fun `Put Message on CAS 2 Domain Events Queue Request is successful`() {
    putMessageOnCas2DomainEventsQueue()
    TimeUnit.MILLISECONDS.sleep(10000)
    verify(exactly = 1) { mockMessageListener.processMessage(any()) }
    verify(exactly = 1) { mockMessageService.handleMessage(any()) }
  }

  @Test
  fun `Put Unwanted Message on CAS 2 Domain Events Queue Request is successful`() {
    putUnwantedMessageOnCas2DomainEventsQueue()
    TimeUnit.MILLISECONDS.sleep(10000)
    verify(exactly = 0) { mockMessageListener.processMessage(any()) }
  }
}
