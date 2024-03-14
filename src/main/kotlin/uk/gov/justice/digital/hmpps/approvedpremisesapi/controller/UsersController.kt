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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole as JpaUserRole

@Service
class UsersController(
  private val userService: UserService,
  private val userTransformer: UserTransformer,
) : UsersApiDelegate {

  override fun usersIdGet(id: UUID, xServiceName: ServiceName): ResponseEntity<User> {
    val userEntity = when (val result = userService.updateUserFromCommunityApiById(id)) {
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
    val user = userService.getUserForRequest()
    if (xServiceName != ServiceName.approvedPremises || !user.hasAnyRole(
        JpaUserRole.CAS1_ADMIN,
        JpaUserRole.CAS1_WORKFLOW_MANAGER,
      )
    ) {
      throw ForbiddenProblem()
    }

    var roles = roles?.map(::transformApiRole)
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
    val user = userService.getUserForRequest()
    if (xServiceName != ServiceName.approvedPremises || !user.hasAnyRole(
        JpaUserRole.CAS1_ADMIN,
        JpaUserRole.CAS1_WORKFLOW_MANAGER,
      )
    ) {
      throw ForbiddenProblem()
    }

    val userEntity = when (val result = userService.updateUser(id, userRolesAndQualifications)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(id, "User")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> result.entity
    }

    return ResponseEntity(userTransformer.transformJpaToApi(userEntity, ServiceName.approvedPremises), HttpStatus.OK)
  }

  override fun usersIdDelete(id: UUID, xServiceName: ServiceName): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()
    if (xServiceName != ServiceName.approvedPremises || !user.hasAnyRole(
        JpaUserRole.CAS1_ADMIN,
        JpaUserRole.CAS1_WORKFLOW_MANAGER,
      )
    ) {
      throw ForbiddenProblem()
    }
    return ResponseEntity.ok(
      userService.deleteUser(id),
    )
  }

  override fun usersSearchGet(name: String, xServiceName: ServiceName): ResponseEntity<List<User>> {
    val user = userService.getUserForRequest()
    if (xServiceName != ServiceName.approvedPremises || !user.hasAnyRole(
        JpaUserRole.CAS1_ADMIN,
        JpaUserRole.CAS1_WORKFLOW_MANAGER,
      )
    ) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(
      userService.getUsersByPartialName(name)
        .map { userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) },
    )
  }

  override fun usersDeliusGet(name: String, xServiceName: ServiceName): ResponseEntity<User> {
    val user = userService.getUserForRequest()
    if (xServiceName != ServiceName.approvedPremises || !user.hasAnyRole(
        JpaUserRole.CAS1_ADMIN,
        JpaUserRole.CAS1_WORKFLOW_MANAGER,
      )
    ) {
      throw ForbiddenProblem()
    }

    val userEntity = userService.getExistingUserOrCreate(name, true)
    val userTransformed = userTransformer.transformJpaToApi(userEntity, xServiceName)
    return ResponseEntity.ok(userTransformed)
  }

  private fun transformApiRole(apiRole: ApprovedPremisesUserRole): JpaUserRole = when (apiRole) {
    ApprovedPremisesUserRole.roleAdmin -> JpaUserRole.CAS1_ADMIN
    ApprovedPremisesUserRole.applicant -> JpaUserRole.CAS1_APPLICANT
    ApprovedPremisesUserRole.assessor -> JpaUserRole.CAS1_ASSESSOR
    ApprovedPremisesUserRole.manager -> JpaUserRole.CAS1_MANAGER
    ApprovedPremisesUserRole.matcher -> JpaUserRole.CAS1_MATCHER
    ApprovedPremisesUserRole.workflowManager -> JpaUserRole.CAS1_WORKFLOW_MANAGER
    ApprovedPremisesUserRole.reportViewer -> JpaUserRole.CAS1_REPORT_VIEWER
    ApprovedPremisesUserRole.excludedFromAssessAllocation -> JpaUserRole.CAS1_EXCLUDED_FROM_ASSESS_ALLOCATION
    ApprovedPremisesUserRole.excludedFromMatchAllocation -> JpaUserRole.CAS1_EXCLUDED_FROM_MATCH_ALLOCATION
    ApprovedPremisesUserRole.excludedFromPlacementApplicationAllocation -> JpaUserRole.CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION
    ApprovedPremisesUserRole.appealsManager -> JpaUserRole.CAS1_APPEALS_MANAGER
  }

  private fun transformApiQualification(apiQualification: uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification): uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification =
    when (apiQualification) {
      UserQualification.pipe -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.PIPE
      UserQualification.womens -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.WOMENS
      UserQualification.lao -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.LAO
      UserQualification.emergency -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.EMERGENCY
      UserQualification.esap -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.ESAP
    }
}
