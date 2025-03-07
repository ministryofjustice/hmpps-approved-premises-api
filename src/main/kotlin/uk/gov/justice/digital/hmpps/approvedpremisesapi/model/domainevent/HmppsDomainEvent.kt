package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent

import java.time.ZonedDateTime

data class HmppsDomainEvent(
  val eventType: String,
  val version: Int,
  val detailUrl: String? = null,
  val occurredAt: ZonedDateTime = ZonedDateTime.now(),
  val description: String? = null,
  val additionalInformation: AdditionalInformation,
  val personReference: PersonReference = PersonReference(),
)

data class PersonReference(val identifiers: List<PersonIdentifier> = listOf()) {
  fun findNomsNumber() = get("NOMS")
  operator fun get(key: String) = identifiers.find { it.type == key }?.value
}

data class PersonIdentifier(val type: String, val value: String)

data class AdditionalInformation(private val mutableMap: MutableMap<String, Any?> = mutableMapOf()) : MutableMap<String, Any?> by mutableMap

val AdditionalInformation.categoriesChanged get() = (get("categoriesChanged") as List<String>).toSet()
