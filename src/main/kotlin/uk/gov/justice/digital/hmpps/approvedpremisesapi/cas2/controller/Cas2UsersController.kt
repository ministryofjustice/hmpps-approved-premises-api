package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2v2UserDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcUserService

@Cas2Controller
class Cas2UsersController(
  private val cas2HdcUserService: Cas2HdcUserService,
) {

  @GetMapping("/users/{userName}")
  fun getUserDetails(@PathVariable userName: String): Cas2v2UserDto = cas2HdcUserService.getCas2v2UserDetails(userName)
}
