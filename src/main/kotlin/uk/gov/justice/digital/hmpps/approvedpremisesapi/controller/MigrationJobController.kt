package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated.MigrationJobApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService

@Service
class MigrationJobController(private val migrationJobService: MigrationJobService) : MigrationJobApiDelegate {
  override fun migrationJobPost(migrationJobRequest: MigrationJobRequest): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    migrationJobService.runMigrationJobAsync(migrationJobRequest.jobType)

    return ResponseEntity(HttpStatus.ACCEPTED)
  }
}
