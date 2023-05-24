package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository

class PopulateNomsNumbersOnBookingsJob(
  private val bookingRepository: BookingRepository,
  private val communityApiClient: CommunityApiClient
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun process() {
    bookingRepository.findAll().forEach {
      if (it.nomsNumber != null) {
        log.info("Booking ${it.id} already has nomsNumber")
        return@forEach
      }

      log.info("Updating Noms Number on Booking ${it.id}")

      try {
        val offenderDetailsResult = communityApiClient.getOffenderDetailSummary(it.crn)
        if (offenderDetailsResult is ClientResult.Failure) {
          offenderDetailsResult.throwException()
        }

        val offenderDetails = (offenderDetailsResult as ClientResult.Success).body

        if (offenderDetails.otherIds.nomsNumber != null) {
          log.error("No nomsNumber present for ${it.crn}")
        }

        it.nomsNumber = offenderDetails.otherIds.nomsNumber
        bookingRepository.save(it)
      } catch (exception: Exception) {
        log.error("Unable to update nomsNumber on Booking ${it.id}", exception)
      }

      Thread.sleep(500)
    }
  }
}
