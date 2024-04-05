package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util

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
