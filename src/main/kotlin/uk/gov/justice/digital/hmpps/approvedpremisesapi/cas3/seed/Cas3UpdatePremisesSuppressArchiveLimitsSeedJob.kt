package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.seed

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedJob
import java.util.UUID

@Component
class Cas3UpdatePremisesSuppressArchiveLimitsSeedJob(
  private val cas3PremisesRepository: Cas3PremisesRepository,
) : SeedJob<Cas3UpdateSuppressArchiveLimitsRow>(
  requiredHeaders = setOf("cas3PremisesId", "suppressArchiveLimits"),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas3UpdateSuppressArchiveLimitsRow(
    cas3PremisesId = UUID.fromString(columns["cas3PremisesId"]),
    suppressArchiveLimits = columns["suppressArchiveLimits"]!!.trim().toBooleanStrict(),
  )

  override fun processRow(row: Cas3UpdateSuppressArchiveLimitsRow) {
    val cas3PremisesId = row.cas3PremisesId

    cas3PremisesRepository.findByIdOrNull(cas3PremisesId)?.let {
      log.info(
        "Updating suppressArchiveLimits from {} to {} for CAS3 Premises with id: {}",
        it.suppressArchiveLimits,
        row.suppressArchiveLimits,
        cas3PremisesId,
      )
      it.suppressArchiveLimits = row.suppressArchiveLimits
      cas3PremisesRepository.save(it)
    } ?: log.warn("Cas3Premises with id: {} not found", cas3PremisesId)
  }
}

data class Cas3UpdateSuppressArchiveLimitsRow(
  val cas3PremisesId: UUID,
  val suppressArchiveLimits: Boolean,
)
