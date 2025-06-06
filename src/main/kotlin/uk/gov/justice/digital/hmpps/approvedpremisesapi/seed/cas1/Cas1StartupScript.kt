package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService

@SuppressWarnings("MagicNumber", "MaxLineLength", "TooGenericExceptionCaught")
@Component
class Cas1StartupScript(
  private val seedLogger: SeedLogger,
  private val environmentService: EnvironmentService,
  private val cas1ApplicationSeedService: Cas1ApplicationSeedService,
) {

  @Transactional
  fun script() {
    seedLogger.info("Running Startup Script for CAS1")

    if (environmentService.isDev()) {
      scriptDev()
    }
  }

  fun createApplicationPendingSubmission(
    deliusUserName: String,
    crn: String,
  ) {
    seedLogger.info("Auto-scripting application for CRN $crn")
    try {
      cas1ApplicationSeedService.createApplication(
        deliusUserName = deliusUserName,
        crn = crn,
        state = Cas1ApplicationSeedService.ApplicationState.PENDING_SUBMISSION,
      )
    } catch (e: Exception) {
      seedLogger.error("Creating application with crn $crn failed", e)
    }
  }

  fun createOfflineApplicationWithBooking(deliusUserName: String, crn: String) {
    if (environmentService.isNotATestEnvironment()) {
      error("Cannot create test applications as not in a test environment")
    }

    seedLogger.info("Auto-scripting offline for CRN $crn")
    try {
      cas1ApplicationSeedService.createOfflineApplicationWithBooking(deliusUserName, crn)
    } catch (e: Exception) {
      seedLogger.error("Creating offline application with crn $crn failed", e)
    }
  }

  fun scriptDev() {
    seedLogger.info("Running Startup Script for CAS1 dev")

    createApplicationPendingSubmission(
      deliusUserName = "AP_USER_TEST_1",
      crn = "X320741",
    )
    createOfflineApplicationWithBooking(deliusUserName = "AP_USER_TEST_1", crn = "X320741")
  }
}
