package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RiskToTheIndividualTransformer
import java.time.OffsetDateTime

class RiskToTheIndividualTransformerTest {
  private val riskToTheIndividualTransformer = RiskToTheIndividualTransformer()

  @Test
  fun `transformToApi transforms correctly when incomplete`() {
    val apiResponse = RiskToTheIndividualFactory().apply {
      withAssessmentId(34853487)
      withDateCompleted(null)
      withCurrentConcernsSelfHarmSuicide("currentConcernsSelfHarmSuicide")
      withPreviousConcernsSelfHarmSuicide("previousConcernsSelfHarmSuicide")
      withCurrentCustodyHostelCoping("currentCustodyHostelCoping")
      withPreviousCustodyHostelCoping("previousCustodyHostelCoping")
      withCurrentVulnerability("currentVulnerability")
      withPreviousVulnerability("previousVulnerability")
      withRiskOfSeriousHarm("riskOfSeriousHarm")
      withCurrentConcernsBreachOfTrustText("currentConcernsBreachOfTrustText")
    }.produce()

    val result = riskToTheIndividualTransformer.transformToApi(apiResponse)

    assertThat(result.assessmentId).isEqualTo(apiResponse.assessmentId)
    assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
    assertThat(result.dateStarted).isEqualTo(apiResponse.initiationDate)
    assertThat(result.dateCompleted).isEqualTo(apiResponse.dateCompleted)
    assertThat(result.risksToTheIndividual).containsAll(
      listOf(
        OASysQuestion(
          label = "Current concerns about self-harm or suicide",
          questionNumber = "R8.1.1",
          answers = listOf(apiResponse.riskToTheIndividual.currentConcernsSelfHarmSuicide)
        ),
        OASysQuestion(
          label = "Previous concerns about self-harm or suicide",
          questionNumber = "R8.1.4",
          answers = listOf(apiResponse.riskToTheIndividual.previousConcernsSelfHarmSuicide)
        ),
        OASysQuestion(
          label = "Current concerns about Coping in Custody or Hostel",
          questionNumber = "R8.2.1",
          answers = listOf(apiResponse.riskToTheIndividual.currentCustodyHostelCoping)
        ),
        OASysQuestion(
          label = "Previous concerns about Coping in Custody or Hostel",
          questionNumber = "R8.2.2",
          answers = listOf(apiResponse.riskToTheIndividual.previousCustodyHostelCoping)
        ),
        OASysQuestion(
          label = "Current concerns about Vulnerability",
          questionNumber = "R8.3.1",
          answers = listOf(apiResponse.riskToTheIndividual.currentVulnerability)
        ),
        OASysQuestion(
          label = "Previous concerns about Vulnerability",
          questionNumber = "R8.3.2",
          answers = listOf(apiResponse.riskToTheIndividual.previousVulnerability)
        ),
        OASysQuestion(
          label = "Risk of serious harm",
          questionNumber = "R8.4.1",
          answers = listOf(apiResponse.riskToTheIndividual.riskOfSeriousHarm)
        ),
        OASysQuestion(
          label = "Breach of trust (current): describe circumstances, relevant issues and needs",
          questionNumber = "R9.3.1",
          answers = listOf(apiResponse.riskToTheIndividual.currentConcernsBreachOfTrustText)
        )
      )
    )
  }

  @Test
  fun `transformToApi transforms correctly when complete`() {
    val completedAt = OffsetDateTime.now()

    val apiResponse = RiskToTheIndividualFactory().apply {
      withAssessmentId(34853487)
      withDateCompleted(completedAt)
      withCurrentConcernsSelfHarmSuicide("currentConcernsSelfHarmSuicide")
      withPreviousConcernsSelfHarmSuicide("previousConcernsSelfHarmSuicide")
      withCurrentCustodyHostelCoping("currentCustodyHostelCoping")
      withPreviousCustodyHostelCoping("previousCustodyHostelCoping")
      withCurrentVulnerability("currentVulnerability")
      withPreviousVulnerability("previousVulnerability")
      withRiskOfSeriousHarm("riskOfSeriousHarm")
      withCurrentConcernsBreachOfTrustText("currentConcernsBreachOfTrustText")
    }.produce()

    val result = riskToTheIndividualTransformer.transformToApi(apiResponse)

    assertThat(result.assessmentId).isEqualTo(apiResponse.assessmentId)
    assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.completed)
    assertThat(result.dateStarted).isEqualTo(apiResponse.initiationDate)
    assertThat(result.dateCompleted).isEqualTo(apiResponse.dateCompleted)
    assertThat(result.risksToTheIndividual).containsAll(
      listOf(
        OASysQuestion(
          label = "Current concerns about self-harm or suicide",
          questionNumber = "R8.1.1",
          answers = listOf(apiResponse.riskToTheIndividual.currentConcernsSelfHarmSuicide)
        ),
        OASysQuestion(
          label = "Previous concerns about self-harm or suicide",
          questionNumber = "R8.1.4",
          answers = listOf(apiResponse.riskToTheIndividual.previousConcernsSelfHarmSuicide)
        ),
        OASysQuestion(
          label = "Current concerns about Coping in Custody or Hostel",
          questionNumber = "R8.2.1",
          answers = listOf(apiResponse.riskToTheIndividual.currentCustodyHostelCoping)
        ),
        OASysQuestion(
          label = "Previous concerns about Coping in Custody or Hostel",
          questionNumber = "R8.2.2",
          answers = listOf(apiResponse.riskToTheIndividual.previousCustodyHostelCoping)
        ),
        OASysQuestion(
          label = "Current concerns about Vulnerability",
          questionNumber = "R8.3.1",
          answers = listOf(apiResponse.riskToTheIndividual.currentVulnerability)
        ),
        OASysQuestion(
          label = "Previous concerns about Vulnerability",
          questionNumber = "R8.3.2",
          answers = listOf(apiResponse.riskToTheIndividual.previousVulnerability)
        ),
        OASysQuestion(
          label = "Risk of serious harm",
          questionNumber = "R8.4.1",
          answers = listOf(apiResponse.riskToTheIndividual.riskOfSeriousHarm)
        ),
        OASysQuestion(
          label = "Breach of trust (current): describe circumstances, relevant issues and needs",
          questionNumber = "R9.3.1",
          answers = listOf(apiResponse.riskToTheIndividual.currentConcernsBreachOfTrustText)
        )
      )
    )
  }
}
