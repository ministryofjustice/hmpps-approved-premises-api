package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Document(

  @get:JsonProperty("id", required = true) val id: String,

  @get:JsonProperty("level", required = true) val level: DocumentLevel,

  @get:JsonProperty("fileName", required = true) val fileName: String,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("typeCode", required = true) val typeCode: String,

  @get:JsonProperty("typeDescription", required = true) val typeDescription: String,

  @get:JsonProperty("description") val description: String? = null,
)
