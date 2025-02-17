package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream

fun generateXlsxStreamingResponse(outputProvider: (OutputStream) -> Unit) = generateStreamingResponse(
  contentType = ContentType.XLSX,
  fileName = null,
  outputProvider = outputProvider,
)

fun generateStreamingResponse(
  contentType: ContentType,
  fileName: String? = null,
  outputProvider: (OutputStream) -> Unit,
) = ResponseEntity(
  StreamingResponseBody { outputStream -> outputProvider.invoke(outputStream) },
  HttpHeaders().apply {
    put(HttpHeaders.CONTENT_TYPE, listOf(contentType.mimeType))
    fileName?.let {
      put(HttpHeaders.CONTENT_DISPOSITION, listOf("attachment; filename=\"$it\""))
    }
  },
  HttpStatus.OK,
)

enum class ContentType(val mimeType: String, val extension: String) {
  CSV("text/csv", "csv"),
  XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
}
