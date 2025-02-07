package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.ApplicationsCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Service
class Cas3ApplicationsController(
  private val cas3ApplicationService: Cas3ApplicationService,
) : ApplicationsCas3Delegate {

  override fun deleteApplication(applicationId: UUID): ResponseEntity<Unit> {
    return ResponseEntity.ok(
      extractEntityFromCasResult(cas3ApplicationService.markApplicationAsDeleted(applicationId)),
    )
  }
}
