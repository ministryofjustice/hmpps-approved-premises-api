package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.random.Random

private val charPoolMultiCaseNumbers = ('a'..'z') + ('A'..'Z') + ('0'..'9')
private val charPoolUpperCase = ('A'..'Z').toList()
private val charPoolLowerCase = ('a'..'z').toList()
private val charPoolNumbers = ('0'..'9').toList()

private fun randomWithCharPool(charPool: List<Char>, length: Int) = (1..length)
  .map { Random.nextInt(0, charPool.size) }
  .map(charPool::get)
  .joinToString("")

fun randomStringMultiCaseWithNumbers(length: Int) = randomWithCharPool(charPoolMultiCaseNumbers, length)

fun randomStringUpperCase(length: Int) = randomWithCharPool(charPoolUpperCase, length)

fun randomEmailAddress() = randomWithCharPool(charPoolLowerCase, 5) + "." + randomWithCharPool(charPoolLowerCase, 8) + "@" + randomWithCharPool(charPoolLowerCase, 6) + ".com"

fun randomNumberChars(length: Int) = randomWithCharPool(charPoolNumbers, length)

fun randomPostCode() = randomStringUpperCase(2) + randomNumberChars(1) + " " +
  randomStringUpperCase(2) + randomNumberChars(1)

fun randomInt(min: Int, max: Int) = Random.nextInt(min, max)

fun LocalDate.randomDateAfter(maxDays: Int = 14): LocalDate = this.plusDays(randomInt(1, maxDays).toLong())
fun LocalDate.randomDateBefore(maxDays: Int = 14): LocalDate = this.minusDays(randomInt(1, maxDays).toLong())

fun LocalDateTime.randomDateTimeAfter(maxDays: Int = 14): LocalDateTime = this.plusDays(randomInt(1, maxDays).toLong())
fun LocalDateTime.randomDateTimeBefore(maxDays: Int = 14): LocalDateTime = this.minusDays(randomInt(1, maxDays).toLong())

fun OffsetDateTime.randomDateTimeAfter(maxDays: Int = 14): OffsetDateTime = this.plusMinutes(randomInt(1, 60 * 24 * maxDays).toLong()).truncatedTo(ChronoUnit.SECONDS)
fun OffsetDateTime.randomDateTimeBefore(maxDays: Int = 14): OffsetDateTime = this.minusMinutes(randomInt(1, 60 * 24 * maxDays).toLong()).truncatedTo(ChronoUnit.SECONDS)

fun <T> randomOf(options: List<T>) = options[randomInt(0, options.size - 1)]
