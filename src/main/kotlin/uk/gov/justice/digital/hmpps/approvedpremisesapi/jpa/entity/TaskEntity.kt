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
    private const val ASSESSMENT_QUERY = """
       SELECT
        cast(assessment.id as TEXT) AS id,
        assessment.created_at AS created_at,
        assessment.due_at AS due_at,
        'ASSESSMENT' AS type
      FROM
        assessments assessment
        INNER JOIN approved_premises_applications application ON assessment.application_id = application.id
        LEFT JOIN ap_areas area ON area.id = application.ap_area_id
      WHERE
        'ASSESSMENT' in :taskTypes AND
        assessment.is_withdrawn IS NOT TRUE
        AND assessment.reallocated_at IS NULL
        AND assessment.submitted_at IS NULL
        AND (
          (:isAllocated IS NULL) OR 
          (
            (:isAllocated = true and assessment.allocated_to_user_id IS NOT NULL) OR
            (:isAllocated = false and assessment.allocated_to_user_id IS NULL)
          )
        ) AND (
          (cast(:apAreaId as uuid) IS NULL) OR
          (area.id = :apAreaId)
        ) AND (
          (cast(:allocatedToUserId as uuid) IS NULL) OR
          assessment.allocated_to_user_id = :allocatedToUserId
        )
    """

    private const val PLACEMENT_APPLICATION_QUERY = """
       SELECT
        cast(placement_application.id as TEXT) AS id,
        placement_application.created_at AS created_at,
        placement_application.due_at AS due_at,
        'PLACEMENT_APPLICATION' AS type
      from
        placement_applications placement_application
        INNER JOIN approved_premises_applications application ON placement_application.application_id = application.id
        LEFT JOIN ap_areas area ON area.id = application.ap_area_id
      WHERE
        'PLACEMENT_APPLICATION' in :taskTypes AND
        placement_application.submitted_at IS NOT NULL
        AND placement_application.reallocated_at IS NULL
        AND placement_application.decision IS NULL
        AND (
          (:isAllocated IS NULL) OR 
          (
              (:isAllocated = true AND placement_application.allocated_to_user_id IS NOT NULL) OR
              (:isAllocated = false AND placement_application.allocated_to_user_id IS NULL)
          )
        ) AND (
          (cast(:apAreaId as uuid) IS NULL) OR
          (area.id = :apAreaId)
        ) AND (
          (cast(:allocatedToUserId as uuid) IS NULL) OR
          placement_application.allocated_to_user_id = :allocatedToUserId
        )
    """

    private const val PLACEMENT_REQUEST_QUERY = """
      SELECT
        cast(placement_request.id as TEXT) AS id,
        placement_request.created_at AS created_at,
        placement_request.due_at AS due_at,
        'PLACEMENT_REQUEST' AS type
      FROM
        placement_requests placement_request
        INNER JOIN approved_premises_applications application ON placement_request.application_id = application.id
        LEFT JOIN booking_not_mades booking_not_made ON booking_not_made.placement_request_id = placement_request.id
        LEFT JOIN ap_areas area ON area.id = application.ap_area_id
      WHERE
        'PLACEMENT_REQUEST' IN :taskTypes AND
        placement_request.booking_id IS NULL
        AND placement_request.reallocated_at IS NULL
        AND placement_request.is_withdrawn IS FALSE
        AND booking_not_made.id IS NULL
        AND (
          (:isAllocated IS NULL) OR 
          (
              (:isAllocated = true AND placement_request.allocated_to_user_id IS NOT NULL) OR
              (:isAllocated = false AND placement_request.allocated_to_user_id IS NULL)
          )
        ) AND (
          (cast(:apAreaId as uuid) IS NULL) OR
          (area.id = :apAreaId)
        ) AND (
          (cast(:allocatedToUserId as uuid) IS NULL) OR
          placement_request.allocated_to_user_id = :allocatedToUserId
        )
        """

    private const val ALL_QUERY = """
      $ASSESSMENT_QUERY
      UNION ALL
      $PLACEMENT_APPLICATION_QUERY
      UNION ALL
      $PLACEMENT_REQUEST_QUERY
    """
  }

  @Query(
    ALL_QUERY,
    countQuery = "SELECT COUNT(1) FROM ($ALL_QUERY) as count",
    nativeQuery = true,
  )
  fun getAll(
    isAllocated: Boolean?,
    apAreaId: UUID?,
    taskTypes: List<String>,
    allocatedToUserId: UUID?,
    pageable: Pageable?,
  ): Page<Task>

  @Query(
    PLACEMENT_REQUEST_QUERY,
    countQuery = "SELECT COUNT(1) FROM ($PLACEMENT_REQUEST_QUERY) as count",
    nativeQuery = true,
  )
  fun getAllPlacementRequests(
    isAllocated: Boolean?,
    apAreaId: UUID?,
    taskTypes: List<String>,
    allocatedToUserId: UUID?,
    pageable: Pageable?,
  ): Page<Task>

  @Query(
    PLACEMENT_APPLICATION_QUERY,
    countQuery = "SELECT COUNT(1) FROM ($PLACEMENT_APPLICATION_QUERY) as count",
    nativeQuery = true,
  )
  fun getAllPlacementApplications(
    isAllocated: Boolean?,
    apAreaId: UUID?,
    taskTypes: List<String>,
    allocatedToUserId: UUID?,
    pageable: Pageable?,
  ): Page<Task>

  @Query(
    ASSESSMENT_QUERY,
    countQuery = "SELECT COUNT(1) FROM ($ASSESSMENT_QUERY) as count",
    nativeQuery = true,
  )
  fun getAllAssessments(
    isAllocated: Boolean?,
    apAreaId: UUID?,
    taskTypes: List<String>,
    allocatedToUserId: UUID?,
    pageable: Pageable?,
  ): Page<Task>
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
