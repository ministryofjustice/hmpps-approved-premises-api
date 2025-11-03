package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param applicationId
 * @param crn
 * @param deletedBy
 */
data class CAS3DraftReferralDeletedEventDetails(

  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @get:JsonProperty("crn", required = true) val crn: kotlin.String,

  @get:JsonProperty("deletedBy", required = true) val deletedBy: java.util.UUID,
)
