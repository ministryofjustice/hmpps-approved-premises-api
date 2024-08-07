package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

fun findRootCause(throwable: Throwable, topMost: Boolean = true): Throwable? {
  if (throwable.cause == null && !topMost) return throwable
  if (throwable.cause == null && topMost) return null

  return findRootCause(throwable.cause!!, false)
}

fun <T> isTypeInThrowableChain(throwable: Throwable, causeType: Class<T>): Boolean {
  if (throwable.javaClass == causeType) {
    return true
  }

  val cause = throwable.cause
  if (cause != null) {
    return isTypeInThrowableChain(cause, causeType)
  } else {
    return false
  }
}
