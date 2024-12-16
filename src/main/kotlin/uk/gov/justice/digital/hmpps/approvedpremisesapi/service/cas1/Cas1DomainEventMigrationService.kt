package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate

/**
This is tested by the [DomainEventTest] integration test
 */
@Service
class Cas1DomainEventMigrationService(val objectMapper: ObjectMapper) {
  fun bookingCancelledJson(entity: DomainEventEntity) =
    when (entity.schemaVersion) {
      2 -> entity.data
      else -> bookingCancelledV1JsonToV2Json(entity)
    }

  fun bookingCancelledV1JsonToV2Json(domainEventEntity: DomainEventEntity): String {
    val dataModel: JsonNode = objectMapper.readTree(domainEventEntity.data)
    val eventDetails = dataModel["eventDetails"] as ObjectNode

    val cancellationRecordedAt = objectMapper.convertValue(domainEventEntity.occurredAt, TextNode::class.java)
    eventDetails.set<TextNode>("cancellationRecordedAt", cancellationRecordedAt)

    val cancelledAt = objectMapper.convertValue(dataModel["eventDetails"]["cancelledAt"], java.time.Instant::class.java)
    val cancelledAtDate = objectMapper.convertValue(cancelledAt.toLocalDate(), TextNode::class.java)
    eventDetails.set<ArrayNode>("cancelledAtDate", cancelledAtDate)

    return objectMapper.writeValueAsString(dataModel)
  }
}
