package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ReferenceData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3ReferenceDataService

@Cas3Controller
class Cas3ReferenceDataController(
  private val cas3RefDataService: Cas3ReferenceDataService,
) {

  @GetMapping("/reference-data")
  fun getReferenceData(@RequestParam(required = true) type: Cas3RefDataType): ResponseEntity<List<Cas3ReferenceData>> {
    val result = cas3RefDataService.getReferenceData(type)
    return ResponseEntity.ok(result)
  }
}

enum class Cas3RefDataType {
  BEDSPACE_CHARACTERISTICS,
  PREMISES_CHARACTERISTICS,
}
