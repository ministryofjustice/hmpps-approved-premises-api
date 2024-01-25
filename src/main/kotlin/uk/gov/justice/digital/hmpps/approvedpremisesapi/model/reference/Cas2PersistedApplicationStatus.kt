package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

import java.util.UUID

data class Cas2PersistedApplicationStatus(
  val id: UUID,
  val name: String,
  val label: String,
  val description: String,
  val isActive: Boolean = true,
)
