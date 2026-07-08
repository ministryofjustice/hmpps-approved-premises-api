package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

class UnknownPerson(
  override val crn: String,
  override val type: PersonType,
) : Person
