package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.converter.StringListConverter
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID
import javax.persistence.Convert
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

  @Query("SELECT DISTINCT u FROM UserEntity u join u.roles r where r.role = :role and u.isActive = true")
  fun findActiveUsersWithRole(role: UserRole): List<UserEntity>

  @Query("SELECT DISTINCT u FROM UserEntity u join u.roles r where r.role in (:roles) and u.isActive = true")
  fun findActiveUsersWithRoles(roles: List<UserRole>): List<UserEntity>

  @Query("SELECT DISTINCT u FROM UserEntity u join u.qualifications q where q.qualification = :qualification and u.isActive = true")
  fun findActiveUsersWithQualification(qualification: UserQualification): List<UserEntity>

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
    SELECT u.*,
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
    WHERE u.id IN (:userIds)
    ORDER BY score ASC 
    LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findUserWithLeastPlacementRequestsPendingOrCompletedInLastWeek(userIds: List<UUID>): UserEntity?

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
          and placement_application.reallocated_at is null
          and placement_application.submitted_at is null
          and placement_application.decision is null
      ) as pendingPlacementApplications,
      (
        SELECT
          count(*)
        from
          placement_applications placement_application
        where
          placement_application.allocated_to_user_id = u.id
          and placement_application.reallocated_at is null
          and placement_application.submitted_at > current_date - interval '7' day
      ) as completedPlacementApplicationsInTheLastSevenDays,
      (
        SELECT
          count(*)
        from
          placement_applications placement_application
        where
          placement_application.allocated_to_user_id = u.id
          and placement_application.reallocated_at is null
          and placement_application.submitted_at > current_date - interval '30' day
      ) as completedPlacementApplicationsInTheLastThirtyDays,
      (
        SELECT
          count(*)
        from
          placement_requests placement_request
        where
          placement_request.allocated_to_user_id = u.id
          and placement_request.booking_id is null
          and placement_request.reallocated_at is null
          and placement_request.is_withdrawn != true
      ) as pendingPlacementRequests,
      (
        SELECT
          count(*)
        from
          placement_requests placement_request
          left join bookings booking on booking.id = placement_request.booking_id
        where
          placement_request.allocated_to_user_id = u.id
          and placement_request.booking_id is not null
          and placement_request.reallocated_at is null
          and booking.created_at > current_date - interval '7' day
      ) as completedPlacementRequestsInTheLastSevenDays,
      (
        SELECT
          count(*)
        from
          placement_requests placement_request
          left join bookings booking on booking.id = placement_request.booking_id
        where
          placement_request.allocated_to_user_id = u.id
          and placement_request.booking_id is not null
          and placement_request.reallocated_at is null
          and booking.created_at > current_date - interval '30' day
      ) as completedPlacementRequestsInTheLastThirtyDays
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
  var probationRegion: ProbationRegionEntity,
  @ManyToOne
  var probationDeliveryUnit: ProbationDeliveryUnitEntity?,
  @ManyToOne
  @JoinColumn(name = "ap_area_id")
  var apArea: ApAreaEntity?,
  @Convert(converter = StringListConverter::class)
  var teamCodes: List<String>?,
  val createdAt: OffsetDateTime?,
  @UpdateTimestamp
  var updatedAt: OffsetDateTime?,
) {
  fun hasRole(userRole: UserRole) = roles.any { it.role == userRole }
  fun hasAnyRole(vararg userRoles: UserRole) = userRoles.any(::hasRole)
  fun hasQualification(userQualification: UserQualification) =
    qualifications.any { it.qualification === userQualification }

  fun hasAllQualifications(requiredQualifications: List<UserQualification>) =
    requiredQualifications.all(::hasQualification)

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
  CAS1_LEGACY_MANAGER(ServiceName.approvedPremises),
  CAS1_FUTURE_MANAGER(ServiceName.approvedPremises),
  CAS1_WORKFLOW_MANAGER(ServiceName.approvedPremises),
  CAS1_CRU_MEMBER(ServiceName.approvedPremises),
  CAS1_APPLICANT(ServiceName.approvedPremises),
  CAS1_ADMIN(ServiceName.approvedPremises),
  CAS1_REPORT_VIEWER(ServiceName.approvedPremises),
  CAS1_EXCLUDED_FROM_ASSESS_ALLOCATION(ServiceName.approvedPremises),
  CAS1_EXCLUDED_FROM_MATCH_ALLOCATION(ServiceName.approvedPremises),
  CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION(ServiceName.approvedPremises),
  CAS1_APPEALS_MANAGER(ServiceName.approvedPremises),
  CAS3_ASSESSOR(ServiceName.temporaryAccommodation),
  CAS3_REFERRER(ServiceName.temporaryAccommodation),
  CAS3_REPORTER(ServiceName.temporaryAccommodation),
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
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as UserQualificationAssignmentEntity

    if (id != other.id) return false
    if (user.id != other.user.id) return false
    if (qualification != other.qualification) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + user.id.hashCode()
    result = 31 * result + qualification.hashCode()
    return result
  }
}

enum class UserQualification {
  WOMENS,
  PIPE,
  LAO,
  ESAP,
  EMERGENCY,
  RECOVERY_FOCUSED,
  MENTAL_HEALTH_SPECIALIST,
}

interface UserWorkload {
  fun getUserId(): UUID
  fun getPendingAssessments(): Int
  fun getCompletedAssessmentsInTheLastSevenDays(): Int
  fun getCompletedAssessmentsInTheLastThirtyDays(): Int
  fun getPendingPlacementRequests(): Int
  fun getCompletedPlacementRequestsInTheLastSevenDays(): Int
  fun getCompletedPlacementRequestsInTheLastThirtyDays(): Int
  fun getPendingPlacementApplications(): Int
  fun getCompletedPlacementApplicationsInTheLastSevenDays(): Int
  fun getCompletedPlacementApplicationsInTheLastThirtyDays(): Int
}
