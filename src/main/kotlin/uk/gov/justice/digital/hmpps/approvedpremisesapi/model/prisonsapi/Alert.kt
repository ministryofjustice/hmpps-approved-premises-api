package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi

import java.time.LocalDate

data class Alert(
  val alertId: Long,
  val bookingId: Long,
  val offenderNo: String,
  val alertType: String,
  val alertTypeDescription: String,
  val alertCode: String,
  val alertCodeDescription: String,
  val comment: String,
  val dateCreated: LocalDate,
  val dateExpires: LocalDate?,
  val expired: Boolean,
  val active: Boolean
)
