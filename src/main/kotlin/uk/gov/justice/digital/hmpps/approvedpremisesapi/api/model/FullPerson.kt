package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class FullPerson(

  @get:JsonProperty("name", required = true) val name: String,

  @get:JsonProperty("dateOfBirth", required = true) val dateOfBirth: java.time.LocalDate,

  @get:JsonProperty("sex", required = true) val sex: String,

  @get:JsonProperty("status", required = true) val status: PersonStatus,

  @get:JsonProperty("crn", required = true) override val crn: String,

  @get:JsonProperty("type", required = true) override val type: PersonType,

  @get:JsonProperty("nomsNumber") val nomsNumber: String? = null,

  @get:JsonProperty("pncNumber") val pncNumber: String? = null,

  @get:JsonProperty("ethnicity") val ethnicity: String? = null,

  @get:JsonProperty("nationality") val nationality: String? = null,

  @get:JsonProperty("religionOrBelief") val religionOrBelief: String? = null,

  @get:JsonProperty("genderIdentity") val genderIdentity: String? = null,

  @get:JsonProperty("prisonName") val prisonName: String? = null,

  @get:JsonProperty("isRestricted") val isRestricted: Boolean? = null,
) : Person
