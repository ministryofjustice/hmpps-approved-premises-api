package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger

@Component
class Cas3UpdateBedspaceStartDateJob(
  private val bedspaceRepository: BedRepository,
  private val bookingRepository: BookingRepository,
  private val migrationLogger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = true

  @SuppressWarnings("TooGenericExceptionCaught")
  override fun process(pageSize: Int) {
    val bedspaces = bedspaceRepository.findCas3BedspacesWithStartDateAfterBookingArrivalDate()

    val bedspacesIds = bedspaces.map { it.id }.toSet()

    try {
      migrationLogger.info("Updating CAS3 bedspaces start date for bedspaces Ids ${bedspacesIds.map { it }}")

      bedspaces.forEach { bedspace ->
        val booking = bookingRepository.findFirstBookingByBedId(bedspace.id)
        if (booking != null && booking.arrivalDate.isBefore(bedspace.startDate)) {
          val updatedBedspace = bedspace.copy(startDate = booking.arrivalDate)
          bedspaceRepository.save(updatedBedspace)
          migrationLogger.info("Updated bedspace ${bedspace.id} start date to ${booking.arrivalDate}")
        }
      }

      migrationLogger.info("Updating CAS3 bedspaces start date for bedspaces Ids ${bedspacesIds.map { it }} is completed")
    } catch (exception: Exception) {
      migrationLogger.error("Unable to CAS3 bedspaces start date for bedspaces Ids ${bedspacesIds.joinToString()}", exception)
    }
  }
}
