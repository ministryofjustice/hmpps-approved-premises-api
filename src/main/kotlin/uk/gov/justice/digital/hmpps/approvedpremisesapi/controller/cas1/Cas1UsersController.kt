package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.UsersCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1UpdateUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Service
class Cas1UsersController(
  private val userService: UserService,
  private val userTransformer: UserTransformer,
  private val userAccessService: UserAccessService,
) : UsersCas1Delegate {

  override fun getUser(id: UUID): ResponseEntity<ApprovedPremisesUser> {
    return when (
      val getUserResponse = extractEntityFromCasResult(
        userService.updateUserFromDelius(id, ServiceName.approvedPremises),
      )
    ) {
      is UserService.GetUserResponse.Success -> ResponseEntity.ok(
        userTransformer.transformCas1JpaToApi(getUserResponse.user),
      )
      UserService.GetUserResponse.StaffRecordNotFound -> userService.findByIdOrNull(id)
        ?.let { ResponseEntity.ok(userTransformer.transformCas1JpaToApi(it)) }
        ?: ResponseEntity.notFound().build()
    }
  }

  override fun deleteUser(id: UUID): ResponseEntity<Unit> {
    if (!userAccessService.currentUserCanManageUsers(ServiceName.approvedPremises)) {
      throw ForbiddenProblem()
    }
    return ResponseEntity.ok(
      userService.deleteUser(id),
    )
  }

  override fun updateUser(id: UUID, cas1UpdateUser: Cas1UpdateUser): ResponseEntity<User> {
    if (!userAccessService.currentUserCanManageUsers(ServiceName.approvedPremises)) {
      throw ForbiddenProblem()
    }

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
}
