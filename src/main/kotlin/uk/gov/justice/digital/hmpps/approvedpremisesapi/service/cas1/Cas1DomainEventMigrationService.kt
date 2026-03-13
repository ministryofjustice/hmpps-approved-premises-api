package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import tools.jackson.module.kotlin.convertValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate

/**
This is tested by the DomainEventTest integration test
 */
@Service
class Cas1DomainEventMigrationService(
  val jsonMapper: JsonMapper,
  val userService: UserService,
) {

  fun bookingCancelledJson(entity: DomainEventEntity) = when (entity.schemaVersion) {
    2 -> entity.data
    else -> bookingCancelledV1JsonToV2Json(entity)
  }

  fun personArrivedJson(entity: DomainEventEntity) = when (entity.schemaVersion) {
    2 -> entity.data
    else -> personArrivedDepartedV1JsonToV2Json(entity)
  }

  fun personDepartedJson(entity: DomainEventEntity) = when (entity.schemaVersion) {
    2 -> entity.data
    else -> personArrivedDepartedV1JsonToV2Json(entity)
  }

  private fun bookingCancelledV1JsonToV2Json(domainEventEntity: DomainEventEntity): String = modifyEventDetails(domainEventEntity) { eventDetailsNode ->
    val cancellationRecordedAt = jsonMapper.convertValue(domainEventEntity.occurredAt, StringNode::class.java)
    eventDetailsNode.set("cancellationRecordedAt", cancellationRecordedAt)

    val cancelledAt =
      jsonMapper.convertValue(eventDetailsNode["cancelledAt"], java.time.Instant::class.java)
    val cancelledAtDate = jsonMapper.convertValue(cancelledAt.toLocalDate(), StringNode::class.java)
    eventDetailsNode.set("cancelledAtDate", cancelledAtDate)
  }

  private fun personArrivedDepartedV1JsonToV2Json(domainEventEntity: DomainEventEntity) = modifyEventDetails(domainEventEntity) { eventDetailsNode ->
    val triggeredByUser = domainEventEntity.triggeredByUserId?.let {
      userService.findByIdOrNull(it)
    }

      /*
      The primary purpose of this migration is to make v1 domain events schema valid.
      `recordedBy` is not used to render the timeline, and these old domain events
      will not be consumed externally, so the imperfect nature of the back-fill is acceptable
       */
    eventDetailsNode.set(
      "recordedBy",
      jsonMapper.convertValue(
        StaffMember(
          staffCode = triggeredByUser?.deliusStaffCode ?: "unknown",
          forenames = triggeredByUser?.name ?: "unknown",
          surname = "unknown",
          username = triggeredByUser?.deliusUsername ?: "unknown",
        ),
      ),
    )
  }

  private fun modifyEventDetails(
    domainEventEntity: DomainEventEntity,
    mutator: (eventDetailsNode: ObjectNode) -> Unit,
  ): String {
    val dataModel: JsonNode = jsonMapper.readTree(domainEventEntity.data)
    val eventDetails = dataModel["eventDetails"] as ObjectNode
    mutator(eventDetails)
    return jsonMapper.writeValueAsString(dataModel)
  }
}
