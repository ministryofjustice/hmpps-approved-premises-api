package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param serviceScope
 * @param modelScope
 * @param propertyName
 */
data class Characteristic(

  @Schema(example = "952790c0-21d7-4fd6-a7e1-9018f08d8bb0", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "Is this premises catered (rather than self-catered)?", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("serviceScope", required = true) val serviceScope: Characteristic.ServiceScope,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("modelScope", required = true) val modelScope: Characteristic.ModelScope,

  @Schema(example = "isCatered", description = "")
  @get:JsonProperty("propertyName") val propertyName: kotlin.String? = null,
) {

  /**
   *
   * Values: approvedMinusPremises,temporaryMinusAccommodation,star
   */
  enum class ServiceScope(val value: kotlin.String) {

    @JsonProperty("approved-premises")
    approvedMinusPremises("approved-premises"),

    @JsonProperty("temporary-accommodation")
    temporaryMinusAccommodation("temporary-accommodation"),

    @JsonProperty("*")
    star("*"),
  }

  /**
   *
   * Values: premises,room,star
   */
  enum class ModelScope(val value: kotlin.String) {

    @JsonProperty("premises")
    premises("premises"),

    @JsonProperty("room")
    room("room"),

    @JsonProperty("*")
    star("*"),
  }
}
