package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobRequest
import java.util.Optional

/**
 * A delegate to be called by the {@link MigrationJobApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface MigrationJobApiDelegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  /**
   * @see MigrationJobApi#migrationJobPost
   */
  fun migrationJobPost(migrationJobRequest: MigrationJobRequest): ResponseEntity<Unit> = ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
}
