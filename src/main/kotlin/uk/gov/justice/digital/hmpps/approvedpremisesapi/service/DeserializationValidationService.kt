package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.jvmErasure

@Service
class DeserializationValidationService {
  fun validateArray(path: String = "$", targetType: KClass<*>, jsonArray: ArrayNode, elementsNullable: Boolean = true): Map<String, String> {
    val result = mutableMapOf<String, String>()

    if (getExpectedJsonPrimitiveType(targetType.java) != null) {
      jsonArray.forEachIndexed { index, jsonNode ->
        val expectedJsonPrimitiveType = getExpectedJsonPrimitiveType(targetType.java)
        if (jsonNode.nodeType != expectedJsonPrimitiveType) {
          result["$path[$index]"] = "expected${expectedJsonPrimitiveType!!.name.lowercase().replaceFirstChar(Char::uppercase)}"
        } else {
          val expectedSpecialHandlingJsonPrimitiveType = getExpectedSpecialHandlingJsonPrimitiveTypeChecker(targetType.java, jsonNode)
          if (expectedSpecialHandlingJsonPrimitiveType?.isValid() == false) {
            result["$path[$index]"] = "invalid"
          }
        }
      }

      return result
    }

    jsonArray.forEachIndexed { index, jsonNode ->
      if (nullOrNullNode(jsonNode)) {
        if (!elementsNullable) {
          result["$path[$index]"] = "expectedObject"
        }
        return@forEachIndexed
      }
      result.putAll(validateObject("$path[$index]", targetType, jsonNode as ObjectNode))
    }
    return result
  }

  fun validateObject(path: String = "$", targetType: KClass<*>, jsonObject: ObjectNode): Map<String, String> {
    val result = mutableMapOf<String, String>()

    targetType.declaredMemberProperties.forEach {
      val jsonNode = jsonObject.get(it.name)

      if (it.returnType.isMarkedNullable && nullOrNullNode(jsonNode)) {
        return@forEach
      }

      if (!it.returnType.isMarkedNullable && nullOrNullNode(jsonNode)) {
        result["$path.${it.name}"] = "empty"
        return@forEach
      }

      if (!nullOrNullNode(jsonNode)) {
        if (isArrayType(it.returnType.jvmErasure.java)) {
          if (jsonObject.get(it.name) !is ArrayNode) {
            result["$path.${it.name}"] = "expectedArray"
            return@forEach
          }

          val genericType = it.returnType.arguments.first().type!!.jvmErasure
          result.putAll(validateArray("$path.${it.name}", genericType, jsonObject.get(it.name) as ArrayNode, it.returnType.arguments.first().type!!.isMarkedNullable))
          return@forEach
        }

        if ((it.returnType.jvmErasure.java as Class<*>).isEnum) {
          if (jsonObject.get(it.name) !is TextNode) {
            result["$path.${it.name}"] = "expectedString"
          }
          return@forEach
        }

        if (getExpectedJsonPrimitiveType(it.returnType.jvmErasure.java) == null) {
          if (jsonObject.get(it.name) !is ObjectNode) {
            result["$path.${it.name}"] = "expectedObject"
            return@forEach
          }

          result.putAll(
            validateObject("$path.${it.name}", it.returnType.jvmErasure.java.kotlin, jsonObject.get(it.name) as ObjectNode),
          )
          return@forEach
        }

        val expectedJsonPrimitiveType = getExpectedJsonPrimitiveType(it.returnType.jvmErasure.java)
        if (jsonNode.nodeType != expectedJsonPrimitiveType) {
          result["$path.${it.name}"] = "expected${expectedJsonPrimitiveType!!.name.lowercase().replaceFirstChar(Char::uppercase)}"
        } else {
          val expectedSpecialHandlingJsonPrimitiveType = getExpectedSpecialHandlingJsonPrimitiveTypeChecker(it.returnType.jvmErasure.java, jsonNode)
          if (expectedSpecialHandlingJsonPrimitiveType?.isValid() == false) {
            result["$path.${it.name}"] = "invalid"
          }
        }
      }
    }

    return result
  }

