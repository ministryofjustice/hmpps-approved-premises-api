package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param name
 * @param isActive
 * @param serviceScope
 */
data class CancellationReason(

  val id: java.util.UUID,

  val name: kotlin.String,

  val isActive: kotlin.Boolean,

  val serviceScope: kotlin.String,
)
