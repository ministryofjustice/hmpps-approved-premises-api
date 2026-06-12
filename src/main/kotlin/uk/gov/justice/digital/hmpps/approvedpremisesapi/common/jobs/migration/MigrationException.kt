package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration

class MigrationException(override val message: String?) : RuntimeException(message)
