package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param personReference
 * @param applicationId
 * @param applicationUrl
 */
data class CAS3ReferralSubmittedEventDetails(

  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: java.net.URI,
)
