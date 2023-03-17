package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Repository
interface PlacementRequestRepository : JpaRepository<PlacementRequestEntity, UUID> {
  fun findByApplication(application: ApplicationEntity): PlacementRequestEntity
}

@Entity
@Table(name = "placement_requests")
data class PlacementRequestEntity(
  @Id
  val id: UUID,
  val gender: Gender,
  val apType: ApType,
  val expectedArrival: LocalDate,
  val duration: Int,

  @ManyToOne
  @JoinColumn(name = "postcode_district_id")
  val postcodeDistrict: PostCodeDistrictEntity,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApplicationEntity,

  val radius: Int,

  @ManyToMany
  @JoinTable(
    name = "placement_request_essential_criteria",
    joinColumns = [JoinColumn(name = "placement_request_id")],
    inverseJoinColumns = [JoinColumn(name = "characteristic_id")],
  )
  val essentialCriteria: List<CharacteristicEntity>,

  @ManyToMany
  @JoinTable(
    name = "placement_request_desirable_criteria",
    joinColumns = [JoinColumn(name = "placement_request_id")],
    inverseJoinColumns = [JoinColumn(name = "characteristic_id")],
  )
  val desirableCriteria: List<CharacteristicEntity>,

  val mentalHealthSupport: Boolean,
  val createdAt: OffsetDateTime,

  @ManyToOne
  @JoinColumn(name = "booking_id")
  val booking: BookingEntity?,

  @ManyToOne
  @JoinColumn(name = "allocated_to_user_id")
  val allocatedToUser: UserEntity,
)
