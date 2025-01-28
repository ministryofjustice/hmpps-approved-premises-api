package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@ConditionalOnProperty(prefix = "feature-flags", name = ["cas2-sqs-listener-enabled"], havingValue = "true")
@Service
class MessageListener(private val messageService: MessageService) {

  @SqsListener("castwodomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(message: Message) {
    messageService.handleMessage(message)
  }
}

@SuppressWarnings("ConstructorParameterNaming")
data class MessageAttribute(val Value: String, val Type: String)
typealias EventType = MessageAttribute

class MessageAttributes() : HashMap<String, MessageAttribute>() {
  constructor(attribute: EventType) : this() {
    put(attribute.Value, attribute)
  }
}

@SuppressWarnings("ConstructorParameterNaming")
data class Message(
  val Message: String,
  val MessageId: String,
  val MessageAttributes: MessageAttributes,
)
