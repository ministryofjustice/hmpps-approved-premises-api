package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import java.time.OffsetDateTime
import java.util.UUID

class ApprovedPremisesOfflineApplicationsSeedJob(
  fileName: String,
  private val offlineApplicationRepository: OfflineApplicationRepository,
) : SeedJob<OfflineApplicationsSeedCsvRow>(
  fileName = fileName,
  requiredColumns = 12,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun verifyPresenceOfRequiredHeaders(headers: Set<String>) {
    val missingHeaders = requiredHeaders() - headers

    if (missingHeaders.any()) {
      throw RuntimeException("required headers: $missingHeaders")
    }
  }

  override fun deserializeRow(columns: Map<String, String>) = OfflineApplicationsSeedCsvRow(
    crn = columns["crn"]!!,
  )

  override fun processRow(row: OfflineApplicationsSeedCsvRow) {
    val existingOfflineApplications = offlineApplicationRepository.findAllByServiceAndCrn(ServiceName.approvedPremises.value, row.crn)

    if (existingOfflineApplications.none()) {
      offlineApplicationRepository.save(
        OfflineApplicationEntity(
          id = UUID.randomUUID(),
          crn = row.crn,
          service = ServiceName.approvedPremises.value,
          createdAt = OffsetDateTime.now(),
        ),
      )
    }
  }
}

private fun requiredHeaders(): Set<String> {
  return setOf(
    "crn",
  )
}

data class OfflineApplicationsSeedCsvRow(
  val crn: String,
)
