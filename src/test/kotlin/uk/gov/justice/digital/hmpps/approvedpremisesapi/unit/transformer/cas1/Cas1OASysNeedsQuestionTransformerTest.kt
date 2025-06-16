package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysSupportingInformationQuestionMetaData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysNeedsQuestionTransformer

@ExtendWith(MockKExtension::class)
class Cas1OASysNeedsQuestionTransformerTest {

  @MockK
  lateinit var featureFlagService: FeatureFlagService

  @InjectMockKs
  lateinit var transformer: Cas1OASysNeedsQuestionTransformer

  @Nested
  inner class TransformToSupportingInformationMetadataQuestion {

    @Test
    fun `if no supporting information, return an empty list`() {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns false

      val result = transformer.transformToSupportingInformationMetadata(null)

      assertThat(result).isEmpty()
    }

    @Test
    fun `exclude irrelevant questions`() {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns false

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

      val result = transformer.transformToSupportingInformationMetadata(needsDetails)

      assertThat(result.map { it.sectionLabel }).containsExactlyInAnyOrder(
        "Emotional",
        "Accommodation",
        "Relationships",
        "Lifestyle",
        "Drugs",
        "Alcohol",
        "Thinking and Behavioural",
        "Attitude",
      )
    }

    @Test
    fun `If linked to harm is true, question is not optional`() {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns false

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

      val result = transformer.transformToSupportingInformationMetadata(needsDetails)

      assertThat(result).containsExactlyInAnyOrder(
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 10,
          sectionLabel = "Emotional",
          inclusionOptional = false,
          oasysAnswerLinkedToHarm = true,
          oasysAnswerLinkedToReOffending = false,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 3,
          sectionLabel = "Accommodation",
          inclusionOptional = false,
          oasysAnswerLinkedToHarm = true,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 6,
          sectionLabel = "Relationships",
          inclusionOptional = false,
          oasysAnswerLinkedToHarm = true,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 7,
          sectionLabel = "Lifestyle",
          inclusionOptional = false,
          oasysAnswerLinkedToHarm = true,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 8,
          sectionLabel = "Drugs",
          inclusionOptional = false,
          oasysAnswerLinkedToHarm = true,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 9,
          sectionLabel = "Alcohol",
          inclusionOptional = false,
          oasysAnswerLinkedToHarm = true,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 11,
          sectionLabel = "Thinking and Behavioural",
          inclusionOptional = false,
          oasysAnswerLinkedToHarm = true,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 12,
          sectionLabel = "Attitude",
          inclusionOptional = false,
          oasysAnswerLinkedToHarm = true,
          oasysAnswerLinkedToReOffending = null,
        ),
      )
    }

    @ParameterizedTest
    @CsvSource("false", "null", nullValues = ["null"])
    fun `If linked to harm is false or null, questions other than Drugs and Alcohol are optional - New Question Feature Flag Disabled`(linkedToHarm: Boolean?) {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns false

      val needsDetails = NeedsDetailsFactory()
        .withEmotionalIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = false)
        .withLifestyleIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .withDrugIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .withAlcoholIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .withRelationshipIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .withAccommodationIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .withAttitudeIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .withThinkingBehaviouralIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .produce()

      val result = transformer.transformToSupportingInformationMetadata(needsDetails)

