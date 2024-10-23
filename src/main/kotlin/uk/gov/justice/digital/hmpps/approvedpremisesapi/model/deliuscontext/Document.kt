package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext

import java.time.LocalDateTime

data class Document(
  val id: String,
  val description: String,
  val level: String,
  val eventNumber: String,
  val filename: String,
  val typeCode: String,
  val typeDescription: String,
  val dateSaved: LocalDateTime,
  val dateCreated: LocalDateTime,
)
