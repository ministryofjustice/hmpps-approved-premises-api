package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysNeedsQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NeedsDetailsTransformer.Companion.sectionToName

@Service
class Cas1OASysNeedsQuestionTransformer {

  fun transformToApi(needsDetails: NeedsDetails) = toQuestionState(needsDetails).map {
    Cas1OASysNeedsQuestion(
      section = it.section,
      name = it.sectionLabel,
      optional = it.optional,
      linkedToHarm = it.linkedToHarm,
      linkedToReOffending = it.linkedToReOffending,
    )
  }

  private data class QuestionState(
    val section: Int,
    val sectionLabel: String,
    val optional: Boolean,
    val linkedToHarm: Boolean?,
    val linkedToReOffending: Boolean?,
  )

  private fun toQuestionState(needsDetails: NeedsDetails) = listOf(
    QuestionState(
      section = 3,
      sectionLabel = sectionToName[3]!!,
      optional = isOptional(needsDetails.linksToHarm?.accommodationLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.accommodationLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.accommodationLinkedToReOffending,
    ),
    QuestionState(
      section = 6,
      sectionLabel = sectionToName[6]!!,
      optional = isOptional(needsDetails.linksToHarm?.relationshipLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.relationshipLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.relationshipLinkedToReOffending,
    ),
    QuestionState(
      section = 7,
      sectionLabel = sectionToName[7]!!,
      optional = isOptional(needsDetails.linksToHarm?.lifestyleLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.lifestyleLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.lifestyleLinkedToReOffending,
    ),
    QuestionState(
      section = 10,
      sectionLabel = sectionToName[10]!!,
      optional = isOptional(needsDetails.linksToHarm?.emotionalLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.emotionalLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.emotionalLinkedToReOffending,
    ),
    QuestionState(
      section = 11,
      sectionLabel = sectionToName[11]!!,
      optional = isOptional(needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.thinkingBehaviouralLinkedToReOffending,
    ),
    QuestionState(
      section = 12,
      sectionLabel = sectionToName[12]!!,
      optional = isOptional(needsDetails.linksToHarm?.attitudeLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.attitudeLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.attitudeLinkedToReOffending,
    ),
    QuestionState(
      section = 13,
      sectionLabel = sectionToName[13]!!,
      optional = true,
      linkedToHarm = null,
      linkedToReOffending = null,
    ),
  )

  private fun isOptional(linkedToHarm: Boolean?) = linkedToHarm != true
}
