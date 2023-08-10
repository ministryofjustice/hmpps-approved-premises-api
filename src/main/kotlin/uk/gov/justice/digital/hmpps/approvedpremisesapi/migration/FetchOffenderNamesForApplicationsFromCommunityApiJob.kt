package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

class FetchOffenderNamesForApplicationsFromCommunityApiJob(
  private val applicationRepository: ApplicationRepository,
  private val offenderService: OffenderService,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = false

  override fun process() {
    applicationRepository.findAllForService(ApprovedPremisesApplicationEntity::class.java)
      .map { it as ApprovedPremisesApplicationEntity }
      .forEach { application ->
        if (application.name != null) return@forEach

        log.info("Fetching Offender name for Application: ${application.id}")

        try {
          val offenderDetailsResult = offenderService.getOffenderByCrn(application.crn, "", true)

          val offenderDetails = when (offenderDetailsResult) {
            is AuthorisableActionResult.Success -> offenderDetailsResult.entity
            is AuthorisableActionResult.NotFound -> throw RuntimeException("Could not find Offender")
            is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unauthorised when trying to find Offender")
          }

          application.name = "${offenderDetails.firstName.uppercase()} ${offenderDetails.surname.uppercase()}"
          applicationRepository.save(application)
        } catch (exception: Exception) {
          log.error("Unable to update Offender name for Application: ${application.id}", exception)
        }

        Thread.sleep(500)
      }
  }
}
