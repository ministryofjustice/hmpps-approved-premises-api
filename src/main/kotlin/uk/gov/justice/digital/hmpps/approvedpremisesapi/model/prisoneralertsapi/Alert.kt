package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisoneralertsapi

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Alert(
  val alertUuid: UUID,
  val prisonNumber: String,
  val alertCode: AlertCodeSummary,
  val description: String?,
  val authorisedBy: String?,
  val activeFrom: LocalDate,
  val activeTo: LocalDate?,
  val isActive: Boolean,
  val createdAt: LocalDateTime,
  val createdBy: String,
  val createdByDisplayName: String,
  val lastModifiedAt: LocalDateTime?,
  val lastModifiedBy: String?,
  val lastModifiedByDisplayName: String?,
  val activeToLastSetAt: LocalDateTime?,
  val activeToLastSetBy: String?,
  val activeToLastSetByDisplayName: String?,
)
