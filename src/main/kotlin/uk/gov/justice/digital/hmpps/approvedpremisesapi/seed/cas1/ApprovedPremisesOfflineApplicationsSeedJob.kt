package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.time.OffsetDateTime
import java.util.UUID

class ApprovedPremisesOfflineApplicationsSeedJob(
  fileName: String,
  private val offlineApplicationRepository: OfflineApplicationRepository,
) : SeedJob<OfflineApplicationsSeedCsvRow>(
  fileName = fileName,
  requiredHeaders = setOf(
    "crn",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

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
          eventNumber = null,
        ),
      )
    }
  }
}

data class OfflineApplicationsSeedCsvRow(
  val crn: String,
)
