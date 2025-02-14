package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskOfSeriousHarm
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskToSelf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSections
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSupportingInformationQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummary

@Component
class OASysSectionsTransformer : OASysTransformer() {
  fun transformToApi(
    offenceDetails: OffenceDetails,
    roshSummary: RoshSummary,
    risksToTheIndividual: RisksToTheIndividual,
    riskManagementPlan: RiskManagementPlan,
    needsDetails: NeedsDetails,
    requestedOptionalSections: List<Int>,
  ): OASysSections = OASysSections(
    assessmentId = offenceDetails.assessmentId,
    assessmentState = if (offenceDetails.dateCompleted != null) OASysAssessmentState.completed else OASysAssessmentState.incomplete,
    dateStarted = offenceDetails.initiationDate.toInstant(),
    dateCompleted = offenceDetails.dateCompleted?.toInstant(),
    offenceDetails = listOf(
      oASysQuestionWithSingleAnswer("Offence analysis", "2.1", offenceDetails.offence?.offenceAnalysis),
      oASysQuestionWithSingleAnswer("Victim - perpetrator relationship", "2.4.1", offenceDetails.offence?.victimPerpetratorRel),
      oASysQuestionWithSingleAnswer("Other victim information", "2.4.2", offenceDetails.offence?.victimInfo),
      oASysQuestionWithSingleAnswer("Impact on the victim", "2.5", offenceDetails.offence?.victimImpact),
      oASysQuestionWithSingleAnswer("Motivation and triggers", "2.8.3", offenceDetails.offence?.offenceMotivation),
      oASysQuestionWithSingleAnswer("Issues contributing to risks", "2.98", offenceDetails.offence?.issueContributingToRisk),
      oASysQuestionWithSingleAnswer("Pattern of offending", "2.12", offenceDetails.offence?.patternOffending),
    ),
    roshSummary = listOf(
      oASysQuestionWithSingleAnswer("Who is at risk", "R10.1", roshSummary.roshSummary?.whoIsAtRisk),
      oASysQuestionWithSingleAnswer("What is the nature of the risk", "R10.2", roshSummary.roshSummary?.natureOfRisk),
      oASysQuestionWithSingleAnswer("When is the risk likely to be the greatest", "R10.3", roshSummary.roshSummary?.riskGreatest),
      oASysQuestionWithSingleAnswer("What circumstances are likely to increase risk", "R10.4", roshSummary.roshSummary?.riskIncreaseLikelyTo),
      oASysQuestionWithSingleAnswer("What circumstances are likely to reduce the risk", "R10.5", roshSummary.roshSummary?.riskReductionLikelyTo),
    ),
    supportingInformation = transformSupportingInformation(needsDetails, requestedOptionalSections),
    riskToSelf = listOf(
      oASysQuestionWithSingleAnswer("Current concerns about self-harm or suicide", "R8.1.1", risksToTheIndividual.riskToTheIndividual?.currentConcernsSelfHarmSuicide),
      oASysQuestionWithSingleAnswer("Current concerns about Coping in Custody or Hostel", "R8.2.1", risksToTheIndividual.riskToTheIndividual?.currentCustodyHostelCoping),
      oASysQuestionWithSingleAnswer("Current concerns about Vulnerability", "R8.3.1", risksToTheIndividual.riskToTheIndividual?.currentVulnerability),
    ),
    riskManagementPlan = listOf(
      oASysQuestionWithSingleAnswer("Further considerations", "RM28", riskManagementPlan.riskManagementPlan?.furtherConsiderations),
      oASysQuestionWithSingleAnswer("Additional comments", "RM35", riskManagementPlan.riskManagementPlan?.additionalComments),
      oASysQuestionWithSingleAnswer("Contingency plans", "RM34", riskManagementPlan.riskManagementPlan?.contingencyPlans),
      oASysQuestionWithSingleAnswer("Victim safety planning", "RM33", riskManagementPlan.riskManagementPlan?.victimSafetyPlanning),
      oASysQuestionWithSingleAnswer("Interventions and treatment", "RM32", riskManagementPlan.riskManagementPlan?.interventionsAndTreatment),
      oASysQuestionWithSingleAnswer("Monitoring and control", "RM31", riskManagementPlan.riskManagementPlan?.monitoringAndControl),
      oASysQuestionWithSingleAnswer("Supervision", "RM30", riskManagementPlan.riskManagementPlan?.supervision),
      oASysQuestionWithSingleAnswer("Key information about current situation", "RM28.1", riskManagementPlan.riskManagementPlan?.keyInformationAboutCurrentSituation),
    ),
  )

