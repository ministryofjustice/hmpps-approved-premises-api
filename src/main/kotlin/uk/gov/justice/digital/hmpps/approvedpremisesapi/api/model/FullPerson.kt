package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class FullPerson(

  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @get:JsonProperty("dateOfBirth", required = true) val dateOfBirth: java.time.LocalDate,

  @get:JsonProperty("sex", required = true) val sex: kotlin.String,

  @get:JsonProperty("status", required = true) val status: PersonStatus,

  @get:JsonProperty("crn", required = true) override val crn: kotlin.String,

  @get:JsonProperty("type", required = true) override val type: PersonType,

  @get:JsonProperty("nomsNumber") val nomsNumber: kotlin.String? = null,

  @get:JsonProperty("pncNumber") val pncNumber: kotlin.String? = null,

  @get:JsonProperty("ethnicity") val ethnicity: kotlin.String? = null,

  @get:JsonProperty("nationality") val nationality: kotlin.String? = null,

  @get:JsonProperty("religionOrBelief") val religionOrBelief: kotlin.String? = null,

  @get:JsonProperty("genderIdentity") val genderIdentity: kotlin.String? = null,

  @get:JsonProperty("prisonName") val prisonName: kotlin.String? = null,

  @get:JsonProperty("isRestricted") val isRestricted: kotlin.Boolean? = null,
) : Person
