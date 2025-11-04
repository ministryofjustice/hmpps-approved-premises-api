package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param userId
 */
data class NewReallocation(

  @get:JsonProperty("userId") val userId: java.util.UUID? = null,
)
