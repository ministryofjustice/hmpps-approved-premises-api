package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.UsersApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserRolesAndQualifications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.util.UUID

@Service
class UsersController(
  private val userService: UserService,
  private val userTransformer: UserTransformer,
  private val userAccessService: UserAccessService,
) : UsersApiDelegate {

  override fun usersIdGet(id: UUID, xServiceName: ServiceName): ResponseEntity<User> {
    val userEntity = when (val result = userService.updateUserFromCommunityApiById(id, xServiceName)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(id, "User")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> result.entity
    }

    return ResponseEntity(userTransformer.transformJpaToApi(userEntity, xServiceName), HttpStatus.OK)
  }

  override fun usersGet(
    xServiceName: ServiceName,
    roles: List<ApprovedPremisesUserRole>?,
    qualifications: List<UserQualification>?,
    probationRegionId: UUID?,
    apAreaId: UUID?,
    page: Int?,
    sortBy: UserSortField?,
    sortDirection: SortDirection?,
  ): ResponseEntity<List<User>> {
    if (!userAccessService.currentUserCanManageUsers(xServiceName)) {
      throw ForbiddenProblem()
    }

    var roles = roles?.map(UserRole::valueOf)
    var qualifications = qualifications?.map(::transformApiQualification)

    val (users, metadata) = userService.getUsersWithQualificationsAndRoles(
      qualifications,
      roles,
      sortBy,
      sortDirection,
      page,
      probationRegionId,
      apAreaId,
    )

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      users.map { userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) },
    )
  }

  override fun usersIdPut(
    xServiceName: ServiceName,
    id: java.util.UUID,
    userRolesAndQualifications: UserRolesAndQualifications,
  ): ResponseEntity<User> {
    if (!userAccessService.currentUserCanManageUsers(xServiceName)) {
      throw ForbiddenProblem()
    }

    val userEntity = when (val result = userService.updateUserRolesAndQualifications(id, userRolesAndQualifications)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(id, "User")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> result.entity
    }

    return ResponseEntity(userTransformer.transformJpaToApi(userEntity, ServiceName.approvedPremises), HttpStatus.OK)
  }

  override fun usersIdDelete(id: UUID, xServiceName: ServiceName): ResponseEntity<Unit> {
    if (!userAccessService.currentUserCanManageUsers(xServiceName)) {
      throw ForbiddenProblem()
    }
    return ResponseEntity.ok(
      userService.deleteUser(id),
    )
  }

  override fun usersSearchGet(name: String, xServiceName: ServiceName): ResponseEntity<List<User>> {
    if (!userAccessService.currentUserCanManageUsers(xServiceName)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(
      userService.getUsersByPartialName(name)
        .map { userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) },
    )
  }

  override fun getCurrentUserDetails(xServiceName: ServiceName): ResponseEntity<User> {
    if (!userAccessService.userCanViewOwnUserDetails()) {
      throw ForbiddenProblem()
    }

    val userFromDelius = userService.getUpdatedCurrentUserDetails(xServiceName)
    val userTransformed = userTransformer.transformJpaToApi(userFromDelius, xServiceName)
    return ResponseEntity.ok(userTransformed)
  }

  override fun usersDeliusGet(name: String, xServiceName: ServiceName): ResponseEntity<User> {
    if (!userAccessService.currentUserCanManageUsers(xServiceName)) {
      throw ForbiddenProblem()
    }

    val userEntity = userService.getExistingUserOrCreate(name, throwExceptionOnStaffRecordNotFound = false)
    if (!userEntity.staffRecordFound) throw NotFoundProblem(name, "user", "username")
    val userTransformed = userTransformer.transformJpaToApi(userEntity.user!!, xServiceName)
    return ResponseEntity.ok(userTransformed)
  }

  private fun transformApiQualification(apiQualification: UserQualification): uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification =
    when (apiQualification) {
      UserQualification.pipe -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.PIPE
      UserQualification.womens -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.WOMENS
      UserQualification.lao -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.LAO
      UserQualification.emergency -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.EMERGENCY
      UserQualification.esap -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.ESAP
      UserQualification.mentalHealthSpecialist -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.MENTAL_HEALTH_SPECIALIST
      UserQualification.recoveryFocused -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.RECOVERY_FOCUSED
    }
}
