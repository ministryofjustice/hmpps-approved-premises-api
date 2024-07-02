package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

class MigrationException(override val message: String?) : RuntimeException(message)
