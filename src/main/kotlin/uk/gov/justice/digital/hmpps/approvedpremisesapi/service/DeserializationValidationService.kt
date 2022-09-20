package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
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
          result["$path[$index]"] = "Expected a ${expectedJsonPrimitiveType!!.name.lowercase()}"
        }
      }

      return result
    }

    jsonArray.forEachIndexed { index, jsonNode ->
      if (nullOrNullNode(jsonNode)) {
        if (!elementsNullable) {
          result["$path[$index]"] = "Expected an object"
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
        result["$path.${it.name}"] = "A value must be provided for this property"
        return@forEach
      }

      if (!nullOrNullNode(jsonNode)) {
        if (isArrayType(it.returnType.jvmErasure.java)) {
          if (jsonObject.get(it.name) !is ArrayNode) {
            result["$path.${it.name}"] = "Expected an array"
            return@forEach
          }

          val genericType = it.returnType.arguments.first().type!!.jvmErasure
          result.putAll(validateArray("$path.${it.name}", genericType, jsonObject.get(it.name) as ArrayNode, it.returnType.arguments.first().type!!.isMarkedNullable))
          return@forEach
        }

        if (getExpectedJsonPrimitiveType(it.returnType.jvmErasure.java) == null) {
          if (jsonObject.get(it.name) !is ObjectNode) {
            result["$path.${it.name}"] = "Expected an object"
            return@forEach
          }

          result.putAll(
            validateObject("$path.${it.name}", it.returnType.jvmErasure.java.kotlin, jsonObject.get(it.name) as ObjectNode)
          )
          return@forEach
        }

        val expectedJsonPrimitiveType = getExpectedJsonPrimitiveType(it.returnType.jvmErasure.java)
        if (jsonNode.nodeType != expectedJsonPrimitiveType) {
          result["$path.${it.name}"] = "Expected a ${expectedJsonPrimitiveType!!.name.lowercase()}"
        }
      }
    }

    return result
  }

  private fun getExpectedJsonPrimitiveType(jvmPrimitive: Class<*>): JsonNodeType? {
    return when (jvmPrimitive) {
      String::class.java -> JsonNodeType.STRING
      LocalDate::class.java -> JsonNodeType.STRING
      LocalDateTime::class.java -> JsonNodeType.STRING
      OffsetDateTime::class.java -> JsonNodeType.STRING
      Boolean::class.java -> JsonNodeType.BOOLEAN
      Boolean::class.javaObjectType -> JsonNodeType.BOOLEAN
      BigDecimal::class.java -> JsonNodeType.NUMBER
      Int::class.java -> JsonNodeType.NUMBER
      Int::class.javaObjectType -> JsonNodeType.NUMBER
      else -> null
    }
  }

  private fun nullOrNullNode(jsonNode: JsonNode?) = jsonNode == null || jsonNode is NullNode

  private val arrayTypes = listOf(java.util.List::class.java, java.util.HashSet::class.java)
  fun isArrayType(type: Class<*>): Boolean {
    return arrayTypes.any { arrayType -> arrayType.isAssignableFrom(type) } || type.isArray
  }
}
