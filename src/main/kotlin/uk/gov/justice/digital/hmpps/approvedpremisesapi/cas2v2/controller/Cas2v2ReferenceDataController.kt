package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.ApplicationStatusTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.reporting.model.reference.Cas2v2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger.PaginationHeaders

@RestController
@RequestMapping(
  "\${openapi.communityAccommodationServicesTier2CAS2Version2.base-path:/cas2v2}",
  produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE],
)
class Cas2v2ReferenceDataController(
  private val statusTransformer: ApplicationStatusTransformer,
  private val statusFinder: Cas2v2PersistedApplicationStatusFinder,
) {
  @GetMapping("/reference-data/application-status")
  @PaginationHeaders
  fun referenceDataApplicationStatusGet(): ResponseEntity<List<Cas2v2ApplicationStatus>> = ResponseEntity.ok(transformToApi(statusFinder.active()))

  private fun transformToApi(statusList: List<Cas2PersistedApplicationStatus>): List<Cas2v2ApplicationStatus> = statusList.map { status -> statusTransformer.transformV2ModelToApi(status) }
}
