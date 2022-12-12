package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

import java.time.LocalDateTime

data class GroupedDocuments(
  val documents: List<OffenderLevelDocument>,
  val convictions: List<ConvictionDocuments>
) {
  fun documentExists(documentId: String) = documents.any { it.id == documentId } || convictions.flatMap { it.documents }.any { it.id == documentId }
}

data class OffenderLevelDocument(
  val id: String,
  val documentName: String,
  val author: String,
  val type: DocumentType,
  val extendedDescription: String?,
  val createdAt: LocalDateTime,
  val lastModifiedAt: LocalDateTime?,
  val parentPrimaryKeyId: Long?
)

data class ConvictionDocuments(
  val convictionId: String,
  val documents: List<ConvictionLevelDocument>
)

data class ConvictionLevelDocument(
  val id: String,
  val documentName: String,
  val author: String,
  val type: DocumentType,
  val extendedDescription: String?,
  val lastModifiedAt: LocalDateTime?,
  val createdAt: LocalDateTime,
  val parentPrimaryKeyId: Long?
)

data class DocumentType(
  val code: String,
  val description: String
)
