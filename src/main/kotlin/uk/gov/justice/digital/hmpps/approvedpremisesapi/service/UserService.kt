package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import java.util.UUID

@Service
class UserService(
  @Value("\${assign-default-region-to-users-with-unknown-region}") private val assignDefaultRegionToUsersWithUnknownRegion: Boolean,
  private val httpAuthService: HttpAuthService,
  private val communityApiClient: CommunityApiClient,
  private val userRepository: UserRepository,
  private val userRoleAssignmentRepository: UserRoleAssignmentRepository,
  private val userQualificationAssignmentRepository: UserQualificationAssignmentRepository,
  private val probationRegionRepository: ProbationRegionRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getUserForRequest(): UserEntity {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    return getUserForUsername(username)
  }

  fun getUsersWithQualificationsAndRoles(qualifications: List<UserQualification>?, roles: List<UserRole>?): List<UserEntity> {
    return userRepository.findAll(hasQualificationsAndRoles(qualifications, roles), Sort.by(Sort.Direction.ASC, "name"))
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

  fun getUserForUsername(username: String): UserEntity {
    val normalisedUsername = username.uppercase()

    val existingUser = userRepository.findByDeliusUsername(normalisedUsername)
    if (existingUser != null) return existingUser

    val staffUserDetailsResponse = communityApiClient.getStaffUserDetails(normalisedUsername)

    val staffUserDetails = when (staffUserDetailsResponse) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure -> staffUserDetailsResponse.throwException()
    }

    var staffProbationRegion = probationRegionRepository.findByDeliusCode(staffUserDetails.probationArea.code)

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
      )
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
          role = role
        )
      )
    )
  }

  fun addQualificationToUser(user: UserEntity, qualification: UserQualification) {
    if (user.hasQualification(qualification)) return

    user.qualifications.add(
      userQualificationAssignmentRepository.save(
        UserQualificationAssignmentEntity(
          id = UUID.randomUUID(),
          user = user,
          qualification = qualification
        )
      )
    )
  }

  fun clearRoles(user: UserEntity) {
    userRoleAssignmentRepository.deleteAllById(user.roles.map(UserRoleAssignmentEntity::id))
  }

  fun clearQualifications(user: UserEntity) {
    userQualificationAssignmentRepository.deleteAllById(user.qualifications.map(UserQualificationAssignmentEntity::id))
  }

  private fun userHasChanged(user: UserEntity, deliusUser: StaffUserDetails): Boolean {
    return (deliusUser.email !== user.email) || (deliusUser.telephoneNumber !== user.telephoneNumber) || (deliusUser.staff.fullName != user.name) || (deliusUser.staffCode != user.deliusStaffCode)
  }
}
