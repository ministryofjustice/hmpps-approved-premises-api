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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.specification.hasQualificationsAndRoles
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.GetUserResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserMappingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.AllocationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.transformQualifications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.transformUserRoles
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
  private val cas1UserMappingService: Cas1UserMappingService,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
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
    if (userResponse.staffRecordFound) {
      ensureCas3UserHasCas3ReferrerRole(userResponse.user!!)
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
    allocationType: AllocationType,
  ): List<UserEntity> {
    val isLao = offenderService.isLao(crn)
    val userQualifications = mutableListOf<UserQualification>()
    userQualifications.addAll(qualifications)

    if (isLao) {
      userQualifications.add(UserQualification.LAO)
    }

    val requiredRole = when (allocationType) {
      AllocationType.Assessment -> UserRole.CAS1_ASSESSOR
      AllocationType.PlacementRequest -> UserRole.CAS1_MATCHER
      AllocationType.PlacementApplication -> UserRole.CAS1_MATCHER
    }

    var users = userRepository.findActiveUsersWithRole(requiredRole)

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
      this.addRoleToUser(user, transformUserRoles(it))
    }

    qualifications.forEach {
      this.addQualificationToUser(user, transformQualifications(it))
    }

    return AuthorisableActionResult.Success(user)
  }

  fun updateUserFromCommunityApiById(id: UUID, forService: ServiceName): AuthorisableActionResult<UserEntity> {
    var user = userRepository.findByIdOrNull(id) ?: return AuthorisableActionResult.NotFound()

    val deliusUser = when (val staffUserDetailsResponse = communityApiClient.getStaffUserDetails(user.deliusUsername)) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure -> staffUserDetailsResponse.throwException()
    }

    if (userHasChanged(user, deliusUser)) {
      user = updateUser(user, deliusUser, forService)
    }

    return AuthorisableActionResult.Success(user)
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  fun updateUserPduFromCommunityApiById(id: UUID): AuthorisableActionResult<UserEntity> {
    val user = userRepository.findByIdOrNull(id) ?: return AuthorisableActionResult.NotFound()

    val deliusUser = when (val staffUserDetailsResponse = communityApiClient.getStaffUserDetails(user.deliusUsername)) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure -> staffUserDetailsResponse.throwException()
    }

    when (val probationDeliveryUnit = findDeliusUserLastPdu(deliusUser)) {
      null -> {
        val userLastBoroughCode = deliusUser.teams?.filter { it.endDate == null }?.maxByOrNull { it.startDate }?.borough?.code
        throw Exception("Unable to find community API borough code $userLastBoroughCode in CAS")
      }

      else -> {
        if (user.probationDeliveryUnit?.id != probationDeliveryUnit.id) {
          user.probationDeliveryUnit = probationDeliveryUnit
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

    findDeliusUserLastPdu(deliusUser)?.let { probationDeliveryUnit ->
      user.probationDeliveryUnit = probationDeliveryUnit
    }

    if (forService == ServiceName.approvedPremises) {
      user.apArea = cas1UserMappingService.determineApArea(user.probationRegion, deliusUser)
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

  fun getExistingUserOrCreate(username: String) = getExistingUserOrCreate(username, throwExceptionOnStaffRecordNotFound = true).user!!

  fun getExistingUserOrCreate(username: String, throwExceptionOnStaffRecordNotFound: Boolean): GetUserResponse {
    val normalisedUsername = username.uppercase()

    val existingUser = userRepository.findByDeliusUsername(normalisedUsername)
    if (existingUser != null) return GetUserResponse(existingUser, true)

    val staffUserDetailsResponse = communityApiClient.getStaffUserDetails(normalisedUsername)

    val staffUserDetails = when (staffUserDetailsResponse) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure.StatusCode -> {
        if (!throwExceptionOnStaffRecordNotFound && staffUserDetailsResponse.status.equals(HttpStatus.NOT_FOUND)) {
          return GetUserResponse(null, false)
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
        throw BadRequestProblem(errorDetail = "Unknown probation region code '${staffUserDetails.probationArea.code}' for user '$normalisedUsername'")
      }
    }

    val staffProbationDeliveryUnit = findDeliusUserLastPdu(staffUserDetails)

    val apArea = cas1UserMappingService.determineApArea(staffProbationRegion, staffUserDetails)

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
        probationDeliveryUnit = staffProbationDeliveryUnit,
        isActive = true,
        apArea = apArea,
        teamCodes = staffUserDetails.getTeamCodes(),
        createdAt = OffsetDateTime.now(),
        updatedAt = null,
      ),
    )
    return GetUserResponse(savedUser, true)
  }

  private fun findProbationRegionFromArea(probationArea: StaffProbationArea): ProbationRegionEntity? {
    return probationAreaProbationRegionMappingRepository
      .findByProbationAreaDeliusCode(probationArea.code)?.probationRegion
  }

  private fun findDeliusUserLastPdu(deliusUser: StaffUserDetails): ProbationDeliveryUnitEntity? {
    deliusUser.teams?.filter { t -> t.endDate == null }?.sortedByDescending { t -> t.startDate }?.forEach {
      val probationDeliveryUnit = probationDeliveryUnitRepository.findByDeliusCode(it.borough.code)
      if (probationDeliveryUnit != null) {
        return probationDeliveryUnit
      }
    }

    return null
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
}

fun StaffUserDetails.getTeamCodes() = teams?.let { teams -> teams.map { it.code } } ?: emptyList()
