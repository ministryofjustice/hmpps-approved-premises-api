package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskOfSeriousHarm
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskToSelf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskToTheIndividualInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummaryInner

@Service
class Cas2OAsysSectionsTransformer {
  fun transformRiskToIndividual(
    offenceDetails: OffenceDetails,
    risksToTheIndividual: RisksToTheIndividual,
  ): OASysRiskToSelf = OASysRiskToSelf(
    assessmentId = offenceDetails.assessmentId,
    assessmentState = if (offenceDetails.dateCompleted != null) OASysAssessmentState.completed else OASysAssessmentState.incomplete,
    dateStarted = offenceDetails.initiationDate.toInstant(),
    dateCompleted = offenceDetails.dateCompleted?.toInstant(),
    riskToSelf = riskToSelfAnswers(risksToTheIndividual.riskToTheIndividual),
  )

  fun transformRiskOfSeriousHarm(
    offenceDetails: OffenceDetails,
    roshSummary: RoshSummary,
  ): OASysRiskOfSeriousHarm = OASysRiskOfSeriousHarm(
    assessmentId = offenceDetails.assessmentId,
    assessmentState = if (offenceDetails.dateCompleted != null) OASysAssessmentState.completed else OASysAssessmentState.incomplete,
    dateStarted = offenceDetails.initiationDate.toInstant(),
    dateCompleted = offenceDetails.dateCompleted?.toInstant(),
    rosh = roshSummaryAnswers(roshSummary.roshSummary),
  )

  private fun riskToSelfAnswers(risksToTheIndividual: RiskToTheIndividualInner?) = listOf(
    OASysQuestion("Current concerns about self-harm or suicide", "R8.1.1", risksToTheIndividual?.currentConcernsSelfHarmSuicide),
    OASysQuestion("Current concerns about Coping in Custody or Hostel", "R8.2.1", risksToTheIndividual?.currentCustodyHostelCoping),
    OASysQuestion("Current concerns about Vulnerability", "R8.3.1", risksToTheIndividual?.currentVulnerability),
    OASysQuestion("Previous concerns about self-harm or suicide", "R8.1.4", risksToTheIndividual?.previousConcernsSelfHarmSuicide),
  )

  private fun roshSummaryAnswers(roshSummary: RoshSummaryInner?) = listOf(
    OASysQuestion("Who is at risk", "R10.1", roshSummary?.whoIsAtRisk),
    OASysQuestion("What is the nature of the risk", "R10.2", roshSummary?.natureOfRisk),
    OASysQuestion("When is the risk likely to be the greatest", "R10.3", roshSummary?.riskGreatest),
    OASysQuestion("What circumstances are likely to increase risk", "R10.4", roshSummary?.riskIncreaseLikelyTo),
    OASysQuestion("What circumstances are likely to reduce the risk", "R10.5", roshSummary?.riskReductionLikelyTo),
  )
}
