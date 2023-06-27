package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

fun findRootCause(throwable: Throwable, topMost: Boolean = true): Throwable? {
  if (throwable.cause == null && !topMost) return throwable
  if (throwable.cause == null && topMost) return null

  return findRootCause(throwable.cause!!, false)
}
