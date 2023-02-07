package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.UsersApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserRole
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
  private val userTransformer: UserTransformer
) : UsersApiDelegate {

  override fun usersIdGet(id: UUID, xServiceName: ServiceName): ResponseEntity<User> {
    val userEntity = when (val result = userService.getUserForId(id)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(id, "User")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> result.entity
    }

    return ResponseEntity(userTransformer.transformJpaToApi(userEntity, xServiceName), HttpStatus.OK)
  }

  override fun usersGet(xServiceName: ServiceName, roles: List<UserRole>?, qualifications: List<UserQualification>?): ResponseEntity<List<User>> {
    val user = userService.getUserForRequest()
    if (!user.hasAnyRole(JpaUserRole.ROLE_ADMIN, JpaUserRole.WORKFLOW_MANAGER)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(
      userService.getAllUsers()
        .map { userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) }
    )
  }
}
