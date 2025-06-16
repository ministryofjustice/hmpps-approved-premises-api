package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSupportingInformationQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskManagementPlanFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer

@ExtendWith(MockKExtension::class)
class OASysSectionsTransformerTest {

  @MockK
  lateinit var featureFlagService: FeatureFlagService

  @InjectMockKs
  lateinit var oaSysSectionsTransformer: OASysSectionsTransformer

  @Nested
  inner class OffenceDetailsAnswers {

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

      val result = oaSysSectionsTransformer.offenceDetailsAnswers(offenceDetailsApiResponse.offence)

      assertThat(result).containsExactly(
        OASysQuestion(
          label = "Offence analysis",
          questionNumber = "2.1",
          answer = offenceDetailsApiResponse.offence?.offenceAnalysis,
        ),
        OASysQuestion(
          label = "Victim - perpetrator relationship",
          questionNumber = "2.4.1",
          answer = offenceDetailsApiResponse.offence?.victimPerpetratorRel,
        ),
        OASysQuestion(
          label = "Other victim information",
          questionNumber = "2.4.2",
          answer = offenceDetailsApiResponse.offence?.victimInfo,
        ),
        OASysQuestion(
          label = "Impact on the victim",
          questionNumber = "2.5",
          answer = offenceDetailsApiResponse.offence?.victimImpact,
        ),
        OASysQuestion(
          label = "Motivation and triggers",
          questionNumber = "2.8.3",
          answer = offenceDetailsApiResponse.offence?.offenceMotivation,
        ),
        OASysQuestion(
          label = "Issues contributing to risks",
          questionNumber = "2.98",
          answer = offenceDetailsApiResponse.offence?.issueContributingToRisk,
        ),
        OASysQuestion(
          label = "Pattern of offending",
          questionNumber = "2.12",
          answer = offenceDetailsApiResponse.offence?.patternOffending,
        ),
      )
    }

