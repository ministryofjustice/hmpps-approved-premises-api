package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id

@Repository
interface TaskRepository : JpaRepository<Task, UUID> {
  companion object {
    private const val ALLOCATABLE_QUERY = """
      SELECT
        cast(assessment.id as TEXT) as id,
        assessment.created_at as created_at,
        'ASSESSMENT' as type
      from
        assessments assessment
        inner join approved_premises_applications application on assessment.application_id = application.id
        left join ap_areas area on area.id = application.ap_area_id
      where
        'ASSESSMENT' in :taskTypes AND
        assessment.is_withdrawn is not true
        and assessment.reallocated_at is null
        and assessment.submitted_at is null
        and (
          (:isAllocated is null) OR 
          (
            (:isAllocated = true and assessment.allocated_to_user_id is not null) or
            (:isAllocated = false and assessment.allocated_to_user_id is null)
          )
        ) and (
          (cast(:apAreaId as uuid) is null) OR
          (area.id = :apAreaId)
        )
      UNION ALL
      SELECT
        cast(placement_application.id as TEXT) as id,
        placement_application.created_at as created_at,
        'PLACEMENT_APPLICATION' as type
      from
        placement_applications placement_application
        inner join approved_premises_applications application on placement_application.application_id = application.id
        left join ap_areas area on area.id = application.ap_area_id
      where
        'PLACEMENT_APPLICATION' in :taskTypes AND
        placement_application.submitted_at is not null
        and placement_application.reallocated_at is null
        and placement_application.decision is null
        and (
          (:isAllocated is null) OR 
          (
              (:isAllocated = true and placement_application.allocated_to_user_id is not null) or
              (:isAllocated = false and placement_application.allocated_to_user_id is null)
          )
        ) and (
          (cast(:apAreaId as uuid) is null) OR
          (area.id = :apAreaId)
        )
      UNION ALL
      SELECT
        cast(placement_request.id as TEXT) as id,
        placement_request.created_at as created_at,
        'PLACEMENT_REQUEST' as type
      from
        placement_requests placement_request
        inner join approved_premises_applications application on placement_request.application_id = application.id
        left join booking_not_mades booking_not_made on booking_not_made.placement_request_id = placement_request.id
        left join ap_areas area on area.id = application.ap_area_id
      where
        'PLACEMENT_REQUEST' in :taskTypes AND
        placement_request.booking_id IS NULL
        AND placement_request.reallocated_at IS NULL
        AND placement_request.is_withdrawn is false
        AND booking_not_made.id IS NULL
        AND (
          (:isAllocated is null) OR 
          (
              (:isAllocated = true and placement_request.allocated_to_user_id is not null) or
              (:isAllocated = false and placement_request.allocated_to_user_id is null)
          )
        ) and (
          (cast(:apAreaId as uuid) is null) OR
          (area.id = :apAreaId)
        )
    """
  }

  @Query(
    ALLOCATABLE_QUERY,
    countQuery = "SELECT COUNT(1) FROM ($ALLOCATABLE_QUERY) as count",
    nativeQuery = true,
  )
  fun getAllReallocatable(isAllocated: Boolean?, apAreaId: UUID?, taskTypes: List<String>, pageable: Pageable?): Page<Task>
}

@Entity
data class Task(
  @Id
  val id: UUID,
  val createdAt: LocalDateTime,
  @Enumerated(EnumType.STRING)
  val type: TaskEntityType,
)

enum class TaskEntityType {
  ASSESSMENT,
  PLACEMENT_APPLICATION,
  PLACEMENT_REQUEST,
}
