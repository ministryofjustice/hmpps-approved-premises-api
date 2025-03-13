package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface NomisUserRepository : JpaRepository<NomisUserEntity, UUID> {
  fun findByNomisUsername(nomisUserName: String): NomisUserEntity?
  fun findByNomisStaffId(nomisStaffId: Long): NomisUserEntity?
}

@Entity
@Table(name = "nomis_users")
data class NomisUserEntity(
  @Id
  val id: UUID,
  val nomisUsername: String,
  var nomisStaffId: Long,
  override var name: String,
  var accountType: String,
  var isEnabled: Boolean,
  var isActive: Boolean,
  override var email: String?,
  var activeCaseloadId: String? = null,

  @CreationTimestamp
  private val createdAt: OffsetDateTime? = null,

  @OneToMany(mappedBy = "createdByUser")
  val applications: MutableList<Cas2ApplicationEntity> = mutableListOf(),
) : Cas2User {
  override fun toString() = "Nomis user $id"
}
