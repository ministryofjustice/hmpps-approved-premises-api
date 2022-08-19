package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.CrnApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.CrnSearch
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.CrnSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

@Service
class CrnController(private val offenderService: OffenderService) : CrnApiDelegate {
  override fun crnSearchPost(crnSearch: CrnSearch): ResponseEntity<CrnSearchResult> {
    val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken

    if (principal.token.claims["auth_source"] != "delius") {
      throw ForbiddenProblem()
    }

    val offender = offenderService.getOffenderByCrn(crnSearch.crn, principal.name)
      ?: throw NotFoundProblem(crnSearch.crn, "Person")

    return ResponseEntity.ok(
      CrnSearchResult(
        crn = crnSearch.crn,
        name = "${offender.firstName} ${offender.surname}"
      )
    )
  }
}
