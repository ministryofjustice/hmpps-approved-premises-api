package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSupportingInformationQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskManagementPlanFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer

class OASysSectionsTransformerTest {
  private val oaSysSectionsTransformer = OASysSectionsTransformer()

  @Test
  fun `transformToApi sections that are always present (offenceDetails, roshSummary, riskToSelf, riskManagementPlan) transform correctly`() {
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
    assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.INCOMPLETE)
    assertThat(result.dateStarted).isEqualTo(offenceDetailsApiResponse.initiationDate.toInstant())
    assertThat(result.dateCompleted).isEqualTo(offenceDetailsApiResponse.dateCompleted?.toInstant())
    assertThat(result.offenceDetails).containsAll(
      listOf(
        OASysQuestion(
          label = "Offence analysis",
          questionNumber = "2.1",
          answer = offenceDetailsApiResponse.offence?.offenceAnalysis,
        ),
        OASysQuestion(
          label = "Pattern of offending",
          questionNumber = "2.12",
          answer = offenceDetailsApiResponse.offence?.patternOffending,
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
      ),
    )

    assertThat(result.roshSummary).containsAll(
      listOf(
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
      ),
    )

    assertThat(result.riskToSelf).containsAll(
      listOf(
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
      ),
    )

    assertThat(result.riskManagementPlan).containsAll(
      listOf(
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
      ),
    )
  }

  @Test
  fun `transformToApi supportingInformation returns only sections explicitly requested, alcohol and drugs and those that are linked to harm`() {
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

    assertThat(result.supportingInformation).containsExactlyInAnyOrder(
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
        label = "Issues of emotional well-being contributing to risks of offending and harm",
        questionNumber = "10.9",
        sectionNumber = 10,
        linkedToHarm = true,
        linkedToReOffending = false,
        answer = "Emotional",
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
    )
  }

  @Test
  fun `transformRiskToIndividual transforms correctly`() {
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

    val result = oaSysSectionsTransformer.transformRiskToIndividual(
      offenceDetailsApiResponse,
      risksToTheIndividualApiResponse,
    )

    assertThat(result.assessmentId).isEqualTo(offenceDetailsApiResponse.assessmentId)
    assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.INCOMPLETE)
    assertThat(result.dateStarted).isEqualTo(offenceDetailsApiResponse.initiationDate.toInstant())
    assertThat(result.dateCompleted).isEqualTo(offenceDetailsApiResponse.dateCompleted?.toInstant())

    assertThat(result.riskToSelf).containsAll(
      listOf(
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
      ),
    )
  }

  @Test
  fun `transformRiskOfSeriousHarm transforms correctly`() {
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

    val result = oaSysSectionsTransformer.transformRiskOfSeriousHarm(
      offenceDetailsApiResponse,
      roshApiResponse,
    )

    assertThat(result.assessmentId).isEqualTo(offenceDetailsApiResponse.assessmentId)
    assertThat(result.assessmentState).isEqualTo(OASysAssessmentState.INCOMPLETE)
    assertThat(result.dateStarted).isEqualTo(offenceDetailsApiResponse.initiationDate.toInstant())
    assertThat(result.dateCompleted).isEqualTo(offenceDetailsApiResponse.dateCompleted?.toInstant())

    assertThat(result.rosh).containsAll(
      listOf(
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
      ),
    )
  }
}
