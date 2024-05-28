package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream

fun generateXlsxStreamingResponse(outputProvider: (OutputStream) -> Unit) =
  ResponseEntity(
    StreamingResponseBody { outputStream -> outputProvider.invoke(outputStream) },
    HttpHeaders().apply {
      put(HttpHeaders.CONTENT_TYPE, listOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    },
    HttpStatus.OK,
  )
