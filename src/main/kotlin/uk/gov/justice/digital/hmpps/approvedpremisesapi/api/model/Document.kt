package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Document(

  val id: kotlin.String,

  val level: DocumentLevel,

  val fileName: kotlin.String,

  val createdAt: java.time.Instant,

  val typeCode: kotlin.String,

  val typeDescription: kotlin.String,

  val description: kotlin.String? = null,
)
