package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration

import jakarta.persistence.EntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.temporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime

@Component
class Cas3UpdateBedspaceCreatedDateJob(
  private val bedRepository: BedRepository,
  private val entityManager: EntityManager,
  private val log: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught", "NestedBlockDepth")
  override fun process(pageSize: Int) {
    var hasNext = true
    var slice: Slice<BedEntity>
    var bedIds = setOf<String>()

    try {
      while (hasNext) {
        log.info("Getting next page")
        slice = bedRepository.findBedByCreatedAtNull(temporaryAccommodation.value, BedEntity::class.java, PageRequest.of(0, pageSize))

        bedIds = slice.map { it.id.toString() }.toSet()

        slice.content.forEach {
          if (it.createdAt == null && it.startDate != null) {
            log.info("Updating bed created_at for bed id ${it.id}")
            it.createdAt = it.startDate!!.toLocalDateTime()
            bedRepository.save(it)
          }
        }

        entityManager.clear()
        hasNext = slice.hasNext()
      }
    } catch (exception: Exception) {
      log.error("Unable to update created_at for beds with ids ${bedIds.joinToString()}", exception)
    }
  }
}
