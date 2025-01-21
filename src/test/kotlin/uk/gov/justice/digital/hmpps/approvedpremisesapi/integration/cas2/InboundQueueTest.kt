package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.setup.putMessageOnQueue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.MessageListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.MessageService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.util.concurrent.TimeUnit

class InboundQueueTest : IntegrationTestBase() {

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  @SpykBean
  private lateinit var mockMessageService: MessageService

  @SpykBean
  private lateinit var mockMessageListener: MessageListener

  private val inboundQueue by lazy {
    hmppsQueueService.findByQueueId("inboundqueue") ?: throw MissingQueueException("HmppsQueue inboundqueue not found")
  }

  private val inboundQueueClient by lazy { inboundQueue.sqsClient }

  fun putMessageOnInboundQueue() = putMessageOnQueue(
    inboundQueueClient,
    inboundQueue.queueUrl,
  )

  @Test
  fun `Put Message on Inbound Queue Request is successful`() {
    putMessageOnInboundQueue()
    TimeUnit.MILLISECONDS.sleep(10000)
    verify(exactly = 1) { mockMessageListener.processMessage(any()) }
    verify(exactly = 1) { mockMessageService.handleMessage(any()) }
  }
}
