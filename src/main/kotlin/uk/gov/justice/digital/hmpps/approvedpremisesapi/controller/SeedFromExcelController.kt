package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated.SeedFromExcelApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedXlsxService

@Service
class SeedFromExcelController(private val seedXslxService: SeedXlsxService) : SeedFromExcelApiDelegate {
  override fun seedFromExcelPost(seedFromExcelRequest: SeedFromExcelRequest): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    seedXslxService.seedExcelData(seedFromExcelRequest.seedType, seedFromExcelRequest.fileName)

    return ResponseEntity(HttpStatus.ACCEPTED)
  }
}
