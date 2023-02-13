package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import java.util.UUID

abstract class MigrationJob(
  val id: UUID = UUID.randomUUID(),
) {
  abstract fun process()
}
