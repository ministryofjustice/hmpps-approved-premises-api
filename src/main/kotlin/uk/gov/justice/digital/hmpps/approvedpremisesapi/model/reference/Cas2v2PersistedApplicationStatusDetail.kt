package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

import java.util.UUID

data class Cas2v2PersistedApplicationStatusDetail(
  val id: UUID,
  val name: String,
  val label: String,
  val children: List<Cas2v2PersistedApplicationStatusDetail>? = emptyList(),
  val isActive: Boolean = true,
)
