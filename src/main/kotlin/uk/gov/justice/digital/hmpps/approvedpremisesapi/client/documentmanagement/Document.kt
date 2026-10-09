package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.documentmanagement

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID

data class Document(
  val documentUuid: UUID,
  val documentType: String,
  val documentFilename: String,
  val filename: String,
  val fileExtension: String,
  val fileSize: Long,
  val fileHash: String? = null,
  val mimeType: String,
  val metadata: JsonNode? = null,
  val createdTime: LocalDateTime,
  val createdByUsername: String? = null,
  val createdByServiceName: String? = null,
)
