package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PersonApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer

@Service
class PersonController(
  private val offenderService: OffenderService,
  private val personTransformer: PersonTransformer
) : PersonApiDelegate {
  override fun personSearchGet(crn: String): ResponseEntity<Person> {
    val principal = getDeliusPrincipalOrThrow()

    return when (val offenderResult = offenderService.getOffenderByCrn(crn, principal.name)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> ResponseEntity.ok(
        personTransformer.transformModelToApi(offenderResult.entity)
      )
    }
  }

  private fun getDeliusPrincipalOrThrow(): AuthAwareAuthenticationToken {
    val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken

    if (principal.token.claims["auth_source"] != "delius") {
      throw ForbiddenProblem()
    }

    return principal
  }
}
