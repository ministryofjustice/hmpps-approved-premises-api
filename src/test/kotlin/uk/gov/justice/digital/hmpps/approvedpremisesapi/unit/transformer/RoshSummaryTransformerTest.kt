package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RoshSummaryTransformer
import java.time.OffsetDateTime

class RoshSummaryTransformerTest {
  private val roshSummaryTransformer = RoshSummaryTransformer()

  @Test
  fun `transformToApi transforms correctly when incomplete`() {
    val apiResponse = RoshSummaryFactory().apply {
      withAssessmentId(34853487)
      withDateCompleted(null)
      withWhoAtRisk("Who is at risk")
      withNatureOfRisk("What is the nature of the risk")
      withRiskGreatest("When is the risk likely to be the greatest")
      withRiskIncreaseLikelyTo("What circumstances are likely to increase risk")
      withRiskReductionLikelyTo("Reduction Likely To")
    }.produce()

    val result = roshSummaryTransformer.transformToApi(apiResponse)

    assertThat(result.assessmentId).isEqualTo(apiResponse.assessmentId)
    assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
    assertThat(result.dateStarted).isEqualTo(apiResponse.initiationDate)
    assertThat(result.dateCompleted).isEqualTo(apiResponse.dateCompleted)
    assertThat(result.riskOfSeriousHarmSummary).containsAll(
      listOf(
        OASysQuestion(
          label = "Who is at risk",
          questionNumber = "R10.1",
          answers = listOf(apiResponse.roshSummary.whoAtRisk)
        ),
        OASysQuestion(
          label = "What is the nature of the risk",
          questionNumber = "R10.2",
          answers = listOf(apiResponse.roshSummary.natureOfRisk)
        ),
        OASysQuestion(
          label = "When is the risk likely to be the greatest",
          questionNumber = "R10.3",
          answers = listOf(apiResponse.roshSummary.riskGreatest)
        ),
        OASysQuestion(
          label = "What circumstances are likely to increase risk",
          questionNumber = "R10.4",
          answers = listOf(apiResponse.roshSummary.riskIncreaseLikelyTo)
        ),
        OASysQuestion(
          label = "What circumstances are likely to reduce the risk",
          questionNumber = "R10.5",
          answers = listOf(apiResponse.roshSummary.riskReductionLikelyTo)
        )
      )
    )
  }

  @Test
  fun `transformToApi transforms correctly when complete`() {
    val completedAt = OffsetDateTime.now()

    val apiResponse = RoshSummaryFactory().apply {
      withAssessmentId(34853487)
      withDateCompleted(completedAt)
      withWhoAtRisk("Who is at risk")
      withNatureOfRisk("What is the nature of the risk")
      withRiskGreatest("When is the risk likely to be the greatest")
      withRiskIncreaseLikelyTo("What circumstances are likely to increase risk")
      withRiskReductionLikelyTo("Reduction Likely To")
    }.produce()

    val result = roshSummaryTransformer.transformToApi(apiResponse)

    assertThat(result.assessmentId).isEqualTo(apiResponse.assessmentId)
    assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.completed)
    assertThat(result.dateStarted).isEqualTo(apiResponse.initiationDate)
    assertThat(result.dateCompleted).isEqualTo(apiResponse.dateCompleted)
    assertThat(result.riskOfSeriousHarmSummary).containsAll(
      listOf(
        OASysQuestion(
          label = "Who is at risk",
          questionNumber = "R10.1",
          answers = listOf(apiResponse.roshSummary.whoAtRisk)
        ),
        OASysQuestion(
          label = "What is the nature of the risk",
          questionNumber = "R10.2",
          answers = listOf(apiResponse.roshSummary.natureOfRisk)
        ),
        OASysQuestion(
          label = "When is the risk likely to be the greatest",
          questionNumber = "R10.3",
          answers = listOf(apiResponse.roshSummary.riskGreatest)
        ),
        OASysQuestion(
          label = "What circumstances are likely to increase risk",
          questionNumber = "R10.4",
          answers = listOf(apiResponse.roshSummary.riskIncreaseLikelyTo)
        ),
        OASysQuestion(
          label = "What circumstances are likely to reduce the risk",
          questionNumber = "R10.5",
          answers = listOf(apiResponse.roshSummary.riskReductionLikelyTo)
        )
      )
    )
  }
}
