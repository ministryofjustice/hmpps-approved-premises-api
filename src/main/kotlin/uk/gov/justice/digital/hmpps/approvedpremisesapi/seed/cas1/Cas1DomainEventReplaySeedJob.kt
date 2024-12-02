package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import java.util.UUID

class Cas1DomainEventReplaySeedJob(
  private val domainEventService: DomainEventService,
) : SeedJob<Cas1DomainEventReplaySeedCsvRow>(
  requiredHeaders = setOf(
    "domain_event_id",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1DomainEventReplaySeedCsvRow(
    domainEventId = columns["domain_event_id"]!!.trim(),
  )

  override fun processRow(row: Cas1DomainEventReplaySeedCsvRow) {
    log.info("Replaying domain event ${row.domainEventId}")
    domainEventService.replay(UUID.fromString(row.domainEventId))
  }
}

data class Cas1DomainEventReplaySeedCsvRow(
  val domainEventId: String,
)
