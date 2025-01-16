package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subscription

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service

@Service
class MessageListener(private val messageService: MessageService, private val objectMapper: ObjectMapper) {

  @SqsListener("inboundqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(message: Message) {
    val event = objectMapper.readValue(message.message, HmppsEvent::class.java)
    messageService.handleMessage(event)
  }
}

data class HmppsEvent(val id: String, val type: String, val contents: String)
data class MessageAttribute(val value: String, val type: String)
typealias EventType = MessageAttribute
class MessageAttributes() : HashMap<String, MessageAttribute>() {
  constructor(attribute: EventType) : this() {
    put(attribute.value, attribute)
  }
}
data class Message(
  val message: String,
  val messageId: String,
  val messageAttributes: MessageAttributes,
)
