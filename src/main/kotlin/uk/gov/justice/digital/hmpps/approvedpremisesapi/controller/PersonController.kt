package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PersonApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

@Service
class PersonController(private val offenderService: OffenderService) : PersonApiDelegate {
  override fun personSearchGet(crn: String): ResponseEntity<Person> {
    val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken

    if (principal.token.claims["auth_source"] != "delius") {
      throw ForbiddenProblem()
    }

    val offender = offenderService.getOffenderByCrn(crn, principal.name)
      ?: throw NotFoundProblem(crn, "Person")

    return ResponseEntity.ok(
      Person(
        crn = crn,
        name = "${offender.firstName} ${offender.surname}"
      )
    )
  }
}
