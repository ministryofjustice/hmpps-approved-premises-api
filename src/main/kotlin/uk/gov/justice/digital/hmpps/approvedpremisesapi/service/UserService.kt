package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.apache.commons.collections4.CollectionUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserRolesAndQualifications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.specification.hasQualificationsAndRoles
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasSimpleResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApAreaMappingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.transformQualifications
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as APIUserQualification

@Service
class UserService(
  @Value("\${assign-default-region-to-users-with-unknown-region}") private val assignDefaultRegionToUsersWithUnknownRegion: Boolean,
  private val requestContextService: RequestContextService,
  private val httpAuthService: HttpAuthService,
  private val offenderService: OffenderService,
  private val communityApiClient: CommunityApiClient,
  private val userRepository: UserRepository,
  private val userRoleAssignmentRepository: UserRoleAssignmentRepository,
  private val userQualificationAssignmentRepository: UserQualificationAssignmentRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val probationAreaProbationRegionMappingRepository: ProbationAreaProbationRegionMappingRepository,
  private val cas1ApAreaMappingService: Cas1ApAreaMappingService,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val featureFlagService: FeatureFlagService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getUsersByPartialName(name: String): List<UserEntity> {
    return userRepository.findByNameContainingIgnoreCase(name)
  }

  fun getDeliusUserNameForRequest() = httpAuthService.getDeliusPrincipalOrThrow().name

  fun getUserForRequest(): UserEntity {
    val username = getDeliusUserNameForRequest()
    val user = getExistingUserOrCreate(username)
    ensureCas3UserHasCas3ReferrerRole(user)

    return user
  }

  fun getUserForProfile(username: String): GetUserResponse {
    val userResponse = getExistingUserOrCreate(username, throwExceptionOnStaffRecordNotFound = false)
    if (userResponse is GetUserResponse.Success) {
      ensureCas3UserHasCas3ReferrerRole(userResponse.user)
    }
    return userResponse
  }

  fun getUserForRequestOrNull(): UserEntity? {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrNull() ?: return null
    val username = deliusPrincipal.name

    return userRepository.findByDeliusUsername(username.uppercase())
  }

  fun getUsersWithQualificationsAndRoles(
    qualifications: List<UserQualification>?,
    roles: List<UserRole>?,
    sortBy: UserSortField?,
    sortDirection: SortDirection?,
    page: Int?,
    region: UUID?,
    apArea: UUID?,
  ): Pair<List<UserEntity>, PaginationMetadata?> {
    var metadata: PaginationMetadata? = null
    val users: List<UserEntity>

    val pageable = getPageable(sortBy?.value ?: "name", sortDirection, page)

    if (pageable == null) {
      users = userRepository.findAll(
        hasQualificationsAndRoles(qualifications, roles, region, apArea, true),
        Sort.by(Sort.Direction.ASC, "name"),
      )
    } else {
      val response = userRepository.findAll(
        hasQualificationsAndRoles(qualifications, roles, region, apArea, true),
        pageable,
      )

      users = response.content
      metadata = getMetadata(response, page)
    }

    return Pair(users, metadata)
  }

  private fun ensureCas3UserHasCas3ReferrerRole(user: UserEntity) {
    val serviceForRequest = requestContextService.getServiceForRequest()
    if ((serviceForRequest == ServiceName.temporaryAccommodation) &&
      (!user.hasAnyRole(*UserRole.getAllRolesForService(ServiceName.temporaryAccommodation).toTypedArray()))
    ) {
      user.roles += userRoleAssignmentRepository.save(
        UserRoleAssignmentEntity(
          id = UUID.randomUUID(),
          user = user,
          role = UserRole.CAS3_REFERRER,
        ),
      )
    }
  }

  fun getAllocatableUsersForAllocationType(
    crn: String,
    qualifications: List<UserQualification>,
    permission: UserPermission,
  ): List<UserEntity> {
    val userQualifications = mutableListOf<UserQualification>()
    userQualifications.addAll(qualifications)

    if (offenderService.isLao(crn)) {
      userQualifications.add(UserQualification.LAO)
    }

    var requiredRole = UserRole.getAllRolesForPermission(permission)
    if (!featureFlagService.getBooleanFlag("cas1-appeal-manager-can-assess-applications")) {
      requiredRole = requiredRole.filter { it != UserRole.CAS1_APPEALS_MANAGER }
    }

    var users = userRepository.findActiveUsersWithAtLeastOneRole(requiredRole)

    userQualifications.forEach { qualification ->
      users = users.filter { it.hasQualification(qualification) }
    }

    return users
  }

  fun deleteUser(id: UUID) {
    val user = userRepository.findByIdOrNull(id) ?: return
    user.isActive = false
    userRepository.save(user)
  }

  fun updateUserRolesAndQualifications(
    id: UUID,
    userRolesAndQualifications: UserRolesAndQualifications,
  ): AuthorisableActionResult<UserEntity> {
    val user = userRepository.findByIdOrNull(id) ?: return AuthorisableActionResult.NotFound()
    val roles = userRolesAndQualifications.roles
    val qualifications = userRolesAndQualifications.qualifications
    user.isActive = true
    userRepository.save(user)

    return updateUserRolesAndQualificationsForUser(user, roles, qualifications)
  }

  fun updateUserRolesAndQualificationsForUser(
    user: UserEntity,
    roles: List<ApprovedPremisesUserRole>,
    qualifications: List<APIUserQualification>,
  ): AuthorisableActionResult<UserEntity> {
    clearQualifications(user)
    clearRolesForService(user, ServiceName.approvedPremises)

    roles.forEach {
      this.addRoleToUser(user, UserRole.valueOf(it))
    }

    qualifications.forEach {
      this.addQualificationToUser(user, transformQualifications(it))
    }

    return AuthorisableActionResult.Success(user)
  }

  fun updateUserFromCommunityApiById(
    id: UUID,
    forService: ServiceName,
    force: Boolean = false,
  ): AuthorisableActionResult<GetUserResponse> {
    val user = userRepository.findByIdOrNull(id) ?: return AuthorisableActionResult.NotFound()
    return AuthorisableActionResult.Success(updateUserFromCommunityApi(user, forService, force))
  }

  fun updateUserFromCommunityApi(
    user: UserEntity,
    forService: ServiceName,
    force: Boolean = false,
  ): GetUserResponse {
    val deliusUser = when (val staffUserDetailsResponse = communityApiClient.getStaffUserDetails(user.deliusUsername)) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure -> staffUserDetailsResponse.throwException()
    }

    if (userHasChanged(user, deliusUser) || force) {
      return GetUserResponse.Success(updateUser(user, deliusUser, forService))
    }

    return GetUserResponse.Success(user)
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  fun updateUserPduFromCommunityApiById(id: UUID): AuthorisableActionResult<UserEntity> {
    val user = userRepository.findByIdOrNull(id) ?: return AuthorisableActionResult.NotFound()

    val deliusUser = when (val staffUserDetailsResponse = communityApiClient.getStaffUserDetails(user.deliusUsername)) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure -> staffUserDetailsResponse.throwException()
    }

    when (val pduResult = findDeliusUserLastPdu(deliusUser)) {
      is CasSimpleResult.Failure -> {
        throw Exception(pduResult.message)
      }

      is CasSimpleResult.Success -> {
        if (user.probationDeliveryUnit?.id != pduResult.value.id) {
          user.probationDeliveryUnit = pduResult.value
          userRepository.save(user)
        }
      }
    }

    return AuthorisableActionResult.Success(user)
  }

  fun updateUser(
    user: UserEntity,
    deliusUser: StaffUserDetails,
    forService: ServiceName,
  ): UserEntity {
    user.name = deliusUser.staff.fullName
    user.email = deliusUser.email.toString()
    user.telephoneNumber = deliusUser.telephoneNumber
    user.deliusStaffCode = deliusUser.staffCode
    user.teamCodes = deliusUser.getTeamCodes()

    deliusUser.probationArea.let { probationArea ->
      findProbationRegionFromArea(probationArea)?.let { probationRegion ->
        user.probationRegion = probationRegion
      }
    }

    val pduResult = findDeliusUserLastPdu(deliusUser)
    if (pduResult is CasSimpleResult.Success) {
      user.probationDeliveryUnit = pduResult.value
    }

    if (forService == ServiceName.approvedPremises) {
      user.apArea = cas1ApAreaMappingService.determineApArea(user.probationRegion, deliusUser)
    }

    return userRepository.save(user)
  }

  fun getUserWorkloads(userIds: List<UUID>): Map<UUID, UserWorkload> {
    return userRepository.findWorkloadForUserIds(userIds).associate {
      it.getUserId() to UserWorkload(
        numTasksPending = listOf(
          it.getPendingAssessments(),
          it.getPendingPlacementRequests(),
          it.getPendingPlacementApplications(),
        ).sum(),
        numTasksCompleted7Days = listOf(
          it.getCompletedAssessmentsInTheLastSevenDays(),
          it.getCompletedPlacementApplicationsInTheLastSevenDays(),
          it.getCompletedPlacementRequestsInTheLastSevenDays(),
        ).sum(),
        numTasksCompleted30Days = listOf(
          it.getCompletedAssessmentsInTheLastThirtyDays(),
          it.getCompletedPlacementApplicationsInTheLastThirtyDays(),
          it.getCompletedPlacementRequestsInTheLastThirtyDays(),
        ).sum(),
      )
    }
  }

  fun getExistingUserOrCreate(username: String) = when (val result = getExistingUserOrCreate(username, throwExceptionOnStaffRecordNotFound = false)) {
    GetUserResponse.StaffRecordNotFound -> throw InternalServerErrorProblem("Could not find staff record for user $username")
    is GetUserResponse.Success -> result.user
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  fun getExistingUserOrCreate(username: String, throwExceptionOnStaffRecordNotFound: Boolean): GetUserResponse {
    val normalisedUsername = username.uppercase()

    val existingUser = userRepository.findByDeliusUsername(normalisedUsername)
    if (existingUser != null) return GetUserResponse.Success(existingUser)

    val staffUserDetailsResponse = communityApiClient.getStaffUserDetails(normalisedUsername)

    val staffUserDetails = when (staffUserDetailsResponse) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure.StatusCode -> {
        if (!throwExceptionOnStaffRecordNotFound && staffUserDetailsResponse.status.equals(HttpStatus.NOT_FOUND)) {
          return GetUserResponse.StaffRecordNotFound
        } else {
          staffUserDetailsResponse.throwException()
        }
      }

      is ClientResult.Failure -> staffUserDetailsResponse.throwException()
    }

    var staffProbationRegion = findProbationRegionFromArea(staffUserDetails.probationArea)

    if (staffProbationRegion == null) {
      if (assignDefaultRegionToUsersWithUnknownRegion) {
        log.warn("Unknown probation region code '${staffUserDetails.probationArea.code}' for user '$normalisedUsername', assigning a default region of 'North West'.")
        staffProbationRegion = probationRegionRepository.findByName("North West")!!
      } else {
        throw RuntimeException("Unknown probation region code '${staffUserDetails.probationArea.code}' for user '$normalisedUsername'")
      }
    }

    val pduResult = findDeliusUserLastPdu(staffUserDetails)

    val apArea = cas1ApAreaMappingService.determineApArea(staffProbationRegion, staffUserDetails)

    val savedUser = userRepository.save(
      UserEntity(
        id = UUID.randomUUID(),
        name = "${staffUserDetails.staff.forenames} ${staffUserDetails.staff.surname}",
        deliusUsername = normalisedUsername,
        deliusStaffIdentifier = staffUserDetails.staffIdentifier,
        deliusStaffCode = staffUserDetails.staffCode,
        email = staffUserDetails.email,
        telephoneNumber = staffUserDetails.telephoneNumber,
        applications = mutableListOf(),
        roles = mutableListOf(),
        qualifications = mutableListOf(),
        probationRegion = staffProbationRegion,
        probationDeliveryUnit = when (pduResult) {
          is CasSimpleResult.Failure -> null
          is CasSimpleResult.Success -> pduResult.value
        },
        isActive = true,
        apArea = apArea,
        teamCodes = staffUserDetails.getTeamCodes(),
        createdAt = OffsetDateTime.now(),
        updatedAt = null,
      ),
    )
    return GetUserResponse.Success(savedUser, createdOnGet = true)
  }

  private fun findProbationRegionFromArea(probationArea: StaffProbationArea): ProbationRegionEntity? {
    return probationAreaProbationRegionMappingRepository
      .findByProbationAreaDeliusCode(probationArea.code)?.probationRegion
  }

  private fun findDeliusUserLastPdu(deliusUser: StaffUserDetails): CasSimpleResult<ProbationDeliveryUnitEntity> {
    val activeTeams = deliusUser.teams?.filter { t -> t.endDate == null } ?: emptyList()
    val activeTeamsNewestFirst = activeTeams.sortedByDescending { t -> t.startDate }

    activeTeamsNewestFirst.forEach {
      val probationDeliveryUnit = probationDeliveryUnitRepository.findByDeliusCode(it.borough.code)
      if (probationDeliveryUnit != null) {
        return CasSimpleResult.Success(probationDeliveryUnit)
      }
    }

    val teamsToLog = activeTeamsNewestFirst.joinToString(",") {
      " ${it.description} (${it.code}) with borough ${it.borough.description} (${it.borough.code})"
    }

    return CasSimpleResult.Failure(
      "PDU could not be determined for user ${deliusUser.username}. " +
        "Considered ${activeTeamsNewestFirst.size} teams$teamsToLog",
    )
  }

  fun addRoleToUser(user: UserEntity, role: UserRole) {
    if (user.hasRole(role)) return

    user.roles.add(
      userRoleAssignmentRepository.save(
        UserRoleAssignmentEntity(
          id = UUID.randomUUID(),
          user = user,
          role = role,
        ),
      ),
    )
  }

  fun addQualificationToUser(user: UserEntity, qualification: UserQualification) {
    if (user.hasQualification(qualification)) return

    user.qualifications.add(
      userQualificationAssignmentRepository.save(
        UserQualificationAssignmentEntity(
          id = UUID.randomUUID(),
          user = user,
          qualification = qualification,
        ),
      ),
    )
  }

  fun clearRolesForService(user: UserEntity, service: ServiceName) {
    val rolesToClear = UserRole.getAllRolesForService(service)
    user.roles.filter { rolesToClear.contains(it.role) }.forEach {
      userRoleAssignmentRepository.delete(it)
      user.roles.remove(it)
    }
  }

  fun clearQualifications(user: UserEntity) {
    userQualificationAssignmentRepository.deleteAllById(user.qualifications.map(UserQualificationAssignmentEntity::id))
    user.qualifications.clear()
  }

  private fun userHasChanged(user: UserEntity, deliusUser: StaffUserDetails): Boolean {
    return (deliusUser.email !== user.email) ||
      (deliusUser.telephoneNumber !== user.telephoneNumber) ||
      (deliusUser.staff.fullName != user.name) ||
      (deliusUser.staffCode != user.deliusStaffCode) ||
      (deliusUser.probationArea.code != user.probationRegion.deliusCode) ||
      !CollectionUtils.isEqualCollection(deliusUser.getTeamCodes(), user.teamCodes ?: emptyList<String>())
  }

  sealed interface GetUserResponse {
    data object StaffRecordNotFound : GetUserResponse
    data class Success(val user: UserEntity, val createdOnGet: Boolean = false) : GetUserResponse
  }
}

fun StaffUserDetails.getTeamCodes() = teams?.let { teams -> teams.map { it.code } } ?: emptyList()
