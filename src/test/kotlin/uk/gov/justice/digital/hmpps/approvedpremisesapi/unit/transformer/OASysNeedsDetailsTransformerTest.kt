package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysNeedsDetailsTransformer

class OASysNeedsDetailsTransformerTest {
  private val transformer = OASysNeedsDetailsTransformer()

  @Test
  fun `transformToApi transforms correctly`() {
    val needsDetails = NeedsDetailsFactory()
      .withEmotionalIssuesDetails(emotionalIssuesDetails = null, linkedToHarm = false, linkedToReoffending = false)
      .withDrugIssuesDetails(drugIssuesDetails = null, linkedToHarm = true, linkedToReoffending = false)
      .withAlcoholIssuesDetails(alcoholIssuesDetails = null, linkedToHarm = false, linkedToReoffending = true)
      .withLifestyleIssuesDetails(lifestyleIssuesDetails = null, linkedToHarm = null, linkedToReoffending = null)
      .withRelationshipIssuesDetails(relationshipIssuesDetails = null, linkedToHarm = null, linkedToReoffending = null)
      .withFinanceIssuesDetails(financeIssuesDetails = null, linkedToHarm = null, linkedToReoffending = null)
      .withEducationTrainingEmploymentIssuesDetails(
        educationTrainingEmploymentIssuesDetails = null,
        linkedToHarm = null,
        linkedToReoffending = null,
      )
      .withAccommodationIssuesDetails(
        accommodationIssuesDetails = null,
        linkedToHarm = null,
        linkedToReoffending = null,
      )
      .withAttitudeIssuesDetails(attitudeIssuesDetails = null, linkedToHarm = null, linkedToReoffending = null)
      .withThinkingBehaviouralIssuesDetails(
        thinkingBehaviouralIssuesDetails = null,
        linkedToHarm = null,
        linkedToReoffending = null,
      )
      .produce()

    val result = transformer.transformToApi(needsDetails)

    assertThat(result).containsExactlyInAnyOrder(
      OASysSection(section = 10, name = "Emotional", linkedToHarm = false, linkedToReOffending = false),
      OASysSection(section = 3, name = "Accommodation", linkedToHarm = null, linkedToReOffending = null),
      OASysSection(section = 4, name = "Education, Training and Employment", linkedToHarm = null, linkedToReOffending = null),
      OASysSection(section = 5, name = "Finance", linkedToHarm = null, linkedToReOffending = null),
      OASysSection(section = 6, name = "Relationships", linkedToHarm = null, linkedToReOffending = null),
      OASysSection(section = 7, name = "Lifestyle", linkedToHarm = null, linkedToReOffending = null),
      OASysSection(section = 11, name = "Thinking and Behavioural", linkedToHarm = null, linkedToReOffending = null),
      OASysSection(section = 12, name = "Attitude", linkedToHarm = null, linkedToReOffending = null),
      OASysSection(section = 13, name = "Health", linkedToHarm = null, linkedToReOffending = null),
    )
  }
}
