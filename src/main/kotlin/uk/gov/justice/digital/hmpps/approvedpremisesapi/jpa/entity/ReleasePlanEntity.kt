package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.history.RevisionRepository
import org.springframework.stereotype.Repository
import java.time.LocalTime
import java.util.UUID

@Repository
interface ReleasePlanRepository :
  JpaRepository<ReleasePlanEntity, UUID>,
  RevisionRepository<ReleasePlanEntity, UUID, Int> {

  fun getBySpaceBooking(spaceBooking: Cas1SpaceBookingEntity): List<ReleasePlanEntity>?
}

@Entity
@Table(name = "release_plan")
@Audited
class ReleasePlanEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  val spaceBooking: Cas1SpaceBookingEntity,

  var expectedReleaseTime: LocalTime?,

  var expectedArrivalTime: LocalTime?,

  var description: String?,

  var otherInformation: String?,

  @OneToMany(
    mappedBy = "releasePlan",
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
  )
  var releaseActions: MutableList<ReleaseActionEntity> = mutableListOf(),
)
