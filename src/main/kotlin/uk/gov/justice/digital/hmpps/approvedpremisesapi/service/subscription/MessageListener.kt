package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subscription

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service

@Service
class MessageListener(private val messageService: MessageService) {

  @SqsListener("inboundqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(message: Message) {
    messageService.handleMessage(message)
  }
}

data class MessageAttribute(val Value: String, val Type: String)
typealias EventType = MessageAttribute
class MessageAttributes() : HashMap<String, MessageAttribute>() {
  constructor(attribute: EventType) : this() {
    put(attribute.Value, attribute)
  }
}
data class Message(
  val Message: String,
  val MessageId: String,
  val MessageAttributes: MessageAttributes,
)
