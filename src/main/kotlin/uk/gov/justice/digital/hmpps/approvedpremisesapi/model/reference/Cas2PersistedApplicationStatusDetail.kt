package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

import java.util.UUID

data class Cas2PersistedApplicationStatusDetail(
  val id: UUID,
  val name: String,
  val label: String,
  val isActive: Boolean = true,
)
