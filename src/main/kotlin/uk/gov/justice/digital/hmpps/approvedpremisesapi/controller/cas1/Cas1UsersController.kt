package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1UpdateUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger.PaginationHeaders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas1Controller
@Tag(name = "CAS1 Users")
class Cas1UsersController(
  private val userService: UserService,
  private val userTransformer: UserTransformer,
  private val userAccessService: Cas1UserAccessService,
) {

  @SuppressWarnings("TooGenericExceptionThrown")
  @Operation(summary = "Get information about a specific user")
  @GetMapping("/users/{id}")
  fun getUser(@PathVariable id: UUID): ResponseEntity<ApprovedPremisesUser> {
    val getUserResponse = extractEntityFromCasResult(
      userService.updateUserFromDelius(id, ServiceName.approvedPremises),
    )

    return when (getUserResponse) {
      is UserService.GetUserResponse.Success -> {
        val user = userTransformer.transformCas1JpaToApi(getUserResponse.user)
        ResponseEntity.ok(user)
      }
      UserService.GetUserResponse.StaffRecordNotFound -> {
        val user = userService.findByIdOrNull(id)
        if (user != null) {
          val transformedUser = userTransformer.transformCas1JpaToApi(user)
          ResponseEntity.ok(transformedUser)
        } else {
          ResponseEntity.notFound().build()
        }
      }

      is UserService.GetUserResponse.StaffProbationRegionNotSupported -> throw RuntimeException("Probation region ${getUserResponse.unsupportedRegionId} not supported for user $id")
    }
  }

  @Operation(summary = "Deletes the user")
  @DeleteMapping("/users/{id}")
  fun deleteUser(@PathVariable id: UUID): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_USER_MANAGEMENT)

    return ResponseEntity.ok(
      userService.deleteUser(id),
    )
  }

  @Operation(summary = "Update a user")
  @PutMapping("/users/{id}")
  fun updateUser(
    @PathVariable id: UUID,
    @RequestBody cas1UpdateUser: Cas1UpdateUser,
  ): ResponseEntity<User> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_USER_MANAGEMENT)

    val userEntity = extractEntityFromCasResult(
      userService.updateUser(
        id,
        cas1UpdateUser.roles,
        cas1UpdateUser.qualifications,
        cas1UpdateUser.cruManagementAreaOverrideId,
      ),
    )

    return ResponseEntity(userTransformer.transformJpaToApi(userEntity, ServiceName.approvedPremises), HttpStatus.OK)
  }

  @PaginationHeaders
  @Operation(summary = "Returns a list of users. If only the user's ID and Name are required, use /users/summary")
  @GetMapping("/users")
  fun usersGet(
    @RequestParam roles: List<ApprovedPremisesUserRole>?,
    @RequestParam qualifications: List<UserQualification>?,
    @RequestParam probationRegionId: UUID?,
    @RequestParam apAreaId: UUID?,
    @RequestParam cruManagementAreaId: UUID?,
    @RequestParam nameOrEmail: String?,
    @RequestParam page: Int?,
    @RequestParam sortBy: UserSortField?,
    @RequestParam sortDirection: SortDirection?,
  ): ResponseEntity<List<ApprovedPremisesUser>> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_USER_LIST)
    return getUsers(
      roles ?: emptyList(),
      qualifications,
      probationRegionId,
      apAreaId,
      permission = null,
      nameOrEmail,
      page,
      sortBy,
      sortDirection,
      cruManagementAreaId,
    ) { user ->
      userTransformer.transformCas1JpaToApi(user)
    }
  }

  @Operation(summary = "Returns a list of user summaries (i.e. id and name only)")
  @GetMapping("/users/summary")
  fun usersSummaryGet(
    @RequestParam roles: List<ApprovedPremisesUserRole>?,
    @RequestParam qualifications: List<UserQualification>?,
    @RequestParam probationRegionId: UUID?,
    @RequestParam apAreaId: UUID?,
    @RequestParam permission: ApprovedPremisesUserPermission?,
    @RequestParam nameOrEmail: String?,
    @RequestParam page: Int?,
    @RequestParam sortBy: UserSortField?,
    @RequestParam sortDirection: SortDirection?,
  ): ResponseEntity<List<UserSummary>> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_USER_SUMMARY_LIST)
    return getUsers(
      roles ?: emptyList(),
      qualifications,
      probationRegionId,
      apAreaId,
      permission,
      nameOrEmail,
      page,
      sortBy,
      sortDirection,
    ) { user ->
      userTransformer.transformJpaToSummaryApi(user)
    }
  }

  private fun <T> getUsers(
    roles: List<ApprovedPremisesUserRole>,
    qualifications: List<UserQualification>?,
    probationRegionId: UUID?,
    apAreaId: UUID?,
    permission: ApprovedPremisesUserPermission? = null,
    nameOrEmail: String? = null,
    page: Int?,
    sortBy: UserSortField?,
    sortDirection: SortDirection?,
    cruManagementAreaId: UUID? = null,
    resultTransformer: (UserEntity) -> T,
  ): ResponseEntity<List<T>> {
    if (roles.isNotEmpty() && permission != null) {
      throw BadRequestProblem(errorDetail = "Cannot filter on roles and permissions")
    }

    val apiRoles = if (permission != null) {
      UserRole.getAllRolesForPermission(UserPermission.forApiPermission(permission))
    } else {
      roles.map(UserRole::valueOf)
    }

    val (users, metadata) = userService.getUsers(
      qualifications?.map(::transformApiQualification),
      apiRoles,
      sortBy,
      sortDirection,
      page,
      probationRegionId,
      apAreaId,
      cruManagementAreaId,
      nameOrEmail,
    )

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      users.map { resultTransformer(it) },
    )
  }

  @Operation(deprecated = true, summary = "Returns a list of users with partial match on name. Deprecated, use /cas1/users instead which supports name (or email) filtering but also supports paging")
  @GetMapping("/users/search")
  fun usersSearchGet(
    @RequestParam name: String,
  ): ResponseEntity<List<ApprovedPremisesUser>> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_USER_LIST)

    return ResponseEntity.ok(
      userService.getUsersByPartialName(name)
        .map { userTransformer.transformCas1JpaToApi(it) },
    )
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  @Operation(summary = "Returns a user with match on name")
  @GetMapping("/users/delius")
  fun usersDeliusGet(@RequestParam name: String): ResponseEntity<ApprovedPremisesUser> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_USER_LIST)

    val getUserResponse = userService.getExistingUserOrCreate(name)
    return when (getUserResponse) {
      UserService.GetUserResponse.StaffRecordNotFound -> throw NotFoundProblem(name, "user", "username")
      is UserService.GetUserResponse.StaffProbationRegionNotSupported -> throw RuntimeException("Probation region ${getUserResponse.unsupportedRegionId} not supported for user $name")
      is UserService.GetUserResponse.Success -> ResponseEntity.ok(userTransformer.transformCas1JpaToApi(getUserResponse.user))
    }
  }

  private fun transformApiQualification(apiQualification: UserQualification): uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification = when (apiQualification) {
    UserQualification.pipe -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.PIPE
    UserQualification.lao -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.LAO
    UserQualification.emergency -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.EMERGENCY
    UserQualification.esap -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.ESAP
    UserQualification.mentalHealthSpecialist -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.MENTAL_HEALTH_SPECIALIST
    UserQualification.recoveryFocused -> uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.RECOVERY_FOCUSED
  }
}
