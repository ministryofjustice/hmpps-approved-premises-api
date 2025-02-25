package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import java.time.ZonedDateTime

data class HmppsDomainEvent(
  val eventType: String,
  val version: Int,
  val detailUrl: String? = null,
  val occurredAt: ZonedDateTime = ZonedDateTime.now(),
  val description: String? = null,
  @JsonSetter(nulls = Nulls.SKIP)
  val additionalInformation: Map<String, Any>? = mapOf(),
  val personReference: PersonReference = PersonReference(),
) {
  val staffCode = additionalInformation?.get("staffCode") as String?
  val categoriesChanged = additionalInformation?.get("categoriesChanged") as List<String>?
}

data class PersonReference(val identifiers: List<PersonIdentifier> = listOf()) {
  fun findCrn() = get("CRN")
  fun findNomsNumber() = get("NOMS")
  operator fun get(key: String) = identifiers.find { it.type == key }?.value
}

data class PersonIdentifier(val type: String, val value: String)
