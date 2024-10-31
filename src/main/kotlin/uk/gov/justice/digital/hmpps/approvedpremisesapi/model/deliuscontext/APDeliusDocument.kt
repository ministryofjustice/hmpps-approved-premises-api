package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext

import java.time.ZonedDateTime

data class APDeliusDocument(
  val id: String?,
  val level: String,
  val eventNumber: String?,
  val filename: String,
  val typeCode: String,
  val typeDescription: String,
  val dateSaved: ZonedDateTime,
  val dateCreated: ZonedDateTime,
  val description: String?,
)
