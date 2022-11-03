package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
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
interface UserRepository : JpaRepository<UserEntity, UUID> {
  fun findByDeliusUsername(deliusUsername: String): UserEntity?

  @Query(
    """
    SELECT u.*, ura.*, uqa2.* 
    FROM "users"  u
	    LEFT JOIN user_role_assignments ura ON ura.user_id = u.id 
	    LEFT JOIN user_qualification_assignments uqa2 ON uqa2.user_id = u.id 
    WHERE ura.role = 'ASSESSOR' AND 
        (SELECT COUNT(1) FROM user_qualification_assignments uqa WHERE uqa.user_id = u.id AND uqa.qualification IN (:requiredQualifications)) = :totalRequiredQualifications 
    ORDER BY 
      (SELECT COUNT(1) FROM assessments a WHERE a.allocated_to_user_id = u.id AND a.submitted_at IS NULL) ASC 
    LIMIT 1
    """,
    nativeQuery = true
  )
  fun findQualifiedAssessorWithLeastPendingAllocations(requiredQualifications: List<String>, totalRequiredQualifications: Long): UserEntity?
}

@Entity
@Table(name = "users")
data class UserEntity(
  @Id
  val id: UUID,
  val name: String,
  val deliusUsername: String,
  val deliusStaffIdentifier: Long,
  @OneToMany(mappedBy = "createdByUser")
  val applications: MutableList<ApplicationEntity>,
  @OneToMany(mappedBy = "user")
  val roles: MutableList<UserRoleAssignmentEntity>,
  @OneToMany(mappedBy = "user")
  val qualifications: MutableList<UserQualificationAssignmentEntity>
) {
  fun hasRole(userRole: UserRole) = roles.any { it.role == userRole }
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
  val role: UserRole
)

enum class UserRole {
  ASSESSOR,
  MATCHER,
  MANAGER,
  WORKFLOW_MANAGER,
  APPLICANT,
  ADMIN
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
  val qualification: UserQualification
)

enum class UserQualification {
  WOMENS,
  PIPE
}
