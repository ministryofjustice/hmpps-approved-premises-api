package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface ExternalUserRepository : JpaRepository<ExternalUserEntity, UUID> {
  fun findByUsername(userName: String): ExternalUserEntity?

  @Query("SELECT n.id FROM ExternalUserEntity n")
  fun findExternalUserIds(): List<UUID>
}

@Entity
@Table(name = "external_users")
data class ExternalUserEntity(
  @Id
  val id: UUID,
  val username: String,
  var isEnabled: Boolean,
  var origin: String,
  var name: String,
  var email: String,

  val createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
  override fun toString() = "External user $id"
}
