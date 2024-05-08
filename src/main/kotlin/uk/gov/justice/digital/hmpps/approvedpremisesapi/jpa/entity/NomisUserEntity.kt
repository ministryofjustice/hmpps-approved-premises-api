package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table

@Repository
interface NomisUserRepository : JpaRepository<NomisUserEntity, UUID> {
  fun findByNomisUsername(nomisUserName: String): NomisUserEntity?

  @Modifying
  @Transactional
  @Query(
    value = """
          INSERT INTO nomis_user (id, name, nomis_username, nomis_staff_id, account_type, email, is_enabled, is_active, active_caseload_id)
          VALUES (:id, :name, :nomisUsername, :nomisStaffId, :accountType, :email, :isEnabled, :isActive, :activeCaseloadId)
          ON CONFLICT (nomis_username) DO UPDATE SET
          name = EXCLUDED.name,
          nomis_staff_id = EXCLUDED.nomis_staff_id,
          account_type = EXCLUDED.account_type,
          email = EXCLUDED.email,
          is_enabled = EXCLUDED.is_enabled,
          is_active = EXCLUDED.is_active,
          active_caseload_id = EXCLUDED.active_caseload_id
          RETURNING *
      """,
    nativeQuery = true,
  )
  fun saveOrUpdate(
    id: UUID,
    name: String,
    nomisUsername: String,
    nomisStaffId: Long,
    accountType: String,
    email: String?,
    isEnabled: Boolean,
    isActive: Boolean,
    activeCaseloadId: String?,
  ): NomisUserEntity
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
