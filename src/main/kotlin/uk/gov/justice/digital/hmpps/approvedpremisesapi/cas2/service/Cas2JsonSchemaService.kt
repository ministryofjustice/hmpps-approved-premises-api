package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaRepository

@Deprecated("APS-1570 Schema validation will be removed in the future")
@Service
class Cas2JsonSchemaService(
  private val jsonSchemaRepository: JsonSchemaRepository,
) {
  @SuppressWarnings("FunctionOnlyReturningConstant", "UnusedParameter")
  @Deprecated("APS-1570 As a intermediary step to remove schema logic, this always returns true")
  fun validate(schema: JsonSchemaEntity, json: String) = true

  fun <T : JsonSchemaEntity> getNewestSchema(type: Class<T>): JsonSchemaEntity = jsonSchemaRepository.getSchemasForType(type).maxBy { it.addedAt }
}
