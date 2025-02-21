package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.tryGetDetails
import java.util.UUID

fun UUID.toShortBase58(): String {
  val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
  val base = alphabet.length.toULong()

  val xorResult = mostSignificantBits xor leastSignificantBits
  var num = xorResult.toULong()

  val sb = StringBuilder()
  while (num > 0uL) {
    val remainder = (num % base).toInt()
    sb.append(alphabet[remainder])
    num /= base
  }

  return sb.reverse().toString()
}

fun Boolean?.toYesNo(): String? = when (this) {
  null -> null
  true -> "Yes"
  false -> "No"
}

fun PersonSummaryInfoResult.getPersonName(): String? = this.tryGetDetails {
  val nameParts = listOf(it.name.forename) + it.name.middleNames + it.name.surname
  nameParts.joinToString(" ")
}

fun PersonSummaryInfoResult.getPersonGender(): String = when (this.tryGetDetails { it.profile?.genderIdentity }) {
  "Prefer to self-describe" -> "Prefer to self-describe"
  else -> this.tryGetDetails { it.profile?.genderIdentity }
}.toString()
