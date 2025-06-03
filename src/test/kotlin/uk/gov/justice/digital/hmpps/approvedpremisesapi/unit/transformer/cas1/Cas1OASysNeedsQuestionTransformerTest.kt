package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysNeedsQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysNeedsQuestionTransformer

class Cas1OASysNeedsQuestionTransformerTest {

  private val transformer = Cas1OASysNeedsQuestionTransformer()

  @Test
  fun `exclude irrelevant questions`() {
    val needsDetails = NeedsDetailsFactory()
      .withEmotionalIssuesDetails(linkedToHarm = true, linkedToReoffending = false)
      .withLifestyleIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      .withDrugIssuesDetails(linkedToHarm = true, linkedToReoffending = false)
      .withAlcoholIssuesDetails(linkedToHarm = false, linkedToReoffending = true)
      .withRelationshipIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      .withAccommodationIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      .withAttitudeIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      .withThinkingBehaviouralIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      // the following are excluded
      .withEducationTrainingEmploymentIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      .withFinanceIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      .produce()

    val result = transformer.transformToApi(needsDetails)

    assertThat(result.map { it.name }).containsExactlyInAnyOrder(
      "Emotional",
      "Accommodation",
      "Relationships",
      "Lifestyle",
      "Drugs",
      "Alcohol",
      "Thinking and Behavioural",
      "Attitude",
      "Health",
    )
  }

  @Test
  fun `If linked to harm is true, question is not optional`() {
    val needsDetails = NeedsDetailsFactory()
      .withEmotionalIssuesDetails(linkedToHarm = true, linkedToReoffending = false)
      .withLifestyleIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      .withDrugIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      .withAlcoholIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      .withRelationshipIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      .withAccommodationIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      .withAttitudeIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      .withThinkingBehaviouralIssuesDetails(linkedToHarm = true, linkedToReoffending = null)
      .produce()

    val result = transformer.transformToApi(needsDetails)

    assertThat(result).containsExactlyInAnyOrder(
      Cas1OASysNeedsQuestion(section = 10, name = "Emotional", optional = false, linkedToHarm = true, linkedToReOffending = false),
      Cas1OASysNeedsQuestion(section = 3, name = "Accommodation", optional = false, linkedToHarm = true, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 6, name = "Relationships", optional = false, linkedToHarm = true, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 7, name = "Lifestyle", optional = false, linkedToHarm = true, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 8, name = "Drugs", optional = false, linkedToHarm = true, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 9, name = "Alcohol", optional = false, linkedToHarm = true, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 11, name = "Thinking and Behavioural", optional = false, linkedToHarm = true, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 12, name = "Attitude", optional = false, linkedToHarm = true, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 13, name = "Health", optional = true, linkedToHarm = null, linkedToReOffending = null),
    )
  }

  @Test
  fun `If linked to harm is false, questions other than Drugs and Alcohol are optional`() {
    val needsDetails = NeedsDetailsFactory()
      .withEmotionalIssuesDetails(linkedToHarm = false, linkedToReoffending = false)
      .withLifestyleIssuesDetails(linkedToHarm = false, linkedToReoffending = null)
      .withDrugIssuesDetails(linkedToHarm = false, linkedToReoffending = null)
      .withAlcoholIssuesDetails(linkedToHarm = false, linkedToReoffending = null)
      .withRelationshipIssuesDetails(linkedToHarm = false, linkedToReoffending = null)
      .withAccommodationIssuesDetails(linkedToHarm = false, linkedToReoffending = null)
      .withAttitudeIssuesDetails(linkedToHarm = false, linkedToReoffending = null)
      .withThinkingBehaviouralIssuesDetails(linkedToHarm = false, linkedToReoffending = null)
      .produce()

    val result = transformer.transformToApi(needsDetails)

    assertThat(result).containsExactlyInAnyOrder(
      Cas1OASysNeedsQuestion(section = 10, name = "Emotional", optional = true, linkedToHarm = false, linkedToReOffending = false),
      Cas1OASysNeedsQuestion(section = 3, name = "Accommodation", optional = true, linkedToHarm = false, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 6, name = "Relationships", optional = true, linkedToHarm = false, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 7, name = "Lifestyle", optional = true, linkedToHarm = false, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 8, name = "Drugs", optional = false, linkedToHarm = false, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 9, name = "Alcohol", optional = false, linkedToHarm = false, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 11, name = "Thinking and Behavioural", optional = true, linkedToHarm = false, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 12, name = "Attitude", optional = true, linkedToHarm = false, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 13, name = "Health", optional = true, linkedToHarm = null, linkedToReOffending = null),
    )
  }

  @Test
  fun `If linked to harm is null, questions other than Drugs and Alcohol are optional`() {
    val needsDetails = NeedsDetailsFactory()
      .withEmotionalIssuesDetails(linkedToHarm = null, linkedToReoffending = false)
      .withLifestyleIssuesDetails(linkedToHarm = null, linkedToReoffending = null)
      .withDrugIssuesDetails(linkedToHarm = null, linkedToReoffending = null)
      .withAlcoholIssuesDetails(linkedToHarm = null, linkedToReoffending = null)
      .withRelationshipIssuesDetails(linkedToHarm = null, linkedToReoffending = null)
      .withAccommodationIssuesDetails(linkedToHarm = null, linkedToReoffending = null)
      .withAttitudeIssuesDetails(linkedToHarm = null, linkedToReoffending = null)
      .withThinkingBehaviouralIssuesDetails(linkedToHarm = null, linkedToReoffending = null)
      .produce()

    val result = transformer.transformToApi(needsDetails)

    assertThat(result).containsExactlyInAnyOrder(
      Cas1OASysNeedsQuestion(section = 10, name = "Emotional", optional = true, linkedToHarm = null, linkedToReOffending = false),
      Cas1OASysNeedsQuestion(section = 3, name = "Accommodation", optional = true, linkedToHarm = null, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 6, name = "Relationships", optional = true, linkedToHarm = null, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 7, name = "Lifestyle", optional = true, linkedToHarm = null, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 8, name = "Drugs", optional = false, linkedToHarm = null, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 9, name = "Alcohol", optional = false, linkedToHarm = null, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 11, name = "Thinking and Behavioural", optional = true, linkedToHarm = null, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 12, name = "Attitude", optional = true, linkedToHarm = null, linkedToReOffending = null),
      Cas1OASysNeedsQuestion(section = 13, name = "Health", optional = true, linkedToHarm = null, linkedToReOffending = null),
    )
  }
}
