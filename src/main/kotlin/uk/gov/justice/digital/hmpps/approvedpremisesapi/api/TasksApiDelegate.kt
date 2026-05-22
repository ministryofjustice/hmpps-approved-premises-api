package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface TasksApiDelegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  fun tasksTaskTypeIdAllocationsDelete(
    id: java.util.UUID,
    taskType: String,
  ): ResponseEntity<Unit> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
          ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }
}
