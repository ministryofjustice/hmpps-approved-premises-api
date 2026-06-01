package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskOfSeriousHarm
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskToSelf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RiskToTheIndividualInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RoshSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RoshSummaryInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService

@Service
class Cas2OAsysSectionsTransformer(val featureFlagService: FeatureFlagService) {
  fun transformRiskToIndividual(
    risksToTheIndividual: RisksToTheIndividual,
  ): OASysRiskToSelf = OASysRiskToSelf(
    assessmentId = risksToTheIndividual.assessmentId,
    assessmentState = if (risksToTheIndividual.dateCompleted != null) OASysAssessmentState.completed else OASysAssessmentState.incomplete,
    dateStarted = risksToTheIndividual.initiationDate.toInstant(),
    dateCompleted = risksToTheIndividual.dateCompleted?.toInstant(),
    riskToSelf = riskToSelfAnswers(risksToTheIndividual.riskToTheIndividual),
  )

  fun transformRiskOfSeriousHarm(
    roshSummary: RoshSummary,
  ): OASysRiskOfSeriousHarm = OASysRiskOfSeriousHarm(
    assessmentId = roshSummary.assessmentId,
    assessmentState = if (roshSummary.dateCompleted != null) OASysAssessmentState.completed else OASysAssessmentState.incomplete,
    dateStarted = roshSummary.initiationDate.toInstant(),
    dateCompleted = roshSummary.dateCompleted?.toInstant(),
    rosh = roshSummaryAnswers(roshSummary.roshSummary),
  )

  private fun oldRiskToSelfAnswers(risksToTheIndividual: RiskToTheIndividualInner?) = listOf(
    OASysQuestion("Current concerns about self-harm or suicide", "R8.1.1", risksToTheIndividual?.currentConcernsSelfHarmSuicide),
    OASysQuestion("Current concerns about Vulnerability", "R8.3.1", risksToTheIndividual?.currentVulnerability),
    OASysQuestion("Previous concerns about self-harm or suicide", "R8.1.4", risksToTheIndividual?.previousConcernsSelfHarmSuicide),
  )

  private fun riskToSelfAnswers(risksToTheIndividual: RiskToTheIndividualInner?) = if (cas2UseNewQuestions()) {
    if (risksToTheIndividual.isPreNod1057()) {
      riskToSelfAnswersPreNod1057(risksToTheIndividual)
    } else {
      riskToSelfAnswersPostNod1057(risksToTheIndividual)
    }
  } else {
    oldRiskToSelfAnswers(risksToTheIndividual)
  }

  private fun riskToSelfAnswersPostNod1057(risksToTheIndividual: RiskToTheIndividualInner?) = listOf(
    OASysQuestion("Analysis of current or previous self-harm and/or suicide concerns", "FA62", risksToTheIndividual?.analysisSuicideSelfharm),
    // Whilst the questionNumber is labelled as 'R8.3.1', it's being sourced from FA64. Ideally we'd change the question number here, but a UI change would
    // be required first as it relies on the question number
    OASysQuestion("Current concerns about Vulnerability", "R8.3.1", risksToTheIndividual?.analysisVulnerabilities),
  )

  private fun riskToSelfAnswersPreNod1057(risksToTheIndividual: RiskToTheIndividualInner?) = listOf(
    OASysQuestion("Analysis of current or previous self-harm and/or suicide concerns", "FA62", combineAnswers(risksToTheIndividual?.previousConcernsSelfHarmSuicide, risksToTheIndividual?.currentConcernsSelfHarmSuicide)),
    OASysQuestion("Current concerns about Vulnerability", "R8.3.1", risksToTheIndividual?.currentVulnerability),
  )

  private fun roshSummaryAnswers(roshSummary: RoshSummaryInner?) = listOf(
    OASysQuestion("Who is at risk", "R10.1", roshSummary?.whoIsAtRisk),
    OASysQuestion("What is the nature of the risk", "R10.2", roshSummary?.natureOfRisk),
  )

  private fun combineAnswers(answer1: String?, answer2: String?) = if (answer1 != null && answer2 != null) {
    answer1 + "\n\n" + answer2
  } else {
    answer1 ?: answer2
  }

  private fun cas2UseNewQuestions() = featureFlagService.getBooleanFlag("cas2-oasys-use-new-questions")

  private fun RiskToTheIndividualInner?.isPreNod1057() = this?.currentConcernsSelfHarmSuicide != null ||
    this?.previousConcernsSelfHarmSuicide != null ||
    this?.currentVulnerability != null
}
