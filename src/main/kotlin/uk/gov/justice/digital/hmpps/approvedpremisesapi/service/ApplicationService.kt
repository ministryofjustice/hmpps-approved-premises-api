package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
@Suppress(
  "LongParameterList",
  "TooManyFunctions",
  "ReturnCount",
  "ThrowsCount",
  "TooGenericExceptionThrown",
)
class ApplicationService(
  private val userRepository: UserRepository,
  private val applicationRepository: ApplicationRepository,
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val offlineApplicationRepository: OfflineApplicationRepository,
  private val userAccessService: UserAccessService,
  private val lockableApplicationRepository: LockableApplicationRepository,
) {
  fun getApplication(applicationId: UUID) = applicationRepository.findByIdOrNull(applicationId)

  fun getAllApplicationsForUsername(userEntity: UserEntity, serviceName: ServiceName): List<ApplicationSummary> = when (serviceName) {
    ServiceName.approvedPremises -> getAllApprovedPremisesApplicationsForUser(userEntity)
    ServiceName.cas2 -> throw RuntimeException("CAS2 applications now require NomisUser")
    ServiceName.cas2v2 -> throw RuntimeException("CAS2v2 applications now require Cas2v2User")
    ServiceName.temporaryAccommodation -> getAllTemporaryAccommodationApplicationsForUser(userEntity)
  }

  fun getAllApprovedPremisesApplicationsForUser(user: UserEntity) = applicationRepository.findNonWithdrawnApprovedPremisesSummariesForUser(user.id)

  private fun getAllTemporaryAccommodationApplicationsForUser(user: UserEntity): List<ApplicationSummary> = applicationRepository.findAllTemporaryAccommodationSummariesCreatedByUser(user.id)

  fun getApplicationForUsername(
    applicationId: UUID,
    userDistinguishedName: String,
  ): CasResult<ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    if (applicationEntity is TemporaryAccommodationApplicationEntity && applicationEntity.deletedAt != null) {
      return CasResult.NotFound("Application", applicationId.toString())
    }

    val userEntity = userRepository.findByDeliusUsername(userDistinguishedName)
      ?: throw RuntimeException("Could not get user")

    val canAccess = userAccessService.userCanViewApplication(userEntity, applicationEntity)

    return if (canAccess) {
      CasResult.Success(applicationEntity)
    } else {
      CasResult.Unauthorised()
    }
  }

  fun getOfflineApplicationForUsername(
    applicationId: UUID,
    deliusUsername: String,
  ): CasResult<OfflineApplicationEntity> {
    val applicationEntity = offlineApplicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    val userEntity = userRepository.findByDeliusUsername(deliusUsername)
      ?: throw RuntimeException("Could not get user")

    if (userEntity.hasPermission(UserPermission.CAS1_OFFLINE_APPLICATION_VIEW) &&
      offenderService.canAccessOffender(deliusUsername, applicationEntity.crn)
    ) {
      return CasResult.Success(applicationEntity)
    }

    return CasResult.Unauthorised()
  }

  fun updateApprovedPremisesApplicationStatus(applicationId: UUID, status: ApprovedPremisesApplicationStatus) {
    applicationRepository.updateStatus(applicationId, status)
  }

  fun updateTemporaryAccommodationApplication(
    applicationId: UUID,
    data: String,
  ): CasResult<ApplicationEntity> {
    lockableApplicationRepository.acquirePessimisticLock(applicationId)
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    if (application !is TemporaryAccommodationApplicationEntity) {
      return CasResult.GeneralValidationError("onlyCas3Supported")
    }

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return CasResult.Unauthorised()
    }

    if (application.deletedAt != null) {
      return CasResult.GeneralValidationError("This application has already been deleted")
    }

    if (application.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    application.apply {
      this.data = data
    }

    val savedApplication = applicationRepository.save(application)

    return CasResult.Success(savedApplication)
  }

  fun getArrivalDate(arrivalDate: LocalDate?): OffsetDateTime? {
    if (arrivalDate !== null) {
      return OffsetDateTime.of(arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC)
    }
    return null
  }
}
