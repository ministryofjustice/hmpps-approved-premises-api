package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas2ApplicationNoteRepository : JpaRepository<Cas2ApplicationNoteEntity, UUID>

@Entity
@Table(name = "cas_2_application_notes")
data class Cas2ApplicationNoteEntity(
  @Id
  val id: UUID,

  @Transient
  final val createdByUser: Cas2User,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: Cas2ApplicationEntity,

  val createdAt: OffsetDateTime,

  var body: String,
) {

  @ManyToOne
  @JoinColumn(name = "created_by_nomis_user_id")
  var createdByNomisUser: NomisUserEntity? = null

  @ManyToOne
  @JoinColumn(name = "created_by_external_user_id")
  var createdByExternalUser: ExternalUserEntity? = null

  init {
    when (this.createdByUser) {
      is NomisUserEntity -> this.createdByNomisUser = this.createdByUser
      is ExternalUserEntity -> this.createdByExternalUser = this.createdByUser
    }
  }

  fun getUser(): Cas2User {
    return if (this.createdByNomisUser != null) {
      this.createdByNomisUser!!
    } else {
      this.createdByExternalUser!!
    }
  }

  override fun toString() = "Cas2ApplicationNoteEntity: $id"
}
