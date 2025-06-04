package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysNeedsQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysLabels

@Service
class Cas1OASysNeedsQuestionTransformer {

  fun transformToNeedsQuestion(needsDetails: NeedsDetails) = toQuestionState(needsDetails).map {
    Cas1OASysNeedsQuestion(
      section = it.sectionNumber,
      sectionLabel = it.sectionLabel,
      optional = it.optional,
      linkedToHarm = it.linkedToHarm,
      linkedToReOffending = it.linkedToReOffending,
    )
  }

  fun transformToOASysQuestion(needsDetails: NeedsDetails, includeOptionalSections: List<Int>) = toQuestionState(needsDetails)
    .filter { !it.optional || includeOptionalSections.contains(it.sectionNumber) }
    .map {
      OASysQuestion(
        label = it.questionLabel,
        questionNumber = it.questionNumber,
        answer = it.answer,
      )
    }

  private data class QuestionState(
    val sectionNumber: Int,
    val sectionLabel: String,
    val questionNumber: String,
    val questionLabel: String,
    val answer: String?,
    val optional: Boolean,
    val linkedToHarm: Boolean?,
    val linkedToReOffending: Boolean?,
  )

  private fun toQuestionState(needsDetails: NeedsDetails) = listOf(
    QuestionState(
      sectionNumber = 3,
      sectionLabel = OASysLabels.sectionToLabel.getValue("3"),
      questionNumber = "3.9",
      questionLabel = OASysLabels.questionToLabel.getValue("3.9"),
      answer = needsDetails.needs?.accommodationIssuesDetails,
      optional = isOptional(needsDetails.linksToHarm?.accommodationLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.accommodationLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.accommodationLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 6,
      sectionLabel = OASysLabels.sectionToLabel.getValue("6"),
      questionNumber = "6.9",
      questionLabel = OASysLabels.questionToLabel.getValue("6.9"),
      answer = needsDetails.needs?.relationshipIssuesDetails,
      optional = isOptional(needsDetails.linksToHarm?.relationshipLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.relationshipLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.relationshipLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 7,
      sectionLabel = OASysLabels.sectionToLabel.getValue("7"),
      questionNumber = "7.9",
      questionLabel = OASysLabels.questionToLabel.getValue("7.9"),
      answer = needsDetails.needs?.lifestyleIssuesDetails,
      optional = isOptional(needsDetails.linksToHarm?.lifestyleLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.lifestyleLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.lifestyleLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 8,
      sectionLabel = OASysLabels.sectionToLabel.getValue("8"),
      questionNumber = "8.9",
      questionLabel = OASysLabels.questionToLabel.getValue("8.9"),
      answer = needsDetails.needs?.drugIssuesDetails,
      optional = false,
      linkedToHarm = needsDetails.linksToHarm?.drugLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.drugLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 9,
      sectionLabel = OASysLabels.sectionToLabel.getValue("9"),
      questionNumber = "9.9",
      questionLabel = OASysLabels.questionToLabel.getValue("9.9"),
      answer = needsDetails.needs?.alcoholIssuesDetails,
      optional = false,
      linkedToHarm = needsDetails.linksToHarm?.alcoholLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.alcoholLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 10,
      sectionLabel = OASysLabels.sectionToLabel.getValue("10"),
      questionNumber = "10.9",
      questionLabel = OASysLabels.questionToLabel.getValue("10.9"),
      answer = needsDetails.needs?.emotionalIssuesDetails,
      optional = isOptional(needsDetails.linksToHarm?.emotionalLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.emotionalLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.emotionalLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 11,
      sectionLabel = OASysLabels.sectionToLabel.getValue("11"),
      questionNumber = "11.9",
      questionLabel = OASysLabels.questionToLabel.getValue("11.9"),
      answer = needsDetails.needs?.thinkingBehaviouralIssuesDetails,
      optional = isOptional(needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.thinkingBehaviouralLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 12,
      sectionLabel = OASysLabels.sectionToLabel.getValue("12"),
      questionNumber = "12.9",
      questionLabel = OASysLabels.questionToLabel.getValue("12.9"),
      answer = needsDetails.needs?.attitudeIssuesDetails,
      optional = isOptional(needsDetails.linksToHarm?.attitudeLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.attitudeLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.attitudeLinkedToReOffending,
    ),
  )

  private fun isOptional(linkedToHarm: Boolean?) = linkedToHarm != true
}
