package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent

@ConditionalOnProperty(prefix = "feature-flags", name = ["cas2-sqs-listener-enabled"], havingValue = "true")
@Service
class Cas2DomainEventListener(
  private val objectMapper: ObjectMapper,
  private val cas2ApplicationAssignmentService: Cas2ApplicationAssignmentService,
) {

  @SqsListener("castwodomaineventslistenerqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(msg: String) {
    val (message) = objectMapper.readValue<SQSMessage>(msg)
    val event = objectMapper.readValue<HmppsDomainEvent>(message)
    handleEvent(event)
  }

  private fun handleEvent(event: HmppsDomainEvent) {
    when (event.eventType) {
      "prisoner-offender-search.prisoner.updated" -> cas2ApplicationAssignmentService.handlePrisonerUpdatedEvent(event)
      "offender-management.allocation.changed" -> cas2ApplicationAssignmentService.handleAllocationChangedEvent(event)
    }
  }

  data class SQSMessage(
    @JsonProperty("Message") val message: String,
  )
}
