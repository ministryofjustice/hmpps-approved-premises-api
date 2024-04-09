package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface Cas3DutyToReferOutcomeRepository : JpaRepository<Cas3DutyToReferOutcomeEntity, UUID> {
  fun findByIsActiveTrue(): List<Cas3DutyToReferOutcomeEntity>
}

@Entity
@Table(name = "cas_3_duty_to_refer_outcome")
data class Cas3DutyToReferOutcomeEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
) {
  override fun toString() = "Cas3DutyToReferOutcomeEntity:$id"
}
