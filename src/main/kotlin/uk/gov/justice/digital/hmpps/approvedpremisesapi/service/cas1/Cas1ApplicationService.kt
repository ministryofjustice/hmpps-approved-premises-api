package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import jakarta.transaction.Transactional
import org.springframework.data.domain.Limit
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.util.UUID

@SuppressWarnings("TooGenericExceptionThrown")
@Service
class Cas1ApplicationService(
  private val approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository,
  private val applicationRepository: ApplicationRepository,
  private val offlineApplicationRepository: OfflineApplicationRepository,
  private val userRepository: UserRepository,
  private val cas1ApplicationStatusService: Cas1ApplicationStatusService,
  private val cas1ApplicationDomainEventService: Cas1ApplicationDomainEventService,
  private val cas1ApplicationEmailService: Cas1ApplicationEmailService,
  private val cas1AssessmentService: Cas1AssessmentService,
  private val cas1UserAccessService: Cas1UserAccessService,
  private val userAccessService: UserAccessService,
  private val offenderService: OffenderService,
) {
  fun getApplication(applicationId: UUID) = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)

  fun getApplicationForUsername(
    applicationId: UUID,
    userDistinguishedName: String,
  ): CasResult<ApprovedPremisesApplicationEntity> {
    val applicationEntity = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())

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

  fun getSubmittedApplicationsForCrn(crn: String, limit: Int) = approvedPremisesApplicationRepository.findByCrnAndSubmittedAtIsNotNull(crn, Limit.of(limit))

  fun getOfflineApplicationsForCrn(crn: String, limit: Int) = offlineApplicationRepository.findAllByCrn(crn, Limit.of(limit))

  fun getAllApprovedPremisesApplications(
    page: Int?,
    crnOrName: String?,
    sortDirection: SortDirection?,
    status: List<ApprovedPremisesApplicationStatus>,
    sortBy: ApplicationSortField?,
    apAreaId: UUID?,
    releaseType: String?,
    pageSize: Int? = 10,
    createdByUserId: UUID? = null,
  ): Pair<List<ApprovedPremisesApplicationSummary>, PaginationMetadata?> {
    val sortField = when (sortBy) {
      ApplicationSortField.arrivalDate -> "arrivalDate"
      ApplicationSortField.createdAt -> "a.created_at"
      ApplicationSortField.tier -> "tier"
      ApplicationSortField.releaseType -> "releaseType"
      else -> "a.created_at"
    }
    val pageable = getPageableOrAllPages(sortField, sortDirection, page, pageSize)

    val statusNames = status.map { it.name }

    val response = applicationRepository.findAllApprovedPremisesSummaries(
      pageable = pageable,
      crnOrName = crnOrName,
      statusProvided = statusNames.isNotEmpty(),
      status = statusNames,
      apAreaId = apAreaId,
      releaseType,
      createdByUserId,
    )

    return Pair(response.content, getMetadata(response, page, pageSize))
  }

  /**
   * This function should not be called directly. Instead, use [Cas1WithdrawableService.withdrawApplication] that
   * will indirectly invoke this function. It will also ensure that:
   *
   * 1. The entity is withdrawable, and error if not
   * 2. The user is allowed to withdraw it, and error if not
   * 3. If withdrawn, all descdents entities are withdrawn, where applicable
   */
  @Transactional
  fun withdrawApprovedPremisesApplication(
    applicationId: UUID,
    user: UserEntity,
    withdrawalReason: String,
    otherReason: String?,
  ): CasResult<Unit> {
    val application = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound(entityType = "application", applicationId.toString())

    if (application.isWithdrawn) {
      return CasResult.Success(Unit)
    }

    val updatedApplication = application.apply {
      this.isWithdrawn = true
      this.withdrawalReason = withdrawalReason
      this.otherWithdrawalReason = if (withdrawalReason == WithdrawalReason.other.value) {
        otherReason
      } else {
        null
      }
    }

    approvedPremisesApplicationRepository.save(updatedApplication)

    cas1ApplicationStatusService.applicationWithdrawn(updatedApplication)
    cas1ApplicationDomainEventService.applicationWithdrawn(updatedApplication, withdrawingUser = user)
    cas1ApplicationEmailService.applicationWithdrawn(updatedApplication, user)

    updatedApplication.assessments.map {
      cas1AssessmentService.updateAssessmentWithdrawn(it.id, user)
    }

    return CasResult.Success(Unit)
  }

  @Transactional
  fun expireApprovedPremisesApplication(applicationId: UUID, user: UserEntity, expiredReason: String): CasResult<Unit> {
    val application = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound(entityType = "application", applicationId.toString())

    if (application.status == ApprovedPremisesApplicationStatus.EXPIRED) {
      return CasResult.Success(Unit)
    }

    val updatedApplication = application.apply {
      this.status = ApprovedPremisesApplicationStatus.EXPIRED
      this.expiredReason = expiredReason
    }

    approvedPremisesApplicationRepository.save(updatedApplication)

    cas1ApplicationDomainEventService.applicationExpiredManually(application, user, expiredReason)

    return CasResult.Success(Unit)
  }

  fun getWithdrawableState(application: ApprovedPremisesApplicationEntity, user: UserEntity): WithdrawableState = WithdrawableState(
    withdrawable = !application.isWithdrawn,
    withdrawn = application.isWithdrawn,
    userMayDirectlyWithdraw = cas1UserAccessService.userMayWithdrawApplication(user, application),
  )
}
