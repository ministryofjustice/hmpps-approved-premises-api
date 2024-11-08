package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

fun <T> Collection<T>.containsAny(vararg values: T): Boolean = values.toSet().intersect(this.toSet()).isNotEmpty()

fun <T, U, ID> Iterable<T>.zipBy(
  other: Iterable<U>,
  keySelector1: T.() -> ID,
  keySelector2: U.() -> ID,
): Iterable<Pair<T, U>> {
  val otherMap = other.associateBy(keySelector2)

  return this.map {
    val key = it.keySelector1()

    Pair(it, otherMap[key]!!)
  }
}
