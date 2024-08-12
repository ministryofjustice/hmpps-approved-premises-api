package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
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
fun randomStringLowerCase(length: Int) = randomWithCharPool(charPoolLowerCase, length)

fun randomEmailAddress() = randomWithCharPool(charPoolLowerCase, 5) + "." + randomWithCharPool(charPoolLowerCase, 8) + "@" + randomWithCharPool(charPoolLowerCase, 6) + ".com"

fun randomNumberChars(length: Int) = randomWithCharPool(charPoolNumbers, length)

fun randomPostCode() = randomStringUpperCase(2) + randomNumberChars(1) + " " +
  randomStringUpperCase(2) + randomNumberChars(1)

fun randomInt(min: Int, max: Int) = Random.nextInt(min, max)
fun randomDouble(min: Double, max: Double) = Random.nextDouble(min, max)

fun LocalDate.randomDateAfter(maxDays: Int = 14): LocalDate = this.plusDays(randomInt(1, maxDays).toLong())
fun LocalDate.randomDateBefore(maxDays: Int = 14): LocalDate = this.minusDays(randomInt(1, maxDays).toLong())
fun LocalDate.randomDateAround(maxDays: Int = 14): LocalDate = this.minusDays(maxDays.toLong()).randomDateAfter(maxDays * 2)

fun LocalDateTime.randomDateTimeAfter(maxDays: Int = 14): LocalDateTime = this.plusDays(randomInt(1, maxDays).toLong())
fun LocalDateTime.randomDateTimeBefore(maxDays: Int = 14): LocalDateTime = this.minusDays(randomInt(1, maxDays).toLong())

fun OffsetDateTime.randomDateTimeAfter(maxDays: Int = 14): OffsetDateTime = this.plusMinutes(randomInt(1, 60 * 24 * maxDays).toLong()).truncatedTo(ChronoUnit.SECONDS)
fun OffsetDateTime.randomDateTimeBefore(maxDays: Int): OffsetDateTime = this.minusMinutes(randomInt(1, 60 * 24 * maxDays).toLong()).truncatedTo(ChronoUnit.SECONDS)

fun Instant.randomDateTimeAfter(maxDays: Int = 14): Instant = this.plus(randomInt(1, 60 * 24 * maxDays).toLong(), ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS)
fun Instant.randomDateTimeBefore(maxDays: Int = 14): Instant = this.minus(randomInt(1, 60 * 24 * maxDays).toLong(), ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS)

fun <T> randomOf(options: List<T>) = options[randomInt(0, options.size - 1)]
