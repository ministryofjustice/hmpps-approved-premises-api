package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3

import jakarta.persistence.EntityManager
import org.springframework.data.domain.Limit
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.temporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger

@Component
class Cas3UpdateBedSpaceStartDateJob(
  private val entityManager: EntityManager,
  private val migrationLogger: MigrationLogger,
  private val bookingRepository: BookingRepository,
  private val bedRepository: BedRepository,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught", "NestedBlockDepth")
  override fun process(pageSize: Int) {
    var page = 0
    var hasNext = true
    var slice: Slice<BedEntity>
    var currentSliceBeds = setOf<String>()

    try {
      while (hasNext) {
        migrationLogger.info("Getting page $page for max page size $pageSize")
        slice = bedRepository.findAllByService(temporaryAccommodation.value, PageRequest.of(page, pageSize))

        currentSliceBeds = slice.map { it.id.toString() }.toSet()

        for (bed in slice.content) {
          if (bed.startDate == null) {
            migrationLogger.info("Updating bed start_date for bed id ${bed.id}")
            if (bed.createdAt != null) {
              migrationLogger.info("Using created_date as start_date for bed id ${bed.id}")
              val updatedBed = bed.copy(startDate = bed.createdAt!!.toLocalDate()!!)
              bedRepository.save(updatedBed)
            } else {
              migrationLogger.info("Using oldest booking arrival_date as start_date for bed id ${bed.id}")
              val oldestBooking = bookingRepository.findByBedId(bed.id, Sort.by(ASC, "arrivalDate"), Limit.of(1))
              if (oldestBooking.isEmpty()) {
                migrationLogger.info("No bookings found for bed id ${bed.id}")
              } else {
                val updatedBed = bed.copy(startDate = oldestBooking[0].arrivalDate)
                bedRepository.save(updatedBed)
                migrationLogger.info("Updated start_date for bed id ${bed.id} from booking arrival_date")
              }
            }
          }
        }
        entityManager.clear()
        hasNext = slice.hasNext()
        page++
      }
    } catch (exception: Exception) {
      migrationLogger.error("Unable to update bed(s) start_date with id ${currentSliceBeds.joinToString()}", exception)
    }
  }
}
