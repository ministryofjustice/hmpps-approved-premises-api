package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class FullPersonSummary(
  val name: String,
  val isRestricted: Boolean,
  override val crn: String,
  override val personType: PersonSummaryDiscriminator,
) : PersonSummary
