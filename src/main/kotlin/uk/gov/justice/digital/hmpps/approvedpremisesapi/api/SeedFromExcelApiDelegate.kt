package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import java.util.Optional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelDirectoryRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileRequest

/**
 * A delegate to be called by the {@link SeedFromExcelApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface SeedFromExcelApiDelegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  /**
   * @see SeedFromExcelApi#seedFromExcelDirectory
   */
  fun seedFromExcelDirectory(seedFromExcelDirectoryRequest: SeedFromExcelDirectoryRequest): ResponseEntity<Unit> = ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

  /**
   * @see SeedFromExcelApi#seedFromExcelFile
   */
  fun seedFromExcelFile(seedFromExcelFileRequest: SeedFromExcelFileRequest): ResponseEntity<Unit> = ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
}
