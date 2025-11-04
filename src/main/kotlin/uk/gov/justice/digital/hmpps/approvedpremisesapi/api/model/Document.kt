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

  val id: kotlin.String,

  val level: DocumentLevel,

  val fileName: kotlin.String,

  val createdAt: java.time.Instant,

  val typeCode: kotlin.String,

  val typeDescription: kotlin.String,

  val description: kotlin.String? = null,
)
