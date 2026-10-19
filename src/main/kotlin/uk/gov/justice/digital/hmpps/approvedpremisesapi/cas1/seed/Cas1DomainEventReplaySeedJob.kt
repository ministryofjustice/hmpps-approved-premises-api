package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@Component
class Cas1DomainEventReplaySeedJob(
  private val domainEventService: Cas1DomainEventService,
) : SeedJob<Cas1DomainEventReplaySeedCsvRow>(
  requiredHeaders = setOf(
    "domain_event_id",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1DomainEventReplaySeedCsvRow(
    domainEventId = columns["domain_event_id"]!!.trim(),
    personReferenceCrnOverride = columns["person_reference_crn_override"]?.trim()?.takeIf { it.isNotEmpty() },
  )

  override fun processRow(row: Cas1DomainEventReplaySeedCsvRow) {
    log.info("Replaying domain event ${row.domainEventId}")
    domainEventService.replay(
      domainEventId = UUID.fromString(row.domainEventId),
      personReferenceCrnOverride = row.personReferenceCrnOverride,
    )
  }
}

data class Cas1DomainEventReplaySeedCsvRow(
  val domainEventId: String,
  val personReferenceCrnOverride: String? = null,
)
