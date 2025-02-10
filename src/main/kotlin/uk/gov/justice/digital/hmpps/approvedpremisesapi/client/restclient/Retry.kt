package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.restclient

import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

@SuppressWarnings("TooGenericExceptionCaught", "ForEachOnRange", "MagicNumber", "TooGenericExceptionThrown")
fun <T> retry(
  maxRetries: Int,
  exceptions: List<KClass<out Exception>> = listOf(Exception::class),
  delay: Duration = Duration.ofMillis(100),
  code: () -> T,
): T {
  (1..maxRetries).forEach { count ->
    try {
      return code()
    } catch (e: Throwable) {
      if (exceptions.any { it.isInstance(e) } && count < maxRetries) {
        TimeUnit.MILLISECONDS.sleep(delay.toMillis() * count * count)
      } else {
        throw e
      }
    }
  }
  throw RuntimeException("unknown error")
}
