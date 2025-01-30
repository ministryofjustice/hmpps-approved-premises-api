package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import java.time.ZonedDateTime
import java.util.*

data class HmppsDomainEvent(
  val eventType: String,
  val version: Int,
  val detailUrl: String? = null,
  val occurredAt: ZonedDateTime = ZonedDateTime.now(),
  val description: String? = null,
  @JsonSetter(nulls = Nulls.SKIP)
  val additionalInformation: AllocationChangedAdditionalInformation?,
  val personReference: PersonReference = PersonReference()
)

data class PersonReference(val identifiers: List<PersonIdentifier> = listOf()) {
  fun findCrn() = get("CRN")
  fun findNomsNumber() = get("NOMS")
  operator fun get(key: String) = identifiers.find { it.type == key }?.value
}

data class AllocationChangedAdditionalInformation(
  val staffCode: String,
  val prisonId: String,
)

data class PersonIdentifier(val type: String, val value: String)