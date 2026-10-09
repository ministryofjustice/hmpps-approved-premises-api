package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.documentmanagement.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig
import java.util.UUID

@Component
class DocumentManagementApiClient(
  @Qualifier("documentManagementApiWebClient") webClientConfig: WebClientConfig,
  jsonMapper: JsonMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, jsonMapper, webClientCache) {

  companion object {
    const val SERVICE_NAME_HEADER = "Service-Name"
    const val APPROVED_PREMISES_SERVICE_NAME = "approved-premises-api"
    val DEFAULT_CONTENT_TYPE: MediaType = MediaType.parseMediaType("text/csv")
  }

  /**
   * Uploads a document resource to the HMPPS Document Management API via POST /documents/{documentType}/{documentUuid}.
   */
  fun uploadDocument(
    documentType: String,
    documentUuid: UUID,
    filename: String,
    fileResource: Resource,
    metadata: Map<String, Any>? = null,
    contentType: MediaType = DEFAULT_CONTENT_TYPE,
  ): ClientResult<Document> = postRequest {
    path = "/documents/$documentType/$documentUuid"
    body = buildMultipartBody(fileResource, filename, contentType, metadata)
    withHeader(SERVICE_NAME_HEADER, APPROVED_PREMISES_SERVICE_NAME)
  }

  private fun buildMultipartBody(
    fileResource: Resource,
    filename: String,
    contentType: MediaType,
    metadata: Map<String, Any>?,
  ): MultiValueMap<String, HttpEntity<*>> {
    val builder = MultipartBodyBuilder()
    val filePart = builder.part("file", fileResource)
    filePart.filename(filename)
    filePart.contentType(contentType)

    if (!metadata.isNullOrEmpty()) {
      val metadataJson = jsonMapper.writeValueAsString(metadata)
      val metadataHeaders = HttpHeaders().apply {
        this.contentType = MediaType.APPLICATION_JSON
      }
      builder.part("metadata", HttpEntity(metadataJson, metadataHeaders))
    }
    return builder.build()
  }
}
