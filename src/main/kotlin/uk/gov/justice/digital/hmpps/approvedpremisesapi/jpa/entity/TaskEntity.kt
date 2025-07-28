package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface TaskRepository : JpaRepository<Task, UUID> {

  companion object {
    private const val QUALIFICATION_QUERY = """
      (:requiredQualification IS NULL) OR
      (
        CASE
          WHEN :requiredQualification = 'womens' THEN apa.is_womens_application = true
          WHEN :requiredQualification = 'pipe' THEN apa.ap_type = 'PIPE'
          WHEN :requiredQualification = 'emergency' THEN (apa.notice_type = 'emergency' OR apa.notice_type = 'shortNotice')
          WHEN :requiredQualification = 'esap' THEN apa.ap_type = 'ESAP'
          WHEN :requiredQualification = 'recovery_focused' THEN apa.ap_type = 'RFAP'
          WHEN :requiredQualification = 'mental_health_specialist' THEN (apa.ap_type = 'MHAP_ST_JOSEPHS' OR apa.ap_type = 'MHAP_ELLIOTT_HOUSE')
          ELSE true
        END
      )
    """

    private const val CRN_OR_NAME_QUERY = """
      :crnOrName IS NULL OR 
      (
          application.crn = UPPER(:crnOrName)
          OR
          apa.name LIKE UPPER('%' || :crnOrName || '%')
      )
    """

    private const val ASSESSMENT_QUERY = """
       SELECT
        assessment.id AS id,
        assessment.created_at AS created_at,
        assessment.due_at AS due_at,
        'ASSESSMENT' AS type,
        apa.name as person,
        u.name as allocated_to,
        assessment.submitted_at as completed_at,
        assessment.decision as decision,
        apa.arrival_date AS sorting_date,
        apa.ap_type AS ap_type
      FROM
        assessments assessment
        INNER JOIN applications application ON assessment.application_id = application.id
        INNER JOIN approved_premises_applications apa ON application.id = apa.id
        LEFT JOIN ap_areas area ON area.id = apa.ap_area_id
        LEFT JOIN users u ON u.id = assessment.allocated_to_user_id
      WHERE
        'ASSESSMENT' in :taskTypes AND
        assessment.is_withdrawn IS NOT TRUE
        AND assessment.reallocated_at IS NULL
        AND (
          (:completed = true AND assessment.submitted_at IS NOT NULL) OR (:completed = false AND assessment.submitted_at IS NULL)
        )
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
          (cast(:cruManagementAreaId as uuid) IS NULL) OR
          (apa.cas1_cru_management_area_id = :cruManagementAreaId)          
        ) AND (
          (cast(:allocatedToUserId as uuid) IS NULL) OR
          assessment.allocated_to_user_id = :allocatedToUserId
        ) AND (
          $QUALIFICATION_QUERY
        ) AND (
          $CRN_OR_NAME_QUERY
        )
    """

    private const val PLACEMENT_APPLICATION_QUERY = """
       SELECT
        placement_application.id AS id,
        placement_application.created_at AS created_at,
        placement_application.due_at AS due_at,
        'PLACEMENT_APPLICATION' AS type,
        apa.name as person,
        u.name as allocated_to,
        placement_application.decision_made_at as completed_at,
        placement_application.decision as decision,
        placement_application.expected_arrival AS sorting_date,
        apa.ap_type AS ap_type
      from
        placement_applications placement_application
        INNER JOIN applications application ON placement_application.application_id = application.id
        INNER JOIN approved_premises_applications apa ON application.id = apa.id
        LEFT JOIN ap_areas area ON area.id = apa.ap_area_id
        LEFT JOIN users u ON u.id = placement_application.allocated_to_user_id
      WHERE
        'PLACEMENT_APPLICATION' in :taskTypes 
        AND placement_application.automatic IS FALSE 
        AND placement_application.submitted_at IS NOT NULL
        AND placement_application.reallocated_at IS NULL
        AND placement_application.is_withdrawn is FALSE
        AND (
          (:completed = true AND placement_application.decision IS NOT NULL) OR (:completed = false AND placement_application.decision IS NULL)
        )
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
          (cast(:cruManagementAreaId as uuid) IS NULL) OR
          (apa.cas1_cru_management_area_id = :cruManagementAreaId)          
        ) AND (
          (cast(:allocatedToUserId as uuid) IS NULL) OR
          placement_application.allocated_to_user_id = :allocatedToUserId
        ) AND (
          $QUALIFICATION_QUERY
        ) AND (
          $CRN_OR_NAME_QUERY
        )
    """

    private const val ALL_QUERY = """
      $ASSESSMENT_QUERY
      UNION ALL
      $PLACEMENT_APPLICATION_QUERY
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
    cruManagementAreaId: UUID?,
    taskTypes: List<String>,
    allocatedToUserId: UUID?,
    requiredQualification: String?,
    crnOrName: String?,
    completed: Boolean,
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
    cruManagementAreaId: UUID?,
    taskTypes: List<String>,
    allocatedToUserId: UUID?,
    requiredQualification: String?,
    crnOrName: String?,
    completed: Boolean,
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
    cruManagementAreaId: UUID?,
    taskTypes: List<String>,
    allocatedToUserId: UUID?,
    requiredQualification: String?,
    crnOrName: String?,
    completed: Boolean,
    pageable: Pageable?,
  ): Page<Task>

  @Query(
    """
    SELECT u.*,
     RANK() OVER (
      ORDER BY
        (
          SELECT COUNT(1)
            FROM assessments a
            WHERE a.allocated_to_user_id = u.id
            AND 
              (
                a.submitted_at IS NULL
                OR (
                  a.submitted_at IS NOT NULL
                    AND a.created_at BETWEEN 
                      (CURRENT_TIMESTAMP - interval '1 week') AND
                  		CURRENT_TIMESTAMP
                )
            )
        ) ASC
     ) as score
    FROM "users" u
    WHERE u.id IN (:userIds)
    ORDER BY score ASC
    LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findUserWithLeastAssessmentsPendingOrCompletedInLastWeek(userIds: List<UUID>): UserEntity?

  @Query(
    """
    SELECT u.*,
    RANK() OVER (
      ORDER BY
        (
          SELECT COUNT(1)
            FROM placement_applications pa
            WHERE pa.allocated_to_user_id = u.id AND
            pa.automatic IS FALSE
            AND 
              (
                pa.submitted_at IS NULL
                OR (
                  pa.submitted_at IS NOT NULL
                    AND pa.created_at BETWEEN 
                      (CURRENT_TIMESTAMP - interval '1 week') AND
                  		CURRENT_TIMESTAMP
                )
            )
        ) ASC
     ) as score
    FROM "users" u
    WHERE u.id IN (:userIds)
    ORDER BY score ASC 
    LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findUserWithLeastPlacementApplicationsPendingOrCompletedInLastWeek(userIds: List<UUID>): UserEntity?

  @Query(
    """
    SELECT
      CAST(u.id as TEXT) as userId,
      (
        SELECT
          count(*)
        from
          assessments a
        where
          a.allocated_to_user_id = u.id
          and a.reallocated_at is null
          and a.submitted_at is null
          and a.is_withdrawn != true
      ) as pendingAssessments,
      (
        SELECT
          count(*)
        from
          assessments a
        where
          a.allocated_to_user_id = u.id
          and a.reallocated_at is null
          and a.submitted_at > current_date - interval '7' day
      ) as completedAssessmentsInTheLastSevenDays,
      (
        SELECT
          count(*)
        from
          assessments a
        where
          a.allocated_to_user_id = u.id
          and a.reallocated_at is null
          and a.submitted_at > current_date - interval '30' day
      ) as completedAssessmentsInTheLastThirtyDays,
      (
        SELECT
          count(*)
        from
          placement_applications placement_application
        where
          placement_application.allocated_to_user_id = u.id
          and placement_application.automatic is false
          and placement_application.reallocated_at is null
          and placement_application.is_withdrawn != true
          and placement_application.decision_made_at is null
      ) as pendingPlacementApplications,
      (
        SELECT
          count(*)
        from
          placement_applications placement_application
        where
          placement_application.allocated_to_user_id = u.id
          and placement_application.automatic is false
          and placement_application.reallocated_at is null
          and placement_application.decision_made_at > current_date - interval '7' day
      ) as completedPlacementApplicationsInTheLastSevenDays,
      (
        SELECT
          count(*)
        from
          placement_applications placement_application
        where
          placement_application.allocated_to_user_id = u.id
          and placement_application.automatic is false
          and placement_application.reallocated_at is null
          and placement_application.decision_made_at > current_date - interval '30' day
      ) as completedPlacementApplicationsInTheLastThirtyDays
    FROM
      users u
    WHERE
      u.id IN (:userIds)
    """,
    nativeQuery = true,
  )
  fun findWorkloadForUserIds(userIds: List<UUID>): List<UserWorkload>
}

@Entity
data class Task(
  @Id
  val id: UUID,
  val createdAt: LocalDateTime,
  @Enumerated(EnumType.STRING)
  val type: TaskEntityType,
  val person: String,
  val allocatedTo: String?,
  val completedAt: LocalDateTime?,
  val decision: String?,
)

enum class TaskEntityType {
  ASSESSMENT,
  PLACEMENT_APPLICATION,
}
