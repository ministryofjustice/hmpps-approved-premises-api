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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskManagementPlan
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
    offenceDetails = offenceDetailsAnswers(offenceDetails),
    roshSummary = roshSummaryAnswers(roshSummary),
    supportingInformation = supportingInformationAnswers(needsDetails, requestedOptionalSections),
    riskToSelf = riskToSelfAnswers(risksToTheIndividual),
    riskManagementPlan = riskManagementPlanAnswers(riskManagementPlan),
  )

  fun offenceDetailsAnswers(offenceDetails: OffenceDetails) = listOf(
    OASysQuestion("Offence analysis", "2.1", offenceDetails.offence?.offenceAnalysis),
    OASysQuestion("Victim - perpetrator relationship", "2.4.1", offenceDetails.offence?.victimPerpetratorRel),
    OASysQuestion("Other victim information", "2.4.2", offenceDetails.offence?.victimInfo),
    OASysQuestion("Impact on the victim", "2.5", offenceDetails.offence?.victimImpact),
    OASysQuestion("Motivation and triggers", "2.8.3", offenceDetails.offence?.offenceMotivation),
    OASysQuestion("Issues contributing to risks", "2.98", offenceDetails.offence?.issueContributingToRisk),
    OASysQuestion("Pattern of offending", "2.12", offenceDetails.offence?.patternOffending),
  )

  fun roshSummaryAnswers(roshSummary: RoshSummary) = roshSummaryAnswers(roshSummary.roshSummary)

  fun roshSummaryAnswers(roshSummaryInner: RoshSummaryInner?) = listOf(
    OrderedQuestion(1, OASysQuestion("Who is at risk", "R10.1", roshSummaryInner?.whoIsAtRisk)),
    OrderedQuestion(2, OASysQuestion("What is the nature of the risk", "R10.2", roshSummaryInner?.natureOfRisk)),
    if (roshSummaryInner?.factorsSituationsLikelyToOffend != null) {
      OrderedQuestion(3, OASysQuestion("SUM11 Label TBD", "SUM11", roshSummaryInner.factorsSituationsLikelyToOffend))
    } else {
      OrderedQuestion(3, OASysQuestion("When is the risk likely to be the greatest", "R10.3", roshSummaryInner?.riskGreatest))
    },
    if (roshSummaryInner?.factorsAnalysisOfRisk != null) {
      OrderedQuestion(5, OASysQuestion("SUM9 Label TBD", "SUM9", roshSummaryInner.factorsAnalysisOfRisk))
    } else {
      OrderedQuestion(4, OASysQuestion("What circumstances are likely to increase risk", "R10.4", roshSummaryInner?.riskIncreaseLikelyTo))
    },
    if (roshSummaryInner?.factorsStrengthsAndProtective != null) {
      OrderedQuestion(4, OASysQuestion("SUM10 Label TBD", "SUM10", roshSummaryInner.factorsStrengthsAndProtective))
    } else {
      OrderedQuestion(5, OASysQuestion("What circumstances are likely to reduce the risk", "R10.5", roshSummaryInner?.riskReductionLikelyTo))
    },
  ).sortQuestionsInOrder()

  fun riskToSelfAnswers(risksToTheIndividual: RisksToTheIndividual) = riskToSelfAnswers(risksToTheIndividual.riskToTheIndividual)

  private fun riskToSelfAnswers(riskToTheIndividualInner: RiskToTheIndividualInner?) = listOf(
    OrderedQuestion(
      position = 1,
      question = if (riskToTheIndividualInner?.analysisSuicideSelfharm != null) {
        OASysQuestion("FA62 Label TBD", "FA62", riskToTheIndividualInner.analysisSuicideSelfharm)
      } else {
        OASysQuestion("Current concerns about self-harm or suicide", "R8.1.1", riskToTheIndividualInner?.currentConcernsSelfHarmSuicide)
      },
    ),
    OrderedQuestion(
      position = 2,
      question = if (riskToTheIndividualInner?.analysisCoping != null) {
        OASysQuestion("FA63 Label TBD", "FA63", riskToTheIndividualInner.analysisCoping)
      } else {
        OASysQuestion("Current concerns about Coping in Custody or Hostel", "R8.2.1", riskToTheIndividualInner?.currentCustodyHostelCoping)
      },
    ),
    OrderedQuestion(
      position = 3,
      question = if (riskToTheIndividualInner?.analysisVulnerabilities != null) {
        OASysQuestion("FA64 Label TBD", "FA64", riskToTheIndividualInner.analysisVulnerabilities)
      } else {
        OASysQuestion("Current concerns about Vulnerability", "R8.3.1", riskToTheIndividualInner?.currentVulnerability)
      },
    ),
  ).sortQuestionsInOrder()

  fun riskManagementPlanAnswers(riskManagementPlan: RiskManagementPlan) = listOf(
    OASysQuestion("Further considerations", "RM28", riskManagementPlan.riskManagementPlan?.furtherConsiderations),
    OASysQuestion("Additional comments", "RM35", riskManagementPlan.riskManagementPlan?.additionalComments),
    OASysQuestion("Contingency plans", "RM34", riskManagementPlan.riskManagementPlan?.contingencyPlans),
    OASysQuestion("Victim safety planning", "RM33", riskManagementPlan.riskManagementPlan?.victimSafetyPlanning),
    OASysQuestion("Interventions and treatment", "RM32", riskManagementPlan.riskManagementPlan?.interventionsAndTreatment),
    OASysQuestion("Monitoring and control", "RM31", riskManagementPlan.riskManagementPlan?.monitoringAndControl),
    OASysQuestion("Supervision", "RM30", riskManagementPlan.riskManagementPlan?.supervision),
    OASysQuestion("Key information about current situation", "RM28.1", riskManagementPlan.riskManagementPlan?.keyInformationAboutCurrentSituation),
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
    riskToSelfAnswers(risksToTheIndividual) +
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
    rosh = roshSummaryAnswers(roshSummary),
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

  private fun List<OrderedQuestion>.sortQuestionsInOrder() = this.sortedBy { it.position }.map { it.question }
}
