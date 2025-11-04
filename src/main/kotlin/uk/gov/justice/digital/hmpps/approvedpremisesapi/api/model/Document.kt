package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Meta Info about a file relating to an Offender
 * @param id
 * @param level
 * @param fileName
 * @param createdAt
 * @param typeCode
 * @param typeDescription
 * @param description
 */
data class Document(

  @get:JsonProperty("id", required = true) val id: kotlin.String,

  @get:JsonProperty("level", required = true) val level: DocumentLevel,

  @get:JsonProperty("fileName", required = true) val fileName: kotlin.String,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("typeCode", required = true) val typeCode: kotlin.String,

  @get:JsonProperty("typeDescription", required = true) val typeDescription: kotlin.String,

  @get:JsonProperty("description") val description: kotlin.String? = null,
)
