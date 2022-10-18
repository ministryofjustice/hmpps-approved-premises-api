package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table

@Repository
interface UserRepository : JpaRepository<UserEntity, UUID> {
  fun findByDistinguishedName(distinguishedName: String): UserEntity?
}

@Entity
@Table(name = "users")
data class UserEntity(
  @Id
  val id: UUID,
  val name: String,
  val distinguishedName: String,
  var isActive: Boolean,
  @OneToMany(mappedBy = "createdByUser")
  val applications: MutableList<ApplicationEntity>
)
