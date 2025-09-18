package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService

@ConditionalOnProperty(prefix = "feature-flags", name = ["cas2v2-sqs-listener-enabled"], havingValue = "true")
@Service
class Cas2v2DomainEventListener(
  private val objectMapper: ObjectMapper,
  private val sentryService: SentryService,
  private val cas2v2ApplicationService: Cas2v2ApplicationService,

) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Suppress("TooGenericExceptionCaught")
  @SqsListener("castwovtwodomaineventslistenerqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(msg: String) {
    try {
      val (message) = objectMapper.readValue<SQSMessage>(msg)
      val event = objectMapper.readValue<HmppsDomainEvent>(message)
      handleEvent(event)
    } catch (e: Exception) {
      log.error("Exception caught in Cas2DomainEventListener", e)
      sentryService.captureException(e)
      throw e
    }
  }

  @Suppress("ThrowsCount", "NestedBlockDepth")
  private fun handleEvent(event: HmppsDomainEvent) {
    when (event.eventType) {
      "probation-case.prison-identifier.added", "probation-case.prison-identifier.updated" -> {
        event.personReference.findNomsNumber()?.let { nomsNumber ->
          event.personReference.findCrn()?.let { crn ->
            cas2v2ApplicationService.getCas2v2ApplicationsByCrn(crn).forEach { application ->
              application.nomsNumber = nomsNumber
              // TODO besscerule when should this happen? does the app need to be submitted?
              application.referringPrisonCode = cas2v2ApplicationService.retrievePrisonCode(application)
            }
          } ?: throw error("CRN not found on ${event.eventType} event")
        } ?: throw error("NOMS Number not found on ${event.eventType} event")
      }
      else -> throw NotImplementedError("Unexpected message type received: ${event.eventType}")
    }
  }

  data class SQSMessage(
    @JsonProperty("Message") val message: String,
  )
}
