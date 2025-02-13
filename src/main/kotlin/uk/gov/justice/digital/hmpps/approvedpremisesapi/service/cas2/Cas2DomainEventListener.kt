package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent

@ConditionalOnProperty(prefix = "feature-flags", name = ["cas2-sqs-listener-enabled"], havingValue = "true")
@Service
class Cas2DomainEventListener(
  private val objectMapper: ObjectMapper,
  private val prisonerLocationService: PrisonerLocationService,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @SqsListener("castwodomaineventslistenerqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(msg: String) {
    val (message) = objectMapper.readValue<SQSMessage>(msg)
    val event = objectMapper.readValue<HmppsDomainEvent>(message)
    val prisonNumber = event.personReference.findNomsNumber()
    log.info("Request received to process domain event type ${event.eventType} for prisoner number $prisonNumber")
    handleEvent(event)
  }

  private fun handleEvent(event: HmppsDomainEvent) {
    when (event.eventType) {
      "prisoner-offender-search.prisoner.updated" -> {
        if (event.categoriesChanged?.find { it == "LOCATION" } != null) {
          prisonerLocationService.handleLocationChangedEvent(event)
        } else {
          log.info("No Location category, ignore event")
        }
      }

      "offender-management.allocation.changed" -> prisonerLocationService.handleAllocationChangedEvent(event)
      else -> log.error("Unknown event type: ${event.eventType}")
    }
  }

  data class SQSMessage(
    @JsonProperty("Message") val message: String,
  )
}
