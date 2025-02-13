package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
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
import redis.lock.redlock.RedLock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.MarshallableHttpMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.bankholidaysapi.UKBankHolidays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserOffenderAccess
import java.time.Duration

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
    @Value("21600") crnGetCaseDetailExpirySeconds: Long,
  ): RedisCacheManagerBuilderCustomizer? {
    // this means caches aren't shared across instances (!?)
    val time = buildProperties.time.epochSecond.toString()

    return RedisCacheManagerBuilderCustomizer { builder: RedisCacheManagerBuilder ->
      // i'm not convinced all these caches are used anymore (e.g. community api)
      builder.clientCacheFor<StaffMembersPage>(
        cacheName = "qCodeStaffMembersCache",
        duration = Duration.ofSeconds(staffMembersExpirySeconds),
        cachePrefix = time,
        objectMapper = objectMapper,
      )
        .clientCacheFor<UserOffenderAccess>("userAccessCache", Duration.ofSeconds(userAccessExpirySeconds), time, objectMapper)
        .clientCacheFor<StaffDetail>("staffDetailsCache", Duration.ofSeconds(staffDetailsExpirySeconds), time, objectMapper)
        .clientCacheFor<ManagingTeamsResponse>("teamsManagingCaseCache", Duration.ofSeconds(teamManagingCasesExpirySeconds), time, objectMapper)
        .clientCacheFor<CaseDetail>("crnGetCaseDetailCache", Duration.ofSeconds(crnGetCaseDetailExpirySeconds), time, objectMapper)
        .clientCacheFor<UKBankHolidays>("ukBankHolidaysCache", Duration.ofSeconds(ukBankHolidaysExpirySeconds), time, objectMapper)
    }
  }

  private val log = LoggerFactory.getLogger(this::class.java)

  @Bean
  fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<*, *>? {
    log.info("Redis connection factory is " + connectionFactory)
    val template: RedisTemplate<*, *> = RedisTemplate<Any, Any>()
    template.connectionFactory = connectionFactory
    template.keySerializer = StringRedisSerializer()
    return template
  }

  @Bean
  fun redLock(
    @Value("\${spring.data.redis.host}") host: String,
    @Value("\${spring.data.redis.port}") port: Int,
    @Value("\${spring.data.redis.password}") password: String,
    @Value("\${spring.data.redis.database}") database: Int,
    @Value("\${spring.data.redis.ssl.enabled}") ssl: Boolean,
  ): RedLock {
    val scheme = if (ssl) "rediss" else "redis"
    val passwordString = if (password.isNotEmpty()) ":$password@" else ""
    return RedLock(arrayOf("$scheme://$passwordString$host:$port/$database"))
  }

  private inline fun <reified T> RedisCacheManagerBuilder.clientCacheFor(cacheName: String, duration: Duration, cachePrefix: String, objectMapper: ObjectMapper) =
    this.withCacheConfiguration(
      cacheName,
      RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(duration)
        .serializeValuesWith(SerializationPair.fromSerializer(ClientResultRedisSerializer(objectMapper, object : TypeReference<T>() {})))
        .prefixCacheNameWith(cachePrefix),
    )
}

class ClientResultRedisSerializer(
  private val objectMapper: ObjectMapper,
  private val typeReference: TypeReference<*>,
) : RedisSerializer<ClientResult<*>> {

  private val log = LoggerFactory.getLogger(this::class.java)

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
    log.info("Deserializing result")
    val deserializedWrapper = objectMapper.readValue(bytes, SerializableClientResult::class.java)

    if (deserializedWrapper.discriminator == ClientResultDiscriminator.SUCCESS) {
      val result = ClientResult.Success(
        status = deserializedWrapper.status!!,
        body = objectMapper.readValue(deserializedWrapper.body, typeReference),
      )
      log.info("Deserialized success")
      return result
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

enum class ClientResultDiscriminator {
  SUCCESS,
  STATUS_CODE_FAILURE,
  OTHER_FAILURE,
}

const val IS_NOT_SUCCESSFUL = "!(#result instanceof T(uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult\$Success))"
