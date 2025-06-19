package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.transformer

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2OAsysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService

@ExtendWith(MockKExtension::class)
class Cas2OAsysSectionsTransformerTest {

  @MockK
  lateinit var featureFlagService: FeatureFlagService

  @InjectMockKs
  lateinit var transformer: Cas2OAsysSectionsTransformer

  @Nested
  inner class RiskToIndividual {

    @Nested
    inner class FeatureFlagFalse {

      @Test
      fun `transforms correctly`() {
        every { featureFlagService.getBooleanFlag("cas2-oasys-use-new-questions") } returns false

        val offenceDetailsApiResponse = OffenceDetailsFactory().produce()

        val risksToTheIndividualApiResponse = RiskToTheIndividualFactory().apply {
          withCurrentConcernsSelfHarmSuicide("currentConcernsSelfHarmSuicideAnswer")
          withPreviousConcernsSelfHarmSuicide("previousConcernsSelfHarmSuicideAnswer")
          withCurrentVulnerability("currentVulnerabilityAnswer")
        }.produce()

        val result = transformer.transformRiskToIndividual(
          offenceDetailsApiResponse,
          risksToTheIndividualApiResponse,
        )
        assertThat(result.assessmentId).isEqualTo(offenceDetailsApiResponse.assessmentId)
        assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
        assertThat(result.dateStarted).isEqualTo(offenceDetailsApiResponse.initiationDate.toInstant())
        assertThat(result.dateCompleted).isEqualTo(offenceDetailsApiResponse.dateCompleted?.toInstant())

        assertThat(result.riskToSelf).containsExactly(
          OASysQuestion(
            label = "Current concerns about self-harm or suicide",
            questionNumber = "R8.1.1",
            answer = "currentConcernsSelfHarmSuicideAnswer",
          ),
          OASysQuestion(
            label = "Current concerns about Vulnerability",
            questionNumber = "R8.3.1",
            answer = "currentVulnerabilityAnswer",
          ),
          OASysQuestion(
            label = "Previous concerns about self-harm or suicide",
            questionNumber = "R8.1.4",
            answer = "previousConcernsSelfHarmSuicideAnswer",
          ),
        )
      }

      @Test
      fun `transforms correctly, no answers`() {
        every { featureFlagService.getBooleanFlag("cas2-oasys-use-new-questions") } returns false

        val offenceDetailsApiResponse = OffenceDetailsFactory().produce()
        val risksToTheIndividualApiResponse = RiskToTheIndividualFactory().apply {
          withCurrentConcernsSelfHarmSuicide(null)
          withPreviousConcernsSelfHarmSuicide(null)
          withCurrentVulnerability(null)
        }.produce()

        val result = transformer.transformRiskToIndividual(
          offenceDetailsApiResponse,
          risksToTheIndividualApiResponse,
        )
        assertThat(result.assessmentId).isEqualTo(offenceDetailsApiResponse.assessmentId)
        assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
        assertThat(result.dateStarted).isEqualTo(offenceDetailsApiResponse.initiationDate.toInstant())
        assertThat(result.dateCompleted).isEqualTo(offenceDetailsApiResponse.dateCompleted?.toInstant())

        assertThat(result.riskToSelf).containsExactly(
          OASysQuestion(
            label = "Current concerns about self-harm or suicide",
            questionNumber = "R8.1.1",
            answer = null,
          ),
          OASysQuestion(
            label = "Current concerns about Vulnerability",
            questionNumber = "R8.3.1",
            answer = null,
          ),
          OASysQuestion(
            label = "Previous concerns about self-harm or suicide",
            questionNumber = "R8.1.4",
            answer = null,
          ),
        )
      }
    }

    @Nested
    inner class FeatureFlagTrue {
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
        every { featureFlagService.getBooleanFlag("cas2-oasys-use-new-questions") } returns true

        val offenceDetailsApiResponse = OffenceDetailsFactory().produce()

        val risksToTheIndividualApiResponse = RiskToTheIndividualFactory().apply {
          withCurrentConcernsSelfHarmSuicide(currentConcernsSelfHarmSuicideAnswer)
          withPreviousConcernsSelfHarmSuicide(previousConcernsSelfHarmSuicideAnswer)
          withCurrentVulnerability("currentVulnerabilityAnswer")
        }.produce()

        val result = transformer.transformRiskToIndividual(
          offenceDetailsApiResponse,
          risksToTheIndividualApiResponse,
        )

        assertThat(result.assessmentId).isEqualTo(offenceDetailsApiResponse.assessmentId)
        assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
        assertThat(result.dateStarted).isEqualTo(offenceDetailsApiResponse.initiationDate.toInstant())
        assertThat(result.dateCompleted).isEqualTo(offenceDetailsApiResponse.dateCompleted?.toInstant())

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
        every { featureFlagService.getBooleanFlag("cas2-oasys-use-new-questions") } returns true

        val offenceDetailsApiResponse = OffenceDetailsFactory().produce()

        val risksToTheIndividualApiResponse = RiskToTheIndividualFactory().apply {
          withCurrentConcernsSelfHarmSuicide(null)
          withCurrentVulnerability(null)
          withPreviousConcernsSelfHarmSuicide(null)
          withAnalysisSuicideSelfharm("analysisSuicideSelfHarmAnswer")
          withAnalysisVulnerabilities("analysisVulnerabilitiesAnswer")
        }.produce()

        val result = transformer.transformRiskToIndividual(
          offenceDetailsApiResponse,
          risksToTheIndividualApiResponse,
        )

        assertThat(result.assessmentId).isEqualTo(offenceDetailsApiResponse.assessmentId)
        assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
        assertThat(result.dateStarted).isEqualTo(offenceDetailsApiResponse.initiationDate.toInstant())
        assertThat(result.dateCompleted).isEqualTo(offenceDetailsApiResponse.dateCompleted?.toInstant())

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
        every { featureFlagService.getBooleanFlag("cas2-oasys-use-new-questions") } returns true

        val offenceDetailsApiResponse = OffenceDetailsFactory().produce()

        val risksToTheIndividualApiResponse = RiskToTheIndividualFactory().apply {
          withCurrentConcernsSelfHarmSuicide(null)
          withPreviousConcernsSelfHarmSuicide(null)
          withCurrentVulnerability(null)
        }.produce()

        val result = transformer.transformRiskToIndividual(
          offenceDetailsApiResponse,
          risksToTheIndividualApiResponse,
        )

        assertThat(result.assessmentId).isEqualTo(offenceDetailsApiResponse.assessmentId)
        assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
        assertThat(result.dateStarted).isEqualTo(offenceDetailsApiResponse.initiationDate.toInstant())
        assertThat(result.dateCompleted).isEqualTo(offenceDetailsApiResponse.dateCompleted?.toInstant())

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
  }

  @Nested
  inner class RiskOfSeriousHarm {

    @Test
    fun `transforms correctly`() {
      val offenceDetailsApiResponse = OffenceDetailsFactory().produce()

      val roshApiResponse = RoshSummaryFactory().apply {
        withWhoAtRisk("whoIsAtRiskAnswer")
        withNatureOfRisk("natureOfRiskAnswer")
      }.produce()

      val result = transformer.transformRiskOfSeriousHarm(
        offenceDetailsApiResponse,
        roshApiResponse,
      )

      assertThat(result.assessmentId).isEqualTo(offenceDetailsApiResponse.assessmentId)
      assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
      assertThat(result.dateStarted).isEqualTo(offenceDetailsApiResponse.initiationDate.toInstant())
      assertThat(result.dateCompleted).isEqualTo(offenceDetailsApiResponse.dateCompleted?.toInstant())

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
      val offenceDetailsApiResponse = OffenceDetailsFactory().produce()

      val roshApiResponse = RoshSummaryFactory().apply {
        withWhoAtRisk(null)
        withNatureOfRisk(null)
      }.produce()

      val result = transformer.transformRiskOfSeriousHarm(
        offenceDetailsApiResponse,
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
