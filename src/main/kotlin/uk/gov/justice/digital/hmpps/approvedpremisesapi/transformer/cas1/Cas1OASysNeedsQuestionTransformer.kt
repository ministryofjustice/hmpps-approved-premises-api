package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysNeedsQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysLabels

@Service
class Cas1OASysNeedsQuestionTransformer {

  fun transformToNeedsQuestion(needsDetails: NeedsDetails) = toQuestionState(needsDetails).map {
    Cas1OASysNeedsQuestion(
      section = it.sectionNumber,
      name = it.sectionLabel,
      optional = it.optional,
      linkedToHarm = it.linkedToHarm,
      linkedToReOffending = it.linkedToReOffending,
    )
  }

  private data class QuestionState(
    val sectionNumber: Int,
    val sectionLabel: String,
    val optional: Boolean,
    val linkedToHarm: Boolean?,
    val linkedToReOffending: Boolean?,
  )

  private fun toQuestionState(needsDetails: NeedsDetails) = listOf(
    QuestionState(
      sectionNumber = 3,
      sectionLabel = OASysLabels.sectionToLabel[3]!!,
      optional = isOptional(needsDetails.linksToHarm?.accommodationLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.accommodationLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.accommodationLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 6,
      sectionLabel = OASysLabels.sectionToLabel[6]!!,
      optional = isOptional(needsDetails.linksToHarm?.relationshipLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.relationshipLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.relationshipLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 7,
      sectionLabel = OASysLabels.sectionToLabel[7]!!,
      optional = isOptional(needsDetails.linksToHarm?.lifestyleLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.lifestyleLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.lifestyleLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 8,
      sectionLabel = OASysLabels.sectionToLabel[8]!!,
      optional = false,
      linkedToHarm = needsDetails.linksToHarm?.drugLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.drugLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 9,
      sectionLabel = OASysLabels.sectionToLabel[9]!!,
      optional = false,
      linkedToHarm = needsDetails.linksToHarm?.alcoholLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.alcoholLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 10,
      sectionLabel = OASysLabels.sectionToLabel[10]!!,
      optional = isOptional(needsDetails.linksToHarm?.emotionalLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.emotionalLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.emotionalLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 11,
      sectionLabel = OASysLabels.sectionToLabel[11]!!,
      optional = isOptional(needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.thinkingBehaviouralLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 12,
      sectionLabel = OASysLabels.sectionToLabel[12]!!,
      optional = isOptional(needsDetails.linksToHarm?.attitudeLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.attitudeLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.attitudeLinkedToReOffending,
    ),
    QuestionState(
      sectionNumber = 13,
      sectionLabel = OASysLabels.sectionToLabel[13]!!,
      optional = true,
      linkedToHarm = null,
      linkedToReOffending = null,
    ),
  )

  private fun isOptional(linkedToHarm: Boolean?) = linkedToHarm != true
}
