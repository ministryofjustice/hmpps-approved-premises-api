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
      createDevApplications()
    }
  }

  fun createDevApplications() {
    seedLogger.info("Creating Dev Applications")

    createApplication(
      deliusUserName = "AP_USER_TEST_1",
      crn = "X320741",
      state = Cas1ApplicationSeedService.ApplicationState.PENDING_SUBMISSION,
    )

    createApplication(
      deliusUserName = "AP_USER_TEST_1",
      crn = "X698340",
      state = Cas1ApplicationSeedService.ApplicationState.WITHDRAWN_BEFORE_SUBMISSION,
    )

    createApplication(
      deliusUserName = "AP_USER_TEST_1",
      crn = "X698227",
      state = Cas1ApplicationSeedService.ApplicationState.WITHDRAWN_AFTER_SUBMISSION,
    )

    createApplication(
      deliusUserName = "AP_USER_TEST_1",
      crn = "X698317",
      state = Cas1ApplicationSeedService.ApplicationState.EXPIRED_BEFORE_SUBMISSION,
    )

    createApplication(
      deliusUserName = "AP_USER_TEST_1",
      crn = "X698338",
      state = Cas1ApplicationSeedService.ApplicationState.EXPIRED_AFTER_AUTHORISATION,
    )

    createOfflineApplicationWithBooking(deliusUserName = "AP_USER_TEST_1", crn = "X320741")
  }

  fun createApplication(
    deliusUserName: String,
    crn: String,
    state: Cas1ApplicationSeedService.ApplicationState,
  ) {
    seedLogger.info("Creating application with state $state for CRN X320741")
    try {
      cas1ApplicationSeedService.createApplication(
        deliusUserName = deliusUserName,
        crn = crn,
        state = state,
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
}
