package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysAssessmentState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysOffenceAnalysis
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails

@Component
class OffenceAnalysisTransformer : OASysTransformer() {
  fun transformToApi(offenceDetails: OffenceDetails) = OASysOffenceAnalysis(
    assessmentId = offenceDetails.assessmentId,
    assessmentState = if (offenceDetails.dateCompleted != null) OASysAssessmentState.completed else OASysAssessmentState.incomplete,
    dateStarted = offenceDetails.initiationDate,
    dateCompleted = offenceDetails.dateCompleted,
    offenceAnalysis = listOf(
      oASysQuestionWithSingleAnswer("Offence Analysis", "2.1", offenceDetails.offenceDetails.offenceAnalysis),
      oASysQuestionWithSingleAnswer("Others Involved", "2.7.3", offenceDetails.offenceDetails.othersInvolved),
      oASysQuestionWithSingleAnswer("Issue Contributing to Risk", "2.98", offenceDetails.offenceDetails.issueContributingToRisk),
      oASysQuestionWithSingleAnswer("Offence Motivation", "2.8.3", offenceDetails.offenceDetails.offenceMotivation),
      oASysQuestionWithSingleAnswer("Victim Impact", "2.5", offenceDetails.offenceDetails.victimImpact),
      oASysQuestionWithSingleAnswer("Victim Perpetrator Rel", "2.4.2", offenceDetails.offenceDetails.victimPerpetratorRel),
      oASysQuestionWithSingleAnswer("Victim Info", "2.4.1", offenceDetails.offenceDetails.victimInfo),
      oASysQuestionWithSingleAnswer("Pattern Offending", "2.12", offenceDetails.offenceDetails.patternOffending),
      oASysQuestionWithSingleAnswer("Accepts Responsibility", "2.11.t", offenceDetails.offenceDetails.acceptsResponsibility)
    )
  )
}
