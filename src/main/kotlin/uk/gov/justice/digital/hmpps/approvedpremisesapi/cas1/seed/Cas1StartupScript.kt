package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("MagicNumber", "MaxLineLength", "TooGenericExceptionCaught")
@Component
class Cas1StartupScript(
  private val seedLogger: SeedLogger,
  private val environmentService: EnvironmentService,
  private val cas1ApplicationSeedService: Cas1ApplicationSeedService,
  private val userRepository: UserRepository,
  private val userRoleAssignmentRepository: UserRoleAssignmentRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val apAreaRepository: ApAreaRepository,
  private val cruManagementAreaRepository: Cas1CruManagementAreaRepository,
) {

  @Transactional
  fun script() {
    seedLogger.info("Running Startup Script for CAS1")

    if (environmentService.isDev()) {
      createDevApplications()
      createTestKeyWorkerUsers()
    }
  }

  /**
   * This is a temporary fix to allow key worker E2E tests to continue
   * working whilst we migrate over to using the users table for keyworker
   * listing. It adds key workers listed by delius to our users table
   * for the test premises SWSC Test Premises 1
   */
  fun createTestKeyWorkerUsers() {
    fun addTestKeyWorker(userName: String, staffCode: String, name: String) {
      if (userRepository.findByDeliusStaffCode(staffCode) == null) {
        val swscDeliusKeyWorker1 = userRepository.save(
          UserEntity(
            id = UUID.randomUUID(),
            name = name,
            deliusUsername = userName,
            deliusStaffCode = staffCode,
            email = null,
            telephoneNumber = null,
            isActive = true,
            applications = mutableListOf(),
            roles = mutableListOf(),
            qualifications = mutableListOf(),
            probationRegion = probationRegionRepository.findAll().first(),
            probationDeliveryUnit = null,
            apArea = apAreaRepository.findAll().first(),
            cruManagementArea = cruManagementAreaRepository.findAll().first(),
            cruManagementAreaOverride = null,
            teamCodes = emptyList(),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now(),
          ),
        )

        userRoleAssignmentRepository.save(
          UserRoleAssignmentEntity(
            id = UUID.randomUUID(),
            user = swscDeliusKeyWorker1,
            role = UserRole.CAS1_FUTURE_MANAGER,
          ),
        )
      }
    }

    addTestKeyWorker(userName = "deliusKw1", staffCode = "N07B481", name = "R T (Delius Key Worker)")
    addTestKeyWorker(userName = "deliusKw2", staffCode = "N07B477", name = "M R (Delius Key Worker)")
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
