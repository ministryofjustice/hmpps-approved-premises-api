package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id

import javax.persistence.OneToMany
import javax.persistence.Table

@Repository
interface NomisUserRepository : JpaRepository<NomisUserEntity, UUID>,
  JpaSpecificationExecutor<UserEntity> {

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
  var enabled: Boolean,
  var active: Boolean,
  var email: String?,
  @OneToMany(mappedBy = "createdByNomisUser")
  val applications: MutableList<Cas2ApplicationEntity>,
) {
  override fun toString() = "Nomis user $id"
}
