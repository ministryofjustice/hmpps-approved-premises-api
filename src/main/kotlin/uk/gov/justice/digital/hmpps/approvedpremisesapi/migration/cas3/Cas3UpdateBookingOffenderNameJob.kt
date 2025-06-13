package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3

import jakarta.persistence.EntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas3BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

@Component
class Cas3UpdateBookingOffenderNameJob(
  private val cas3BookingRepository: Cas3BookingRepository,
  private val offenderService: OffenderService,
  private val entityManager: EntityManager,
  private val migrationLogger: MigrationLogger,
  private val bookingRepository: BookingRepository,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
  override fun process(pageSize: Int) {
    var page = 1
    var hasNext = true
    var slice: Slice<BookingEntity>
    var offendersCrn = setOf<String>()

    try {
      while (hasNext) {
        migrationLogger.info("Getting page $page for max page size $pageSize")
        slice = cas3BookingRepository.findAllTemporaryAccommodationBookings(BookingEntity::class.java, PageRequest.of(page - 1, pageSize))

        offendersCrn = slice.map { it.crn }.toSet()

        migrationLogger.info("Updating bookings offenders name with crn ${offendersCrn.map { it }}")

        val personInfos = splitAndRetrievePersonInfo(pageSize, offendersCrn)

        slice.content.forEach {
          val personInfo = personInfos[it.crn] ?: PersonSummaryInfoResult.Unknown(it.crn)
          updateBooking(personInfo, it)
        }

        entityManager.clear()
        hasNext = slice.hasNext()
        page += 1
      }
    } catch (exception: Exception) {
      migrationLogger.error("Unable to update bookings offenders name with crn ${offendersCrn.joinToString()}", exception)
    }
  }

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught", "TooGenericExceptionThrown")
  private fun updateBooking(personInfo: PersonSummaryInfoResult, it: BookingEntity) {
    try {
      val offenderName = when (personInfo) {
        is PersonSummaryInfoResult.Success.Full -> "${personInfo.summary.name.forename} ${personInfo.summary.name.surname}"
        is PersonSummaryInfoResult.NotFound, is PersonSummaryInfoResult.Unknown -> throw Exception("Offender not found for crn ${it.crn}")
        is PersonSummaryInfoResult.Success.Restricted -> throw Exception("You are not authorized")
      }

      it.offenderName = offenderName
      bookingRepository.save(it)
    } catch (exception: Exception) {
      migrationLogger.error("Unable to update offender name with crn ${it.crn} for the booking ${it.id}", exception)
    }
  }

  private fun splitAndRetrievePersonInfo(pageSize: Int, crns: Set<String>): Map<String, PersonSummaryInfoResult> = offenderService.getPersonSummaryInfoResultsInBatches(
    crns = crns,
    laoStrategy = LaoStrategy.NeverRestricted,
    batchSize = pageSize,
  ).associateBy { it.crn }
}