  fun transformRiskToIndividual(
    offenceDetails: OffenceDetails,
    risksToTheIndividual: RisksToTheIndividual,
  ): OASysRiskToSelf = OASysRiskToSelf(
    assessmentId = offenceDetails.assessmentId,
    assessmentState = if (offenceDetails.dateCompleted != null) OASysAssessmentState.completed else OASysAssessmentState.incomplete,
    dateStarted = offenceDetails.initiationDate.toInstant(),
    dateCompleted = offenceDetails.dateCompleted?.toInstant(),
    riskToSelf = listOf(
      oASysQuestionWithSingleAnswer("Current concerns about self-harm or suicide", "R8.1.1", risksToTheIndividual.riskToTheIndividual?.currentConcernsSelfHarmSuicide),
      oASysQuestionWithSingleAnswer("Current concerns about Coping in Custody or Hostel", "R8.2.1", risksToTheIndividual.riskToTheIndividual?.currentCustodyHostelCoping),
      oASysQuestionWithSingleAnswer("Current concerns about Vulnerability", "R8.3.1", risksToTheIndividual.riskToTheIndividual?.currentVulnerability),
      oASysQuestionWithSingleAnswer("Previous concerns about self-harm or suicide", "R8.1.4", risksToTheIndividual.riskToTheIndividual?.previousConcernsSelfHarmSuicide),
    ),
  )

  fun transformRiskOfSeriousHarm(
    offenceDetails: OffenceDetails,
    roshSummary: RoshSummary,
  ): OASysRiskOfSeriousHarm = OASysRiskOfSeriousHarm(
    assessmentId = offenceDetails.assessmentId,
    assessmentState = if (offenceDetails.dateCompleted != null) OASysAssessmentState.completed else OASysAssessmentState.incomplete,
    dateStarted = offenceDetails.initiationDate.toInstant(),
    dateCompleted = offenceDetails.dateCompleted?.toInstant(),
    rosh = listOf(
      oASysQuestionWithSingleAnswer("Who is at risk", "R10.1", roshSummary.roshSummary?.whoIsAtRisk),
      oASysQuestionWithSingleAnswer("What is the nature of the risk", "R10.2", roshSummary.roshSummary?.natureOfRisk),
      oASysQuestionWithSingleAnswer("When is the risk likely to be the greatest", "R10.3", roshSummary.roshSummary?.riskGreatest),
      oASysQuestionWithSingleAnswer("What circumstances are likely to increase risk", "R10.4", roshSummary.roshSummary?.riskIncreaseLikelyTo),
      oASysQuestionWithSingleAnswer("What circumstances are likely to reduce the risk", "R10.5", roshSummary.roshSummary?.riskReductionLikelyTo),
    ),
  )

  private fun transformSupportingInformation(needsDetails: NeedsDetails, requestedOptionalSections: List<Int>): List<OASysSupportingInformationQuestion> {
    val supportingInformation = mutableListOf<OASysSupportingInformationQuestion>()

    if (needsDetails.linksToHarm?.accommodationLinkedToHarm == true || requestedOptionalSections.contains(3)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = "Accommodation issues contributing to risks of offending and harm",
        questionNumber = "3.9",
        sectionNumber = 3,
        linkedToHarm = needsDetails.linksToHarm?.accommodationLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.accommodationLinkedToReOffending,
        answer = needsDetails.needs?.accommodationIssuesDetails,
      )
    }

