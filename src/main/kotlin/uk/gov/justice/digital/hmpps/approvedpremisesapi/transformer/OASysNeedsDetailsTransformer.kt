package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysNeedsQuestionTransformer

/**
 * Note that there is a CAS1 specific version of this transformer [Cas1OASysNeedsQuestionTransformer]
 * that deduplicates logic in this transformer and [OASysSectionsTransformer]
 */
@Component
class OASysNeedsDetailsTransformer {

  fun transformToApi(needsDetails: NeedsDetails) = listOf(
    OASysSection(
      section = 3,
      name = OASysLabels.sectionToLabel[3]!!,
      linkedToHarm = needsDetails.linksToHarm?.accommodationLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.accommodationLinkedToReOffending,
    ),
    OASysSection(
      section = 4,
      name = OASysLabels.sectionToLabel[4]!!,
      linkedToHarm = needsDetails.linksToHarm?.educationTrainingEmploymentLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.educationTrainingEmploymentLinkedToReOffending,
    ),
    OASysSection(
      section = 5,
      name = OASysLabels.sectionToLabel[5]!!,
      linkedToHarm = needsDetails.linksToHarm?.financeLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.financeLinkedToReOffending,
    ),
    OASysSection(
      section = 6,
      name = OASysLabels.sectionToLabel[6]!!,
      linkedToHarm = needsDetails.linksToHarm?.relationshipLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.relationshipLinkedToReOffending,
    ),
    OASysSection(
      section = 7,
      name = OASysLabels.sectionToLabel[7]!!,
      linkedToHarm = needsDetails.linksToHarm?.lifestyleLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.lifestyleLinkedToReOffending,
    ),
    OASysSection(
      section = 10,
      name = OASysLabels.sectionToLabel[10]!!,
      linkedToHarm = needsDetails.linksToHarm?.emotionalLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.emotionalLinkedToReOffending,
    ),
    OASysSection(
      section = 11,
      name = OASysLabels.sectionToLabel[11]!!,
      linkedToHarm = needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.thinkingBehaviouralLinkedToReOffending,
    ),
    OASysSection(
      section = 12,
      name = OASysLabels.sectionToLabel[12]!!,
      linkedToHarm = needsDetails.linksToHarm?.attitudeLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.attitudeLinkedToReOffending,
    ),
    OASysSection(
      section = 13,
      name = OASysLabels.sectionToLabel[13]!!,
      linkedToHarm = null,
      linkedToReOffending = null,
    ),
  )
}
