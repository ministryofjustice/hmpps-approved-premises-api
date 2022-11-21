package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskManagementPlanFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RiskManagementPlanTransformer
import java.time.OffsetDateTime

class RiskManagementPlanTransformerTest {
  private val riskManagementPlanTransformer = RiskManagementPlanTransformer()

  @Test
  fun `transformToApi transforms correctly when incomplete`() {
    val apiResponse = RiskManagementPlanFactory().apply {
      withAssessmentId(34853487)
      withDateCompleted(null)
      withFurtherConsiderations("Further Considerations")
      withAdditionalComments("Additional Comments")
      withContingencyPlans("Contingency Plans")
      withVictimSafetyPlanning("Victim Safety Planning")
      withInterventionsAndTreatment("Interventions and Treatment")
      withMonitoringAndControl("Monitoring and Control")
      withSupervision("Supervision")
      withKeyInformationAboutCurrentSituation("Key Information About Current Situation")
    }.produce()

    val result = riskManagementPlanTransformer.transformToApi(apiResponse)

    assertThat(result.assessmentId).isEqualTo(apiResponse.assessmentId)
    assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
    assertThat(result.dateStarted).isEqualTo(apiResponse.initiationDate)
    assertThat(result.dateCompleted).isEqualTo(apiResponse.dateCompleted)
    assertThat(result.riskManagementPlan).containsAll(
      listOf(
        OASysQuestion(
          label = "Further Considerations",
          questionNumber = "RM28",
          answers = listOf(apiResponse.riskManagementPlan.furtherConsiderations)
        ),
        OASysQuestion(
          label = "Additional Comments",
          questionNumber = "RM35",
          answers = listOf(apiResponse.riskManagementPlan.additionalComments)
        ),
        OASysQuestion(
          label = "Contingency Plans",
          questionNumber = "RM34",
          answers = listOf(apiResponse.riskManagementPlan.contingencyPlans)
        ),
        OASysQuestion(
          label = "Victim Safety Planning",
          questionNumber = "RM33",
          answers = listOf(apiResponse.riskManagementPlan.victimSafetyPlanning)
        ),
        OASysQuestion(
          label = "Interventions and Treatment",
          questionNumber = "RM32",
          answers = listOf(apiResponse.riskManagementPlan.interventionsAndTreatment)
        ),
        OASysQuestion(
          label = "Monitoring and Control",
          questionNumber = "RM31",
          answers = listOf(apiResponse.riskManagementPlan.monitoringAndControl)
        ),
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answers = listOf(apiResponse.riskManagementPlan.supervision)
        ),
        OASysQuestion(
          label = "Key Information About Current Situation",
          questionNumber = "RM28.1",
          answers = listOf(apiResponse.riskManagementPlan.keyInformationAboutCurrentSituation)
        )
      )
    )
  }

  @Test
  fun `transformToApi transforms correctly when complete`() {
    val completedAt = OffsetDateTime.now()

    val apiResponse = RiskManagementPlanFactory().apply {
      withAssessmentId(34853487)
      withDateCompleted(completedAt)
      withFurtherConsiderations("Further Considerations")
      withAdditionalComments("Additional Comments")
      withContingencyPlans("Contingency Plans")
      withVictimSafetyPlanning("Victim Safety Planning")
      withInterventionsAndTreatment("Interventions and Treatment")
      withMonitoringAndControl("Monitoring and Control")
      withSupervision("Supervision")
      withKeyInformationAboutCurrentSituation("Key Information About Current Situation")
    }.produce()

    val result = riskManagementPlanTransformer.transformToApi(apiResponse)

    assertThat(result.assessmentId).isEqualTo(apiResponse.assessmentId)
    assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.completed)
    assertThat(result.dateStarted).isEqualTo(apiResponse.initiationDate)
    assertThat(result.dateCompleted).isEqualTo(apiResponse.dateCompleted)
    assertThat(result.riskManagementPlan).containsAll(
      listOf(
        OASysQuestion(
          label = "Further Considerations",
          questionNumber = "RM28",
          answers = listOf(apiResponse.riskManagementPlan.furtherConsiderations)
        ),
        OASysQuestion(
          label = "Additional Comments",
          questionNumber = "RM35",
          answers = listOf(apiResponse.riskManagementPlan.additionalComments)
        ),
        OASysQuestion(
          label = "Contingency Plans",
          questionNumber = "RM34",
          answers = listOf(apiResponse.riskManagementPlan.contingencyPlans)
        ),
        OASysQuestion(
          label = "Victim Safety Planning",
          questionNumber = "RM33",
          answers = listOf(apiResponse.riskManagementPlan.victimSafetyPlanning)
        ),
        OASysQuestion(
          label = "Interventions and Treatment",
          questionNumber = "RM32",
          answers = listOf(apiResponse.riskManagementPlan.interventionsAndTreatment)
        ),
        OASysQuestion(
          label = "Monitoring and Control",
          questionNumber = "RM31",
          answers = listOf(apiResponse.riskManagementPlan.monitoringAndControl)
        ),
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answers = listOf(apiResponse.riskManagementPlan.supervision)
        ),
        OASysQuestion(
          label = "Key Information About Current Situation",
          questionNumber = "RM28.1",
          answers = listOf(apiResponse.riskManagementPlan.keyInformationAboutCurrentSituation)
        )
      )
    )
  }
}
