package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OffenceAnalysisTransformer
import java.time.OffsetDateTime

class OffenceAnalysisTransformerTest {
  private val offenceAnalysisTransformer = OffenceAnalysisTransformer()

  @Test
  fun `transformToApi transforms correctly when incomplete`() {
    val apiResponse = OffenceDetailsFactory().apply {
      withAssessmentId(34853487)
      withDateCompleted(null)
      withOffenceAnalysis("Offence Analysis")
      withOthersInvolved("Others Involved")
      withIssueContributingToRisk("Issue Contributing to Risk")
      withOffenceMotivation("Offence Motivation")
      withVictimImpact("Victim Impact")
      withVictimPerpetratorRel("Victim Perpetrator Rel")
      withVictimInfo("Victim Info")
      withPatternOffending("Pattern Reoffending")
      withAcceptsResponsibility("Accepts Responsibility")
    }.produce()

    val result = offenceAnalysisTransformer.transformToApi(apiResponse)

    assertThat(result.assessmentId).isEqualTo(apiResponse.assessmentId)
    assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
    assertThat(result.dateStarted).isEqualTo(apiResponse.initiationDate)
    assertThat(result.dateCompleted).isEqualTo(apiResponse.dateCompleted)
    assertThat(result.offenceAnalysis).containsAll(
      listOf(
        OASysQuestion(
          label = "Offence Analysis",
          questionNumber = "2.1",
          answers = listOf(apiResponse.offenceDetails.offenceAnalysis)
        ),
        OASysQuestion(
          label = "Others Involved",
          questionNumber = "2.7.3",
          answers = listOf(apiResponse.offenceDetails.othersInvolved)
        ),
        OASysQuestion(
          label = "Issue Contributing to Risk",
          questionNumber = "2.98",
          answers = listOf(apiResponse.offenceDetails.issueContributingToRisk)
        ),
        OASysQuestion(
          label = "Offence Motivation",
          questionNumber = "2.8.3",
          answers = listOf(apiResponse.offenceDetails.offenceMotivation)
        ),
        OASysQuestion(
          label = "Victim Impact",
          questionNumber = "2.5",
          answers = listOf(apiResponse.offenceDetails.victimImpact)
        ),
        OASysQuestion(
          label = "Victim Perpetrator Rel",
          questionNumber = "2.4.2",
          answers = listOf(apiResponse.offenceDetails.victimPerpetratorRel)
        ),
        OASysQuestion(
          label = "Victim Info",
          questionNumber = "2.4.1",
          answers = listOf(apiResponse.offenceDetails.victimInfo)
        ),
        OASysQuestion(
          label = "Pattern Offending",
          questionNumber = "2.12",
          answers = listOf(apiResponse.offenceDetails.patternOffending)
        ),
        OASysQuestion(
          label = "Accepts Responsibility",
          questionNumber = "2.11.t",
          answers = listOf(apiResponse.offenceDetails.acceptsResponsibility)
        )
      )
    )
  }

  @Test
  fun `transformToApi transforms correctly when complete`() {
    val completedAt = OffsetDateTime.now()

    val apiResponse = OffenceDetailsFactory().apply {
      withAssessmentId(34853487)
      withDateCompleted(completedAt)
      withOffenceAnalysis("Offence Analysis")
      withOthersInvolved("Others Involved")
      withIssueContributingToRisk("Issue Contributing to Risk")
      withOffenceMotivation("Offence Motivation")
      withVictimImpact("Victim Impact")
      withVictimPerpetratorRel("Victim Perpetrator Rel")
      withVictimInfo("Victim Info")
      withPatternOffending("Pattern Reoffending")
      withAcceptsResponsibility("Accepts Responsibility")
    }.produce()

    val result = offenceAnalysisTransformer.transformToApi(apiResponse)

    assertThat(result.assessmentId).isEqualTo(apiResponse.assessmentId)
    assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.completed)
    assertThat(result.dateStarted).isEqualTo(apiResponse.initiationDate)
    assertThat(result.dateCompleted).isEqualTo(apiResponse.dateCompleted)
    assertThat(result.offenceAnalysis).containsAll(
      listOf(
        OASysQuestion(
          label = "Offence Analysis",
          questionNumber = "2.1",
          answers = listOf(apiResponse.offenceDetails.offenceAnalysis)
        ),
        OASysQuestion(
          label = "Others Involved",
          questionNumber = "2.7.3",
          answers = listOf(apiResponse.offenceDetails.othersInvolved)
        ),
        OASysQuestion(
          label = "Issue Contributing to Risk",
          questionNumber = "2.98",
          answers = listOf(apiResponse.offenceDetails.issueContributingToRisk)
        ),
        OASysQuestion(
          label = "Offence Motivation",
          questionNumber = "2.8.3",
          answers = listOf(apiResponse.offenceDetails.offenceMotivation)
        ),
        OASysQuestion(
          label = "Victim Impact",
          questionNumber = "2.5",
          answers = listOf(apiResponse.offenceDetails.victimImpact)
        ),
        OASysQuestion(
          label = "Victim Perpetrator Rel",
          questionNumber = "2.4.2",
          answers = listOf(apiResponse.offenceDetails.victimPerpetratorRel)
        ),
        OASysQuestion(
          label = "Victim Info",
          questionNumber = "2.4.1",
          answers = listOf(apiResponse.offenceDetails.victimInfo)
        ),
        OASysQuestion(
          label = "Pattern Offending",
          questionNumber = "2.12",
          answers = listOf(apiResponse.offenceDetails.patternOffending)
        ),
        OASysQuestion(
          label = "Accepts Responsibility",
          questionNumber = "2.11.t",
          answers = listOf(apiResponse.offenceDetails.acceptsResponsibility)
        )
      )
    )
  }
}
