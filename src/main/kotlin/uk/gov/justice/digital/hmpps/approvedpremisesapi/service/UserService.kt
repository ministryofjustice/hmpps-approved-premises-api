package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserRolesAndQualifications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingRepository
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.transformQualifications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.transformUserRoles
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as APIUserQualification

@Service
class UserService(
  @Value("\${assign-default-region-to-users-with-unknown-region}") private val assignDefaultRegionToUsersWithUnknownRegion: Boolean,
  private val currentRequest: HttpServletRequest,
  private val httpAuthService: HttpAuthService,
  private val offenderService: OffenderService,
  private val communityApiClient: CommunityApiClient,
  private val userRepository: UserRepository,
  private val userRoleAssignmentRepository: UserRoleAssignmentRepository,
  private val userQualificationAssignmentRepository: UserQualificationAssignmentRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val probationAreaProbationRegionMappingRepository: ProbationAreaProbationRegionMappingRepository,
  private val userTransformer: UserTransformer,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getUsersByPartialName(name: String): List<UserEntity> {
    return userRepository.findByNameContainingIgnoreCase(name)
  }

  fun getUserForRequest(): UserEntity {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val user = getUserForUsername(username)

    if (currentRequest.getHeader("X-Service-Name") == ServiceName.temporaryAccommodation.value) {
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

  fun getUsersWithQualificationsAndRoles(qualifications: List<UserQualification>?, roles: List<UserRole>?) =
    userRepository.findAll(hasQualificationsAndRoles(qualifications, roles), Sort.by(Sort.Direction.ASC, "name"))

  fun getUsersWithQualificationsAndRolesPassingLAO(crn: String, qualifications: List<UserQualification>?, roles: List<UserRole>?): List<UserEntity> {
    val isLao = offenderService.isLao(crn)

    return userRepository.findAll(hasQualificationsAndRoles(qualifications, roles), Sort.by(Sort.Direction.ASC, "name")).filter {
      !isLao || it.hasQualification(UserQualification.LAO) || offenderService.getOffenderByCrn(crn, it.deliusUsername) is AuthorisableActionResult.Success
    }
  }

  fun getUserForAssessmentAllocation(application: ApplicationEntity): UserEntity {
    val unsuitableUsers = mutableListOf<UUID>(UUID.randomUUID())
    val qualifications = application.getRequiredQualifications().toMutableList()

    if (offenderService.isLao(application.crn)) {
      qualifications += UserQualification.LAO
    }

    while (true) {
      val potentialUser = userRepository.findQualifiedAssessorWithLeastPendingOrCompletedInLastWeekAssessments(qualifications.map(UserQualification::toString), qualifications.size.toLong(), unsuitableUsers)
        ?: throw RuntimeException("Could not find a suitable assessor for assessment with qualifications (${qualifications.joinToString(",")}): ${application.crn}")

      if ((qualifications.isEmpty() && potentialUser.qualifications.isNotEmpty()) || potentialUser.hasRole(UserRole.CAS1_EXCLUDED_FROM_ASSESS_ALLOCATION)) {
        unsuitableUsers += potentialUser.id
        continue
      }

      return potentialUser
    }
  }

  fun getUserForPlacementRequestAllocation(crn: String): UserEntity {
    val qualifications = mutableListOf<UserQualification>()
    val unsuitableUsers = mutableListOf<UUID>(UUID.randomUUID())

    if (offenderService.isLao(crn)) {
      qualifications += UserQualification.LAO
    }

    while (true) {
      val potentialUser = userRepository.findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementRequests(qualifications.map(UserQualification::toString), qualifications.size.toLong(), unsuitableUsers)
        ?: throw RuntimeException("Could not find a suitable matcher for placement request with qualifications (${qualifications.joinToString(",")}): $crn")

      if (potentialUser.hasRole(UserRole.CAS1_EXCLUDED_FROM_MATCH_ALLOCATION)) {
        unsuitableUsers += potentialUser.id
        continue
      }

      return potentialUser
    }
  }

  fun getUserForPlacementApplicationAllocation(crn: String): UserEntity {
    val qualifications = mutableListOf<UserQualification>()
    val unsuitableUsers = mutableListOf<UUID>(UUID.randomUUID())

    if (offenderService.isLao(crn)) {
      qualifications += UserQualification.LAO
    }

    while (true) {
      val potentialUser = userRepository.findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementApplications(qualifications.map(UserQualification::toString), qualifications.size.toLong(), unsuitableUsers)
        ?: throw RuntimeException("Could not find a suitable matcher for placement application with qualifications (${qualifications.joinToString(",")}): $crn")

      if (potentialUser.hasRole(UserRole.CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION)) {
        unsuitableUsers += potentialUser.id
        continue
      }

      return potentialUser
    }
  }

  fun updateUserRolesAndQualifications(id: UUID, userRolesAndQualifications: UserRolesAndQualifications): AuthorisableActionResult<UserEntity> {
    val user = userRepository.findByIdOrNull(id) ?: return AuthorisableActionResult.NotFound()
    val roles = userRolesAndQualifications.roles
    val qualifications = userRolesAndQualifications.qualifications
    return updateUserRolesAndQualificationsForUser(user, roles, qualifications)
  }

  fun updateUserRolesAndQualificationsForUser(user: UserEntity, roles: List<ApprovedPremisesUserRole>, qualifications: List<APIUserQualification>): AuthorisableActionResult<UserEntity> {
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

      user = userRepository.save(user)
    }

    return AuthorisableActionResult.Success(user)
  }

  fun getUserForUsername(username: String, throwProblemOn404: Boolean = false): UserEntity {
    val normalisedUsername = username.uppercase()

    val existingUser = userRepository.findByDeliusUsername(normalisedUsername)
    if (existingUser != null) return existingUser

    val staffUserDetailsResponse = communityApiClient.getStaffUserDetails(normalisedUsername)

    val staffUserDetails = when (staffUserDetailsResponse) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure.StatusCode -> {
        if (throwProblemOn404 && staffUserDetailsResponse.status === HttpStatus.NOT_FOUND) {
          throw NotFoundProblem(username, "user", "username")
        } else {
          staffUserDetailsResponse.throwException()
        }
      }
      is ClientResult.Failure -> staffUserDetailsResponse.throwException()
    }

    var staffProbationRegion = probationAreaProbationRegionMappingRepository
      .findByProbationAreaDeliusCode(staffUserDetails.probationArea.code)?.probationRegion

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
      ),
    )
  }

  private fun updateUserFromCommunityApi(user: UserEntity): UserEntity {
    val staffUserDetailsResponse = communityApiClient.getStaffUserDetails(user.deliusUsername)

    val staffUserDetails = when (staffUserDetailsResponse) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure -> staffUserDetailsResponse.throwException()
    }

    user.apply {
      name = "${staffUserDetails.staff.forenames} ${staffUserDetails.staff.surname}"
      deliusStaffIdentifier = staffUserDetails.staffIdentifier
      deliusStaffCode = staffUserDetails.staffCode
      email = staffUserDetails.email
      telephoneNumber = staffUserDetails.telephoneNumber
    }

    return userRepository.save(user)
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
    return (deliusUser.email !== user.email) || (deliusUser.telephoneNumber !== user.telephoneNumber) || (deliusUser.staff.fullName != user.name) || (deliusUser.staffCode != user.deliusStaffCode)
  }
}
