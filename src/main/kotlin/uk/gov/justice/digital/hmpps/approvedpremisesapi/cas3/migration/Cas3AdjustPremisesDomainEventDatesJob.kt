package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationInBatchesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Component
class Cas3AdjustPremisesDomainEventDatesJob(
  private val domainEventRepository: DomainEventRepository,
  private val bedRepository: BedRepository,
  private val migrationLogger: MigrationLogger,
  private val objectMapper: ObjectMapper,
  transactionTemplate: TransactionTemplate,
) : MigrationInBatchesJob(migrationLogger, transactionTemplate) {
  override val shouldRunInTransaction = false

  private val createdAtCutoff: OffsetDateTime = OffsetDateTime.parse("2025-10-15T00:00:00Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  private val dateToUpdate = LocalDate.parse("2025-10-15")
  private val dateToSet = LocalDate.parse("2025-10-13")

  @Suppress("MagicNumber")
  private val twoDaysInSeconds = 172800L

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
  override fun process(pageSize: Int) {
    val bedIds: List<UUID> = listOf(
      UUID.fromString("6d481c4d-e1b1-40a5-84bf-b194851a377f"),
      UUID.fromString("37262519-4ef0-4381-902d-e6fc07e0644d"),
      UUID.fromString("6f4ed5ba-cf99-4b72-b607-6a8e57d6dbeb"),
    )

    bedRepository.findAllById(bedIds).forEach { bed ->
      migrationLogger.info("Updating bedId startDate ${bed.id}")
      bedRepository.save(bed.copy(startDate = dateToSet))
    }

    val domainEventIds: List<UUID> = listOf(
      UUID.fromString("69f21552-fc3d-44cc-a8d1-0480ee9f80b9"),
      UUID.fromString("abc9660e-dac3-4b5d-8bc8-7cd3fc2b5bdc"),
      UUID.fromString("753df5e1-750f-44b6-971f-34caacd75156"),
      UUID.fromString("36375c5f-1c0f-4c49-a8f9-0a645ff432ec"),
      UUID.fromString("5851b0eb-2661-45a9-bf79-7a0d234eb1d8"),
      UUID.fromString("feb281a4-885b-49e8-9c35-71cb579922fb"),
      UUID.fromString("833395e5-b691-4b1b-9ce3-00de3ac5437c"),
      UUID.fromString("d6462c6e-ac85-4c3a-84f3-7ec6728636fc"),
      UUID.fromString("aba460a6-86ac-4b7b-a502-5dd46edae6fc"),
      UUID.fromString("f515a0c1-15d7-49b2-bcba-9c11807819a9"),
      UUID.fromString("63aee153-341c-43fd-8d21-e627c23fca9e"),
      UUID.fromString("8b58f102-b668-4586-a0f7-c53a15fbbdc4"),
      UUID.fromString("bc2a8aa0-dd44-4e32-bd3a-6cfa1ec178bc"),
      UUID.fromString("404e2282-7066-4e49-bccf-8468935d0fc0"),
      UUID.fromString("9c8a3bed-0fb6-4aa7-81c9-3ede221763fe"),
      UUID.fromString("ee977363-22e1-418e-aaf6-756cb9771cb2"),
      UUID.fromString("e0bb900d-3d00-4e53-87df-03608e8ca273"),
      UUID.fromString("b4ba462a-9fd6-4808-b279-f0443e1a2fb4"),
    )

    migrationLogger.info("Starting CAS3 domain event timestamp adjustment for premises=$domainEventIds, cutoff=$createdAtCutoff")

    val events = domainEventRepository.findByCas3PremisesIdAndCreatedAtAfter(domainEventIds, createdAtCutoff)

    if (events.isEmpty()) {
      migrationLogger.info("No domain events found for premises=$domainEventIds at/after $createdAtCutoff")
      return
    }

    migrationLogger.info("Found ${events.size} domain events to update")

    events.forEach { event ->
      val newOccurredAt = event.occurredAt.minusDays(2)
      val newCreatedAt = event.createdAt.minusDays(2)

      val updatedData = try {
        val node = objectMapper.readTree(event.data)
        updateEventJsonDates(node)
        objectMapper.writeValueAsString(node)
      } catch (e: Exception) {
        migrationLogger.error("Failed to update JSON data for domainEventId=${event.id}", e)
        event.data
      }

      val updatedDomainEvent = event.copy(occurredAt = newOccurredAt, createdAt = newCreatedAt, data = updatedData)
      domainEventRepository.save(updatedDomainEvent)

      migrationLogger.info("Updated domainEventId=${event.id} occurredAt=${event.occurredAt} -> $newOccurredAt, createdAt=${event.createdAt} -> $newCreatedAt")
    }

    migrationLogger.info("Finished CAS3 domain event timestamp adjustment for premises=$domainEventIds")
  }

  @Suppress("NestedBlockDepth")
  private fun updateEventJsonDates(root: JsonNode) {
    if (root is ObjectNode) {
      root.get("timestamp")?.let { tsNode ->
        parseInstant(tsNode)?.let { inst ->
          root.put("timestamp", inst.minusSeconds(twoDaysInSeconds).toString())
        }
      }

      val eventDetails = root.get("eventDetails")
      if (eventDetails is ObjectNode) {
        listOf("newStartDate", "currentEndDate", "currentStartDate", "endDate").forEach { field ->
          eventDetails.get(field)?.let { dateNode ->
            parseLocalDateString(dateNode)?.let { d ->
              if (d.isEqual(dateToUpdate)) {
                eventDetails.put(field, d.minusDays(2).toString())
              }
            }
          }
        }
      }
    }
  }

  private fun parseInstant(node: JsonNode): Instant? = try {
    when {
      node.isTextual -> Instant.parse(node.asText())
      else -> null
    }
  } catch (_: Exception) {
    migrationLogger.error("unable to parse instant from node: $node")
    null
  }

  private fun parseLocalDateString(node: JsonNode): LocalDate? = try {
    when {
      node.isTextual -> LocalDate.parse(node.asText())
      else -> null
    }
  } catch (_: Exception) {
    migrationLogger.error("unable to parse local date from node: $node")
    null
  }
}
