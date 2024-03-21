package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DocumentLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DocumentFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.GroupedDocumentsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import java.time.ZoneOffset
import java.util.UUID

class DocumentTransformerTest {
  private val documentTransformer = DocumentTransformer()

  @ParameterizedTest
  @EnumSource
  fun `transformToApi transforms single documents correctly`(documentLevel: DocumentLevel) {
    val document = DocumentFactory()
      .withId(UUID.randomUUID().toString())
      .produce()

    val transformedDocument = documentTransformer.transformToApi(document, documentLevel)

    assertThat(transformedDocument.id).isEqualTo(document.id)
    assertThat(transformedDocument.level).isEqualTo(documentLevel)
    assertThat(transformedDocument.fileName).isEqualTo(document.documentName)
    assertThat(transformedDocument.createdAt).isEqualTo(document.createdAt.toInstant(ZoneOffset.UTC))
    assertThat(transformedDocument.typeCode).isEqualTo(document.type.code)
    assertThat(transformedDocument.typeDescription).isEqualTo(document.type.description)
    assertThat(transformedDocument.description).isEqualTo(document.extendedDescription)
  }

  @Test
  fun `transformToApi transforms document list correctly - filters out convictions other than one specified`() {
    val offenderLevelDocument = DocumentFactory()
      .withId(UUID.randomUUID().toString())
      .produce()
    val offenderLevelDocumentWithoutId = DocumentFactory()
      .withId(null)
      .produce()

    val convictionLevelDocument = DocumentFactory()
      .withId(UUID.randomUUID().toString())
      .produce()
    val convictionLevelDocumentWithoutId = DocumentFactory()
      .withId(null)
      .produce()

    val groupedDocuments = GroupedDocumentsFactory()
      .withOffenderLevelDocument(
        offenderLevelDocument,
      )
      .withOffenderLevelDocument(
        offenderLevelDocumentWithoutId,
      )
      .withConvictionLevelDocument(
        "12345",
        convictionLevelDocument,
      )
      .withConvictionLevelDocument(
        "12345",
        convictionLevelDocumentWithoutId,
      )
      .withConvictionLevelDocument(
        "6789",
        DocumentFactory().produce(),
      )
      .produce()

    val result = documentTransformer.transformToApi(groupedDocuments, 12345)

    assertThat(result).containsExactlyInAnyOrder(
      documentTransformer.transformToApi(offenderLevelDocument, DocumentLevel.offender),
      documentTransformer.transformToApi(offenderLevelDocumentWithoutId, DocumentLevel.offender),
      documentTransformer.transformToApi(convictionLevelDocument, DocumentLevel.conviction),
      documentTransformer.transformToApi(convictionLevelDocumentWithoutId, DocumentLevel.conviction),
    )
  }
}
