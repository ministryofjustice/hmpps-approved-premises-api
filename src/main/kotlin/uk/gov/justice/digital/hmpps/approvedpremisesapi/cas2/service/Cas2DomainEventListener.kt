package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService

@ConditionalOnProperty(prefix = "feature-flags", name = ["cas2-sqs-listener-enabled"], havingValue = "true")
@Service
class Cas2DomainEventListener(
  private val jsonMapper: JsonMapper,
  private val locationChangedService: Cas2LocationChangedService,
  private val allocationChangedService: Cas2AllocationChangedService,
  private val sentryService: SentryService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Suppress("TooGenericExceptionCaught")
  @SqsListener("castwodomaineventslistenerqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(msg: String) {
    try {
      val (message) = jsonMapper.readValue<SQSMessage>(msg)
      val event = jsonMapper.readValue<HmppsDomainEvent>(message)
      handleEvent(event)
    } catch (e: Exception) {
      log.error("Exception caught in Cas2DomainEventListener", e)
      sentryService.captureException(e)
      throw e
    }
  }

  private fun handleEvent(event: HmppsDomainEvent) {
    when (event.eventType) {
      "prisoner-offender-search.prisoner.updated" -> locationChangedService.process(event)
      "offender-management.allocation.changed" -> allocationChangedService.process(event)
    }
  }

  data class SQSMessage(
    @JsonProperty("Message") val message: String,
  )
}
