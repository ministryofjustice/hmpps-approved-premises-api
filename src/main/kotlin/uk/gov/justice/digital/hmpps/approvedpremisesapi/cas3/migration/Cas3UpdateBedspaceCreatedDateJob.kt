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

@Component
class Cas3UpdateBedspaceCreatedDateJob(
  private val bedRepository: BedRepository,
  private val entityManager: EntityManager,
  private val log: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught", "NestedBlockDepth")
  override fun process(pageSize: Int) {
    var page = 1
    var hasNext = true
    var slice: Slice<BedEntity>
    var bedIds = setOf<String>()

    try {
      while (hasNext) {
        log.info("Getting next page")
        slice = bedRepository.findAllCas3Bedspaces(temporaryAccommodation.value, BedEntity::class.java, PageRequest.of(page - 1, pageSize))

        bedIds = slice.map { it.id.toString() }.toSet()

        slice.content.forEach {
          log.info("Updating bed created date for bed id ${it.id}")
          it.createdDate = it.startDate
          bedRepository.save(it)
        }

        entityManager.clear()
        hasNext = slice.hasNext()
        page += 1
      }
    } catch (exception: Exception) {
      log.error("Unable to update created date for beds with ids ${bedIds.joinToString()}", exception)
    }
  }
}
