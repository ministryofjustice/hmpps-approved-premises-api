package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

import java.time.LocalDateTime

data class GroupedDocuments(
  val documents: List<Document>,
  val convictions: List<ConvictionDocuments>,
) {
  fun findDocument(documentId: String) = documents.firstOrNull { it.id == documentId } ?: convictions.flatMap { it.documents }.firstOrNull { it.id == documentId }
}

data class Document(
  val id: String?,
  val documentName: String?,
  val author: String?,
  val type: DocumentType,
  val extendedDescription: String?,
  val createdAt: LocalDateTime,
  val lastModifiedAt: LocalDateTime?,
  val parentPrimaryKeyId: Long?,
)

data class ConvictionDocuments(
  val convictionId: String,
  val documents: List<Document>,
)

data class DocumentType(
  val code: String,
  val description: String,
)
