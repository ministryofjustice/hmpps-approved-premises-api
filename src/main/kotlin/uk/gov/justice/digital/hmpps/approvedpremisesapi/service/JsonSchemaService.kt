package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersionDetector
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSchemaRepository
import java.util.Collections.synchronizedMap
import java.util.UUID

@Service
class JsonSchemaService(
  private val objectMapper: ObjectMapper,
  private val applicationSchemaRepository: ApplicationSchemaRepository,
  private val applicationRepository: ApplicationRepository
) {
  private val schemas = synchronizedMap<UUID, JsonSchema>(mutableMapOf())

  private fun validate(schema: ApplicationSchemaEntity, json: String): Boolean {
    val schemaJsonNode = objectMapper.readTree(schema.schema)
    val jsonNode = objectMapper.readTree(json)

    if (!schemas.containsKey(schema.id)) {
      schemas[schema.id] = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaJsonNode))
        .getSchema(schemaJsonNode)
    }

    val validationErrors = schemas[schema.id]!!.validate(jsonNode)

    return validationErrors.isEmpty()
  }

  fun attemptSchemaUpgrade(application: ApplicationEntity): ApplicationEntity {
    if (application.data == null) return application.apply { schemaUpToDate = true }

    val newestSchema = applicationSchemaRepository.findFirstByOrderByAddedAtDesc()

    if (application.schemaVersion.id != newestSchema.id) {
      if (!validate(newestSchema, application.data!!)) {
        return application.apply { schemaUpToDate = false }
      }

      application.schemaVersion = newestSchema
      applicationRepository.save(application)
    }

    application.schemaUpToDate = true

    return application
  }

  fun getNewestSchema(): ApplicationSchemaEntity = applicationSchemaRepository.findFirstByOrderByAddedAtDesc()
}
