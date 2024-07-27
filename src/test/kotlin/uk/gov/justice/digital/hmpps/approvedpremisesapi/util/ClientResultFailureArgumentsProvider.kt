package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import java.util.stream.Stream

/**
 * Provides an instance of each subtype of sealed interface ClientResult.Failure
 */
class ClientResultFailureArgumentsProvider<T> : ArgumentsProvider {
  override fun provideArguments(context: ExtensionContext): Stream<Arguments> {
    return Stream.of(
      Arguments.of(ClientResult.Failure.CachedValueUnavailable<T>("some-cache-key")),
      Arguments.of(
        ClientResult.Failure.StatusCode<T>(
          HttpMethod.GET,
          "/",
          HttpStatus.NOT_FOUND,
          null,
          false,
        ),
      ),
      Arguments.of(
        ClientResult.Failure.Other<T>(
          HttpMethod.POST,
          "/",
          RuntimeException("Some error"),
        ),
      ),
      Arguments.of(
        ClientResult.Failure.PreemptiveCacheTimeout<T>("some-cache", "some-cache-key", 1000),
      ),
    )
  }
}
