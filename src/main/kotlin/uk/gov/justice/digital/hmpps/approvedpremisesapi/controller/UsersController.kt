package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserRolesAndQualifications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.UsersApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Service
class UsersController(
  private val userService: UserService,
  private val userTransformer: UserTransformer,
  private val userAccessService: UserAccessService,
) : UsersApiDelegate {

  override fun usersIdGet(id: UUID, xServiceName: ServiceName): ResponseEntity<User> {
    return when (val getUserResponse = extractEntityFromCasResult(userService.updateUserFromDelius(id, xServiceName))) {
      UserService.GetUserResponse.StaffRecordNotFound -> throw NotFoundProblem(id, "Staff")
      is UserService.GetUserResponse.Success -> ResponseEntity(userTransformer.transformJpaToApi(getUserResponse.user, xServiceName), HttpStatus.OK)
    }
  }

  override fun usersGet(
    xServiceName: ServiceName,
    roles: List<ApprovedPremisesUserRole>?,
    qualifications: List<UserQualification>?,
    probationRegionId: UUID?,
    apAreaId: UUID?,
    cruManagementAreaId: UUID?,
    page: Int?,
    sortBy: UserSortField?,
    sortDirection: SortDirection?,
  ) =
    getUsers(
      xServiceName,
      roles,
      qualifications,
      probationRegionId,
      apAreaId,
      page,
      sortBy,
      sortDirection,
      cruManagementAreaId,
    ) { user ->
      userTransformer.transformJpaToApi(user, ServiceName.approvedPremises)
    }

  override fun usersSummaryGet(
    xServiceName: ServiceName,
    roles: List<ApprovedPremisesUserRole>?,
    qualifications: List<UserQualification>?,
    probationRegionId: UUID?,
    apAreaId: UUID?,
    page: Int?,
    sortBy: UserSortField?,
    sortDirection: SortDirection?,
  ) =
    getUsers(
      xServiceName,
      roles,
      qualifications,
      probationRegionId,
      apAreaId,
      page,
      sortBy,
      sortDirection,
    ) { user ->
      userTransformer.transformJpaToSummaryApi(user)
    }

  private fun <T> getUsers(
    xServiceName: ServiceName,
    roles: List<ApprovedPremisesUserRole>?,
    qualifications: List<UserQualification>?,
    probationRegionId: UUID?,
    apAreaId: UUID?,
    page: Int?,
    sortBy: UserSortField?,
    sortDirection: SortDirection?,
    cruManagementAreaId: UUID? = null,
    resultTransformer: (UserEntity) -> T,
  ): ResponseEntity<List<T>> {
    if (!userAccessService.currentUserCanListUsers(xServiceName)) {
      throw ForbiddenProblem()
    }

    val (users, metadata) = userService.getUsers(
      qualifications?.map(::transformApiQualification),
      roles?.map(UserRole::valueOf),
      sortBy,
      sortDirection,
      page,
      probationRegionId,
      apAreaId,
      cruManagementAreaId,
    )

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      users.map { resultTransformer(it) },
    )
  }

  override fun usersIdPut(
    xServiceName: ServiceName,
    id: UUID,
    userRolesAndQualifications: UserRolesAndQualifications,
  ): ResponseEntity<User> {
    if (!userAccessService.currentUserCanManageUsers(xServiceName)) {
      throw ForbiddenProblem()
    }

    val userEntity = extractEntityFromCasResult(
      userService.updateUser(
        id = id,
        roles = userRolesAndQualifications.roles,
        qualifications = userRolesAndQualifications.qualifications,
        cruManagementAreaOverrideId = null,
      ),
    )

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
    if (!userAccessService.currentUserCanListUsers(xServiceName)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(
      userService.getUsersByPartialName(name)
        .map { userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) },
    )
  }

  override fun usersDeliusGet(name: String, xServiceName: ServiceName): ResponseEntity<User> {
    if (!userAccessService.currentUserCanListUsers(xServiceName)) {
      throw ForbiddenProblem()
    }

    val getUserResponse = userService.getExistingUserOrCreate(name)
    return when (getUserResponse) {
      UserService.GetUserResponse.StaffRecordNotFound -> throw NotFoundProblem(name, "user", "username")
      is UserService.GetUserResponse.Success -> ResponseEntity.ok(userTransformer.transformJpaToApi(getUserResponse.user, xServiceName))
    }
  }

  private fun transformApiQualification(apiQualification: UserQualification): uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification =
    when (apiQualification) {
      UserQualification.pipe -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.PIPE
      UserQualification.lao -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.LAO
      UserQualification.emergency -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.EMERGENCY
      UserQualification.esap -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.ESAP
      UserQualification.mentalHealthSpecialist -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.MENTAL_HEALTH_SPECIALIST
      UserQualification.recoveryFocused -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.RECOVERY_FOCUSED
    }
}
