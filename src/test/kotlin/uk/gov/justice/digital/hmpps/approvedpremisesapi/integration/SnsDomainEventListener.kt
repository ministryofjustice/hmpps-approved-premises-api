package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.fail
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import org.springframework.test.context.event.annotation.BeforeTestMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import java.time.Duration

@Service
class SnsDomainEventListener(private val objectMapper: ObjectMapper) {
  private val log = LoggerFactory.getLogger(this::class.java)
  private val messages = mutableListOf<SnsEvent>()

  /**
   * The [Jmslistener](https://www.baeldung.com/spring-jms) annotation will automatically set up a
   * consumer for the corresponding SNS queue and invoke this function for every message.
   *
   * We utilise the [hmpss spring boot sqs starter](https://github.com/ministryofjustice/hmpps-spring-boot-sqs)
   * to configure the queue container factory, using localstack to emulate SNS.
   */
  @JmsListener(destination = "domaineventsqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String?) {
    val (message) = objectMapper.readValue(rawMessage, Message::class.java)
    val event = objectMapper.readValue(message, SnsEvent::class.java)

    log.info("Received Domain Event: $event")
    synchronized(messages) {
      messages.add(event)
    }
  }

  @BeforeTestMethod
  fun clearMessages() = messages.clear()

  fun blockForMessage(eventType: String): SnsEvent {
    var waitedCount = 0
    while (!contains(eventType)) {
      if (waitedCount >= Duration.ofSeconds(15).toMillis()) {
        fail<Any>("Did not receive SQS message of type $eventType from SNS topic after 15s")
      }

      Thread.sleep(100)
      waitedCount += 100
    }

    synchronized(messages) {
      return messages.first { it.eventType == eventType }
    }
  }

  fun isEmpty(): Boolean {
    synchronized(messages) {
      return messages.isEmpty()
    }
  }

  fun contains(eventType: String): Boolean {
    synchronized(messages) {
      return messages.firstOrNull { it.eventType == eventType } != null
    }
  }
}

// Warning suppressed because we have to match the SNS attribute naming case
@SuppressWarnings("ConstructorParameterNaming")
data class Message(
  val Message: String,
  val MessageId: String,
  val MessageAttributes: MessageAttributes,
)

data class MessageAttributes(val eventType: EventType)

// Warning suppressed because we have to match the SNS attribute naming case
@SuppressWarnings("ConstructorParameterNaming")
data class EventType(val Value: String, val Type: String)
