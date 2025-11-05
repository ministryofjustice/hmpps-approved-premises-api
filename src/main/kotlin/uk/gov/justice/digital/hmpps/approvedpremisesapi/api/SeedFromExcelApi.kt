
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelDirectoryRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileRequest

@RestController
interface SeedFromExcelApi {

  fun getDelegate(): SeedFromExcelApiDelegate = object : SeedFromExcelApiDelegate {}

  @Operation(
    tags = ["Seed excel"],
    summary = "Starts the data seeding from Excel process for a directory, can only be called from a local connection",
    operationId = "seedFromExcelDirectory",
    description = """""",
    responses = [
      ApiResponse(responseCode = "202", description = "successfully requested task"),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/seedFromExcel/directory"],
    consumes = ["application/json"],
  )
  fun seedFromExcelDirectory(@Parameter(description = "", required = true) @RequestBody seedFromExcelDirectoryRequest: SeedFromExcelDirectoryRequest): ResponseEntity<Unit> = getDelegate().seedFromExcelDirectory(seedFromExcelDirectoryRequest)

  @Operation(
    tags = ["Seed excel"],
    summary = "Starts the data seeding from Excel process, can only be called from a local connection",
    operationId = "seedFromExcelFile",
    description = """""",
    responses = [
      ApiResponse(responseCode = "202", description = "successfully requested task"),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/seedFromExcel/file"],
    consumes = ["application/json"],
  )
  fun seedFromExcelFile(@Parameter(description = "", required = true) @RequestBody seedFromExcelFileRequest: SeedFromExcelFileRequest): ResponseEntity<Unit> = getDelegate().seedFromExcelFile(seedFromExcelFileRequest)
}
