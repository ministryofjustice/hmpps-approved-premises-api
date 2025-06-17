package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.transformer

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2OAsysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory

@ExtendWith(MockKExtension::class)
class Cas2OAsysSectionsTransformerTest {

  @InjectMockKs
  lateinit var transformer: Cas2OAsysSectionsTransformer

  @Nested
  inner class RiskToIndividual {

    @Test
    fun `transforms correctly`() {
      val offenceDetailsApiResponse = OffenceDetailsFactory().produce()

      val risksToTheIndividualApiResponse = RiskToTheIndividualFactory().apply {
        withCurrentConcernsSelfHarmSuicide("currentConcernsSelfHarmSuicideAnswer")
        withPreviousConcernsSelfHarmSuicide("previousConcernsSelfHarmSuicideAnswer")
        withCurrentCustodyHostelCoping("currentCustodyHostelCopingAnswer")
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
          label = "Current concerns about Coping in Custody or Hostel",
          questionNumber = "R8.2.1",
          answer = "currentCustodyHostelCopingAnswer",
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
      val offenceDetailsApiResponse = OffenceDetailsFactory().produce()

      val risksToTheIndividualApiResponse = RiskToTheIndividualFactory().apply {
        withCurrentConcernsSelfHarmSuicide(null)
        withPreviousConcernsSelfHarmSuicide(null)
        withCurrentCustodyHostelCoping(null)
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
          label = "Current concerns about Coping in Custody or Hostel",
          questionNumber = "R8.2.1",
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
  inner class RiskOfSeriousHarm {

    @Test
    fun `transforms correctly`() {
      val offenceDetailsApiResponse = OffenceDetailsFactory().produce()

      val roshApiResponse = RoshSummaryFactory().apply {
        withWhoAtRisk("whoIsAtRiskAnswer")
        withNatureOfRisk("natureOfRiskAnswer")
        withRiskGreatest("riskGreatestAnswer")
        withRiskIncreaseLikelyTo("riskIncreaseLikelyToAnswer")
        withRiskReductionLikelyTo("riskReductionLikelyToAnswer")
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
        OASysQuestion(
          label = "When is the risk likely to be the greatest",
          questionNumber = "R10.3",
          answer = "riskGreatestAnswer",
        ),
        OASysQuestion(
          label = "What circumstances are likely to increase risk",
          questionNumber = "R10.4",
          answer = "riskIncreaseLikelyToAnswer",
        ),
        OASysQuestion(
          label = "What circumstances are likely to reduce the risk",
          questionNumber = "R10.5",
          answer = "riskReductionLikelyToAnswer",
        ),
      )
    }

    @Test
    fun `transforms correctly, no answers`() {
      val offenceDetailsApiResponse = OffenceDetailsFactory().produce()

      val roshApiResponse = RoshSummaryFactory().apply {
        withWhoAtRisk(null)
        withNatureOfRisk(null)
        withRiskGreatest(null)
        withRiskIncreaseLikelyTo(null)
        withRiskReductionLikelyTo(null)
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
        OASysQuestion(
          label = "When is the risk likely to be the greatest",
          questionNumber = "R10.3",
          answer = null,
        ),
        OASysQuestion(
          label = "What circumstances are likely to increase risk",
          questionNumber = "R10.4",
          answer = null,
        ),
        OASysQuestion(
          label = "What circumstances are likely to reduce the risk",
          questionNumber = "R10.5",
          answer = null,
        ),
      )
    }
  }
}
