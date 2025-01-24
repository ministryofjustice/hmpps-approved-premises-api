package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.client

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.WebClientCache
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUnsuccesfullCaseDetailCall

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
  @Qualifier("nomisUserRolesApiWebClient")
  lateinit var nomisUserRolesApiWebClientConfig: WebClientConfig

  @Autowired
  lateinit var environment: Environment

  fun setupTestClientWithoutRetriesEnabled(maxRetries: Long) =
    ApDeliusContextApiClient(
      WebClientConfig(
        apDeliusContextApiWebClientConfig.webClient,
        maxRetries,
        retryOnReadTimeout = false,
      ),
      objectMapper,
      webClientCache,
    )

  fun setupTestClientWithRetriesEnabled(maxRetries: Long) =
    NomisUserRolesApiClient(
      WebClientConfig(
        nomisUserRolesApiWebClientConfig.webClient,
        maxRetries,
        retryOnReadTimeout = true,
      ),
      objectMapper,
      webClientCache,
    )

  @ParameterizedTest
  @MethodSource("successHttpStatusCode")
  fun `Don't retry for success outcomes`(httpStatusCode: Int) {
    apDeliusContextMockSuccessfulCaseDetailCall(
      crn = CRN,
      response = CaseDetailFactory().produce(),
      responseStatus = httpStatusCode,
    )

    val client = setupTestClientWithoutRetriesEnabled(maxRetries = 2)

    val result = client.getCaseDetail(CRN)

    assertThat(result).isInstanceOf(ClientResult.Success::class.java)

    wiremockManager.wiremockServer.verify(1, getRequestedFor(urlEqualTo("/probation-cases/$CRN/details")))
  }

  @Test
  fun `Dont retry if maxRetries is 0`() {
    apDeliusContextMockUnsuccesfullCaseDetailCall(
      crn = CRN,
      responseStatus = 500,
    )

    val client = setupTestClientWithoutRetriesEnabled(maxRetries = 0)

    val result = client.getCaseDetail(CRN)

    assertThat(result).isInstanceOf(ClientResult.Failure.StatusCode::class.java)

    wiremockManager.wiremockServer.verify(1, getRequestedFor(urlEqualTo("/probation-cases/$CRN/details")))
  }

  @ParameterizedTest
  @MethodSource("errorHttpStatusCodesForRetries")
  fun `Retry for all applicable error status codes`(httpStatusCode: Int) {
    apDeliusContextMockUnsuccesfullCaseDetailCall(
      crn = CRN,
      responseStatus = httpStatusCode,
    )

    val client = setupTestClientWithoutRetriesEnabled(maxRetries = 2)

    val result = client.getCaseDetail(CRN)

    assertThat(result).isInstanceOf(ClientResult.Failure.StatusCode::class.java)
    result as ClientResult.Failure.StatusCode
    assertThat(result.status.value()).isEqualTo(httpStatusCode)

    wiremockManager.wiremockServer.verify(3, getRequestedFor(urlEqualTo("/probation-cases/$CRN/details")))
  }

  @ParameterizedTest
  @MethodSource("errorHttpStatusCodeThatDontRetry")
  fun `Don't retry for all applicable error status codes`(httpStatusCode: Int) {
    apDeliusContextMockUnsuccesfullCaseDetailCall(
      crn = CRN,
      responseStatus = httpStatusCode,
    )

    val client = setupTestClientWithoutRetriesEnabled(maxRetries = 2)

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

    val client = setupTestClientWithoutRetriesEnabled(maxRetries = 2)

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

    val client = setupTestClientWithoutRetriesEnabled(maxRetries = 2)

    val result = client.getCaseDetail(CRN)

    assertThat(result).isInstanceOf(ClientResult.Failure.Other::class.java)
    result as ClientResult.Failure.Other
    assertThat(result.exception.cause).isInstanceOf(io.netty.handler.timeout.ReadTimeoutException::class.java)

    wiremockManager.wiremockServer.verify(1, getRequestedFor(urlEqualTo("/probation-cases/$CRN/details")))
  }

  @Test
  fun `Retry timeouts when enabled`() {
    val clientTimeout = environment["nomis-user-roles-api-upstream-timeout-ms"]!!.toInt()

    wiremockServer.stubFor(
      WireMock.get(urlEqualTo("/me"))
        .inScenario("retry on timeout")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(
          aResponse()
            .withFixedDelay(clientTimeout + 500),
        ).willSetStateTo("successful call"),
    )

    val response = NomisUserDetailFactory().produce()
    wiremockServer.stubFor(
      WireMock.get(urlEqualTo("/me"))
        .inScenario("retry on timeout")
        .whenScenarioStateIs("successful call")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(objectMapper.writeValueAsString(response)),
        ),
    )

    val client = setupTestClientWithRetriesEnabled(maxRetries = 1)
    val clientResponse = client.getUserDetails("token")

    wiremockManager.wiremockServer.verify(2, getRequestedFor(urlEqualTo("/me")))
    assertThat(clientResponse).isInstanceOf(ClientResult.Success::class.java)
    clientResponse as ClientResult.Success
    assertThat(clientResponse.body).isEqualTo(response)
  }
}
