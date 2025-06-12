package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.OAsysCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroupName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1CreateApplicationLaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysNeedsQuestionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysOffenceDetailsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Service
class Cas1OasysController(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val cas1OASysNeedsQuestionTransformer: Cas1OASysNeedsQuestionTransformer,
  private val oaSysService: OASysService,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val oaSysOffenceDetailsTransformer: Cas1OASysOffenceDetailsTransformer,
) : OAsysCas1Delegate {

  override fun supportingInformationMetadata(crn: String): ResponseEntity<Cas1OASysMetadata> {
    ensureOffenderAccess(crn)

    return ResponseEntity.ok(
      Cas1OASysMetadata(
        assessmentMetadata = oaSysOffenceDetailsTransformer.toAssessmentMetadata(
          extractEntityFromCasResult(oaSysService.getOASysOffenceDetails(crn)),
        ),
        supportingInformation = cas1OASysNeedsQuestionTransformer.transformToSupportingInformationMetadata(
          extractEntityFromCasResult(oaSysService.getOASysNeeds(crn)),
        ),
      ),
    )
  }

  override fun answers(
    crn: String,
    group: Cas1OASysGroupName,
    includeOptionalSections: List<Int>?,
  ): ResponseEntity<Cas1OASysGroup> {
    ensureOffenderAccess(crn)

    val offenceDetails = extractEntityFromCasResult(oaSysService.getOASysOffenceDetails(crn))

    val assessmentMetadata = oaSysOffenceDetailsTransformer.toAssessmentMetadata(offenceDetails)

    val answers = when (group) {
      Cas1OASysGroupName.RISK_MANAGEMENT_PLAN -> oaSysSectionsTransformer.riskManagementPlanAnswers(
        extractEntityFromCasResult(oaSysService.getOASysRiskManagementPlan(crn)).riskManagementPlan,
      )
      Cas1OASysGroupName.OFFENCE_DETAILS -> oaSysSectionsTransformer.offenceDetailsAnswers(offenceDetails.offence)
      Cas1OASysGroupName.ROSH_SUMMARY -> oaSysSectionsTransformer.roshSummaryAnswers(
        extractEntityFromCasResult(oaSysService.getOASysRoshSummary(crn)).roshSummary,
      )
      Cas1OASysGroupName.SUPPORTING_INFORMATION -> cas1OASysNeedsQuestionTransformer.transformToOASysQuestion(
        needsDetails = extractEntityFromCasResult(oaSysService.getOASysNeeds(crn)),
        includeOptionalSections = includeOptionalSections ?: emptyList(),
      )
      Cas1OASysGroupName.RISK_TO_SELF -> oaSysSectionsTransformer.riskToSelfAnswers(
        extractEntityFromCasResult(oaSysService.getOASysRiskToTheIndividual(crn)).riskToTheIndividual,
      )
    }

    return ResponseEntity.ok(
      Cas1OASysGroup(
        group = group,
        assessmentMetadata = assessmentMetadata,
        answers = answers,
      ),
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
