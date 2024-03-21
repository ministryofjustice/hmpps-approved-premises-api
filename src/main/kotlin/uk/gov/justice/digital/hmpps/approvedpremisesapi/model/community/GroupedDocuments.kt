package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID

data class GroupedDocuments(
  val documents: List<Document>,
  val convictions: List<ConvictionDocuments>,
) {
  fun findDocument(documentId: String) = documents.firstOrNull { it.id == documentId } ?: convictions.flatMap { it.documents }.firstOrNull { it.id == documentId }
}

data class Document(
  @JsonProperty("id")
  private val _id: String?,
  val documentName: String,
  val author: String?,
  val type: DocumentType,
  val extendedDescription: String?,
  val createdAt: LocalDateTime,
  val lastModifiedAt: LocalDateTime?,
  val parentPrimaryKeyId: Long?,
) {
  // Some Documents don't have IDs, so we need to generate an ID based on the document name
  // and created at date, which we assume to be immutable
  val id: String
    get() = this._id ?: UUID.nameUUIDFromBytes(
      listOf(this.documentName, this.createdAt).joinToString("").toByteArray(),
    ).toString()
}

data class ConvictionDocuments(
  val convictionId: String,
  val documents: List<Document>,
)

data class DocumentType(
  val code: String,
  val description: String,
)
