package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.history.RevisionRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ReleaseActionRepository :
  JpaRepository<ReleaseActionEntity, UUID>,
  RevisionRepository<ReleaseActionEntity, UUID, Int>

@Entity
@Table(name = "release_action")
@Audited
class ReleaseActionEntity(
  @Id
  val id: UUID,

  var description: String,
  var actionCadence: String,
  var otherInformation: String? = null,

  @ManyToOne
  @JoinColumn(name = "release_plan_id")
  var releasePlan: ReleasePlanEntity,
)
