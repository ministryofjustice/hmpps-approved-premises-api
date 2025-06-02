package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.OAsysCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysNeedsQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1CreateApplicationLaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysNeedsQuestionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Service
class Cas1OasysController(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val cas1OASysNeedsQuestionTransformer: Cas1OASysNeedsQuestionTransformer,
  private val oaSysService: OASysService,
) : OAsysCas1Delegate {

  override fun optionalNeeds(crn: String): ResponseEntity<List<OASysNeedsQuestion>> {
    when (
      offenderService.canAccessOffender(
        crn = crn,
        laoStrategy = userService.getUserForRequest().cas1CreateApplicationLaoStrategy(),
      )
    ) {
      null -> throw NotFoundProblem(crn, "Offender")
      false -> throw throw ForbiddenProblem()
      else -> Unit
    }

    return ResponseEntity.ok(
      cas1OASysNeedsQuestionTransformer.transformToApi(
        extractEntityFromCasResult(oaSysService.getOASysNeeds(crn)),
      ),
    )
  }
}
