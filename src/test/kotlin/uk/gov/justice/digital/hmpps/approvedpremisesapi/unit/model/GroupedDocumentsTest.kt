package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DocumentFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.GroupedDocumentsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Document
import java.util.UUID

class GroupedDocumentsTest {
  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  @Test
  fun `Document serializes and deserializes correctly`() {
    val id = UUID.randomUUID().toString()
    val document = DocumentFactory()
      .withId(id)
      .produce()

    val jsonString = objectMapper.writeValueAsString(document)
    val convertedObject = objectMapper.readValue(jsonString, Document::class.java)

    assertThat(jsonString).contains(id)
    assertThat(convertedObject.id).isEqualTo(id)
  }

  @Test
  fun `Document sets an ID using the document name when the ID is null`() {
    val document = DocumentFactory()
      .withId(null)
      .produce()

    val generatedId = UUID.nameUUIDFromBytes(
      listOf(document.documentName, document.createdAt).joinToString("").toByteArray(),
    ).toString()

    assertThat(document.id).isEqualTo(generatedId)
  }

  @Test
  fun `Document returns the original ID when it exists`() {
    val id = UUID.randomUUID().toString()
    val document = DocumentFactory()
      .withId(id)
      .produce()

    val generatedId = UUID.nameUUIDFromBytes(
      listOf(document.documentName, document.createdAt).joinToString("").toByteArray(),
    ).toString()

    assertThat(document.id).isEqualTo(id)
  }

  @Test
  fun `findDocument finds a document by ID`() {
    val documentWithId = DocumentFactory().produce()
    val documentWithoutId = DocumentFactory().withId(null).produce()
    val convictionLevelDocument = DocumentFactory().produce()
    val convictionLevelDocumentWithoutId = DocumentFactory().withId(null).produce()

    val groupedDocuments = GroupedDocumentsFactory()
      .withOffenderLevelDocument(
        documentWithId,
      )
      .withOffenderLevelDocument(
        documentWithoutId,
      )
      .withConvictionLevelDocument(
        "12345",
        convictionLevelDocument,
      )
      .withConvictionLevelDocument(
        "12345",
        convictionLevelDocumentWithoutId,
      )
      .produce()

    assertThat(groupedDocuments.findDocument(documentWithId.id)).isEqualTo(documentWithId)
    assertThat(groupedDocuments.findDocument(documentWithoutId.id)).isEqualTo(documentWithoutId)
    assertThat(groupedDocuments.findDocument(convictionLevelDocument.id)).isEqualTo(convictionLevelDocument)
    assertThat(groupedDocuments.findDocument(convictionLevelDocumentWithoutId.id)).isEqualTo(convictionLevelDocumentWithoutId)
  }
}
