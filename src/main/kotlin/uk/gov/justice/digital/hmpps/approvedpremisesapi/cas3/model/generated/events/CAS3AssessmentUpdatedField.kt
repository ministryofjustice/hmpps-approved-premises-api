package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param fieldName
 * @param updatedFrom
 * @param updatedTo
 */
data class CAS3AssessmentUpdatedField(

  @get:JsonProperty("fieldName", required = true) val fieldName: kotlin.String,

  @get:JsonProperty("updatedFrom", required = true) val updatedFrom: kotlin.String,

  @get:JsonProperty("updatedTo", required = true) val updatedTo: kotlin.String,
)
