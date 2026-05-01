package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

class RestrictedPersonSummary(

  override val crn: kotlin.String,

  override val personType: PersonSummaryDiscriminator,
) : PersonSummary
