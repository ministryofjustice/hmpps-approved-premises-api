package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.unit.transformer

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcOAsysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RisksToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory

@ExtendWith(MockKExtension::class)
class Cas2OAsysSectionsTransformerTest {

  @InjectMockKs
  lateinit var transformer: Cas2HdcOAsysSectionsTransformer

  @Nested
  inner class RiskToIndividual {
    @ParameterizedTest
    @CsvSource(
      nullValues = [ "null" ],
      value = [
        "currentConcernsSelfHarmSuicideAnswer,previousConcernsSelfHarmSuicideAnswer,'previousConcernsSelfHarmSuicideAnswer\n\ncurrentConcernsSelfHarmSuicideAnswer'",
        "currentConcernsSelfHarmSuicideAnswer,null,currentConcernsSelfHarmSuicideAnswer",
        "null,previousConcernsSelfHarmSuicideAnswer,previousConcernsSelfHarmSuicideAnswer",
      ],
    )
    fun `transforms correctly, pre NOD 1057 assessment`(
      currentConcernsSelfHarmSuicideAnswer: String?,
      previousConcernsSelfHarmSuicideAnswer: String?,
      combinedAnswer: String,
    ) {
      val risksToTheIndividualApiResponse = RisksToTheIndividualFactory().apply {
        withCurrentConcernsSelfHarmSuicide(currentConcernsSelfHarmSuicideAnswer)
        withPreviousConcernsSelfHarmSuicide(previousConcernsSelfHarmSuicideAnswer)
        withCurrentVulnerability("currentVulnerabilityAnswer")
      }.produce()

      val result = transformer.transformRiskToIndividual(risksToTheIndividualApiResponse)

      assertThat(result.assessmentId).isEqualTo(risksToTheIndividualApiResponse.assessmentId)
      assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
      assertThat(result.dateStarted).isEqualTo(risksToTheIndividualApiResponse.initiationDate.toInstant())
      assertThat(result.dateCompleted).isEqualTo(risksToTheIndividualApiResponse.dateCompleted?.toInstant())

      assertThat(result.riskToSelf).containsExactly(
        OASysQuestion(
          label = "Analysis of current or previous self-harm and/or suicide concerns",
          questionNumber = "FA62",
          answer = combinedAnswer,
        ),
        OASysQuestion(
          label = "Current concerns about Vulnerability",
          questionNumber = "R8.3.1",
          answer = "currentVulnerabilityAnswer",
        ),
      )
    }

    @Test
    fun `transforms correctly, post NOD 1057 assessment`() {
      val risksToTheIndividualApiResponse = RisksToTheIndividualFactory().apply {
        withCurrentConcernsSelfHarmSuicide(null)
        withCurrentVulnerability(null)
        withPreviousConcernsSelfHarmSuicide(null)
        withAnalysisSuicideSelfharm("analysisSuicideSelfHarmAnswer")
        withAnalysisVulnerabilities("analysisVulnerabilitiesAnswer")
      }.produce()

      val result = transformer.transformRiskToIndividual(risksToTheIndividualApiResponse)

      assertThat(result.assessmentId).isEqualTo(risksToTheIndividualApiResponse.assessmentId)
      assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
      assertThat(result.dateStarted).isEqualTo(risksToTheIndividualApiResponse.initiationDate.toInstant())
      assertThat(result.dateCompleted).isEqualTo(risksToTheIndividualApiResponse.dateCompleted?.toInstant())

      assertThat(result.riskToSelf).containsExactly(
        OASysQuestion(
          label = "Analysis of current or previous self-harm and/or suicide concerns",
          questionNumber = "FA62",
          answer = "analysisSuicideSelfHarmAnswer",
        ),
        OASysQuestion(
          label = "Current concerns about Vulnerability",
          questionNumber = "R8.3.1",
          answer = "analysisVulnerabilitiesAnswer",
        ),
      )
    }

    @Test
    fun `transforms correctly, no answers`() {
      val risksToTheIndividualApiResponse = RisksToTheIndividualFactory().apply {
        withCurrentConcernsSelfHarmSuicide(null)
        withPreviousConcernsSelfHarmSuicide(null)
        withCurrentVulnerability(null)
      }.produce()

      val result = transformer.transformRiskToIndividual(risksToTheIndividualApiResponse)

      assertThat(result.assessmentId).isEqualTo(risksToTheIndividualApiResponse.assessmentId)
      assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
      assertThat(result.dateStarted).isEqualTo(risksToTheIndividualApiResponse.initiationDate.toInstant())
      assertThat(result.dateCompleted).isEqualTo(risksToTheIndividualApiResponse.dateCompleted?.toInstant())

      assertThat(result.riskToSelf).containsExactly(
        OASysQuestion(
          label = "Analysis of current or previous self-harm and/or suicide concerns",
          questionNumber = "FA62",
          answer = null,
        ),
        OASysQuestion(
          label = "Current concerns about Vulnerability",
          questionNumber = "R8.3.1",
          answer = null,
        ),
      )
    }
  }

  @Nested
  inner class RiskOfSeriousHarm {

    @Test
    fun `transforms correctly`() {
      val roshApiResponse = RoshSummaryFactory().apply {
        withWhoAtRisk("whoIsAtRiskAnswer")
        withNatureOfRisk("natureOfRiskAnswer")
      }.produce()

      val result = transformer.transformRiskOfSeriousHarm(
        roshApiResponse,
      )

      assertThat(result.assessmentId).isEqualTo(roshApiResponse.assessmentId)
      assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
      assertThat(result.dateStarted).isEqualTo(roshApiResponse.initiationDate.toInstant())
      assertThat(result.dateCompleted).isEqualTo(roshApiResponse.dateCompleted?.toInstant())

      assertThat(result.rosh).containsExactly(
        OASysQuestion(
          label = "Who is at risk",
          questionNumber = "R10.1",
          answer = "whoIsAtRiskAnswer",
        ),
        OASysQuestion(
          label = "What is the nature of the risk",
          questionNumber = "R10.2",
          answer = "natureOfRiskAnswer",
        ),
      )
    }

    @Test
    fun `transforms correctly, no answers`() {
      val roshApiResponse = RoshSummaryFactory().apply {
        withWhoAtRisk(null)
        withNatureOfRisk(null)
      }.produce()

      val result = transformer.transformRiskOfSeriousHarm(
        roshApiResponse,
      )

      assertThat(result.rosh).containsExactly(
        OASysQuestion(
          label = "Who is at risk",
          questionNumber = "R10.1",
          answer = null,
        ),
        OASysQuestion(
          label = "What is the nature of the risk",
          questionNumber = "R10.2",
          answer = null,
        ),
      )
    }
  }
}
