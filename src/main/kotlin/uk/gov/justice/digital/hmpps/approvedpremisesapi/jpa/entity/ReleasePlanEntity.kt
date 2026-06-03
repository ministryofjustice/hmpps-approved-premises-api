package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.javers.core.metamodel.annotation.DiffIgnore
import org.javers.core.metamodel.annotation.Entity as JaversEntity
import org.javers.core.metamodel.annotation.TypeName
import org.javers.spring.annotation.JaversSpringDataAuditable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalTime
import java.util.UUID

@Repository
@JaversSpringDataAuditable
interface ReleasePlanRepository : JpaRepository<ReleasePlanEntity, UUID> {

  fun getBySpaceBooking(spaceBooking: Cas1SpaceBookingEntity): List<ReleasePlanEntity>?
}

@Entity
@JaversEntity
@TypeName("ReleasePlan")
@Table(name = "release_plan")
class ReleasePlanEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @DiffIgnore
  val spaceBooking: Cas1SpaceBookingEntity,

  var expectedReleaseTime: LocalTime?,

  var expectedArrivalTime: LocalTime?,

  var description: String?,

  var otherInformation: String?,

  @OneToMany(
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
  )
  @JoinColumn(name = "release_plan_id", nullable = false)
  var releaseActions: MutableList<ReleaseActionEntity> = mutableListOf(),
)
