package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails

@Component
class NeedsDetailsTransformer {
  fun transformToApi(needsDetails: NeedsDetails) = listOf(
    OASysSection(
      section = 3,
      name = "Accommodation",
      linkedToHarm = needsDetails.linksToHarm?.accommodationLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.accommodationLinkedToReOffending
    ),
    OASysSection(
      section = 4,
      name = "Education, Training and Employment",
      linkedToHarm = needsDetails.linksToHarm?.educationTrainingEmploymentLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.educationTrainingEmploymentLinkedToReOffending
    ),
    OASysSection(
      section = 5,
      name = "Finance",
      linkedToHarm = needsDetails.linksToHarm?.financeLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.financeLinkedToReOffending
    ),
    OASysSection(
      section = 6,
      name = "Relationships",
      linkedToHarm = needsDetails.linksToHarm?.relationshipLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.relationshipLinkedToReOffending
    ),
    OASysSection(
      section = 7,
      name = "Lifestyle",
      linkedToHarm = needsDetails.linksToHarm?.lifestyleLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.lifestyleLinkedToReOffending
    ),
    OASysSection(
      section = 8,
      name = "Drugs",
      linkedToHarm = needsDetails.linksToHarm?.drugLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.drugLinkedToReOffending
    ),
    OASysSection(
      section = 9,
      name = "Alcohol",
      linkedToHarm = needsDetails.linksToHarm?.alcoholLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.alcoholLinkedToReOffending
    ),
    OASysSection(
      section = 10,
      name = "Emotional",
      linkedToHarm = needsDetails.linksToHarm?.emotionalLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.emotionalLinkedToReOffending
    ),
    OASysSection(
      section = 11,
      name = "Thinking and Behavioural",
      linkedToHarm = needsDetails.linksToHarm?.thinkingBehaviouralLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.thinkingBehaviouralLinkedToReOffending
    ),
    OASysSection(
      section = 12,
      name = "Attitude",
      linkedToHarm = needsDetails.linksToHarm?.attitudeLinkedToHarm,
      linkedToReOffending = needsDetails.linksToReOffending?.attitudeLinkedToReOffending
    )
  )
}