    @Test
    fun `return empty questions if no assessment available`() {
      val result = oaSysSectionsTransformer.offenceDetailsAnswers(null)

      assertThat(result).containsExactly(
        OASysQuestion(
          label = "Offence analysis",
          questionNumber = "2.1",
          answer = null,
        ),
        OASysQuestion(
          label = "Victim - perpetrator relationship",
          questionNumber = "2.4.1",
          answer = null,
        ),
        OASysQuestion(
          label = "Other victim information",
          questionNumber = "2.4.2",
          answer = null,
        ),
        OASysQuestion(
          label = "Impact on the victim",
          questionNumber = "2.5",
          answer = null,
        ),
        OASysQuestion(
          label = "Motivation and triggers",
          questionNumber = "2.8.3",
          answer = null,
        ),
        OASysQuestion(
          label = "Issues contributing to risks",
          questionNumber = "2.98",
          answer = null,
        ),
        OASysQuestion(
          label = "Pattern of offending",
          questionNumber = "2.12",
          answer = null,
        ),
      )
    }
  }

  @Nested
  inner class RiskManagementAnswers {

    @Test
    fun `transforms correctly`() {
      val riskManagementPlanApiResponse = RiskManagementPlanFactory().apply {
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

      val result = oaSysSectionsTransformer.riskManagementPlanAnswers(riskManagementPlanApiResponse.riskManagementPlan)

      assertThat(result).containsExactly(
        OASysQuestion(
          label = "Further considerations",
          questionNumber = "RM28",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.furtherConsiderations,
        ),
        OASysQuestion(
          label = "Additional comments",
          questionNumber = "RM35",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.additionalComments,
        ),
        OASysQuestion(
          label = "Contingency plans",
          questionNumber = "RM34",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.contingencyPlans,
        ),
        OASysQuestion(
          label = "Victim safety planning",
          questionNumber = "RM33",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.victimSafetyPlanning,
        ),
        OASysQuestion(
          label = "Interventions and treatment",
          questionNumber = "RM32",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.interventionsAndTreatment,
        ),
        OASysQuestion(
          label = "Monitoring and control",
          questionNumber = "RM31",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.monitoringAndControl,
        ),
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.supervision,
        ),
        OASysQuestion(
          label = "Key information about current situation",
          questionNumber = "RM28.1",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.keyInformationAboutCurrentSituation,
        ),
      )
    }

    @Test
    fun `return empty questions if no assessment available`() {
      val result = oaSysSectionsTransformer.riskManagementPlanAnswers(null)

      assertThat(result).containsExactly(
        OASysQuestion(
          label = "Further considerations",
          questionNumber = "RM28",
          answer = null,
        ),
        OASysQuestion(
          label = "Additional comments",
          questionNumber = "RM35",
          answer = null,
        ),
        OASysQuestion(
          label = "Contingency plans",
          questionNumber = "RM34",
          answer = null,
        ),
        OASysQuestion(
          label = "Victim safety planning",
          questionNumber = "RM33",
          answer = null,
        ),
        OASysQuestion(
          label = "Interventions and treatment",
          questionNumber = "RM32",
          answer = null,
        ),
        OASysQuestion(
          label = "Monitoring and control",
          questionNumber = "RM31",
          answer = null,
        ),
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = null,
        ),
        OASysQuestion(
          label = "Key information about current situation",
          questionNumber = "RM28.1",
          answer = null,
        ),
      )
    }
  }

  @Nested
  inner class RiskToSelfAnswers {

    @Test
    fun `transforms correctly, post NOD 1057 assessment, use new question feature flag on`() {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns true

      val risksToTheIndividualApiResponse = RiskToTheIndividualFactory().apply {
        withCurrentConcernsSelfHarmSuicide(null)
        withCurrentCustodyHostelCoping(null)
        withCurrentVulnerability(null)
        withPreviousConcernsSelfHarmSuicide(null)
        withAnalysisSuicideSelfharm("analysisSuicideSelfHarmAnswer")
        withAnalysisCoping("analysisCopingAnswer")
        withAnalysisVulnerabilities("analysisVulnerabilitiesAnswer")
      }.produce()

      val result = oaSysSectionsTransformer.riskToSelfAnswers(risksToTheIndividualApiResponse.riskToTheIndividual)

      assertThat(result).containsExactly(
        OASysQuestion(
          label = "Analysis of current or previous self-harm and/or suicide concerns",
          questionNumber = "FA62",
          answer = "analysisSuicideSelfHarmAnswer",
        ),
        OASysQuestion(
          label = "Coping in custody / approved premises / hostel / secure hospital",
          questionNumber = "FA63",
          answer = "analysisCopingAnswer",
        ),
        OASysQuestion(
          label = "Analysis of vulnerabilities",
          questionNumber = "FA64",
          answer = "analysisVulnerabilitiesAnswer",
        ),
      )
    }

    @Test
    fun `transforms correctly, post NOD 1057 assessment, feature flag off`() {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns false

      val risksToTheIndividualApiResponse = RiskToTheIndividualFactory().apply {
        withCurrentConcernsSelfHarmSuicide(null)
        withCurrentCustodyHostelCoping(null)
        withCurrentVulnerability(null)
        withAnalysisSuicideSelfharm("analysisSuicideSelfHarmAnswer")
        withAnalysisCoping("analysisCopingAnswer")
        withAnalysisVulnerabilities("analysisVulnerabilitiesAnswer")
      }.produce()

      val result = oaSysSectionsTransformer.riskToSelfAnswers(risksToTheIndividualApiResponse.riskToTheIndividual)

      assertThat(result).containsExactly(
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
      )
    }

    @Test
    fun `transforms correctly, pre NOD 1057 assessment`() {
      val risksToTheIndividualApiResponse = RiskToTheIndividualFactory().apply {
        withCurrentConcernsSelfHarmSuicide("currentConcernsSelfHarmSuicide")
        withCurrentCustodyHostelCoping("currentCustodyHostelCoping")
        withCurrentVulnerability("currentVulnerability")
      }.produce()

      val result = oaSysSectionsTransformer.riskToSelfAnswers(risksToTheIndividualApiResponse.riskToTheIndividual)

      assertThat(result).containsExactly(
        OASysQuestion(
          label = "Current concerns about self-harm or suicide",
          questionNumber = "R8.1.1",
          answer = "currentConcernsSelfHarmSuicide",
        ),
        OASysQuestion(
          label = "Current concerns about Coping in Custody or Hostel",
          questionNumber = "R8.2.1",
          answer = "currentCustodyHostelCoping",
        ),
        OASysQuestion(
          label = "Current concerns about Vulnerability",
          questionNumber = "R8.3.1",
          answer = "currentVulnerability",
        ),
      )
    }

    @Test
    fun `return empty questions if no assessment available`() {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns true

      val result = oaSysSectionsTransformer.riskToSelfAnswers(null)

      assertThat(result).containsExactly(
        OASysQuestion(
          label = "Analysis of current or previous self-harm and/or suicide concerns",
          questionNumber = "FA62",
          answer = null,
        ),
        OASysQuestion(
          label = "Coping in custody / approved premises / hostel / secure hospital",
          questionNumber = "FA63",
          answer = null,
        ),
        OASysQuestion(
          label = "Analysis of vulnerabilities",
          questionNumber = "FA64",
          answer = null,
        ),
      )
    }
  }

  @Nested
  inner class RoshSummaryAnswers {

    @Test
    fun `transforms correctly, post NOD 1057 assessment, use new question feature flag on`() {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns true

      val roshSummaryApiResponse = RoshSummaryFactory().apply {
        withWhoAtRisk("Who is at risk answer")
        withNatureOfRisk("What is the nature of the risk answer")
        withRiskGreatest(null)
        withRiskIncreaseLikelyTo(null)
        withRiskReductionLikelyTo(null)
        withFactorsSituationsLikelyToOffend("likely to offend answers")
        withFactorsAnalysisOfRisk("analysis of risk answers")
        withFactorsStrengthsAndProtective("strengths and protective answers")
      }.produce()

      val result = oaSysSectionsTransformer.roshSummaryAnswers(roshSummaryApiResponse.roshSummary)

      assertThat(result).containsExactly(
        OASysQuestion(
          label = "Who is at risk",
          questionNumber = "R10.1",
          answer = "Who is at risk answer",
        ),
        OASysQuestion(
          label = "What is the nature of the risk",
          questionNumber = "R10.2",
          answer = "What is the nature of the risk answer",
        ),
        OASysQuestion(
          label = "Circumstances or situations where offending is most likely to occur",
          questionNumber = "SUM11",
          answer = "likely to offend answers",
        ),
        OASysQuestion(
          label = "Analysis of risk factors",
          questionNumber = "SUM9",
          answer = "analysis of risk answers",
        ),
        OASysQuestion(
          label = "Strengths and protective factors",
          questionNumber = "SUM10",
          answer = "strengths and protective answers",
        ),
      )
    }

    @Test
    fun `transforms correctly, post NOD 1057 assessment, use new question feature flag off`() {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns false

      val roshSummaryApiResponse = RoshSummaryFactory().apply {
        withWhoAtRisk("Who is at risk answer")
        withNatureOfRisk("What is the nature of the risk answer")
        withRiskGreatest(null)
        withRiskIncreaseLikelyTo(null)
        withRiskReductionLikelyTo(null)
        withFactorsSituationsLikelyToOffend("likely to offend answers")
        withFactorsAnalysisOfRisk("analysis of risk answers")
        withFactorsStrengthsAndProtective("strengths and protective answers")
      }.produce()

      val result = oaSysSectionsTransformer.roshSummaryAnswers(roshSummaryApiResponse.roshSummary)

      assertThat(result).containsExactly(
        OASysQuestion(
          label = "Who is at risk",
          questionNumber = "R10.1",
          answer = "Who is at risk answer",
        ),
        OASysQuestion(
          label = "What is the nature of the risk",
          questionNumber = "R10.2",
          answer = "What is the nature of the risk answer",
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

    @Test
    fun `transforms correctly, pre NOD 1057 assessment`() {
      val roshSummaryApiResponse = RoshSummaryFactory().apply {
        withAssessmentId(34853487)
        withDateCompleted(null)
        withWhoAtRisk("Who is at risk answer")
        withNatureOfRisk("What is the nature of the risk answer")
        withRiskGreatest("When is the risk likely to be the greatest answer")
        withRiskIncreaseLikelyTo("What circumstances are likely to increase risk answer")
        withRiskReductionLikelyTo("Reduction Likely To answer")
      }.produce()

      val result = oaSysSectionsTransformer.roshSummaryAnswers(roshSummaryApiResponse.roshSummary)

      assertThat(result).containsExactly(
        OASysQuestion(
          label = "Who is at risk",
          questionNumber = "R10.1",
          answer = "Who is at risk answer",
        ),
        OASysQuestion(
          label = "What is the nature of the risk",
          questionNumber = "R10.2",
          answer = "What is the nature of the risk answer",
        ),
        OASysQuestion(
          label = "When is the risk likely to be the greatest",
          questionNumber = "R10.3",
          answer = "When is the risk likely to be the greatest answer",
        ),
        OASysQuestion(
          label = "What circumstances are likely to increase risk",
          questionNumber = "R10.4",
          answer = "What circumstances are likely to increase risk answer",
        ),
        OASysQuestion(
          label = "What circumstances are likely to reduce the risk",
          questionNumber = "R10.5",
          answer = "Reduction Likely To answer",
        ),
      )
    }

    @Test
    fun `return empty questions if no assessment available`() {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns true

      val result = oaSysSectionsTransformer.roshSummaryAnswers(null)

      assertThat(result).containsExactly(
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
          label = "Circumstances or situations where offending is most likely to occur",
          questionNumber = "SUM11",
          answer = null,
        ),
        OASysQuestion(
          label = "Analysis of risk factors",
          questionNumber = "SUM9",
          answer = null,
        ),
        OASysQuestion(
          label = "Strengths and protective factors",
          questionNumber = "SUM10",
          answer = null,
        ),
      )
    }
  }

  @Nested
  inner class TransformToApi {

    @Test
    fun `include mandatory questions`() {
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

      val roshSummaryApiResponse = RoshSummaryFactory().apply {
        withAssessmentId(34853487)
        withDateCompleted(null)
        withWhoAtRisk("Who is at risk")
        withNatureOfRisk("What is the nature of the risk")
        withRiskGreatest("When is the risk likely to be the greatest")
        withRiskIncreaseLikelyTo("What circumstances are likely to increase risk")
        withRiskReductionLikelyTo("Reduction Likely To")
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

      val riskManagementPlanApiResponse = RiskManagementPlanFactory().apply {
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

      val requestedOptionalSections = listOf<Int>()

      val needsDetailsApiResponse = NeedsDetailsFactory().produce()

      val result = oaSysSectionsTransformer.transformToApi(
        offenceDetailsApiResponse,
        roshSummaryApiResponse,
        risksToTheIndividualApiResponse,
        riskManagementPlanApiResponse,
        needsDetailsApiResponse,
        requestedOptionalSections,
      )

      assertThat(result.assessmentId).isEqualTo(offenceDetailsApiResponse.assessmentId)
      assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.incomplete)
      assertThat(result.dateStarted).isEqualTo(offenceDetailsApiResponse.initiationDate.toInstant())
      assertThat(result.dateCompleted).isEqualTo(offenceDetailsApiResponse.dateCompleted?.toInstant())
      assertThat(result.offenceDetails).containsExactly(
        OASysQuestion(
          label = "Offence analysis",
          questionNumber = "2.1",
          answer = offenceDetailsApiResponse.offence?.offenceAnalysis,
        ),
        OASysQuestion(
          label = "Victim - perpetrator relationship",
          questionNumber = "2.4.1",
          answer = offenceDetailsApiResponse.offence?.victimPerpetratorRel,
        ),
        OASysQuestion(
          label = "Other victim information",
          questionNumber = "2.4.2",
          answer = offenceDetailsApiResponse.offence?.victimInfo,
        ),
        OASysQuestion(
          label = "Impact on the victim",
          questionNumber = "2.5",
          answer = offenceDetailsApiResponse.offence?.victimImpact,
        ),
        OASysQuestion(
          label = "Motivation and triggers",
          questionNumber = "2.8.3",
          answer = offenceDetailsApiResponse.offence?.offenceMotivation,
        ),
        OASysQuestion(
          label = "Issues contributing to risks",
          questionNumber = "2.98",
          answer = offenceDetailsApiResponse.offence?.issueContributingToRisk,
        ),
        OASysQuestion(
          label = "Pattern of offending",
          questionNumber = "2.12",
          answer = offenceDetailsApiResponse.offence?.patternOffending,
        ),
      )

      assertThat(result.roshSummary).containsExactly(
        OASysQuestion(
          label = "Who is at risk",
          questionNumber = "R10.1",
          answer = roshSummaryApiResponse.roshSummary?.whoIsAtRisk,
        ),
        OASysQuestion(
          label = "What is the nature of the risk",
          questionNumber = "R10.2",
          answer = roshSummaryApiResponse.roshSummary?.natureOfRisk,
        ),
        OASysQuestion(
          label = "When is the risk likely to be the greatest",
          questionNumber = "R10.3",
          answer = roshSummaryApiResponse.roshSummary?.riskGreatest,
        ),
        OASysQuestion(
          label = "What circumstances are likely to increase risk",
          questionNumber = "R10.4",
          answer = roshSummaryApiResponse.roshSummary?.riskIncreaseLikelyTo,
        ),
        OASysQuestion(
          label = "What circumstances are likely to reduce the risk",
          questionNumber = "R10.5",
          answer = roshSummaryApiResponse.roshSummary?.riskReductionLikelyTo,
        ),
      )

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
      )

      assertThat(result.riskManagementPlan).containsExactly(
        OASysQuestion(
          label = "Further considerations",
          questionNumber = "RM28",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.furtherConsiderations,
        ),
        OASysQuestion(
          label = "Additional comments",
          questionNumber = "RM35",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.additionalComments,
        ),
        OASysQuestion(
          label = "Contingency plans",
          questionNumber = "RM34",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.contingencyPlans,
        ),
        OASysQuestion(
          label = "Victim safety planning",
          questionNumber = "RM33",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.victimSafetyPlanning,
        ),
        OASysQuestion(
          label = "Interventions and treatment",
          questionNumber = "RM32",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.interventionsAndTreatment,
        ),
        OASysQuestion(
          label = "Monitoring and control",
          questionNumber = "RM31",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.monitoringAndControl,
        ),
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.supervision,
        ),
        OASysQuestion(
          label = "Key information about current situation",
          questionNumber = "RM28.1",
          answer = riskManagementPlanApiResponse.riskManagementPlan?.keyInformationAboutCurrentSituation,
        ),
      )
    }

    @Test
    fun `for supporting information includes optional sections, alcohol, drugs and those that are linked to harm`() {
      val offenceDetailsApiResponse = OffenceDetailsFactory().produce()
      val roshSummaryApiResponse = RoshSummaryFactory().produce()
      val risksToTheIndividualApiResponse = RiskToTheIndividualFactory().produce()
      val riskManagementPlanApiResponse = RiskManagementPlanFactory().produce()

      val requestedOptionalSections = listOf(4, 5)
      val needsDetailsApiResponse = NeedsDetailsFactory()
        .withEducationTrainingEmploymentIssuesDetails("Education, Training, Employment", linkedToHarm = false, linkedToReoffending = false)
        .withFinanceIssuesDetails(null, linkedToHarm = null, linkedToReoffending = null)
        .withEmotionalIssuesDetails("Emotional", linkedToHarm = true, linkedToReoffending = false)
        .produce()

      val result = oaSysSectionsTransformer.transformToApi(
        offenceDetailsApiResponse,
        roshSummaryApiResponse,
        risksToTheIndividualApiResponse,
        riskManagementPlanApiResponse,
        needsDetailsApiResponse,
        requestedOptionalSections,
      )

      assertThat(result.supportingInformation).containsExactly(
        OASysSupportingInformationQuestion(
          label = "Education, training and employability issues contributing to risks of offending and harm",
          questionNumber = "4.9",
          sectionNumber = 4,
          linkedToHarm = false,
          linkedToReOffending = false,
          answer = "Education, Training, Employment",
        ),
        OASysSupportingInformationQuestion(
          label = "Financial management issues contributing to risks of offending and harm",
          questionNumber = "5.9",
          sectionNumber = 5,
          linkedToHarm = null,
          linkedToReOffending = null,
          answer = null,
        ),
        OASysSupportingInformationQuestion(
          label = "Drug misuse issues contributing to risks of offending and harm",
          questionNumber = "8.9",
          sectionNumber = 8,
          linkedToHarm = needsDetailsApiResponse.linksToHarm?.drugLinkedToHarm,
          linkedToReOffending = needsDetailsApiResponse.linksToReOffending?.drugLinkedToReOffending,
          answer = needsDetailsApiResponse.needs?.drugIssuesDetails,
        ),
        OASysSupportingInformationQuestion(
          label = "Alcohol misuse issues contributing to risks of offending and harm",
          questionNumber = "9.9",
          sectionNumber = 9,
          linkedToHarm = needsDetailsApiResponse.linksToHarm?.alcoholLinkedToHarm,
          linkedToReOffending = needsDetailsApiResponse.linksToReOffending?.alcoholLinkedToReOffending,
          answer = needsDetailsApiResponse.needs?.alcoholIssuesDetails,
        ),
        OASysSupportingInformationQuestion(
          label = "Issues of emotional well-being contributing to risks of offending and harm",
          questionNumber = "10.9",
          sectionNumber = 10,
          linkedToHarm = true,
          linkedToReOffending = false,
          answer = "Emotional",
        ),
      )
    }
  }

  @Nested
  inner class Cas2TransformRiskToIndividual {

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

      val result = oaSysSectionsTransformer.cas2TransformRiskToIndividual(
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
  inner class Cas2TransformRiskOfSeriousHarm {

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

      val result = oaSysSectionsTransformer.cas2TransformRiskOfSeriousHarm(
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
