package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2

import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.converter.StringListConverter
import java.time.OffsetDateTime
import java.util.UUID

enum class Cas2v2UserType(val authSource: String) {
  DELIUS("delius"),
  NOMIS("nomis"),
  EXTERNAL("auth"),
}

sealed interface User

sealed interface Cas2v2User : User {
  val name: String
  val email: String?
  val userType: Cas2v2UserType
}

@Repository
interface Cas2v2UserRepository : JpaRepository<Cas2v2UserEntity, UUID> {
  fun findByUsername(username: String): Cas2v2UserEntity?
  fun findByUsernameAndUserType(username: String, type: Cas2v2UserType): Cas2v2UserEntity?
}

@Entity
@Table(name = "cas_2_v2_users")
data class Cas2v2UserEntity(
  @Id
  val id: UUID,
  val username: String,

  // Cas2v2User interface implementation
  override var email: String?,
  override var name: String,

  @Enumerated(EnumType.STRING)
  override var userType: Cas2v2UserType,

  // Nomis specific fields that are only expected to have values if the
  // accountType is Cas2v2UserType.NOMIS
  var nomisStaffId: Long?,
  var activeNomisCaseloadId: String? = null,

  // Delius specific fields that are only expected to have values if the
  // accountType is Cas2v2UserType.DELIUS
  @Convert(converter = StringListConverter::class)
  var deliusTeamCodes: List<String>?,
  var deliusStaffCode: String?,

  var isEnabled: Boolean,
  var isActive: Boolean,

  @CreationTimestamp
  private val createdAt: OffsetDateTime? = null,

  @OneToMany(mappedBy = "createdByUser")
  val applications: MutableList<Cas2v2ApplicationEntity> = mutableListOf(),
) : Cas2v2User {
  override fun toString() = "CAS2 $userType bail user $id"

  fun staffIdentifier() = when (userType) {
    Cas2v2UserType.NOMIS -> nomisStaffId?.toString() ?: ""
    Cas2v2UserType.DELIUS -> deliusStaffCode ?: ""
    Cas2v2UserType.EXTERNAL -> ""
  }

  fun isExternal() = userType == Cas2v2UserType.EXTERNAL
}
