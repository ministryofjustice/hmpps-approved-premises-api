package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param name
 * @param isRestricted
 */
data class FullPersonSummary(

  val name: kotlin.String,

  val isRestricted: kotlin.Boolean,

  override val crn: kotlin.String,

  override val personType: PersonSummaryDiscriminator,
) : PersonSummary
