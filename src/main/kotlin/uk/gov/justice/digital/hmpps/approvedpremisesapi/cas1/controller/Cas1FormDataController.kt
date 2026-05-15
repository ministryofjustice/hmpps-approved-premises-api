package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.service.Cas1FormDataService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1Controller
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Cas1Controller
@Tag(name = "CAS1 Form Data")
class Cas1FormDataController(
  val cas1FormDataService: Cas1FormDataService,
  val jsonMapper: JsonMapper,
) {

  @GetMapping("/form-data/{id}")
  fun getFormData(
    @PathVariable id: String,
  ): Any = jsonMapper.readTree(
    extractEntityFromCasResult(
      cas1FormDataService.get(id),
    ),
  )

  @PutMapping("/form-data/{id}")
  fun updateFormData(
    @PathVariable id: String,
    @RequestBody body: Any,
  ) = cas1FormDataService.createOrUpdate(id, jsonMapper.writeValueAsString(body))
}
