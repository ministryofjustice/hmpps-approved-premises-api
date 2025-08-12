package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1UsersController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger.PaginationHeaders
import java.util.UUID

@Deprecated("Use Cas1UsersController")
@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class UsersController(
  private val cas1UsersController: Cas1UsersController,
) {

  @PaginationHeaders
  @Operation(summary = "Returns a list of users. If only the user's ID and Name are required, use /users/summary. Deprecated, use /cas1/users")
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
  ) = cas1UsersController.usersGet(
    roles,
    qualifications,
    probationRegionId,
    apAreaId,
    cruManagementAreaId,
    page,
    sortBy,
    sortDirection,
  ) as ResponseEntity<List<User>>

  @Operation(summary = "Returns a list of user summaries (i.e. id and name only). Deprecated, use /cas1/users/summary")
  @GetMapping("/users/summary")
  fun usersSummaryGet(
    @RequestParam roles: List<ApprovedPremisesUserRole>?,
    @RequestParam qualifications: List<UserQualification>?,
    @RequestParam probationRegionId: UUID?,
    @RequestParam apAreaId: UUID?,
    @RequestParam page: Int?,
    @RequestParam sortBy: UserSortField?,
    @RequestParam sortDirection: SortDirection?,
  ) = cas1UsersController.usersSummaryGet(
    roles,
    qualifications,
    probationRegionId,
    apAreaId,
    permission = null,
    page,
    sortBy,
    sortDirection,
  )

  @Operation(summary = "Returns a list of users with partial match on name. Deprecated, use /cas1/users/search")
  @GetMapping("/users/search")
  fun usersSearchGet(
    @RequestParam name: String,
  ) = cas1UsersController.usersSearchGet(name) as ResponseEntity<List<User>>

  @SuppressWarnings("TooGenericExceptionThrown")
  @Operation(summary = "Returns a user with match on name. Deprecated, use /cas1/users/delius")
  @GetMapping("/users/delius")
  fun usersDeliusGet(
    @RequestParam name: String,
  ) = cas1UsersController.usersDeliusGet(name) as ResponseEntity<User>
}
