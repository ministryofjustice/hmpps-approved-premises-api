package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.hibernate.Hibernate
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaRepository

@Deprecated("APS-1570 Schema validation will be removed in the future")
@Service
class JsonSchemaService(
  private val jsonSchemaRepository: JsonSchemaRepository,
) {
  @SuppressWarnings("FunctionOnlyReturningConstant", "UnusedParameter")
  @Deprecated("APS-1570 As a intermediary step to remove schema logic, this always returns true")
  fun validate(schema: JsonSchemaEntity, json: String) = true

  fun checkSchemaOutdated(application: ApplicationEntity): ApplicationEntity {
    val newestSchema = getNewestSchema(Hibernate.getClass(application.schemaVersion))

    return application.apply { application.schemaUpToDate = application.schemaVersion.id == newestSchema.id }
  }

  fun <T : JsonSchemaEntity> getNewestSchema(type: Class<T>): T = Hibernate.unproxy(jsonSchemaRepository.getSchemasForType(type).maxBy { it.addedAt }) as T
}
