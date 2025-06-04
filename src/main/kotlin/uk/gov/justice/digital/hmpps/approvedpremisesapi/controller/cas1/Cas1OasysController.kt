package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.OAsysCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroupName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysNeedsQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1CreateApplicationLaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysNeedsQuestionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Service
class Cas1OasysController(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val cas1OASysNeedsQuestionTransformer: Cas1OASysNeedsQuestionTransformer,
  private val oaSysService: OASysService,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
) : OAsysCas1Delegate {

  override fun optionalNeeds(crn: String): ResponseEntity<List<Cas1OASysNeedsQuestion>> {
    ensureOffenderAccess(crn)

    return ResponseEntity.ok(
      cas1OASysNeedsQuestionTransformer.transformToNeedsQuestion(
        extractEntityFromCasResult(oaSysService.getOASysNeeds(crn)),
      ),
    )
  }

  override fun answers(
    crn: String,
    group: Cas1OASysGroupName,
    includeOptionalSections: List<Int>?,
  ): ResponseEntity<Cas1OASysGroup> {
    ensureOffenderAccess(crn)

    val questions = when (group) {
      Cas1OASysGroupName.RISK_MANAGEMENT_PLAN -> oaSysSectionsTransformer.riskManagementPlanAnswers(
        extractEntityFromCasResult(oaSysService.getOASysRiskManagementPlan(crn)),
      )
      Cas1OASysGroupName.OFFENCE_DETAILS -> oaSysSectionsTransformer.offenceDetailsAnswers(
        extractEntityFromCasResult(oaSysService.getOASysOffenceDetails(crn)),
      )
      Cas1OASysGroupName.ROSH_SUMMARY -> oaSysSectionsTransformer.roshSummaryAnswers(
        extractEntityFromCasResult(oaSysService.getOASysRoshSummary(crn)),
      )
      Cas1OASysGroupName.NEEDS -> cas1OASysNeedsQuestionTransformer.transformToOASysQuestion(
        needsDetails = extractEntityFromCasResult(oaSysService.getOASysNeeds(crn)),
        includeOptionalSections = includeOptionalSections ?: emptyList(),
      )
      Cas1OASysGroupName.RISK_TO_SELF -> oaSysSectionsTransformer.riskToSelfAnswers(
        extractEntityFromCasResult(oaSysService.getOASysRiskToTheIndividual(crn)),
      )
    }

    return ResponseEntity.ok(
      Cas1OASysGroup(group, questions),
    )
  }

  @SuppressWarnings("ThrowsCount")
  private fun ensureOffenderAccess(crn: String) {
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
  }
}
