package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.AllocationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.transformQualifications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.transformUserRoles
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
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getUsersByPartialName(name: String): List<UserEntity> {
    return userRepository.findByNameContainingIgnoreCase(name)
  }

  fun getUserForRequest(): UserEntity {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val user = getExistingUserOrCreate(username)

    if (requestContextService.getServiceForRequest() == ServiceName.temporaryAccommodation) {
      if (!user.hasAnyRole(*UserRole.getAllRolesForService(ServiceName.temporaryAccommodation).toTypedArray())) {
        user.roles += userRoleAssignmentRepository.save(
          UserRoleAssignmentEntity(
            id = UUID.randomUUID(),
            user = user,
            role = UserRole.CAS3_REFERRER,
          ),
        )
      }
    }

    return user
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

  fun updateUserFromCommunityApiById(id: UUID): AuthorisableActionResult<UserEntity> {
    var user = userRepository.findByIdOrNull(id) ?: return AuthorisableActionResult.NotFound()
    val staffUserDetailsResponse = communityApiClient.getStaffUserDetails(user.deliusUsername)

    val deliusUser = when (staffUserDetailsResponse) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure -> staffUserDetailsResponse.throwException()
    }

    if (userHasChanged(user, deliusUser)) {
      user.name = deliusUser.staff.fullName
      user.email = deliusUser.email.toString()
      user.telephoneNumber = deliusUser.telephoneNumber
      user.deliusStaffCode = deliusUser.staffCode

      deliusUser.probationArea.let { probationArea ->
        findProbationRegionFromArea(probationArea)?.let { probationRegion ->
          user.probationRegion = probationRegion
        }
      }

      user = userRepository.save(user)
    }

    return AuthorisableActionResult.Success(user)
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

  fun getExistingUserOrCreate(username: String, throwProblemOn404: Boolean = false): UserEntity {
    val normalisedUsername = username.uppercase()

    val existingUser = userRepository.findByDeliusUsername(normalisedUsername)
    if (existingUser != null) return existingUser

    val staffUserDetailsResponse = communityApiClient.getStaffUserDetails(normalisedUsername)

    val staffUserDetails = when (staffUserDetailsResponse) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure.StatusCode -> {
        if (throwProblemOn404 && staffUserDetailsResponse.status.equals(HttpStatus.NOT_FOUND)) {
          throw NotFoundProblem(username, "user", "username")
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

    return userRepository.save(
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
        isActive = true,
        apArea = null,
        teamCodes = null,
      ),
    )
  }

  private fun findProbationRegionFromArea(probationArea: StaffProbationArea): ProbationRegionEntity? {
    return probationAreaProbationRegionMappingRepository
      .findByProbationAreaDeliusCode(probationArea.code)?.probationRegion
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
      (deliusUser.probationArea.code != user.probationRegion.deliusCode)
  }
}

fun StaffUserDetails.getTeamCodes() = teams?.let { teams -> teams.map { it.code } } ?: emptyList()
