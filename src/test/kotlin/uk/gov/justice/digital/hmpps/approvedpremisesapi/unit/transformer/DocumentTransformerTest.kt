package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DocumentLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DocumentFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DocumentFromDeliusApiFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.GroupedDocumentsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class DocumentTransformerTest {
  private val documentTransformer = DocumentTransformer()

  @Test
  fun `transformToApi transforms correctly`() {
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

    val result = documentTransformer.transformToApi(groupedDocuments)

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
  fun `transformToApi returns all documents`() {
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

    val result = documentTransformer.transformToApi(groupedDocuments)

    assertThat(result).hasSize(3)
    assertThat(result[0].fileName).isEqualTo("offender_level_doc.pdf")
    assertThat(result[1].fileName).isEqualTo("conviction_level_doc.pdf")
    assertThat(result[2].fileName).isEqualTo("conviction_level_doc_2.pdf")
  }

  @Test
  fun `transformToApi filters out docs with no id`() {
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

    val result = documentTransformer.transformToApi(groupedDocuments)

    assertThat(result).hasSize(3)
    assertThat(result[0].fileName).isEqualTo("offender_level_doc.pdf")
    assertThat(result[1].fileName).isEqualTo("conviction_level_doc.pdf")
    assertThat(result[2].fileName).isEqualTo("conviction_level_doc_3.pdf")
  }

  @Test
  fun `transformToApi transforms correctly AP and Delius API Doc`() {
    val offenderDocId = UUID.randomUUID()
    val convictionDocId = UUID.randomUUID()
    val deliusDocs = stubDocumentsFromDelius(offenderDocId, convictionDocId)

    val result = documentTransformer.transformToApi(deliusDocs)

    assertThat(result).containsExactlyInAnyOrder(
      Document(
        id = offenderDocId.toString(),
        level = DocumentLevel.offender,
        fileName = "offender_level_doc.pdf",
        createdAt = Instant.parse("2024-03-02T15:20:00Z"),
        typeCode = "TYPE-1",
        typeDescription = "Type 1 Description",
        description = "Offender level doc description",
      ),
      Document(
        id = convictionDocId.toString(),
        level = DocumentLevel.conviction,
        fileName = "conviction_level_doc.pdf",
        createdAt = Instant.parse("2024-10-02T10:40:00Z"),
        typeCode = "TYPE-2",
        typeDescription = "Type 2 Description",
        description = "Conviction level doc description",
      ),
    )
  }

  @Test
  fun `transformToApi filters out doc with no id for AP and Delius API Docs`() {
    val offenderDocId = UUID.randomUUID()
    val convictionDocId = UUID.randomUUID()
    val deliusDocs = stubDocumentsFromDelius(offenderDocId, convictionDocId) + listOf(
      DocumentFromDeliusApiFactory()
        .withId(null)
        .withDescription("Null Id description")
        .withLevel(DocumentLevel.offender.value)
        .withEventNumber("2")
        .withFilename("Null_Id.pdf")
        .withTypeCode("Null Id type")
        .withTypeDescription("Null Id Type Description")
        .withDateSaved(LocalDateTime.parse("2024-04-18T06:00:00").atZone(ZoneId.systemDefault()))
        .withDateCreated(LocalDateTime.parse("2024-04-02T15:20:00").atZone(ZoneId.systemDefault()))
        .produce(),
    )

    val result = documentTransformer.transformToApi(deliusDocs)

    assertThat(result).hasSize(2)
    assertThat(result[0].fileName).isEqualTo("offender_level_doc.pdf")
    assertThat(result[1].fileName).isEqualTo("conviction_level_doc.pdf")
  }

  private fun stubDocumentsFromDelius(offenderDocId: UUID, convictionDocId: UUID): List<APDeliusDocument> =
    listOf(
      DocumentFromDeliusApiFactory()
        .withId(offenderDocId.toString())
        .withDescription("Offender level doc description")
        .withLevel(DocumentLevel.offender.value)
        .withEventNumber("2")
        .withFilename("offender_level_doc.pdf")
        .withTypeCode("TYPE-1")
        .withTypeDescription("Type 1 Description")
        .withDateSaved(LocalDateTime.parse("2024-03-18T06:00:00").atZone(ZoneId.of("UTC")))
        .withDateCreated(LocalDateTime.parse("2024-03-02T15:20:00").atZone(ZoneId.of("UTC")))
        .produce(),
      DocumentFromDeliusApiFactory()
        .withId(convictionDocId.toString())
        .withDescription("Conviction level doc description")
        .withLevel(DocumentLevel.conviction.value)
        .withEventNumber("1")
        .withFilename("conviction_level_doc.pdf")
        .withTypeCode("TYPE-2")
        .withTypeDescription("Type 2 Description")
        .withDateSaved(LocalDateTime.parse("2024-10-05T13:12:00").atZone(ZoneId.of("UTC")))
        .withDateCreated(LocalDateTime.parse("2024-10-02T10:40:00").atZone(ZoneId.of("UTC")))
        .produce(),
    )
}
