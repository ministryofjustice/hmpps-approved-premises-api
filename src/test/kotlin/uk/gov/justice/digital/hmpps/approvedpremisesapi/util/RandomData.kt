package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import kotlin.random.Random

private val charPoolMultiCaseNumbers = ('a'..'z') + ('A'..'Z') + ('0'..'9')
private val charPoolUpperCase = ('A'..'Z').toList()
private val charPoolNumbers = ('0'..'9').toList()

private fun randomWithCharPool(charPool: List<Char>, length: Int) = (1..length)
  .map { Random.nextInt(0, charPool.size) }
  .map(charPool::get)
  .joinToString("")

fun randomStringMultiCaseWithNumbers(length: Int) = randomWithCharPool(charPoolMultiCaseNumbers, length)

fun randomStringUpperCase(length: Int) = randomWithCharPool(charPoolUpperCase, length)

fun randomNumberChars(length: Int) = randomWithCharPool(charPoolNumbers, length)

fun randomPostCode() = randomStringUpperCase(2) + randomNumberChars(1) + " " +
  randomStringUpperCase(2) + randomNumberChars(1)

fun randomInt(min: Int, max: Int) = Random.nextInt(min, max)
