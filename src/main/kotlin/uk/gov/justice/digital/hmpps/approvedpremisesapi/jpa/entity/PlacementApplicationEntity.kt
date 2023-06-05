package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Repository
interface PlacementApplicationRepository : JpaRepository<PlacementApplicationEntity, UUID> {
  fun findAllByReallocatedAtNullAndDecisionNull(): List<PlacementApplicationEntity>

  @Query("SELECT a FROM PlacementApplicationEntity a WHERE a.application.id = :id")
  fun findByApplicationId(id: UUID): PlacementApplicationEntity?

  fun findByApplication_IdAndReallocatedAtNull(id: UUID): PlacementApplicationEntity?
}

@Entity
@Table(name = "placement_applications")
data class PlacementApplicationEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApprovedPremisesApplicationEntity,

  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdByUser: UserEntity,

  @ManyToOne
  @JoinColumn(name = "schema_version")
  var schemaVersion: JsonSchemaEntity,

  @Transient
  var schemaUpToDate: Boolean,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var data: String?,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var document: String?,

  val createdAt: OffsetDateTime,

  var submittedAt: OffsetDateTime?,

  @ManyToOne
  @JoinColumn(name = "allocated_to_user_id")
  var allocatedToUser: UserEntity?,

  var allocatedAt: OffsetDateTime?,
  var reallocatedAt: OffsetDateTime?,

  @Enumerated(value = EnumType.STRING)
  var decision: PlacementApplicationDecision?,

  var placementType: PlacementType?,

  @OneToMany(mappedBy = "placementApplication")
  var placementDates: MutableList<PlacementDateEntity>,
)

enum class PlacementType {
  ROTL,
  RELEASE_FOLLOWING_DECISION,
  ADDITIONAL_PLACEMENT,
}

enum class PlacementApplicationDecision {
  ACCEPTED,
  REJECTED,
}
