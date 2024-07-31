package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface ExternalUserRepository : JpaRepository<ExternalUserEntity, UUID> {
  fun findByUsername(userName: String): ExternalUserEntity?
}

@Entity
@Table(name = "external_users")
data class ExternalUserEntity(
  @Id
  val id: UUID,
  val username: String,
  var isEnabled: Boolean,
  var origin: String,
  override var name: String,
  override var email: String,

  @CreationTimestamp
  private val createdAt: OffsetDateTime? = null,
) : Cas2User {
  override fun toString() = "External user $id"
}
