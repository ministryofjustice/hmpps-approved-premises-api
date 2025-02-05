package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.restclient

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.Builder
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManagePomCasesClient
import java.net.http.HttpClient
import java.time.Duration

@ConditionalOnProperty(prefix = "feature-flags", name = ["cas2-sqs-listener-enabled"], havingValue = "true")
@Configuration
class RestClientConfig(
  private val restClientBuilder: Builder,
  private val clientManager: OAuth2AuthorizedClientManager,
  @Value("\${services.manage-pom-cases.timeout}") private val timeout: String,
) {

  @SuppressWarnings("MagicNumber")
  @Bean
  fun managePomCasesClient() = createClient<ManagePomCasesClient>(
    restClientBuilder
      .requestFactory(withTimeouts(Duration.ofMillis(timeout.toLong()), Duration.ofMillis(timeout.toLong())))
      .requestInterceptor(HmppsAuthInterceptor(clientManager, "manage-pom-cases"))
      .requestInterceptor(RetryInterceptor())
      .build(),
  )

  fun withTimeouts(connection: Duration, read: Duration) =
    JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(connection).build())
      .also { it.setReadTimeout(read) }
}

inline fun <reified T> createClient(client: RestClient): T {
  return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client)).build()
    .createClient(T::class.java)
}
