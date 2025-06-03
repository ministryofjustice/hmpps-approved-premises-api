package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysNeedsQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NeedsDetailsTransformer.Companion.sectionToName

@Service
class Cas1OASysNeedsQuestionTransformer {

  fun transformToApi(needsDetails: NeedsDetails) = listOf(
    Cas1OASysNeedsQuestion(
      section = 3,
      name = sectionToName[3]!!,
      optional = isOptional(needsDetails.linksToHarm?.accommodationLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.accommodationLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.accommodationLinkedToReOffending,
    ),
    Cas1OASysNeedsQuestion(
      section = 6,
      name = sectionToName[6]!!,
      optional = isOptional(needsDetails.linksToHarm?.relationshipLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.relationshipLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.relationshipLinkedToReOffending,
    ),
    Cas1OASysNeedsQuestion(
      section = 7,
      name = sectionToName[7]!!,
      optional = isOptional(needsDetails.linksToHarm?.lifestyleLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.lifestyleLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.lifestyleLinkedToReOffending,
    ),
    Cas1OASysNeedsQuestion(
      section = 10,
      name = sectionToName[10]!!,
      optional = isOptional(needsDetails.linksToHarm?.emotionalLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.emotionalLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.emotionalLinkedToReOffending,
    ),
    Cas1OASysNeedsQuestion(
      section = 11,
      name = sectionToName[11]!!,
      optional = isOptional(needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.thinkingBehaviouralLinkedToReOffending,
    ),
    Cas1OASysNeedsQuestion(
      section = 12,
      name = sectionToName[12]!!,
      optional = isOptional(needsDetails.linksToHarm?.attitudeLinkedToHarm),
      linkedToHarm = needsDetails.linksToHarm?.attitudeLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.attitudeLinkedToReOffending,
    ),
    Cas1OASysNeedsQuestion(
      section = 13,
      name = sectionToName[13]!!,
      optional = true,
      linkedToHarm = null,
      linkedToReOffending = null,
    ),
  )

  private fun isOptional(linkedToHarm: Boolean?) = linkedToHarm != true
}
