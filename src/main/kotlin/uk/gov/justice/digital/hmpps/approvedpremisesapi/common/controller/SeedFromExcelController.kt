package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelDirectoryRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedXlsxService

@RestController
@Tag(name = "Seed excel")
class SeedFromExcelController(private val seedXslxService: SeedXlsxService) {

  @Operation(
    summary = "Starts the data seeding from Excel process, can only be called from a local connection",
    responses = [
      ApiResponse(responseCode = "202", description = "successfully requested task"),
    ],
  )
  @PostMapping(
    value = ["/seedFromExcel/file"],
    consumes = ["application/json"],
  )
  fun seedFromExcelFile(
    @RequestBody seedFromExcelFileRequest: SeedFromExcelFileRequest,
  ): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    seedXslxService.seedFile(
      seedFromExcelFileRequest.seedType,
      seedFromExcelFileRequest.fileName,
    )

    return ResponseEntity(HttpStatus.ACCEPTED)
  }

  @Operation(
    summary = "Starts the data seeding from Excel process for a directory, can only be called from a local connection",
    responses = [
      ApiResponse(responseCode = "202", description = "successfully requested task"),
    ],
  )
  @PostMapping(
    value = ["/seedFromExcel/directory"],
    consumes = ["application/json"],
  )
  fun seedFromExcelDirectory(
    @RequestBody seedFromExcelDirectoryRequest: SeedFromExcelDirectoryRequest,
  ): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    seedXslxService.seedDirectoryRecursive(
      seedFromExcelDirectoryRequest.seedType,
      seedFromExcelDirectoryRequest.directoryName,
    )

    return ResponseEntity(HttpStatus.ACCEPTED)
  }
}
