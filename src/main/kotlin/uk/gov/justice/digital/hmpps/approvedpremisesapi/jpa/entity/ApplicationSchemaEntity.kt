package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface ApplicationSchemaRepository : JpaRepository<ApplicationSchemaEntity, UUID>

@Entity
@Table(name = "application_schemas")
data class ApplicationSchemaEntity(
  @Id
  val id: UUID,
  val addedAt: OffsetDateTime,
  val schema: String
)
