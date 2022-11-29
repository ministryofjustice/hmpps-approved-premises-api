package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RisksToTheIndividual

@Component
class RiskToTheIndividualTransformer : OASysTransformer() {
  fun transformToApi(riskToTheIndividual: RisksToTheIndividual) = OASysRisksToTheIndividual(
    assessmentId = riskToTheIndividual.assessmentId,
    assessmentState = if (riskToTheIndividual.dateCompleted != null) OASysAssessmentState.completed else OASysAssessmentState.incomplete,
    dateStarted = riskToTheIndividual.initiationDate,
    dateCompleted = riskToTheIndividual.dateCompleted,
    risksToTheIndividual = listOf(
      oASysQuestionWithSingleAnswer("Current concerns about self-harm or suicide", "R8.1.1", riskToTheIndividual.riskToTheIndividual.currentConcernsSelfHarmSuicide),
      oASysQuestionWithSingleAnswer("Previous concerns about self-harm or suicide", "R8.1.4", riskToTheIndividual.riskToTheIndividual.previousConcernsSelfHarmSuicide),
      oASysQuestionWithSingleAnswer("Current concerns about Coping in Custody or Hostel", "R8.2.1", riskToTheIndividual.riskToTheIndividual.currentCustodyHostelCoping),
      oASysQuestionWithSingleAnswer("Previous concerns about Coping in Custody or Hostel", "R8.2.2", riskToTheIndividual.riskToTheIndividual.previousCustodyHostelCoping),
      oASysQuestionWithSingleAnswer("Current concerns about Vulnerability", "R8.3.1", riskToTheIndividual.riskToTheIndividual.currentVulnerability),
      oASysQuestionWithSingleAnswer("Previous concerns about Vulnerability", "R8.3.2", riskToTheIndividual.riskToTheIndividual.previousVulnerability),
      oASysQuestionWithSingleAnswer("Risk of serious harm", "R8.4.1", riskToTheIndividual.riskToTheIndividual.riskOfSeriousHarm),
      oASysQuestionWithSingleAnswer("Breach of trust (current): describe circumstances, relevant issues and needs", "R9.3.1", riskToTheIndividual.riskToTheIndividual.currentConcernsBreachOfTrustText)
    )
  )
}
