package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Repository
interface PlacementRequestRepository : JpaRepository<PlacementRequestEntity, UUID> {
  fun findByApplication(application: ApplicationEntity): PlacementRequestEntity?

  fun findAllByApplication(application: ApplicationEntity): List<PlacementRequestEntity>

  fun findAllByAllocatedToUser_IdAndReallocatedAtNullAndIsWithdrawnFalse(userId: UUID): List<PlacementRequestEntity>

  fun findAllByReallocatedAtNullAndBooking_IdNullAndIsWithdrawnFalse(): List<PlacementRequestEntity>

  @Query("SELECT pr FROM PlacementRequestEntity pr WHERE pr.isWithdrawn = FALSE AND pr.reallocatedAt IS NULL AND pr.isParole = :isParole AND (:crn IS NULL OR pr.application.crn = UPPER(:crn))")
  fun findNonWithdrawnNonReallocatedPlacementRequests(isParole: Boolean, crn: String?, pageable: Pageable?): Page<PlacementRequestEntity>
}

@Entity
@Table(name = "placement_requests")
data class PlacementRequestEntity(
  @Id
  val id: UUID,
  val expectedArrival: LocalDate,
  val duration: Int,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApprovedPremisesApplicationEntity,

  @ManyToOne
  @JoinColumn(name = "assessment_id")
  val assessment: AssessmentEntity,

  val createdAt: OffsetDateTime,

  val notes: String?,

  @ManyToOne
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity?,

  @ManyToOne
  @JoinColumn(name = "allocated_to_user_id")
  val allocatedToUser: UserEntity,

  @OneToMany(mappedBy = "placementRequest")
  var bookingNotMades: MutableList<BookingNotMadeEntity>,

  var reallocatedAt: OffsetDateTime?,

  @ManyToOne
  @JoinColumn(name = "placement_requirements_id")
  var placementRequirements: PlacementRequirementsEntity,

  var isParole: Boolean,
  var isWithdrawn: Boolean,
)
