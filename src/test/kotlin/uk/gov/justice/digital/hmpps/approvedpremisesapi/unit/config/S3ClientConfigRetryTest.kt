package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.config

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest
import software.amazon.awssdk.retries.api.RetryStrategy
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException
import software.amazon.awssdk.services.s3.model.S3Exception
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.S3ClientConfig
import java.io.IOException

class S3ClientConfigRetryTest {

  private val retryStrategy = S3ClientConfig().s3RetryStrategy()

  @Test
  fun `s3RetryStrategy allows the configured maximum number of attempts`() {
    assertThat(retryStrategy.maxAttempts()).isEqualTo(S3ClientConfig.MAX_ATTEMPTS)
  }

  @ParameterizedTest
  @ValueSource(ints = [500, 503])
  fun `s3RetryStrategy retries transient server errors`(statusCode: Int) {
    assertRetryAllowedFor(retryStrategy, s3Exception(statusCode))
  }

  @Test
  fun `s3RetryStrategy retries client side network errors`() {
    assertRetryAllowedFor(retryStrategy, SdkClientException.builder().message("connection reset").cause(IOException("Connection reset")).build())
  }

  @ParameterizedTest
  @ValueSource(ints = [400, 403, 404])
  fun `s3RetryStrategy does not retry permanent client errors`(statusCode: Int) {
    assertRetryRejectedFor(retryStrategy, s3Exception(statusCode))
  }

  private fun assertRetryAllowedFor(strategy: RetryStrategy, failure: Throwable) {
    val token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("s3")).token()
    strategy.refreshRetryToken(RefreshRetryTokenRequest.builder().token(token).failure(failure).build())
  }

  private fun assertRetryRejectedFor(strategy: RetryStrategy, failure: Throwable) {
    assertThatThrownBy {
      val token = strategy.acquireInitialToken(
        AcquireInitialTokenRequest.create("s3"),
      ).token()

      strategy.refreshRetryToken(
        RefreshRetryTokenRequest.builder()
          .token(token)
          .failure(failure)
          .build(),
      )
    }.isInstanceOf(TokenAcquisitionFailedException::class.java)
  }

  private fun s3Exception(statusCode: Int) = S3Exception.builder()
    .statusCode(statusCode)
    .awsErrorDetails(AwsErrorDetails.builder().errorCode("Status$statusCode").serviceName("S3").build())
    .message("status $statusCode")
    .build()
}
