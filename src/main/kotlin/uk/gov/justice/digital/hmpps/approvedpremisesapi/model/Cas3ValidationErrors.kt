package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

@JvmInline
value class Cas3ValidationErrors(private val errorMap: MutableMap<String, Cas3ValidationMessage>) : MutableMap<String, Cas3ValidationMessage> by errorMap {
  constructor() : this(mutableMapOf())
}

data class Cas3ValidationMessage(
  val entityId: String,
  val message: String,
  val value: String,
)
