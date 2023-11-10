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
  fun findAllBySubmittedAtNotNullAndReallocatedAtNullAndDecisionNull(): List<PlacementApplicationEntity>

  @Query(
    """
    SELECT
      pa.id,
      pa.application_id,
      pa.created_by_user_id,
      pa.data,
      pa.document,
      pa.schema_version,
      pa.created_at,
      pa.submitted_at,
      pa.allocated_to_user_id,
      pa.allocated_at,
      pa.reallocated_at,
      pa.decision,
      pa.placement_type,
      app.*
    FROM
      placement_applications pa
    LEFT OUTER JOIN
      approved_premises_applications app ON pa.application_id = app.id
    WHERE
      pa.allocated_to_user_id = :userId
      AND pa.reallocated_at IS NULL
    """,
    nativeQuery = true,
  )
  fun findAllByAllocatedToUserIdAndReallocatedAtNull(userId: UUID): List<PlacementApplicationEntity>

  fun findAllByApplication(application: ApprovedPremisesApplicationEntity): List<PlacementApplicationEntity>
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
  WITHDRAW,
  WITHDRAWN_BY_PP,
}
