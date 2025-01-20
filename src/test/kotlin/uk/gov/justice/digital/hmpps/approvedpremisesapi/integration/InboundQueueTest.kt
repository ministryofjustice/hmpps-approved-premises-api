package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.ninjasquad.springmockk.SpykBean
import org.junit.jupiter.api.Test
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.setup.putMessageOnQueue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subscription.MessageListener
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.util.concurrent.TimeUnit
class InboundQueueTest : IntegrationTestBase() {

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @SpykBean
  lateinit var mockMessageListener: MessageListener

  private val inboundQueue by lazy {
    hmppsQueueService.findByQueueId("inboundqueue")
      ?: throw MissingQueueException("HmppsQueue inboundqueue not found")
  }

  private val inboundQueueClient by lazy { inboundQueue.sqsClient }

  fun putMessageOnInboundQueue() = putMessageOnQueue(
    inboundQueueClient,
    inboundQueue.queueUrl
  )

  @Test
  fun `Put Message on Inbound Queue Request is successful`() {
    putMessageOnInboundQueue()
    TimeUnit.MILLISECONDS.sleep(10000)
    verify(exactly = 1) { mockMessageListener.processMessage(any()) }
  }

}