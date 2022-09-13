package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PeopleApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer

@Service
class PeopleController(
  private val offenderService: OffenderService,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer
) : PeopleApiDelegate {
  override fun peopleSearchGet(crn: String): ResponseEntity<Person> {
    val principal = getDeliusPrincipalOrThrow()

    val offenderDetails = when (val offenderResult = offenderService.getOffenderByCrn(crn, principal.name)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> offenderResult.entity
    }

    if (offenderDetails.otherIds.nomsNumber == null) {
      throw InternalServerErrorProblem("No nomsNumber present for CRN")
    }

    val inmateDetail = when (val inmateDetailResult = offenderService.getInmateDetailByNomsNumber(offenderDetails.otherIds.nomsNumber)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(offenderDetails.otherIds.nomsNumber, "Inmate")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> inmateDetailResult.entity
    }

    return ResponseEntity.ok(
      personTransformer.transformModelToApi(offenderDetails, inmateDetail)
    )
  }

  override fun peopleCrnRisksGet(crn: String): ResponseEntity<PersonRisks> {
    val principal = getDeliusPrincipalOrThrow()

    val risks = when (val risksResult = offenderService.getRiskByCrn(crn, principal.token.tokenValue, principal.name)) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Success -> risksResult.entity
    }

    return ResponseEntity.ok(risksTransformer.transformDomainToApi(risks))
  }

  private fun getDeliusPrincipalOrThrow(): AuthAwareAuthenticationToken {
    val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken

    if (principal.token.claims["auth_source"] != "delius") {
      throw ForbiddenProblem()
    }

    return principal
  }
}
