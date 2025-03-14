package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

class IgnorableMessageException(
  override val message: String,
  val additionalProperties: Map<String, String> = mapOf(),
) : RuntimeException(message)
