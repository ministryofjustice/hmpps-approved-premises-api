package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.SeedFromExcelApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelDirectoryRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedXlsxService

@Service
class SeedFromExcelController(private val seedXslxService: SeedXlsxService) : SeedFromExcelApiDelegate {
  override fun seedFromExcelFile(seedFromExcelFileRequest: SeedFromExcelFileRequest): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    seedXslxService.seedFile(
      seedFromExcelFileRequest.seedType,
      seedFromExcelFileRequest.fileName,
    )

    return ResponseEntity(HttpStatus.ACCEPTED)
  }

  override fun seedFromExcelDirectory(seedFromExcelDirectoryRequest: SeedFromExcelDirectoryRequest): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    seedXslxService.seedDirectoryRecursive(
      seedFromExcelDirectoryRequest.seedType,
      seedFromExcelDirectoryRequest.directoryName,
    )

    return ResponseEntity(HttpStatus.ACCEPTED)
  }
}
