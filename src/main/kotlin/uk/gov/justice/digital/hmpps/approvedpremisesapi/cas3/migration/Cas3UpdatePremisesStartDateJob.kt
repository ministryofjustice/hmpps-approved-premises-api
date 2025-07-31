package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration

import jakarta.persistence.EntityManager
import org.springframework.data.domain.Limit
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.temporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger

@Component
class Cas3UpdatePremisesStartDateJob(
  private val entityManager: EntityManager,
  private val migrationLogger: MigrationLogger,
  private val premisesRepository: PremisesRepository,
  private val bookingRepository: BookingRepository,
  private val roomRepository: RoomRepository,
  private val transactionTemplate: TransactionTemplate,
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

        for (premises in slice.content) {
          if (premises.startDate == null) {
            migrationLogger.info("Updating premises start_date for premises id ${premises.id}")
            if (premises.createdAt != null) {
              premises.startDate = premises.createdAt!!.toLocalDate()
              premisesRepository.save(premises)
              migrationLogger.info("Updated start_date with created_date for premises id ${premises.id}")
            } else {
              val oldestBooking = bookingRepository.findByPremisesId(premises.id, Sort.by(ASC, "arrivalDate"), Limit.of(1)).firstOrNull()
              if (oldestBooking == null) {
                transactionTemplate.executeWithoutResult {
                  val oldestBedspaceInPremises =
                    roomRepository.findAllByPremisesId(premises.id).flatMap { it.beds }.sortedBy { it.startDate }
                      .firstOrNull()
                  if (oldestBedspaceInPremises != null) {
                    premises.startDate = oldestBedspaceInPremises.startDate!!
                    premisesRepository.save(premises)
                    migrationLogger.info("Updated start_date using oldest bedspace start_date for premises id ${premises.id}")
                  } else {
                    migrationLogger.info("No bedspace found for premises id ${premises.id}")
                  }
                }
              } else {
                premises.startDate = oldestBooking.arrivalDate
                premisesRepository.save(premises)
                migrationLogger.info("Updated start_date for premises id ${premises.id} from booking arrival_date ${oldestBooking?.arrivalDate}")
              }
            }
          } else {
            migrationLogger.info("Premises start_date already set for premises id ${premises.id}")
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
