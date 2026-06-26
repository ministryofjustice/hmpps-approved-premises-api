package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2UserDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Cas2Controller
class Cas2UsersController(
  private val cas2UserService: Cas2UserService,
  private val cas2HdcUserService: Cas2HdcUserService,
) {

  @SuppressWarnings("UnusedParameter")
  @GetMapping("/users/{userName}")
  fun getUserDetails(@PathVariable userName: String): Cas2UserDto {
    val typedUser = cas2UserService.getUserForRequest()
    return cas2HdcUserService.getUserDetails(
      // This is a quick fix to unblock an issue in production. It will be replaced by an
      // endpoint that explicitly returns information for the current user
      Cas2UserEntity(
        id = typedUser.id,
        username = typedUser.username,
        userType = Cas2UserType.valueOf(typedUser.userType.name),
        email = null,
        name = typedUser.name,
        isEnabled = typedUser.isEnabled,
        isActive = typedUser.isActive,
        serviceOrigin = typedUser.serviceOrigin,
      ),
    )
  }

  @GetMapping("/users/me")
  fun getCurrentUserDetails(): ResponseEntity<Cas2UserDto> = ResponseEntity.ok(extractEntityFromCasResult(cas2UserService.getUserDtoForRequest()))
}
