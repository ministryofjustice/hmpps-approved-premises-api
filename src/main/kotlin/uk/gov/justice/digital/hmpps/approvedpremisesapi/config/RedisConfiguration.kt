package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.shouldNotBeReached
import java.time.Duration

@Configuration
@EnableCaching
class RedisConfiguration {
  @Bean
  fun redisCacheManagerBuilderCustomizer(
    buildProperties: BuildProperties,
    objectMapper: ObjectMapper
  ): RedisCacheManagerBuilderCustomizer? {
    val version = buildProperties.version

    return RedisCacheManagerBuilderCustomizer { builder: RedisCacheManagerBuilder ->
      builder.clientCacheFor<List<StaffMember>>("staffMembersCache", Duration.ofHours(6), version, objectMapper)
        .clientCacheFor<StaffMember>("staffMemberCache", Duration.ofHours(6), version, objectMapper)
    }
  }

  private inline fun <reified T> RedisCacheManagerBuilder.clientCacheFor(cacheName: String, duration: Duration, version: String, objectMapper: ObjectMapper) =
    this.withCacheConfiguration(
      cacheName,
      RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(duration)
        .serializeValuesWith(SerializationPair.fromSerializer(ClientResultRedisSerializer(objectMapper, object : TypeReference<T>() {})))
        .prefixCacheNameWith(version)
    )
}

class ClientResultRedisSerializer(
  private val objectMapper: ObjectMapper,
  private val typeReference: TypeReference<*>
) : RedisSerializer<ClientResult<Any>> {
  override fun serialize(clientResult: ClientResult<Any>?): ByteArray? {
    val toSerialize = when (clientResult) {
      is ClientResult.StatusCodeFailure -> {
        SerializableClientResult(
          discriminator = ClientResultDiscriminator.STATUS_CODE_FAILURE,
          status = clientResult.status,
          body = clientResult.body,
          exceptionMessage = null,
          type = null,
          method = clientResult.method,
          path = clientResult.path
        )
      }
      is ClientResult.OtherFailure -> {
        SerializableClientResult(
          discriminator = ClientResultDiscriminator.OTHER_FAILURE,
          status = null,
          body = null,
          exceptionMessage = clientResult.exception.message,
          type = null,
          method = clientResult.method,
          path = clientResult.path
        )
      }
      is ClientResult.Success -> {
        SerializableClientResult(
          discriminator = ClientResultDiscriminator.SUCCESS,
          status = clientResult.status,
          body = objectMapper.writeValueAsString(clientResult.body),
          exceptionMessage = null,
          type = clientResult.body::class.java.typeName,
          method = null,
          path = null
        )
      }
      else -> shouldNotBeReached()
    }

    return objectMapper.writeValueAsBytes(toSerialize)
  }

  override fun deserialize(bytes: ByteArray?): ClientResult<Any>? {
    val deserializedWrapper = objectMapper.readValue(bytes, SerializableClientResult::class.java)

    if (deserializedWrapper.discriminator == ClientResultDiscriminator.SUCCESS) {
      return ClientResult.Success(
        status = deserializedWrapper.status!!,
        body = objectMapper.readValue(deserializedWrapper.body, typeReference)
      )
    }

    if (deserializedWrapper.discriminator == ClientResultDiscriminator.STATUS_CODE_FAILURE) {
      return ClientResult.StatusCodeFailure(
        method = deserializedWrapper.method!!,
        path = deserializedWrapper.path!!,
        status = deserializedWrapper.status!!,
        body = deserializedWrapper.body
      )
    }

    if (deserializedWrapper.discriminator == ClientResultDiscriminator.OTHER_FAILURE) {
      return ClientResult.StatusCodeFailure(
        method = deserializedWrapper.method!!,
        path = deserializedWrapper.path!!,
        status = deserializedWrapper.status!!,
        body = deserializedWrapper.body
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
  val method: HttpMethod?,
  val path: String?
)

enum class ClientResultDiscriminator {
  SUCCESS,
  STATUS_CODE_FAILURE,
  OTHER_FAILURE
}
