package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersionDetector
import org.hibernate.Hibernate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaRepository
import java.util.Collections.synchronizedMap
import java.util.UUID

@Service
class JsonSchemaService(
  private val objectMapper: ObjectMapper,
  private val jsonSchemaRepository: JsonSchemaRepository,
  private val applicationRepository: ApplicationRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val schemas = synchronizedMap<UUID, JsonSchema>(mutableMapOf())

  fun validate(schema: JsonSchemaEntity, json: String): Boolean {
    val schemaJsonNode = objectMapper.readTree(schema.schema)
    val jsonNode = objectMapper.readTree(json)

    if (!schemas.containsKey(schema.id)) {
      schemas[schema.id] = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaJsonNode))
        .getSchema(schemaJsonNode)
    }

    val validationErrors = schemas[schema.id]!!.validate(jsonNode)

    if (validationErrors.isNotEmpty()) {
      log.warn("Validation errors whilst validating schema: \n\t${validationErrors.joinToString("\n\t,") { "Schema Path: ${it.schemaPath} -> Path: ${it.path}: ${it.message}" }}")
    }

    return validationErrors.isEmpty()
  }

  fun checkSchemaOutdated(application: ApplicationEntity): ApplicationEntity {
    val newestSchema = getNewestSchema(Hibernate.getClass(application.schemaVersion))

    return application.apply { application.schemaUpToDate = application.schemaVersion.id == newestSchema.id }
  }

  fun <T : JsonSchemaEntity> getNewestSchema(type: Class<T>): T = Hibernate.unproxy(jsonSchemaRepository.getSchemasForType(type).maxBy { it.addedAt }) as T
}
