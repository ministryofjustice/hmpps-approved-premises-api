package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DocumentLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DocumentFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.GroupedDocumentsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class DocumentTransformerTest {
  private val documentTransformer = DocumentTransformer()

  @Test
  fun `transformToApiUnfiltered transforms correctly`() {
    val groupedDocuments = GroupedDocumentsFactory()
      .withOffenderLevelDocument(
        DocumentFactory()
          .withId(UUID.fromString("b0df5ec4-5685-4b02-8a95-91b6da80156f").toString())
          .withDocumentName("offender_level_doc.pdf")
          .withTypeCode("TYPE-1")
          .withTypeDescription("Type 1 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T11:40:00"))
          .withExtendedDescription("Extended Description 1")
          .produce(),
      )
      .withConvictionLevelDocument(
        "12345",
        DocumentFactory()
          .withId(UUID.fromString("457af8a5-82b1-449a-ad03-032b39435865").toString())
          .withDocumentName("conviction_level_doc.pdf")
          .withoutAuthor()
          .withTypeCode("TYPE-2")
          .withTypeDescription("Type 2 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T10:40:00"))
          .withExtendedDescription("Extended Description 2")
          .produce(),
      )
      .produce()

    val result = documentTransformer.transformToApiUnfiltered(groupedDocuments)

    assertThat(result).containsExactlyInAnyOrder(
      Document(
        id = UUID.fromString("b0df5ec4-5685-4b02-8a95-91b6da80156f").toString(),
        level = DocumentLevel.offender,
        fileName = "offender_level_doc.pdf",
        createdAt = Instant.parse("2022-12-07T11:40:00Z"),
        typeCode = "TYPE-1",
        typeDescription = "Type 1 Description",
        description = "Extended Description 1",
      ),
      Document(
        id = UUID.fromString("457af8a5-82b1-449a-ad03-032b39435865").toString(),
        level = DocumentLevel.conviction,
        fileName = "conviction_level_doc.pdf",
        createdAt = Instant.parse("2022-12-07T10:40:00Z"),
        typeCode = "TYPE-2",
        typeDescription = "Type 2 Description",
        description = "Extended Description 2",
      ),
    )
  }

  @Test
  fun `transformToApiFiltered filters out convictions other than one specified`() {
    val groupedDocuments = GroupedDocumentsFactory()
      .withOffenderLevelDocument(
        DocumentFactory()
          .withId(UUID.fromString("b0df5ec4-5685-4b02-8a95-91b6da80156f").toString())
          .withDocumentName("offender_level_doc.pdf")
          .withTypeCode("TYPE-1")
          .withTypeDescription("Type 1 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T11:40:00"))
          .withExtendedDescription("Extended Description 1")
          .produce(),
      )
      .withConvictionLevelDocument(
        "12345",
        DocumentFactory()
          .withId(UUID.fromString("457af8a5-82b1-449a-ad03-032b39435865").toString())
          .withDocumentName("conviction_level_doc.pdf")
          .withoutAuthor()
          .withTypeCode("TYPE-2")
          .withTypeDescription("Type 2 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T10:40:00"))
          .withExtendedDescription("Extended Description 2")
          .produce(),
      )
      .withConvictionLevelDocument(
        "6789",
        DocumentFactory()
          .withId(UUID.fromString("e20589b3-7f83-4502-a0df-c8dd645f3f44").toString())
          .withDocumentName("conviction_level_doc_2.pdf")
          .withTypeCode("TYPE-2")
          .withTypeDescription("Type 2 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T10:40:00"))
          .withExtendedDescription("Extended Description 2")
          .produce(),
      )
      .produce()

    val result = documentTransformer.transformToApiFiltered(groupedDocuments, 12345)

    assertThat(result).hasSize(2)
    assertThat(result[0].fileName).isEqualTo("offender_level_doc.pdf")
    assertThat(result[1].fileName).isEqualTo("conviction_level_doc.pdf")
  }

  @Test
  fun `transformToApiUnfiltered returns all documents`() {
    val groupedDocuments = GroupedDocumentsFactory()
      .withOffenderLevelDocument(
        DocumentFactory()
          .withId(UUID.fromString("b0df5ec4-5685-4b02-8a95-91b6da80156f").toString())
          .withDocumentName("offender_level_doc.pdf")
          .withTypeCode("TYPE-1")
          .withTypeDescription("Type 1 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T11:40:00"))
          .withExtendedDescription("Extended Description 1")
          .produce(),
      )
      .withConvictionLevelDocument(
        "12345",
        DocumentFactory()
          .withId(UUID.fromString("457af8a5-82b1-449a-ad03-032b39435865").toString())
          .withDocumentName("conviction_level_doc.pdf")
          .withoutAuthor()
          .withTypeCode("TYPE-2")
          .withTypeDescription("Type 2 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T10:40:00"))
          .withExtendedDescription("Extended Description 2")
          .produce(),
      )
      .withConvictionLevelDocument(
        "6789",
        DocumentFactory()
          .withId(UUID.fromString("e20589b3-7f83-4502-a0df-c8dd645f3f44").toString())
          .withDocumentName("conviction_level_doc_2.pdf")
          .withTypeCode("TYPE-2")
          .withTypeDescription("Type 2 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T10:40:00"))
          .withExtendedDescription("Extended Description 2")
          .produce(),
      )
      .produce()

    val result = documentTransformer.transformToApiUnfiltered(groupedDocuments)

    assertThat(result).hasSize(3)
    assertThat(result[0].fileName).isEqualTo("offender_level_doc.pdf")
    assertThat(result[1].fileName).isEqualTo("conviction_level_doc.pdf")
    assertThat(result[2].fileName).isEqualTo("conviction_level_doc_2.pdf")
  }

  @Test
  fun `transformToApiUnfiltered filters out docs with no id`() {
    val groupedDocuments = GroupedDocumentsFactory()
      .withOffenderLevelDocument(
        DocumentFactory()
          .withId(UUID.fromString("b0df5ec4-5685-4b02-8a95-91b6da80156f").toString())
          .withDocumentName("offender_level_doc.pdf")
          .withTypeCode("TYPE-1")
          .withTypeDescription("Type 1 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T11:40:00"))
          .withExtendedDescription("Extended Description 1")
          .produce(),
      )
      .withOffenderLevelDocument(
        DocumentFactory()
          .withId(null)
          .produce(),
      )
      .withConvictionLevelDocument(
        "12345",
        DocumentFactory()
          .withId(UUID.fromString("457af8a5-82b1-449a-ad03-032b39435865").toString())
          .withDocumentName("conviction_level_doc.pdf")
          .withoutAuthor()
          .withTypeCode("TYPE-2")
          .withTypeDescription("Type 2 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T10:40:00"))
          .withExtendedDescription("Extended Description 2")
          .produce(),
      )
      .withConvictionLevelDocument(
        "12345",
        DocumentFactory()
          .withId(null)
          .withDocumentName("conviction_level_doc_2.pdf")
          .produce(),
      )
      .withConvictionLevelDocument(
        "6789",
        DocumentFactory()
          .withId(UUID.fromString("e20589b3-7f83-4502-a0df-c8dd645f3f44").toString())
          .withDocumentName("conviction_level_doc_3.pdf")
          .withTypeCode("TYPE-2")
          .withTypeDescription("Type 2 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T10:40:00"))
          .withExtendedDescription("Extended Description 2")
          .produce(),
      )
      .produce()

    val result = documentTransformer.transformToApiUnfiltered(groupedDocuments)

    assertThat(result).hasSize(3)
    assertThat(result[0].fileName).isEqualTo("offender_level_doc.pdf")
    assertThat(result[1].fileName).isEqualTo("conviction_level_doc.pdf")
    assertThat(result[2].fileName).isEqualTo("conviction_level_doc_3.pdf")
  }
}
