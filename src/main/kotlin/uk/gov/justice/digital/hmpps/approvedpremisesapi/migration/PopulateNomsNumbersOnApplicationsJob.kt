package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository

class PopulateNomsNumbersOnApplicationsJob(
  private val applicationsRepository: ApplicationRepository,
  private val communityApiClient: CommunityApiClient
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun process() {
    applicationsRepository.findAll().forEach {
      if (it.nomsNumber != null) {
        log.info("Application ${it.id} already has nomsNumber")
        return@forEach
      }

      log.info("Updating Noms Number on Application ${it.id}")

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
        applicationsRepository.save(it)
      } catch (exception: Exception) {
        log.error("Unable to update nomsNumber on Application ${it.id}", exception)
      }

      Thread.sleep(500)
    }
  }
}
