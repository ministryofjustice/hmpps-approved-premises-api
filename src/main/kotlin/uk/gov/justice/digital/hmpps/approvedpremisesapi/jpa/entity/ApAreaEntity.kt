package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface ApAreaRepository : JpaRepository<ApAreaEntity, UUID> {
  fun findByName(name: String): ApAreaEntity?
  fun findByIdentifier(name: String): ApAreaEntity?

  @Modifying
  @Query("UPDATE ApAreaEntity set emailAddress = :emailAddress where id = :id")
  fun updateEmailAddress(id: UUID, emailAddress: String)
}

@Entity
@Table(name = "ap_areas")
data class ApAreaEntity(
  @Id
  val id: UUID,
  val name: String,
  val identifier: String,
  val emailAddress: String?,
  val notifyReplyToEmailId: String?,
)
