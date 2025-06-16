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
      val offenceDetailsApiResponse = OffenceDetailsFactory().apply {
        withAssessmentId(34853487)
        withDateCompleted(null)
        withOffenceAnalysis("Offence Analysis")
        withOthersInvolved("Others Involved")
        withIssueContributingToRisk("Issue Contributing to Risk")
        withOffenceMotivation("Offence Motivation")
        withVictimImpact("Impact on the victim")
        withVictimPerpetratorRel("Other victim information")
        withVictimInfo("Victim Info")
        withPatternOffending("Pattern Reoffending")
        withAcceptsResponsibility("Accepts Responsibility")
      }.produce()

      val risksToTheIndividualApiResponse = RiskToTheIndividualFactory().apply {
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
          answer = risksToTheIndividualApiResponse.riskToTheIndividual?.currentConcernsSelfHarmSuicide,
        ),
        OASysQuestion(
          label = "Current concerns about Coping in Custody or Hostel",
          questionNumber = "R8.2.1",
          answer = risksToTheIndividualApiResponse.riskToTheIndividual?.currentCustodyHostelCoping,
        ),
        OASysQuestion(
          label = "Current concerns about Vulnerability",
          questionNumber = "R8.3.1",
          answer = risksToTheIndividualApiResponse.riskToTheIndividual?.currentVulnerability,
        ),
        OASysQuestion(
          label = "Previous concerns about self-harm or suicide",
          questionNumber = "R8.1.4",
          answer = risksToTheIndividualApiResponse.riskToTheIndividual?.previousConcernsSelfHarmSuicide,
        ),
      )
    }
  }

  @Nested
  inner class RiskOfSeriousHarm {

    @Test
    fun `transforms correctly`() {
      val offenceDetailsApiResponse = OffenceDetailsFactory().apply {
        withAssessmentId(34853487)
        withDateCompleted(null)
        withOffenceAnalysis("Offence Analysis")
        withOthersInvolved("Others Involved")
        withIssueContributingToRisk("Issue Contributing to Risk")
        withOffenceMotivation("Offence Motivation")
        withVictimImpact("Impact on the victim")
        withVictimPerpetratorRel("Other victim information")
        withVictimInfo("Victim Info")
        withPatternOffending("Pattern Reoffending")
        withAcceptsResponsibility("Accepts Responsibility")
      }.produce()

      val roshApiResponse = RoshSummaryFactory().apply {
        withAssessmentId(34853487)
        withDateCompleted(null)
        withWhoAtRisk("whoIsAtRisk")
        withNatureOfRisk("natureOfRisk")
        withRiskGreatest("riskGreatest")
        withRiskIncreaseLikelyTo("riskIncreaseLikelyTo")
        withRiskReductionLikelyTo("riskReductionLikelyTo")
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
          answer = roshApiResponse.roshSummary?.whoIsAtRisk,
        ),
        OASysQuestion(
          label = "What is the nature of the risk",
          questionNumber = "R10.2",
          answer = roshApiResponse.roshSummary?.natureOfRisk,
        ),
        OASysQuestion(
          label = "When is the risk likely to be the greatest",
          questionNumber = "R10.3",
          answer = roshApiResponse.roshSummary?.riskGreatest,
        ),
        OASysQuestion(
          label = "What circumstances are likely to increase risk",
          questionNumber = "R10.4",
          answer = roshApiResponse.roshSummary?.riskIncreaseLikelyTo,
        ),
        OASysQuestion(
          label = "What circumstances are likely to reduce the risk",
          questionNumber = "R10.5",
          answer = roshApiResponse.roshSummary?.riskReductionLikelyTo,
        ),
      )
    }
  }
}
