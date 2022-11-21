package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskOfSeriousHarmSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummary

@Component
class RoshSummaryTransformer : OASysTransformer() {
  fun transformToApi(roshSummary: RoshSummary) = OASysRiskOfSeriousHarmSummary(
    assessmentId = roshSummary.assessmentId,
    assessmentState = if (roshSummary.dateCompleted != null) OASysAssessmentState.completed else OASysAssessmentState.incomplete,
    dateStarted = roshSummary.initiationDate,
    dateCompleted = roshSummary.dateCompleted,
    riskOfSeriousHarmSummary = listOf(
      oASysQuestionWithSingleAnswer("Who is at risk", "R10.1", roshSummary.roshSummary.whoAtRisk),
      oASysQuestionWithSingleAnswer("What is the nature of the risk", "R10.2", roshSummary.roshSummary.natureOfRisk),
      oASysQuestionWithSingleAnswer("When is the risk likely to be the greatest", "R10.3", roshSummary.roshSummary.riskGreatest),
      oASysQuestionWithSingleAnswer("What circumstances are likely to increase risk", "R10.4", roshSummary.roshSummary.riskIncreaseLikelyTo),
      oASysQuestionWithSingleAnswer("What circumstances are likely to reduce the risk", "R10.5", roshSummary.roshSummary.riskReductionLikelyTo)
    )
  )
}
