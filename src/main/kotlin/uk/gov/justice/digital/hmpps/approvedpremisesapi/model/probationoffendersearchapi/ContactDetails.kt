package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi

import java.time.LocalDate

data class ContactDetails(
  val phoneNumbers: List<PhoneNumber>? = null,
  val emailAddresses: List<String>? = null,
  val allowSMS: Boolean? = null,
  val addresses: List<Address>? = null,
)

data class PhoneNumber(
  val number: String? = null,
  val type: PhoneTypes? = null,
) {

  enum class PhoneTypes {
    TELEPHONE,
    MOBILE,
  }
}

data class Address(
  val id: Long,
  val from: LocalDate,
  val to: LocalDate? = null,
  val noFixedAbode: Boolean? = null,
  val notes: String? = null,
  val addressNumber: String? = null,
  val buildingName: String? = null,
  val streetName: String? = null,
  val district: String? = null,
  val town: String? = null,
  val county: String? = null,
  val postcode: String? = null,
  val telephoneNumber: String? = null,
  val status: KeyValue? = null,
  val type: KeyValue? = null,
)

data class KeyValue(
  val code: String? = null,
  val description: String? = null,
)
