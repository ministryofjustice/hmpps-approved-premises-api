package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "release_plan")
@Audited
class ReleasePlanEntity(
  @Id
  val id: UUID,

  @ManyToOne
  val spaceBooking: Cas1SpaceBookingEntity,

  var expectedReleaseTime: LocalTime?,

  var expectedArrivalTime: LocalTime?,

  @OneToMany(
    mappedBy = "releasePlan",
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
  )
  var releaseActions: MutableList<ReleaseActionEntity> = mutableListOf(),
)
