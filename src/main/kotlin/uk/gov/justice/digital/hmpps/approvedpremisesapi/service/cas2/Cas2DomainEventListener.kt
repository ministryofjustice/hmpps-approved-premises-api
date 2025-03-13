package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.categoriesChanged

@ConditionalOnProperty(prefix = "feature-flags", name = ["cas2-sqs-listener-enabled"], havingValue = "true")
@Service
class Cas2DomainEventListener(
  private val objectMapper: ObjectMapper,
  private val locationChangedService: Cas2LocationChangedService,
  private val allocationChangedService: Cas2AllocationChangedService,
) {

  @SqsListener("castwodomaineventslistenerqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(msg: String) {
    val (message) = objectMapper.readValue<SQSMessage>(msg)
    val event = objectMapper.readValue<HmppsDomainEvent>(message)
    handleEvent(event)
  }

  private fun handleEvent(event: HmppsDomainEvent) {
    when (event.eventType) {
      "prisoner-offender-search.prisoner.updated" -> {
        if (event.additionalInformation.categoriesChanged.contains("LOCATION")) {
          locationChangedService.handleLocationChangedEvent(event)
        }
      }

      "offender-management.allocation.changed" -> allocationChangedService.handleAllocationChangedEvent(event)
    }
  }

  data class SQSMessage(
    @JsonProperty("Message") val message: String,
  )
}