    if (needsDetails.linksToHarm?.educationTrainingEmploymentLinkedToHarm == true || requestedOptionalSections.contains(4)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = "Education, training and employability issues contributing to risks of offending and harm",
        questionNumber = "4.9",
        sectionNumber = 4,
        linkedToHarm = needsDetails.linksToHarm?.educationTrainingEmploymentLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.educationTrainingEmploymentLinkedToReOffending,
        answer = needsDetails.needs?.educationTrainingEmploymentIssuesDetails,
      )
    }

    if (needsDetails.linksToHarm?.financeLinkedToHarm == true || requestedOptionalSections.contains(5)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = "Financial management issues contributing to risks of offending and harm",
        questionNumber = "5.9",
        sectionNumber = 5,
        linkedToHarm = needsDetails.linksToHarm?.financeLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.financeLinkedToReOffending,
        answer = needsDetails.needs?.financeIssuesDetails,
      )
    }

    if (needsDetails.linksToHarm?.relationshipLinkedToHarm == true || requestedOptionalSections.contains(6)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = "Relationship issues contributing to risks of offending and harm",
        questionNumber = "6.9",
        sectionNumber = 6,
        linkedToHarm = needsDetails.linksToHarm?.relationshipLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.relationshipLinkedToReOffending,
        answer = needsDetails.needs?.relationshipIssuesDetails,
      )
    }

    if (needsDetails.linksToHarm?.lifestyleLinkedToHarm == true || requestedOptionalSections.contains(7)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = "Lifestyle issues contributing to risks of offending and harm",
        questionNumber = "7.9",
        sectionNumber = 7,
        linkedToHarm = needsDetails.linksToHarm?.lifestyleLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.lifestyleLinkedToReOffending,
        answer = needsDetails.needs?.lifestyleIssuesDetails,
      )
    }

    supportingInformation += OASysSupportingInformationQuestion(
      label = "Drug misuse issues contributing to risks of offending and harm",
      questionNumber = "8.9",
      sectionNumber = 8,
      linkedToHarm = needsDetails.linksToHarm?.drugLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.drugLinkedToReOffending,
      answer = needsDetails.needs?.drugIssuesDetails,
    )

    supportingInformation += OASysSupportingInformationQuestion(
      label = "Alcohol misuse issues contributing to risks of offending and harm",
      questionNumber = "9.9",
      sectionNumber = 9,
      linkedToHarm = needsDetails.linksToHarm?.alcoholLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.alcoholLinkedToReOffending,
      answer = needsDetails.needs?.alcoholIssuesDetails,
    )

    if (needsDetails.linksToHarm?.emotionalLinkedToHarm == true || requestedOptionalSections.contains(10)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = "Issues of emotional well-being contributing to risks of offending and harm",
        questionNumber = "10.9",
        sectionNumber = 10,
        linkedToHarm = needsDetails.linksToHarm?.emotionalLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.emotionalLinkedToReOffending,
        answer = needsDetails.needs?.emotionalIssuesDetails,
      )
    }

    if (needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm == true || requestedOptionalSections.contains(11)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = "Thinking / behavioural issues contributing to risks of offending and harm",
        questionNumber = "11.9",
        sectionNumber = 11,
        linkedToHarm = needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.thinkingBehaviouralLinkedToReOffending,
        answer = needsDetails.needs?.thinkingBehaviouralIssuesDetails,
      )
    }

    if (needsDetails.linksToHarm?.attitudeLinkedToHarm == true || requestedOptionalSections.contains(12)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = "Issues about attitudes contributing to risks of offending and harm",
        questionNumber = "12.9",
        sectionNumber = 12,
        linkedToHarm = needsDetails.linksToHarm?.attitudeLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.attitudeLinkedToReOffending,
        answer = needsDetails.needs?.attitudeIssuesDetails,
      )
    }

    return supportingInformation
  }
}
