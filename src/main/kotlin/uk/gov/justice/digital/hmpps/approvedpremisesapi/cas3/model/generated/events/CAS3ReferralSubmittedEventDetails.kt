package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

/**
 *
 * @param personReference
 * @param applicationId
 * @param applicationUrl
 */
data class CAS3ReferralSubmittedEventDetails(

  val personReference: PersonReference,

  val applicationId: java.util.UUID,

  val applicationUrl: java.net.URI,
)
