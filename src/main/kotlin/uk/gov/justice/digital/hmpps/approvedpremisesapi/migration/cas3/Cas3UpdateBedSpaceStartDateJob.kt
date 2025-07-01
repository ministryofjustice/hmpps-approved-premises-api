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
        migrationLogger.info("Updating bed(s) start_date with id(s) ${currentSliceBeds.joinToString()}")

        for (bed in slice.content) {
          if (bed.startDate == null) {
            migrationLogger.info("Updating bed start_date for bed id ${bed.id}")
            if (bed.createdAt != null) {
              migrationLogger.info("Using created_date as start_date for bed id ${bed.id}")
              val updatedBed = bed.copy(startDate = bed.createdAt!!.toLocalDate()!!)
              bedRepository.save(updatedBed)
            } else {
              val oldestBooking = bookingRepository.findByBedId(bed.id, Sort.by(ASC, "arrivalDate"), Limit.of(1))
              if (oldestBooking.isEmpty()) {
                if (bed.endDate != null) {
                  val updatedBed = bed.copy(startDate = bed.endDate)
                  bedRepository.save(updatedBed)
                  migrationLogger.info("Using oldest booking arrival_date as start_date for bed id ${bed.id}")
                } else {
                  migrationLogger.info("No bookings found for bed ID ${bed.id}, and the bedspace doesn't have a start or end date.")
                }
              } else {
                val updatedBed = bed.copy(startDate = oldestBooking[0].arrivalDate)
                bedRepository.save(updatedBed)
                migrationLogger.info("Updated start_date for bed id ${bed.id} from booking arrival_date")
              }
            }
          } else {
            migrationLogger.info("Bed start_date already set for bed id ${bed.id}")
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
