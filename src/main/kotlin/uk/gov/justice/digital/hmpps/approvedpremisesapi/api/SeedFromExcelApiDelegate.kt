package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelDirectoryRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileRequest
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface SeedFromExcelApiDelegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  fun seedFromExcelDirectory(seedFromExcelDirectoryRequest: SeedFromExcelDirectoryRequest): ResponseEntity<Unit> = ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

  fun seedFromExcelFile(seedFromExcelFileRequest: SeedFromExcelFileRequest): ResponseEntity<Unit> = ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
}
