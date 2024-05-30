package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonRepository
import java.time.OffsetDateTime

class Cas1OutOfServiceBedReasonMigrationJob(
  private val lostBedReasonRepository: LostBedReasonRepository,
  private val cas1OutOfServiceBedReasonRepository: Cas1OutOfServiceBedReasonRepository,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  override fun process() {
    lostBedReasonRepository
      .findAllByServiceScope(ServiceName.approvedPremises.value)
      .map {
        Cas1OutOfServiceBedReasonEntity(
          id = it.id,
          createdAt = OffsetDateTime.now(),
          name = it.name,
          isActive = it.isActive,
        )
      }
      .apply(cas1OutOfServiceBedReasonRepository::saveAll)
  }
}
