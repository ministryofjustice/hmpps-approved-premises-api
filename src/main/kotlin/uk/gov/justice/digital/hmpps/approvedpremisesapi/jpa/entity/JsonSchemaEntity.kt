package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table

@Repository
interface JsonSchemaRepository : JpaRepository<JsonSchemaEntity, UUID> {
  @Query("SELECT js FROM JsonSchemaEntity js WHERE TYPE(js) = :type")
  fun <T : JsonSchemaEntity> getSchemasForType(type: Class<T>): List<JsonSchemaEntity>
}

@Entity
@Table(name = "json_schemas")
@DiscriminatorColumn(name = "type")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class JsonSchemaEntity(
  @Id
  val id: UUID,
  val addedAt: OffsetDateTime,
  val schema: String
)

@Entity
@DiscriminatorValue("APPROVED_PREMISES_APPLICATION")
@Table(name = "approved_premises_application_json_schemas")
@PrimaryKeyJoinColumn(name = "json_schema_id")
class ApprovedPremisesApplicationJsonSchemaEntity(
  id: UUID,
  addedAt: OffsetDateTime,
  schema: String,
) : JsonSchemaEntity(id, addedAt, schema)

@Entity
@DiscriminatorValue("APPROVED_PREMISES_ASSESSMENT")
@Table(name = "approved_premises_assessment_json_schemas")
@PrimaryKeyJoinColumn(name = "json_schema_id")
class ApprovedPremisesAssessmentJsonSchemaEntity(
  id: UUID,
  addedAt: OffsetDateTime,
  schema: String
) : JsonSchemaEntity(id, addedAt, schema)
