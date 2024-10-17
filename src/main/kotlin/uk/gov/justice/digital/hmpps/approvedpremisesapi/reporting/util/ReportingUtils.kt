package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
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

fun Boolean?.toYesNo(): String? {
  return when (this) {
    null -> null
    true -> "Yes"
    false -> "No"
  }
}

fun PersonSummaryInfoResult.getPersonName(): String? {
  return this.tryGetDetails {
    val nameParts = listOf(it.name.forename) + it.name.middleNames + it.name.surname
    nameParts.joinToString(" ")
  }
}

fun PersonSummaryInfoResult.getPersonGender(): String {
  return when (this.tryGetDetails { it.profile?.genderIdentity }) {
    "Prefer to self-describe" -> "Prefer to self-describe"
    else -> this.tryGetDetails { it.profile?.genderIdentity }
  }.toString()
}

fun <V> PersonSummaryInfoResult.tryGetDetails(value: (CaseSummary) -> V): V? {
  return when (this) {
    is PersonSummaryInfoResult.Success.Full -> value(this.summary)
    else -> null
  }
}
