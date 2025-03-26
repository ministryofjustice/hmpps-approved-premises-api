package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface Cas1ChangeRequestRejectionReasonRepository : JpaRepository<Cas1ChangeRequestRejectionReasonEntity, UUID> {
  fun findByChangeRequestTypeAndArchivedIsFalse(type: ChangeRequestType): List<Cas1ChangeRequestRejectionReasonEntity>

  fun findByIdAndArchivedIsFalse(id: UUID): Cas1ChangeRequestRejectionReasonEntity?
}

@Entity
@Table(name = "cas1_change_request_rejection_reasons")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Cas1ChangeRequestRejectionReasonEntity(
  @Id
  val id: UUID,
  val code: String,
  @Enumerated(EnumType.STRING)
  val changeRequestType: ChangeRequestType,
  val archived: Boolean,
)