  private fun getExpectedSpecialHandlingJsonPrimitiveTypeChecker(jvmPrimitive: Class<*>, jsonNode: JsonNode): SpecialJsonPrimitiveTypeChecker? = when (jvmPrimitive) {
    LocalDate::class.java -> LocalDateSpecialJsonPrimitiveTypeChecker(jsonNode)
    LocalDateTime::class.java -> LocalDateTimeSpecialJsonPrimitiveTypeChecker(jsonNode)
    OffsetDateTime::class.java -> OffsetDateTimeSpecialJsonPrimitiveTypeChecker(jsonNode)
    Instant::class.java -> InstantSpecialJsonPrimitiveTypeChecker(jsonNode)
    UUID::class.java -> UUIDSpecialJsonPrimitiveTypeChecker(jsonNode)
    else -> null
  }

  private fun getExpectedJsonPrimitiveType(jvmType: Class<*>): JsonNodeType? = when (jvmType) {
    String::class.java -> JsonNodeType.STRING
    Boolean::class.java -> JsonNodeType.BOOLEAN
    Boolean::class.javaObjectType -> JsonNodeType.BOOLEAN
    BigDecimal::class.java -> JsonNodeType.NUMBER
    Int::class.java -> JsonNodeType.NUMBER
    Int::class.javaObjectType -> JsonNodeType.NUMBER
    LocalDate::class.java -> JsonNodeType.STRING
    LocalDateTime::class.java -> JsonNodeType.STRING
    OffsetDateTime::class.java -> JsonNodeType.STRING
    Instant::class.java -> JsonNodeType.STRING
    UUID::class.java -> JsonNodeType.STRING
    else -> null
  }

  private fun nullOrNullNode(jsonNode: JsonNode?) = jsonNode == null || jsonNode is NullNode

  private val arrayTypes = listOf(java.util.List::class.java, java.util.HashSet::class.java)
  fun isArrayType(type: Class<*>): Boolean = arrayTypes.any { arrayType -> arrayType.isAssignableFrom(type) } || type.isArray
}

interface SpecialJsonPrimitiveTypeChecker {
  fun isValid(): Boolean
}

class LocalDateSpecialJsonPrimitiveTypeChecker(val jsonNode: JsonNode) : SpecialJsonPrimitiveTypeChecker {
  override fun isValid(): Boolean {
    if (jsonNode.nodeType != JsonNodeType.STRING) return false

    return doesNotThrow { LocalDate.parse(jsonNode.textValue()) }
  }
}

class LocalDateTimeSpecialJsonPrimitiveTypeChecker(val jsonNode: JsonNode) : SpecialJsonPrimitiveTypeChecker {
  override fun isValid(): Boolean {
    if (jsonNode.nodeType != JsonNodeType.STRING) return false

    return doesNotThrow { LocalDateTime.parse(jsonNode.textValue()) }
  }
}

class OffsetDateTimeSpecialJsonPrimitiveTypeChecker(val jsonNode: JsonNode) : SpecialJsonPrimitiveTypeChecker {
  override fun isValid(): Boolean {
    if (jsonNode.nodeType != JsonNodeType.STRING) return false

    return doesNotThrow { OffsetDateTime.parse(jsonNode.textValue()) }
  }
}

class InstantSpecialJsonPrimitiveTypeChecker(val jsonNode: JsonNode) : SpecialJsonPrimitiveTypeChecker {
  override fun isValid(): Boolean {
    if (jsonNode.nodeType != JsonNodeType.STRING) return false

    return doesNotThrow { Instant.parse(jsonNode.textValue()) }
  }
}

class UUIDSpecialJsonPrimitiveTypeChecker(val jsonNode: JsonNode) : SpecialJsonPrimitiveTypeChecker {
  override fun isValid(): Boolean {
    if (jsonNode.nodeType != JsonNodeType.STRING) return false

    return doesNotThrow { UUID.fromString(jsonNode.textValue()) }
  }
}

private fun doesNotThrow(block: () -> Unit) = try {
  block()
  true
} catch (e: Exception) {
  false
}
