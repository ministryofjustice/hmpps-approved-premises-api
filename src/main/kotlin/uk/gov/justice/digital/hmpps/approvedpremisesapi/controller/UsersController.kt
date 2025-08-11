package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger.PaginationHeaders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.util.UUID

@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class UsersController(
  private val userService: UserService,
  private val userTransformer: UserTransformer,
  private val userAccessService: Cas1UserAccessService,
) {

  @PaginationHeaders
  @Operation(summary = "Returns a list of users. If only the user's ID and Name are required, use /users/summary")
  @GetMapping("/users")
  fun usersGet(
    @RequestParam roles: List<ApprovedPremisesUserRole>?,
    @RequestParam qualifications: List<UserQualification>?,
    @RequestParam probationRegionId: UUID?,
    @RequestParam apAreaId: UUID?,
    @RequestParam cruManagementAreaId: UUID?,
    @RequestParam page: Int?,
    @RequestParam sortBy: UserSortField?,
    @RequestParam sortDirection: SortDirection?,
  ) = getUsers(
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

  @Operation(summary = "Returns a list of user summaries (i.e. id and name only)")
  @GetMapping("/users/summary")
  fun usersSummaryGet(
    @RequestParam roles: List<ApprovedPremisesUserRole>?,
    @RequestParam qualifications: List<UserQualification>?,
    @RequestParam probationRegionId: UUID?,
    @RequestParam apAreaId: UUID?,
    @RequestParam page: Int?,
    @RequestParam sortBy: UserSortField?,
    @RequestParam sortDirection: SortDirection?,
  ) = getUsers(
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
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_USER_LIST)

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

  @Operation(summary = "Returns a list of users with partial match on name")
  @GetMapping("/users/search")
  fun usersSearchGet(
    @RequestParam name: String,
  ): ResponseEntity<List<User>> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_USER_LIST)

    return ResponseEntity.ok(
      userService.getUsersByPartialName(name)
        .map { userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) },
    )
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  @Operation(summary = "Returns a user with match on name")
  @GetMapping("/users/delius")
  fun usersDeliusGet(
    @RequestParam name: String,
    @RequestHeader(value = "X-Service-Name") xServiceName: ServiceName,
  ): ResponseEntity<User> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_USER_LIST)

    val getUserResponse = userService.getExistingUserOrCreate(name)
    return when (getUserResponse) {
      UserService.GetUserResponse.StaffRecordNotFound -> throw NotFoundProblem(name, "user", "username")
      is UserService.GetUserResponse.StaffProbationRegionNotSupported -> throw RuntimeException("Probation region ${getUserResponse.unsupportedRegionId} not supported for user $name")
      is UserService.GetUserResponse.Success -> ResponseEntity.ok(userTransformer.transformJpaToApi(getUserResponse.user, xServiceName))
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