      assertThat(result).containsExactlyInAnyOrder(
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 10,
          sectionLabel = "Emotional",
          inclusionOptional = true,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = false,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 3,
          sectionLabel = "Accommodation",
          inclusionOptional = true,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 6,
          sectionLabel = "Relationships",
          inclusionOptional = true,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 7,
          sectionLabel = "Lifestyle",
          inclusionOptional = true,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 8,
          sectionLabel = "Drugs",
          inclusionOptional = false,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 9,
          sectionLabel = "Alcohol",
          inclusionOptional = false,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 11,
          sectionLabel = "Thinking and Behavioural",
          inclusionOptional = true,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 12,
          sectionLabel = "Attitude",
          inclusionOptional = true,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
      )
    }

    @ParameterizedTest
    @CsvSource("false", "null", nullValues = ["null"])
    fun `If linked to harm is false or null, questions other than Accommodation, Drugs and Alcohol are optional - New Question Feature Flag Enabled`(linkedToHarm: Boolean?) {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns true

      val needsDetails = NeedsDetailsFactory()
        .withEmotionalIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = false)
        .withLifestyleIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .withDrugIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .withAlcoholIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .withRelationshipIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .withAccommodationIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .withAttitudeIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .withThinkingBehaviouralIssuesDetails(linkedToHarm = linkedToHarm, linkedToReoffending = null)
        .produce()

      val result = transformer.transformToSupportingInformationMetadata(needsDetails)

      assertThat(result).containsExactlyInAnyOrder(
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 10,
          sectionLabel = "Emotional",
          inclusionOptional = true,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = false,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 3,
          sectionLabel = "Accommodation",
          inclusionOptional = false,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 6,
          sectionLabel = "Relationships",
          inclusionOptional = true,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 7,
          sectionLabel = "Lifestyle",
          inclusionOptional = true,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 8,
          sectionLabel = "Drugs",
          inclusionOptional = false,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 9,
          sectionLabel = "Alcohol",
          inclusionOptional = false,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 11,
          sectionLabel = "Thinking and Behavioural",
          inclusionOptional = true,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
        Cas1OASysSupportingInformationQuestionMetaData(
          section = 12,
          sectionLabel = "Attitude",
          inclusionOptional = true,
          oasysAnswerLinkedToHarm = linkedToHarm,
          oasysAnswerLinkedToReOffending = null,
        ),
      )
    }
  }

  @Nested
  inner class TransformToOASysQuestion {

    @Test
    fun `If no assessment is available, always return question`() {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns false

      val result = transformer.transformToOASysQuestion(
        needsDetails = null,
        includeOptionalSections = emptyList(),
      )

      assertThat(result).containsExactlyInAnyOrder(
        OASysQuestion(
          questionNumber = "3.9",
          label = "Accommodation issues contributing to risks of offending and harm",
          answer = null,
        ),
        OASysQuestion(
          questionNumber = "6.9",
          label = "Relationship issues contributing to risks of offending and harm",
          answer = null,
        ),
        OASysQuestion(
          questionNumber = "7.9",
          label = "Lifestyle issues contributing to risks of offending and harm",
          answer = null,
        ),
        OASysQuestion(
          questionNumber = "8.9",
          label = "Drug misuse issues contributing to risks of offending and harm",
          answer = null,
        ),
        OASysQuestion(
          questionNumber = "9.9",
          label = "Alcohol misuse issues contributing to risks of offending and harm",
          answer = null,
        ),
        OASysQuestion(
          questionNumber = "10.9",
          label = "Issues of emotional well-being contributing to risks of offending and harm",
          answer = null,
        ),
        OASysQuestion(
          questionNumber = "11.9",
          label = "Thinking / behavioural issues contributing to risks of offending and harm",
          answer = null,
        ),
        OASysQuestion(
          questionNumber = "12.9",
          label = "Issues about attitudes contributing to risks of offending and harm",
          answer = null,
        ),
      )
    }

    @Test
    fun `If linked to harm is true, always return question`() {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns false

      val needsDetails = NeedsDetailsFactory()
        .withEmotionalIssuesDetails(linkedToHarm = true, linkedToReoffending = false, emotionalIssuesDetails = "emotional answer")
        .withLifestyleIssuesDetails(linkedToHarm = true, linkedToReoffending = null, lifestyleIssuesDetails = "lifestyle answer")
        .withDrugIssuesDetails(linkedToHarm = true, linkedToReoffending = null, drugIssuesDetails = "drug answer")
        .withAlcoholIssuesDetails(linkedToHarm = true, linkedToReoffending = null, alcoholIssuesDetails = "alcohol answer")
        .withRelationshipIssuesDetails(linkedToHarm = true, linkedToReoffending = null, relationshipIssuesDetails = "relationship answer")
        .withAccommodationIssuesDetails(linkedToHarm = true, linkedToReoffending = null, accommodationIssuesDetails = "accommodation answer")
        .withAttitudeIssuesDetails(linkedToHarm = true, linkedToReoffending = null, attitudeIssuesDetails = "attitude answer")
        .withThinkingBehaviouralIssuesDetails(linkedToHarm = true, linkedToReoffending = null, thinkingBehaviouralIssuesDetails = "thinking behavioural answer")
        .produce()

      val result = transformer.transformToOASysQuestion(needsDetails, includeOptionalSections = emptyList())

      assertThat(result).containsExactlyInAnyOrder(
        OASysQuestion(
          questionNumber = "3.9",
          label = "Accommodation issues contributing to risks of offending and harm",
          answer = "accommodation answer",
        ),
        OASysQuestion(
          questionNumber = "6.9",
          label = "Relationship issues contributing to risks of offending and harm",
          answer = "relationship answer",
        ),
        OASysQuestion(
          questionNumber = "7.9",
          label = "Lifestyle issues contributing to risks of offending and harm",
          answer = "lifestyle answer",
        ),
        OASysQuestion(
          questionNumber = "8.9",
          label = "Drug misuse issues contributing to risks of offending and harm",
          answer = "drug answer",
        ),
        OASysQuestion(
          questionNumber = "9.9",
          label = "Alcohol misuse issues contributing to risks of offending and harm",
          answer = "alcohol answer",
        ),
        OASysQuestion(
          questionNumber = "10.9",
          label = "Issues of emotional well-being contributing to risks of offending and harm",
          answer = "emotional answer",
        ),
        OASysQuestion(
          questionNumber = "11.9",
          label = "Thinking / behavioural issues contributing to risks of offending and harm",
          answer = "thinking behavioural answer",
        ),
        OASysQuestion(
          questionNumber = "12.9",
          label = "Issues about attitudes contributing to risks of offending and harm",
          answer = "attitude answer",
        ),
      )
    }

    @ParameterizedTest
    @CsvSource("false", "null", nullValues = ["null"])
    fun `If linked to harm is false or null, only Drugs and Alcohol and selected questions are returned, None selected - New Question Feature Flag Disabled`(linkToHarm: Boolean?) {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns false

      val needsDetails = NeedsDetailsFactory()
        .withEmotionalIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = false)
        .withLifestyleIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null)
        .withDrugIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, drugIssuesDetails = "drug answer")
        .withAlcoholIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, alcoholIssuesDetails = "alcohol answer")
        .withRelationshipIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null)
        .withAccommodationIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null)
        .withAttitudeIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null)
        .withThinkingBehaviouralIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null)
        .produce()

      val result = transformer.transformToOASysQuestion(needsDetails, includeOptionalSections = emptyList())

      assertThat(result).containsExactlyInAnyOrder(
        OASysQuestion(
          questionNumber = "8.9",
          label = "Drug misuse issues contributing to risks of offending and harm",
          answer = "drug answer",
        ),
        OASysQuestion(
          questionNumber = "9.9",
          label = "Alcohol misuse issues contributing to risks of offending and harm",
          answer = "alcohol answer",
        ),
      )
    }

    @ParameterizedTest
    @CsvSource("false", "null", nullValues = ["null"])
    fun `If linked to harm is false or null, only Accommodation, Drugs, Alcohol and selected questions are returned, None selected - New Question Feature Flag Enabled`(linkToHarm: Boolean?) {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns true

      val needsDetails = NeedsDetailsFactory()
        .withEmotionalIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = false)
        .withLifestyleIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null)
        .withDrugIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, drugIssuesDetails = "drug answer")
        .withAlcoholIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, alcoholIssuesDetails = "alcohol answer")
        .withRelationshipIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null)
        .withAccommodationIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, accommodationIssuesDetails = "accommodation answer")
        .withAttitudeIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null)
        .withThinkingBehaviouralIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null)
        .produce()

      val result = transformer.transformToOASysQuestion(needsDetails, includeOptionalSections = emptyList())

      assertThat(result).containsExactlyInAnyOrder(
        OASysQuestion(
          questionNumber = "3.9",
          label = "Accommodation issues contributing to risks of offending and harm",
          answer = "accommodation answer",
        ),
        OASysQuestion(
          questionNumber = "8.9",
          label = "Drug misuse issues contributing to risks of offending and harm",
          answer = "drug answer",
        ),
        OASysQuestion(
          questionNumber = "9.9",
          label = "Alcohol misuse issues contributing to risks of offending and harm",
          answer = "alcohol answer",
        ),
      )
    }

    @ParameterizedTest
    @CsvSource("false", "null", nullValues = ["null"])
    fun `If linked to harm is false or null, only Drugs and Alcohol and selected questions are returned, All selected - New Question Feature Flag Disabled`(linkToHarm: Boolean?) {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns false

      val needsDetails = NeedsDetailsFactory()
        .withEmotionalIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = false, emotionalIssuesDetails = "emotional answer")
        .withLifestyleIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, lifestyleIssuesDetails = "lifestyle answer")
        .withDrugIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, drugIssuesDetails = "drug answer")
        .withAlcoholIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, alcoholIssuesDetails = "alcohol answer")
        .withRelationshipIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, relationshipIssuesDetails = "relationship answer")
        .withAccommodationIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, accommodationIssuesDetails = "accommodation answer")
        .withAttitudeIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, attitudeIssuesDetails = "attitude answer")
        .withThinkingBehaviouralIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, thinkingBehaviouralIssuesDetails = "thinking behavioural answer")
        .produce()

      val result = transformer.transformToOASysQuestion(
        needsDetails,
        includeOptionalSections = listOf(3, 6, 7, 10, 11, 12),
      )

      assertThat(result).containsExactlyInAnyOrder(
        OASysQuestion(
          questionNumber = "3.9",
          label = "Accommodation issues contributing to risks of offending and harm",
          answer = "accommodation answer",
        ),
        OASysQuestion(
          questionNumber = "6.9",
          label = "Relationship issues contributing to risks of offending and harm",
          answer = "relationship answer",
        ),
        OASysQuestion(
          questionNumber = "7.9",
          label = "Lifestyle issues contributing to risks of offending and harm",
          answer = "lifestyle answer",
        ),
        OASysQuestion(
          questionNumber = "8.9",
          label = "Drug misuse issues contributing to risks of offending and harm",
          answer = "drug answer",
        ),
        OASysQuestion(
          questionNumber = "9.9",
          label = "Alcohol misuse issues contributing to risks of offending and harm",
          answer = "alcohol answer",
        ),
        OASysQuestion(
          questionNumber = "10.9",
          label = "Issues of emotional well-being contributing to risks of offending and harm",
          answer = "emotional answer",
        ),
        OASysQuestion(
          questionNumber = "11.9",
          label = "Thinking / behavioural issues contributing to risks of offending and harm",
          answer = "thinking behavioural answer",
        ),
        OASysQuestion(
          questionNumber = "12.9",
          label = "Issues about attitudes contributing to risks of offending and harm",
          answer = "attitude answer",
        ),
      )
    }

    @ParameterizedTest
    @CsvSource("false", "null", nullValues = ["null"])
    fun `If linked to harm is false or null, only Accommodation, Drugs, Alcohol and selected questions are returned, All selected - New Question Feature Flag Enabled`(linkToHarm: Boolean?) {
      every { featureFlagService.getBooleanFlag("cas1-oasys-use-new-questions") } returns true

      val needsDetails = NeedsDetailsFactory()
        .withEmotionalIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = false, emotionalIssuesDetails = "emotional answer")
        .withLifestyleIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, lifestyleIssuesDetails = "lifestyle answer")
        .withDrugIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, drugIssuesDetails = "drug answer")
        .withAlcoholIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, alcoholIssuesDetails = "alcohol answer")
        .withRelationshipIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, relationshipIssuesDetails = "relationship answer")
        .withAccommodationIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, accommodationIssuesDetails = "accommodation answer")
        .withAttitudeIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, attitudeIssuesDetails = "attitude answer")
        .withThinkingBehaviouralIssuesDetails(linkedToHarm = linkToHarm, linkedToReoffending = null, thinkingBehaviouralIssuesDetails = "thinking behavioural answer")
        .produce()

      val result = transformer.transformToOASysQuestion(
        needsDetails,
        includeOptionalSections = listOf(6, 7, 10, 11, 12),
      )

      assertThat(result).containsExactlyInAnyOrder(
        OASysQuestion(
          questionNumber = "3.9",
          label = "Accommodation issues contributing to risks of offending and harm",
          answer = "accommodation answer",
        ),
        OASysQuestion(
          questionNumber = "6.9",
          label = "Relationship issues contributing to risks of offending and harm",
          answer = "relationship answer",
        ),
        OASysQuestion(
          questionNumber = "7.9",
          label = "Lifestyle issues contributing to risks of offending and harm",
          answer = "lifestyle answer",
        ),
        OASysQuestion(
          questionNumber = "8.9",
          label = "Drug misuse issues contributing to risks of offending and harm",
          answer = "drug answer",
        ),
        OASysQuestion(
          questionNumber = "9.9",
          label = "Alcohol misuse issues contributing to risks of offending and harm",
          answer = "alcohol answer",
        ),
        OASysQuestion(
          questionNumber = "10.9",
          label = "Issues of emotional well-being contributing to risks of offending and harm",
          answer = "emotional answer",
        ),
        OASysQuestion(
          questionNumber = "11.9",
          label = "Thinking / behavioural issues contributing to risks of offending and harm",
          answer = "thinking behavioural answer",
        ),
        OASysQuestion(
          questionNumber = "12.9",
          label = "Issues about attitudes contributing to risks of offending and harm",
          answer = "attitude answer",
        ),
      )
    }
  }
}
