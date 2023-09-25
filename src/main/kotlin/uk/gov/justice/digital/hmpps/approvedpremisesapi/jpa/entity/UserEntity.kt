package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.util.Objects
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
interface UserRepository : JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
  @Query(
    """
    SELECT u.* FROM users u WHERE u.name ILIKE '%' || :str || '%' AND u.is_active IS TRUE
  """,
    nativeQuery = true,
  )
  fun findByNameContainingIgnoreCase(str: String): List<UserEntity>

  fun findByDeliusUsername(deliusUsername: String): UserEntity?

  @Query(
    """
    SELECT u.*, ura.*, uqa2.*,
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
    FROM "users"  u
	    LEFT JOIN user_role_assignments ura ON ura.user_id = u.id 
	    LEFT JOIN user_qualification_assignments uqa2 ON uqa2.user_id = u.id 
    WHERE ura.role = 'CAS1_ASSESSOR' AND 
        u.is_active = true AND
        (
            :qualifiedUserRequired = 1
            OR 
            (
                SELECT
                  COUNT(*)
                FROM
                  user_qualification_assignments uqa
                WHERE
                  uqa.qualification NOT IN ('WOMENS', 'PIPE', 'ESAP', 'EMERGENCY')
                  AND uqa.user_id = u.id
            ) > 0
        ) AND
        (SELECT COUNT(1) FROM user_qualification_assignments uqa WHERE uqa.user_id = u.id AND uqa.qualification IN (:requiredQualifications)) = :totalRequiredQualifications AND 
        u.id NOT IN (
            SELECT u.id FROM users u
            LEFT JOIN user_role_assignments ura ON ura.user_id = u.id 
            WHERE ura.role = 'CAS1_EXCLUDED_FROM_ASSESS_ALLOCATION'
        )
    ORDER BY score ASC
    LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findQualifiedAssessorWithLeastPendingOrCompletedInLastWeekAssessments(requiredQualifications: List<String>, totalRequiredQualifications: Long, qualifiedUserRequired: Int): UserEntity?

  @Query(
    """
    SELECT u.*, ura.*, uqa2.*,
    RANK() OVER (
      ORDER BY
        (
          SELECT COUNT(1)
            FROM placement_applications pa
            WHERE pa.allocated_to_user_id = u.id
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
    FROM "users"  u
	    LEFT JOIN user_role_assignments ura ON ura.user_id = u.id 
	    LEFT JOIN user_qualification_assignments uqa2 ON uqa2.user_id = u.id 
    WHERE ura.role = 'CAS1_MATCHER' AND 
        u.is_active = true AND
        (SELECT COUNT(1) FROM user_qualification_assignments uqa WHERE uqa.user_id = u.id AND uqa.qualification IN (:requiredQualifications)) = :totalRequiredQualifications AND 
        u.id NOT IN (
            SELECT u.id FROM users u
            LEFT JOIN user_role_assignments ura ON ura.user_id = u.id 
            WHERE ura.role = 'CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION'
        )
    ORDER BY score ASC 
    LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementApplications(requiredQualifications: List<String>, totalRequiredQualifications: Long): UserEntity?

  @Query(
    """
    SELECT u.*, ura.*, uqa2.*,
    RANK() OVER (
      ORDER BY
        (
          SELECT COUNT(1)
            FROM placement_requests pr
            WHERE pr.allocated_to_user_id = u.id
            AND 
              (
                pr.booking_id IS NULL
                OR (
                  pr.booking_id IS NOT NULL
                    AND pr.created_at BETWEEN 
                      (CURRENT_TIMESTAMP - interval '1 week') AND
                  		CURRENT_TIMESTAMP
                )
            )
        ) ASC
     ) as score
    FROM "users"  u
	    LEFT JOIN user_role_assignments ura ON ura.user_id = u.id 
	    LEFT JOIN user_qualification_assignments uqa2 ON uqa2.user_id = u.id 
    WHERE ura.role = 'CAS1_MATCHER' AND 
        u.is_active = true AND
        (SELECT COUNT(1) FROM user_qualification_assignments uqa WHERE uqa.user_id = u.id AND uqa.qualification IN (:requiredQualifications)) = :totalRequiredQualifications AND 
        u.id NOT IN (
            SELECT u.id FROM users u
            LEFT JOIN user_role_assignments ura ON ura.user_id = u.id 
            WHERE ura.role = 'CAS1_EXCLUDED_FROM_MATCH_ALLOCATION'
        )
    ORDER BY score ASC 
    LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementRequests(requiredQualifications: List<String>, totalRequiredQualifications: Long): UserEntity?
}

@Entity
@Table(name = "users")
data class UserEntity(
  @Id
  val id: UUID,
  var name: String,
  val deliusUsername: String,
  var deliusStaffCode: String,
  var deliusStaffIdentifier: Long,
  var email: String?,
  var telephoneNumber: String?,
  var isActive: Boolean,
  @OneToMany(mappedBy = "createdByUser")
  val applications: MutableList<ApplicationEntity>,
  @OneToMany(mappedBy = "user")
  var roles: MutableList<UserRoleAssignmentEntity>,
  @OneToMany(mappedBy = "user")
  val qualifications: MutableList<UserQualificationAssignmentEntity>,
  @ManyToOne
  val probationRegion: ProbationRegionEntity,
) {
  fun hasRole(userRole: UserRole) = roles.any { it.role == userRole }
  fun hasAnyRole(vararg userRoles: UserRole) = userRoles.any(::hasRole)
  fun hasQualification(userQualification: UserQualification) = qualifications.any { it.qualification === userQualification }
  fun hasAllQualifications(requiredQualifications: List<UserQualification>) = requiredQualifications.all(::hasQualification)

  override fun toString() = "User $id"
}

@Repository
interface UserRoleAssignmentRepository : JpaRepository<UserRoleAssignmentEntity, UUID>

@Entity
@Table(name = "user_role_assignments")
data class UserRoleAssignmentEntity(
  @Id
  val id: UUID,
  @ManyToOne
  @JoinColumn(name = "user_id")
  val user: UserEntity,
  @Enumerated(value = EnumType.STRING)
  val role: UserRole,
) {
  override fun hashCode() = Objects.hash(id, role)
}

enum class UserRole(val service: ServiceName) {
  CAS1_ASSESSOR(ServiceName.approvedPremises),
  CAS1_MATCHER(ServiceName.approvedPremises),
  CAS1_MANAGER(ServiceName.approvedPremises),
  CAS1_WORKFLOW_MANAGER(ServiceName.approvedPremises),
  CAS1_APPLICANT(ServiceName.approvedPremises),
  CAS1_ADMIN(ServiceName.approvedPremises),
  CAS1_REPORT_VIEWER(ServiceName.approvedPremises),
  CAS1_EXCLUDED_FROM_ASSESS_ALLOCATION(ServiceName.approvedPremises),
  CAS1_EXCLUDED_FROM_MATCH_ALLOCATION(ServiceName.approvedPremises),
  CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION(ServiceName.approvedPremises),
  CAS3_ASSESSOR(ServiceName.temporaryAccommodation),
  CAS3_REFERRER(ServiceName.temporaryAccommodation),
  ;

  companion object {
    fun getAllRolesForService(service: ServiceName) = UserRole.values().filter { it.service == service }
  }
}

@Repository
interface UserQualificationAssignmentRepository : JpaRepository<UserQualificationAssignmentEntity, UUID>

@Entity
@Table(name = "user_qualification_assignments")
data class UserQualificationAssignmentEntity(
  @Id
  val id: UUID,
  @ManyToOne
  @JoinColumn(name = "user_id")
  val user: UserEntity,
  @Enumerated(value = EnumType.STRING)
  val qualification: UserQualification,
)

enum class UserQualification {
  WOMENS,
  PIPE,
  LAO,
  ESAP,
  EMERGENCY,
}
