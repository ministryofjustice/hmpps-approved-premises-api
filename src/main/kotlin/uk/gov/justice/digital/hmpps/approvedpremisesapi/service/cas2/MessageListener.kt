package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MessageListener {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("inboundqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(message: Message) {
    log.info("received event: {}", message)

    print(message)
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
