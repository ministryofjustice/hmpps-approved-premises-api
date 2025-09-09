package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration

import jakarta.persistence.EntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime

@Component
class Cas3UpdatePremisesCreatedDateJob(
  private val premisesRepository: TemporaryAccommodationPremisesRepository,
  private val entityManager: EntityManager,
  private val log: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught", "NestedBlockDepth")
  override fun process(pageSize: Int) {
    var page = 1
    var hasNext = true
    var slice: Slice<TemporaryAccommodationPremisesEntity>
    var premisesIds = setOf<String>()

    try {
      while (hasNext) {
        log.info("Getting page $page for max page size $pageSize")
        slice = premisesRepository.findTemporaryAccommodationPremisesByCreatedAtNull(TemporaryAccommodationPremisesEntity::class.java, PageRequest.of(page - 1, pageSize))

        premisesIds = slice.map { it.id.toString() }.toSet()

        slice.content.forEach {
          if (it.createdAt == null) {
            log.info("Updating premises created_at for premises id ${it.id}")
            it.createdAt = it.startDate.toLocalDateTime()
            premisesRepository.save(it)
          }
        }

        entityManager.clear()
        hasNext = slice.hasNext()
        page += 1
      }
    } catch (exception: Exception) {
      log.error("Unable to update created_at for premises with ids ${premisesIds.joinToString()}", exception)
    }
  }
}
