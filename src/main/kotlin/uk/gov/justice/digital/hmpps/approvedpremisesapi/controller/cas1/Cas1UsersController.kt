package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.UsersCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1UpdateUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Service
class Cas1UsersController(
  private val userService: UserService,
  private val userTransformer: UserTransformer,
  private val userAccessService: Cas1UserAccessService,
) : UsersCas1Delegate {

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun getUser(id: UUID): ResponseEntity<ApprovedPremisesUser> {
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

  override fun deleteUser(id: UUID): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_USER_MANAGEMENT)

    return ResponseEntity.ok(
      userService.deleteUser(id),
    )
  }

  override fun updateUser(id: UUID, cas1UpdateUser: Cas1UpdateUser): ResponseEntity<User> {
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
}
