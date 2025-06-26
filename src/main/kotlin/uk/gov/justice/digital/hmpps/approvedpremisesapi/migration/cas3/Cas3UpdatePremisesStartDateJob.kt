package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3

import jakarta.persistence.EntityManager
import org.springframework.data.domain.Limit
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.temporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger

@Component
class Cas3UpdatePremisesStartDateJob(
  private val entityManager: EntityManager,
  private val migrationLogger: MigrationLogger,
  private val premisesRepository: PremisesRepository,
  private val bookingRepository: BookingRepository,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught", "NestedBlockDepth")
  override fun process(pageSize: Int) {
    var page = 0
    var hasNext = true
    var slice: Slice<TemporaryAccommodationPremisesEntity>
    var currentSlice = setOf<String>()

    try {
      while (hasNext) {
        migrationLogger.info("Getting page $page for max page size $pageSize")
        slice = premisesRepository.findTemporaryAccommodationPremisesByService(temporaryAccommodation.value, PageRequest.of(page, pageSize))

        currentSlice = slice.map { it.id.toString() }.toSet()

        for (tap in slice.content) {
          if (tap.startDate == null) {
            migrationLogger.info("Updating premises start_date for premises id ${tap.id}")
            if (tap.createdAt != null) {
              migrationLogger.info("Using created_date as start_date for premises id ${tap.id}")
              tap.startDate = tap.createdAt!!.toLocalDate()
              premisesRepository.save(tap)
            } else {
              migrationLogger.info("Using oldest booking arrival_date as start_date for premises id ${tap.id}")
              val oldestBooking = bookingRepository.findByPremisesId(tap.id, Sort.by(ASC, "arrivalDate"), Limit.of(1)).firstOrNull()
              if (oldestBooking == null) {
                migrationLogger.info("No bookings found for premises id ${tap.id}")
              } else {
                tap.startDate = oldestBooking.arrivalDate
                premisesRepository.save(tap)
                migrationLogger.info("Updated start_date for premises id ${tap.id} from booking arrival_date ${oldestBooking?.arrivalDate}")
              }
            }
          } else {
            migrationLogger.info("Premises start_date already set for premises id ${tap.id}")
          }
        }
        entityManager.clear()
        hasNext = slice.hasNext()
        page++
      }
    } catch (exception: Exception) {
      migrationLogger.error("Unable to update premises start_date with id ${currentSlice.joinToString()}", exception)
    }
  }
}
