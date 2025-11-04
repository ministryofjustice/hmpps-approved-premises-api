package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param applicationId
 * @param crn
 * @param deletedBy
 */
data class CAS3DraftReferralDeletedEventDetails(

  val applicationId: java.util.UUID,

  val crn: kotlin.String,

  val deletedBy: java.util.UUID,
)
