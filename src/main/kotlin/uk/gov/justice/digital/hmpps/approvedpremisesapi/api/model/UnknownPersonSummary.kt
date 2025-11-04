package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 */
class UnknownPersonSummary(

  override val crn: kotlin.String,

  override val personType: PersonSummaryDiscriminator,
) : PersonSummary
