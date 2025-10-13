package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonInclude
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Cas3ReferenceData(
  val id: UUID,
  val description: String,
  val name: String?,
)

interface ReferenceData {
  val id: UUID
  val description: String
  val name: String?
}
