package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2UserDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Cas2Controller
class Cas2UsersController(
  private val cas2UserService: Cas2UserService,
) {

  @GetMapping("/users/me")
  fun getCurrentUserDetails(): ResponseEntity<Cas2UserDto> = ResponseEntity.ok(extractEntityFromCasResult(cas2UserService.getUserDtoForRequest()))
}
