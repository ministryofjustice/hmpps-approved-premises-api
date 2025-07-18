package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity

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

enum class Cas2UserType(val authSource: String) {
  DELIUS("delius"),
  NOMIS("nomis"),
  EXTERNAL("auth"),
  ;

  companion object {
    fun fromString(authSource: String): Cas2UserType = entries.first { it.authSource == authSource }
  }
}

@Repository
interface Cas2UserRepository : JpaRepository<Cas2UserEntity, UUID> {
  fun findByUsername(username: String): Cas2UserEntity?
  fun findByUsernameAndUserType(username: String, type: Cas2UserType): Cas2UserEntity?
}

@Entity
@Table(name = "cas_2_users")
data class Cas2UserEntity(
  @Id
  val id: UUID,
  val username: String,

  // Cas2v2User interface implementation
  var email: String?,
  var name: String,

  @Enumerated(EnumType.STRING)
  var userType: Cas2UserType,

  // Nomis specific fields that are only expected to have values if the
  // accountType is Cas2v2UserType.NOMIS
  var nomisStaffId: Long? = null,
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

  @OneToMany(mappedBy = "createdByCas2User")
  val applications: MutableList<Cas2ApplicationEntity> = mutableListOf(),
) {
  override fun toString() = "CAS2 user $id"

  fun staffIdentifier() = when (userType) {
    Cas2UserType.NOMIS -> nomisStaffId?.toString() ?: error("Couldn't resolve nomis ID for user $id")
    Cas2UserType.DELIUS -> deliusStaffCode ?: error("Couldn't resolve delius ID for user $id")
    Cas2UserType.EXTERNAL -> "" // BAIL-WIP - this currently needs to be not null - refactor them when we add the user type to cas2 user type
  }

  fun isExternal() = userType == Cas2UserType.EXTERNAL
}
