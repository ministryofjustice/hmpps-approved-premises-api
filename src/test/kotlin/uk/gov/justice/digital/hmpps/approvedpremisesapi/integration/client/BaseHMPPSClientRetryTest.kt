package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.client

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.Fault
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.WebClientCache
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockUnsuccesfullCaseDetailCall

class BaseHMPPSClientRetryTest : InitialiseDatabasePerClassTestBase() {

  companion object {
    const val CRN = "ABC123"

    @JvmStatic
    fun successHttpStatusCode() =
      HttpStatus.entries.filter { it.is2xxSuccessful }.filter { it.value() != 204 }.map { it.value() }

    @JvmStatic
    fun errorHttpStatusCodesForRetries(): List<Int> {
      return listOf(500, 501, 502, 503)
    }

    @JvmStatic
    fun errorHttpStatusCodeThatDontRetry(): List<Int> {
      return HttpStatus.entries
        .filter { it.is4xxClientError || it.is5xxServerError }
        .filter { !errorHttpStatusCodesForRetries().contains(it.value()) }
        .map { it.value() }
    }
  }

  @AfterEach
  fun resetRequestJournal() {
    wiremockManager.resetRequestJournal()
  }

  @Autowired
  lateinit var webClientCache: WebClientCache

  @Autowired
  @Qualifier("apDeliusContextApiWebClient")
  lateinit var apDeliusContextApiWebClientConfig: WebClientConfig

  @Autowired
  lateinit var environment: Environment

  fun setupTestClient(maxRetries: Long) =
    ApDeliusContextApiClient(
      WebClientConfig(
        apDeliusContextApiWebClientConfig.webClient,
        maxRetries,
      ),
      objectMapper,
      webClientCache,
    )

  @ParameterizedTest
  @MethodSource("successHttpStatusCode")
  fun `Don't retry for success outcomes`(httpStatusCode: Int) {
    APDeliusContext_mockSuccessfulCaseDetailCall(
      crn = CRN,
      response = CaseDetailFactory().produce(),
      responseStatus = httpStatusCode,
    )

    val client = setupTestClient(maxRetries = 2)

    val result = client.getCaseDetail(CRN)

    assertThat(result).isInstanceOf(ClientResult.Success::class.java)

    wiremockManager.wiremockServer.verify(1, getRequestedFor(urlEqualTo("/probation-cases/$CRN/details")))
  }

  @Test
  fun `Dont retry if maxRetries is 0`() {
    APDeliusContext_mockUnsuccesfullCaseDetailCall(
      crn = CRN,
      responseStatus = 500,
    )

    val client = setupTestClient(maxRetries = 0)

    val result = client.getCaseDetail(CRN)

    assertThat(result).isInstanceOf(ClientResult.Failure.StatusCode::class.java)

    wiremockManager.wiremockServer.verify(1, getRequestedFor(urlEqualTo("/probation-cases/$CRN/details")))
  }

  @ParameterizedTest
  @MethodSource("errorHttpStatusCodesForRetries")
  fun `Retry for all applicable error status codes`(httpStatusCode: Int) {
    APDeliusContext_mockUnsuccesfullCaseDetailCall(
      crn = CRN,
      responseStatus = httpStatusCode,
    )

    val client = setupTestClient(maxRetries = 2)

    val result = client.getCaseDetail(CRN)

    assertThat(result).isInstanceOf(ClientResult.Failure.StatusCode::class.java)
    result as ClientResult.Failure.StatusCode
    assertThat(result.status.value()).isEqualTo(httpStatusCode)

    wiremockManager.wiremockServer.verify(3, getRequestedFor(urlEqualTo("/probation-cases/$CRN/details")))
  }

  @ParameterizedTest
  @MethodSource("errorHttpStatusCodeThatDontRetry")
  fun `Don't retry for all applicable error status codes`(httpStatusCode: Int) {
    APDeliusContext_mockUnsuccesfullCaseDetailCall(
      crn = CRN,
      responseStatus = httpStatusCode,
    )

    val client = setupTestClient(maxRetries = 2)

    val result = client.getCaseDetail(CRN)

    assertThat(result).isInstanceOf(ClientResult.Failure.StatusCode::class.java)
    result as ClientResult.Failure.StatusCode
    assertThat(result.status.value()).isEqualTo(httpStatusCode)

    wiremockManager.wiremockServer.verify(1, getRequestedFor(urlEqualTo("/probation-cases/$CRN/details")))
  }

  @Test
  fun `Retry connection prematurely closed errors`() {
    mockOAuth2ClientCredentialsCallIfRequired {
      wiremockServer.stubFor(
        WireMock.get(urlEqualTo("/probation-cases/$CRN/details"))
          .willReturn(
            aResponse()
              .withFault(Fault.RANDOM_DATA_THEN_CLOSE),
          ),
      )
    }

    val client = setupTestClient(maxRetries = 2)

    val result = client.getCaseDetail(CRN)

    assertThat(result).isInstanceOf(ClientResult.Failure.Other::class.java)
    result as ClientResult.Failure.Other
    assertThat(result.exception.cause!!.message).startsWith("Connection prematurely closed BEFORE response")

    wiremockManager.wiremockServer.verify(3, getRequestedFor(urlEqualTo("/probation-cases/$CRN/details")))
  }

  @Test
  fun `Don't retry timeouts`() {
    val clientTimeout = environment["upstream-timeout-ms"]!!.toInt()

    mockOAuth2ClientCredentialsCallIfRequired {
      wiremockServer.stubFor(
        WireMock.get(urlEqualTo("/probation-cases/$CRN/details"))
          .willReturn(
            aResponse()
              .withFixedDelay(clientTimeout + 500)
              .withFault(Fault.RANDOM_DATA_THEN_CLOSE),
          ),
      )
    }

    val client = setupTestClient(maxRetries = 2)

    val result = client.getCaseDetail(CRN)

    assertThat(result).isInstanceOf(ClientResult.Failure.Other::class.java)
    result as ClientResult.Failure.Other
    assertThat(result.exception.cause).isInstanceOf(io.netty.handler.timeout.ReadTimeoutException::class.java)

    wiremockManager.wiremockServer.verify(1, getRequestedFor(urlEqualTo("/probation-cases/$CRN/details")))
  }
}
