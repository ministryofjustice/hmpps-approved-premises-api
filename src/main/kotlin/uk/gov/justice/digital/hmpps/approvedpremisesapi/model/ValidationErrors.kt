package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

@JvmInline
value class ValidationErrors(private val errorMap: MutableMap<String, String>) : MutableMap<String, String> by errorMap {
  constructor() : this(mutableMapOf())
}

fun singleValidationErrorOf(propertyNameToMessage: Pair<String, String>) = ValidationErrors().apply { this[propertyNameToMessage.first] = propertyNameToMessage.second }
