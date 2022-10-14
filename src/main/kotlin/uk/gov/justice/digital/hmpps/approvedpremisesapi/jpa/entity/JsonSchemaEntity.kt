package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface JsonSchemaRepository : JpaRepository<JsonSchemaEntity, UUID> {
  fun findFirstByTypeOrderByAddedAtDesc(type: JsonSchemaType): JsonSchemaEntity
}

@Entity
@Table(name = "json_schemas")
data class JsonSchemaEntity(
  @Id
  val id: UUID,
  val addedAt: OffsetDateTime,
  val schema: String,
  @Enumerated(value = EnumType.STRING)
  val type: JsonSchemaType
)

enum class JsonSchemaType {
  APPLICATION
}
