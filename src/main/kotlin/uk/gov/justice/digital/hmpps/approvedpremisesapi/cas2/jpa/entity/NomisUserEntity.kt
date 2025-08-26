package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface NomisUserRepository : JpaRepository<NomisUserEntity, UUID> {
  fun findByNomisUsername(nomisUserName: String): NomisUserEntity?
}

@Entity
@Table(name = "nomis_users")
data class NomisUserEntity(
  @Id
  val id: UUID,
  val nomisUsername: String,
  var nomisStaffId: Long,
  var name: String,
  var accountType: String,
  var isEnabled: Boolean,
  var isActive: Boolean,
  var email: String?,
  var activeCaseloadId: String? = null,

  @CreationTimestamp
  private val createdAt: OffsetDateTime? = null,
// TODO removed as no longer needed as isn't in app table
//  @OneToMany(mappedBy = "createdByUser")
//  val applications: MutableList<Cas2ApplicationEntity> = mutableListOf(),
) {
  override fun toString() = "Nomis user $id"
}
