package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param name
 * @param dateOfBirth
 * @param sex
 * @param status
 * @param nomsNumber
 * @param pncNumber
 * @param ethnicity
 * @param nationality
 * @param religionOrBelief
 * @param genderIdentity
 * @param prisonName
 * @param isRestricted
 */
data class FullPerson(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("dateOfBirth", required = true) val dateOfBirth: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("sex", required = true) val sex: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) val status: PersonStatus,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("crn", required = true) override val crn: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: PersonType,

  @Schema(example = "null", description = "")
  @get:JsonProperty("nomsNumber") val nomsNumber: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("pncNumber") val pncNumber: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("ethnicity") val ethnicity: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("nationality") val nationality: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("religionOrBelief") val religionOrBelief: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("genderIdentity") val genderIdentity: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("prisonName") val prisonName: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isRestricted") val isRestricted: kotlin.Boolean? = null,
) : Person {
}
