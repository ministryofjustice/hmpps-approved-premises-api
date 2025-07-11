package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.MarshallableHttpMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.bankholidaysapi.UKBankHolidays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserOffenderAccess
import java.time.Duration

@SuppressWarnings("LongParameterList")
@Configuration
@EnableCaching
class RedisConfiguration {
  @Bean
  fun redisCacheManagerBuilderCustomizer(
    buildProperties: BuildProperties,
    objectMapper: ObjectMapper,
    @Value("\${caches.staffMembers.expiry-seconds}") staffMembersExpirySeconds: Long,
    @Value("\${caches.staffMember.expiry-seconds}") staffMemberExpirySeconds: Long,
    @Value("\${caches.userAccess.expiry-seconds}") userAccessExpirySeconds: Long,
    @Value("\${caches.staffDetails.expiry-seconds}") staffDetailsExpirySeconds: Long,
    @Value("\${caches.teamManagingCases.expiry-seconds}") teamManagingCasesExpirySeconds: Long,
    @Value("\${caches.ukBankHolidays.expiry-seconds}") ukBankHolidaysExpirySeconds: Long,
    @Value("600") crnGetCaseDetailExpirySeconds: Long,
  ): RedisCacheManagerBuilderCustomizer? {
    val uniqueBuildId = buildProperties.time.epochSecond.toString()

    return RedisCacheManagerBuilderCustomizer { builder: RedisCacheManagerBuilder ->
      builder
        .clientCacheFor<StaffMembersPage>(
          cacheName = "qCodeStaffMembersCache",
          duration = Duration.ofSeconds(staffMembersExpirySeconds),
          cacheNamePrefix = uniqueBuildId,
          objectMapper = objectMapper,
        )
        .clientCacheFor<UserOffenderAccess>(
          cacheName = "userAccessCache",
          duration = Duration.ofSeconds(userAccessExpirySeconds),
          cacheNamePrefix = uniqueBuildId,
          objectMapper = objectMapper,
        )
        .clientCacheFor<StaffDetail>(
          cacheName = "staffDetailsCache",
          duration = Duration.ofSeconds(staffDetailsExpirySeconds),
          cacheNamePrefix = uniqueBuildId,
          objectMapper = objectMapper,
        )
        .clientCacheFor<ManagingTeamsResponse>(
          cacheName = "teamsManagingCaseCache",
          duration = Duration.ofSeconds(teamManagingCasesExpirySeconds),
          cacheNamePrefix = uniqueBuildId,
          objectMapper = objectMapper,
        )
        .clientCacheFor<CaseDetail>(
          cacheName = "crnGetCaseDetailCache",
          duration = Duration.ofSeconds(crnGetCaseDetailExpirySeconds),
          cacheNamePrefix = uniqueBuildId,
          objectMapper = objectMapper,
        )
        .clientCacheFor<UKBankHolidays>(
          cacheName = "ukBankHolidaysCache",
          duration = Duration.ofSeconds(ukBankHolidaysExpirySeconds),
          cacheNamePrefix = uniqueBuildId,
          objectMapper = objectMapper,
        )
    }
  }

  @Bean
  fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<*, *>? {
    val template: RedisTemplate<*, *> = RedisTemplate<Any, Any>()
    template.connectionFactory = connectionFactory
    template.keySerializer = StringRedisSerializer()
    return template
  }

  private inline fun <reified T> RedisCacheManagerBuilder.clientCacheFor(
    cacheName: String,
    duration: Duration,
    cacheNamePrefix: String,
    objectMapper: ObjectMapper,
  ) = this.withCacheConfiguration(
    cacheName,
    RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(duration)
      .serializeValuesWith(SerializationPair.fromSerializer(ClientResultRedisSerializer(objectMapper, object : TypeReference<T>() {})))
      .prefixCacheNameWith(cacheNamePrefix),
  )
}

class ClientResultRedisSerializer(
  private val objectMapper: ObjectMapper,
  private val typeReference: TypeReference<*>,
) : RedisSerializer<ClientResult<*>> {
  override fun serialize(clientResult: ClientResult<*>?): ByteArray {
    val toSerialize = when (clientResult) {
      is ClientResult.Failure.StatusCode -> {
        SerializableClientResult(
          discriminator = ClientResultDiscriminator.STATUS_CODE_FAILURE,
          status = clientResult.status,
          body = clientResult.body,
          exceptionMessage = null,
          type = null,
          method = MarshallableHttpMethod.fromHttpMethod(clientResult.method),
          path = clientResult.path,
        )
      }
      is ClientResult.Failure.Other -> {
        SerializableClientResult(
          discriminator = ClientResultDiscriminator.OTHER_FAILURE,
          status = null,
          body = null,
          exceptionMessage = clientResult.exception.message,
          type = null,
          method = MarshallableHttpMethod.fromHttpMethod(clientResult.method),
          path = clientResult.path,
        )
      }
      is ClientResult.Failure.PreemptiveCacheTimeout ->
        throw RuntimeException("Preemptively cached requests should not be annotated with @Cacheable")
      is ClientResult.Success -> {
        SerializableClientResult(
          discriminator = ClientResultDiscriminator.SUCCESS,
          status = clientResult.status,
          body = objectMapper.writeValueAsString(clientResult.body),
          exceptionMessage = null,
          type = clientResult.body!!::class.java.typeName,
          method = null,
          path = null,
        )
      }
      else -> null
    }

    return objectMapper.writeValueAsBytes(toSerialize)
  }

  override fun deserialize(bytes: ByteArray?): ClientResult<Any> {
    val deserializedWrapper = objectMapper.readValue(bytes, SerializableClientResult::class.java)

    if (deserializedWrapper.discriminator == ClientResultDiscriminator.SUCCESS) {
      return ClientResult.Success(
        status = deserializedWrapper.status!!,
        body = objectMapper.readValue(deserializedWrapper.body, typeReference),
      )
    }

    if (deserializedWrapper.discriminator == ClientResultDiscriminator.STATUS_CODE_FAILURE) {
      return ClientResult.Failure.StatusCode(
        method = deserializedWrapper.method!!.toHttpMethod(),
        path = deserializedWrapper.path!!,
        status = deserializedWrapper.status!!,
        body = deserializedWrapper.body,
      )
    }

    if (deserializedWrapper.discriminator == ClientResultDiscriminator.OTHER_FAILURE) {
      return ClientResult.Failure.Other(
        method = deserializedWrapper.method!!.toHttpMethod(),
        path = deserializedWrapper.path!!,
        exception = RuntimeException(deserializedWrapper.exceptionMessage),
      )
    }

    throw RuntimeException("Unhandled discriminator type: ${deserializedWrapper.discriminator}")
  }
}

data class SerializableClientResult(
  val discriminator: ClientResultDiscriminator,
  val type: String?,
  val status: HttpStatus?,
  val body: String?,
  val exceptionMessage: String?,
  val method: MarshallableHttpMethod?,
  val path: String?,
)

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class ClientResultDiscriminator {
  SUCCESS,
  STATUS_CODE_FAILURE,
  OTHER_FAILURE,
}

const val IS_NOT_SUCCESSFUL = "!(#result instanceof T(uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult\$Success))"
