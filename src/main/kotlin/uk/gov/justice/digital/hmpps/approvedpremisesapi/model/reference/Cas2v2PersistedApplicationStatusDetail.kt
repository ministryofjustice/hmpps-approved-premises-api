package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

import java.util.UUID

data class Cas2v2PersistedApplicationStatusDetail(
  val id: UUID,
  val name: String,
  val label: String,
  val children: List<Cas2v2PersistedApplicationStatusDetail>? = emptyList(),
  val isActive: Boolean = true,
)

// Return a single list of all the Cas2v2PersistedApplicationStatusDetail found by flattening
// the `children` field in each object it maps over.
fun List<Cas2v2PersistedApplicationStatusDetail>.flatten(): List<Cas2v2PersistedApplicationStatusDetail> {
  return this + map { it.children?.flatten() ?: emptyList() }.flatten()
}
