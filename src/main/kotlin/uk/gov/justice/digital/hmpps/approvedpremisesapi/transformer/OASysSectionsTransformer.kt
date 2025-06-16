package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskOfSeriousHarm
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskToSelf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSections
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSupportingInformationQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetailsInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskManagementPlanInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskToTheIndividualInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummaryInner

@Component
class OASysSectionsTransformer {
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
    offenceDetails = offenceDetailsAnswers(offenceDetails.offence),
    roshSummary = roshSummaryAnswers(roshSummary.roshSummary),
    supportingInformation = supportingInformationAnswers(needsDetails, requestedOptionalSections),
    riskToSelf = riskToSelfAnswers(risksToTheIndividual.riskToTheIndividual),
    riskManagementPlan = riskManagementPlanAnswers(riskManagementPlan.riskManagementPlan),
  )

  fun offenceDetailsAnswers(offenceDetails: OffenceDetailsInner?) = listOf(
    OASysQuestion("Offence analysis", "2.1", offenceDetails?.offenceAnalysis),
    OASysQuestion("Victim - perpetrator relationship", "2.4.1", offenceDetails?.victimPerpetratorRel),
    OASysQuestion("Other victim information", "2.4.2", offenceDetails?.victimInfo),
    OASysQuestion("Impact on the victim", "2.5", offenceDetails?.victimImpact),
    OASysQuestion("Motivation and triggers", "2.8.3", offenceDetails?.offenceMotivation),
    OASysQuestion("Issues contributing to risks", "2.98", offenceDetails?.issueContributingToRisk),
    OASysQuestion("Pattern of offending", "2.12", offenceDetails?.patternOffending),
  )

  fun roshSummaryAnswers(roshSummary: RoshSummaryInner?) = listOf(
    OASysQuestion("Who is at risk", "R10.1", roshSummary?.whoIsAtRisk),
    OASysQuestion("What is the nature of the risk", "R10.2", roshSummary?.natureOfRisk),
    OASysQuestion("When is the risk likely to be the greatest", "R10.3", roshSummary?.riskGreatest),
    OASysQuestion("What circumstances are likely to increase risk", "R10.4", roshSummary?.riskIncreaseLikelyTo),
    OASysQuestion("What circumstances are likely to reduce the risk", "R10.5", roshSummary?.riskReductionLikelyTo),
  )

  fun riskToSelfAnswers(risksToTheIndividual: RiskToTheIndividualInner?) = listOf(
    OASysQuestion("Current concerns about self-harm or suicide", "R8.1.1", risksToTheIndividual?.currentConcernsSelfHarmSuicide),
    OASysQuestion("Current concerns about Coping in Custody or Hostel", "R8.2.1", risksToTheIndividual?.currentCustodyHostelCoping),
    OASysQuestion("Current concerns about Vulnerability", "R8.3.1", risksToTheIndividual?.currentVulnerability),
  )

  fun riskManagementPlanAnswers(riskManagementPlan: RiskManagementPlanInner?) = listOf(
    OASysQuestion("Further considerations", "RM28", riskManagementPlan?.furtherConsiderations),
    OASysQuestion("Additional comments", "RM35", riskManagementPlan?.additionalComments),
    OASysQuestion("Contingency plans", "RM34", riskManagementPlan?.contingencyPlans),
    OASysQuestion("Victim safety planning", "RM33", riskManagementPlan?.victimSafetyPlanning),
    OASysQuestion("Interventions and treatment", "RM32", riskManagementPlan?.interventionsAndTreatment),
    OASysQuestion("Monitoring and control", "RM31", riskManagementPlan?.monitoringAndControl),
    OASysQuestion("Supervision", "RM30", riskManagementPlan?.supervision),
    OASysQuestion("Key information about current situation", "RM28.1", riskManagementPlan?.keyInformationAboutCurrentSituation),
  )

  fun cas2TransformRiskToIndividual(
    offenceDetails: OffenceDetails,
    risksToTheIndividual: RisksToTheIndividual,
  ): OASysRiskToSelf = OASysRiskToSelf(
    assessmentId = offenceDetails.assessmentId,
    assessmentState = if (offenceDetails.dateCompleted != null) OASysAssessmentState.completed else OASysAssessmentState.incomplete,
    dateStarted = offenceDetails.initiationDate.toInstant(),
    dateCompleted = offenceDetails.dateCompleted?.toInstant(),
    riskToSelf =
    riskToSelfAnswers(risksToTheIndividual.riskToTheIndividual) +
      listOf(
        OASysQuestion("Previous concerns about self-harm or suicide", "R8.1.4", risksToTheIndividual.riskToTheIndividual?.previousConcernsSelfHarmSuicide),
      ),
  )

  fun cas2TransformRiskOfSeriousHarm(
    offenceDetails: OffenceDetails,
    roshSummary: RoshSummary,
  ): OASysRiskOfSeriousHarm = OASysRiskOfSeriousHarm(
    assessmentId = offenceDetails.assessmentId,
    assessmentState = if (offenceDetails.dateCompleted != null) OASysAssessmentState.completed else OASysAssessmentState.incomplete,
    dateStarted = offenceDetails.initiationDate.toInstant(),
    dateCompleted = offenceDetails.dateCompleted?.toInstant(),
    rosh = roshSummaryAnswers(roshSummary.roshSummary),
  )

  @SuppressWarnings("CyclomaticComplexMethod")
  private fun supportingInformationAnswers(needsDetails: NeedsDetails, requestedOptionalSections: List<Int>): List<OASysSupportingInformationQuestion> {
    val supportingInformation = mutableListOf<OASysSupportingInformationQuestion>()

    if (needsDetails.linksToHarm?.accommodationLinkedToHarm == true || requestedOptionalSections.contains(3)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = OASysLabels.questionToLabel.getValue("3.9"),
        questionNumber = "3.9",
        sectionNumber = 3,
        linkedToHarm = needsDetails.linksToHarm?.accommodationLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.accommodationLinkedToReOffending,
        answer = needsDetails.needs?.accommodationIssuesDetails,
      )
    }

    if (needsDetails.linksToHarm?.educationTrainingEmploymentLinkedToHarm == true || requestedOptionalSections.contains(4)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = OASysLabels.questionToLabel.getValue("4.9"),
        questionNumber = "4.9",
        sectionNumber = 4,
        linkedToHarm = needsDetails.linksToHarm?.educationTrainingEmploymentLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.educationTrainingEmploymentLinkedToReOffending,
        answer = needsDetails.needs?.educationTrainingEmploymentIssuesDetails,
      )
    }

    if (needsDetails.linksToHarm?.financeLinkedToHarm == true || requestedOptionalSections.contains(5)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = OASysLabels.questionToLabel.getValue("5.9"),
        questionNumber = "5.9",
        sectionNumber = 5,
        linkedToHarm = needsDetails.linksToHarm?.financeLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.financeLinkedToReOffending,
        answer = needsDetails.needs?.financeIssuesDetails,
      )
    }

    if (needsDetails.linksToHarm?.relationshipLinkedToHarm == true || requestedOptionalSections.contains(6)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = OASysLabels.questionToLabel.getValue("6.9"),
        questionNumber = "6.9",
        sectionNumber = 6,
        linkedToHarm = needsDetails.linksToHarm?.relationshipLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.relationshipLinkedToReOffending,
        answer = needsDetails.needs?.relationshipIssuesDetails,
      )
    }

    if (needsDetails.linksToHarm?.lifestyleLinkedToHarm == true || requestedOptionalSections.contains(7)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = OASysLabels.questionToLabel.getValue("7.9"),
        questionNumber = "7.9",
        sectionNumber = 7,
        linkedToHarm = needsDetails.linksToHarm?.lifestyleLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.lifestyleLinkedToReOffending,
        answer = needsDetails.needs?.lifestyleIssuesDetails,
      )
    }

    supportingInformation += OASysSupportingInformationQuestion(
      label = OASysLabels.questionToLabel.getValue("8.9"),
      questionNumber = "8.9",
      sectionNumber = 8,
      linkedToHarm = needsDetails.linksToHarm?.drugLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.drugLinkedToReOffending,
      answer = needsDetails.needs?.drugIssuesDetails,
    )

    supportingInformation += OASysSupportingInformationQuestion(
      label = OASysLabels.questionToLabel.getValue("9.9"),
      questionNumber = "9.9",
      sectionNumber = 9,
      linkedToHarm = needsDetails.linksToHarm?.alcoholLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.alcoholLinkedToReOffending,
      answer = needsDetails.needs?.alcoholIssuesDetails,
    )

    if (needsDetails.linksToHarm?.emotionalLinkedToHarm == true || requestedOptionalSections.contains(10)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = OASysLabels.questionToLabel.getValue("10.9"),
        questionNumber = "10.9",
        sectionNumber = 10,
        linkedToHarm = needsDetails.linksToHarm?.emotionalLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.emotionalLinkedToReOffending,
        answer = needsDetails.needs?.emotionalIssuesDetails,
      )
    }

    if (needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm == true || requestedOptionalSections.contains(11)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = OASysLabels.questionToLabel.getValue("11.9"),
        questionNumber = "11.9",
        sectionNumber = 11,
        linkedToHarm = needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.thinkingBehaviouralLinkedToReOffending,
        answer = needsDetails.needs?.thinkingBehaviouralIssuesDetails,
      )
    }

    if (needsDetails.linksToHarm?.attitudeLinkedToHarm == true || requestedOptionalSections.contains(12)) {
      supportingInformation += OASysSupportingInformationQuestion(
        label = OASysLabels.questionToLabel.getValue("12.9"),
        questionNumber = "12.9",
        sectionNumber = 12,
        linkedToHarm = needsDetails.linksToHarm?.attitudeLinkedToHarm,
        linkedToReOffending = needsDetails.linksToReOffending?.attitudeLinkedToReOffending,
        answer = needsDetails.needs?.attitudeIssuesDetails,
      )
    }

    return supportingInformation
  }

  private data class OrderedQuestion(val position: Int, val question: OASysQuestion)
}
