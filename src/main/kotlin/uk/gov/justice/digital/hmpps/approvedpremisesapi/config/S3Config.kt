package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.retries.api.BackoffStrategy
import software.amazon.awssdk.retries.api.RetryStrategy
import software.amazon.awssdk.services.s3.S3Client
import java.time.Duration

@Component
@ConfigurationProperties(prefix = "s3")
class S3Config {
  lateinit var bucketName: String
  lateinit var region: String
}

@Configuration
class S3ClientConfig {
  /**
   * The service uses IAM Roles for Service Accounts (IRSA) to authenticate to S3. The SDK will automatically use the WebIdentityTokenFileCredentialsProvider
   * to retrieve credentials from the environment and will fall back to the DefaultCredentialsProvider if that fails.
   */
  @Bean
  fun s3Client(s3Config: S3Config): S3Client {
    val credentialsProvider = AwsCredentialsProviderChain.builder()
      .credentialsProviders(
        WebIdentityTokenFileCredentialsProvider.create(),
        DefaultCredentialsProvider.builder().build(),
      )
      .build()

    return S3Client.builder()
      .region(Region.of(s3Config.region))
      .credentialsProvider(credentialsProvider)
      .overrideConfiguration { it.retryStrategy(s3RetryStrategy()) }
      .build()
  }

  /**
   * The SDK retries only transient failures (throttling, 5xx, network errors);
   * permanent errors such as 403/404 fail immediately.
   */
  fun s3RetryStrategy(): RetryStrategy = AwsRetryStrategy.standardRetryStrategy()
    .toBuilder()
    .maxAttempts(MAX_ATTEMPTS)
    .backoffStrategy(BackoffStrategy.exponentialDelay(BASE_DELAY, MAX_DELAY))
    .throttlingBackoffStrategy(BackoffStrategy.exponentialDelay(BASE_DELAY, MAX_DELAY))
    .build()

  companion object {
    const val MAX_ATTEMPTS = 5
    private val BASE_DELAY: Duration = Duration.ofSeconds(2)
    private val MAX_DELAY: Duration = Duration.ofSeconds(30)
  }
}
