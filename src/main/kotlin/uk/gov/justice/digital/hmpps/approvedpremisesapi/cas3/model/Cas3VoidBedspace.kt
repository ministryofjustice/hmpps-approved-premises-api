package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDate
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Cas3VoidBedspace(
  val id: UUID,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val bedspaceId: UUID,
  val bedspaceName: String,
  val reason: Cas3VoidBedspaceReason,
  val status: Cas3VoidBedspaceStatus,
  val referenceNumber: String?,
  val notes: String?,
  val cancellationDate: LocalDate?,
  val cancellationNotes: String?,
)

data class Cas3VoidBedspaceRequest(
  val startDate: LocalDate,
  val endDate: LocalDate,
  val reasonId: UUID,
  val referenceNumber: String? = null,
  val notes: String? = null,
)

data class Cas3VoidBedspaceCancellation(val cancellationNotes: String?)

data class Cas3VoidBedspaceReason(
  val id: UUID,
  val name: String,
  val isActive: Boolean,
)

enum class Cas3VoidBedspaceStatus(@JsonValue val value: String) {
  ACTIVE("active"),
  CANCELLED("cancelled"),
}
