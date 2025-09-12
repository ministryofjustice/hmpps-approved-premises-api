package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration

import jakarta.persistence.EntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger

@Component
class Cas3UpdateArchivedPremisesEndDateJob(
  private val entityManager: EntityManager,
  private val migrationLogger: MigrationLogger,
  private val premisesRepository: PremisesRepository,
  private val bedRepository: BedRepository,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught", "NestedBlockDepth")
  override fun process(pageSize: Int) {
    var hasNext = true
    var slice: Slice<TemporaryAccommodationPremisesEntity>
    var currentSlice = setOf<String>()

    try {
      while (hasNext) {
        migrationLogger.info("Getting page for max page size $pageSize")
        slice = premisesRepository.findCas3ArchivedPremisesWithoutEndDate(PageRequest.of(0, pageSize))

        currentSlice = slice.map { it.id.toString() }.toSet()

        for (premises in slice.content) {
          if (premises.endDate == null) {
            migrationLogger.info("Updating archived premises end_date for premises id ${premises.id}")
            val lastBed = bedRepository.findByRoomPremisesId(premises.id).sortedByDescending { it.endDate }.firstOrNull()
            if (lastBed != null) {
              premises.endDate = lastBed.endDate
              premisesRepository.save(premises)
              migrationLogger.info("Updated end_date for premises id ${premises.id} from bedspace end_date ${lastBed.endDate}")
            } else {
              migrationLogger.info("Unable to find bedspace with end_date for premises id ${premises.id}")
            }
          } else {
            migrationLogger.info("Premises end_date already set for premises id ${premises.id}")
          }
        }
        entityManager.clear()
        hasNext = slice.hasNext()
      }
    } catch (exception: Exception) {
      migrationLogger.error("Unable to update archived premises end_date with ids ${currentSlice.joinToString()}", exception)
    }
  }
}
