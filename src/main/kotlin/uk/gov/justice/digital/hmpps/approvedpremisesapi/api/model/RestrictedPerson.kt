package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

class RestrictedPerson(
  override val crn: String,
  override val type: PersonType,
) : Person
