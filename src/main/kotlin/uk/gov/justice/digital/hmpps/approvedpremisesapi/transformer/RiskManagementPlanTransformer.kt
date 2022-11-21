package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskManagementPlan

@Component
class RiskManagementPlanTransformer : OASysTransformer() {
  fun transformToApi(riskManagementPlan: RiskManagementPlan) = OASysRiskManagementPlan(
    assessmentId = riskManagementPlan.assessmentId,
    assessmentState = if (riskManagementPlan.dateCompleted != null) OASysAssessmentState.completed else OASysAssessmentState.incomplete,
    dateStarted = riskManagementPlan.initiationDate,
    dateCompleted = riskManagementPlan.dateCompleted,
    riskManagementPlan = listOf(
      oASysQuestionWithSingleAnswer("Further Considerations", "RM28", riskManagementPlan.riskManagementPlan.furtherConsiderations),
      oASysQuestionWithSingleAnswer("Additional Comments", "RM35", riskManagementPlan.riskManagementPlan.additionalComments),
      oASysQuestionWithSingleAnswer("Contingency Plans", "RM34", riskManagementPlan.riskManagementPlan.contingencyPlans),
      oASysQuestionWithSingleAnswer("Victim Safety Planning", "RM33", riskManagementPlan.riskManagementPlan.victimSafetyPlanning),
      oASysQuestionWithSingleAnswer("Interventions and Treatment", "RM32", riskManagementPlan.riskManagementPlan.interventionsAndTreatment),
      oASysQuestionWithSingleAnswer("Monitoring and Control", "RM31", riskManagementPlan.riskManagementPlan.monitoringAndControl),
      oASysQuestionWithSingleAnswer("Supervision", "RM30", riskManagementPlan.riskManagementPlan.supervision),
      oASysQuestionWithSingleAnswer("Key Information About Current Situation", "RM28.1", riskManagementPlan.riskManagementPlan.keyInformationAboutCurrentSituation)
    )
  )
}
