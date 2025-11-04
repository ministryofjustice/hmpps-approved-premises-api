package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param characteristics
 * @param code
 * @param notes
 * @param beds
 */
data class Room(

  val id: java.util.UUID,

  val name: kotlin.String,

  val characteristics: kotlin.collections.List<Characteristic>,

  @Schema(example = "NEABC-4", description = "")
  val code: kotlin.String? = null,

  val notes: kotlin.String? = null,

  val beds: kotlin.collections.List<Bed>? = null,
)
