package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.restclient

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestClientException
import java.io.IOException
import java.time.Duration

@SuppressWarnings("MagicNumber")
class RetryInterceptor(private val retries: Int = 3, private val delay: Duration = Duration.ofMillis(200)) :
  ClientHttpRequestInterceptor {
  override fun intercept(
    request: HttpRequest,
    body: ByteArray,
    execution: ClientHttpRequestExecution,
  ): ClientHttpResponse = retry(retries, listOf(RestClientException::class, IOException::class), delay) {
    execution.execute(request, body)
  }
}
