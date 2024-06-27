package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import java.util.UUID
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

enum class AllocationType {
  Assessment, PlacementRequest, PlacementApplication
}

class UserAllocationsEngine(
  private val userRepository: UserRepository,
  private val allocationType: AllocationType,
  private val requiredQualifications: List<UserQualification>,
  private val isLao: Boolean,
  private val excludeAutoAllocations: Boolean = true,
) {
  fun getAllocatedUser(): UserEntity? {
    val userIds = this.getUserPool().map { it.id }

    return when (this.allocationType) {
      AllocationType.Assessment -> userRepository.findUserWithLeastAssessmentsPendingOrCompletedInLastWeek(userIds)
      AllocationType.PlacementRequest -> userRepository.findUserWithLeastPlacementRequestsPendingOrCompletedInLastWeek(userIds)
      AllocationType.PlacementApplication -> userRepository.findUserWithLeastPlacementApplicationsPendingOrCompletedInLastWeek(userIds)
    }
  }
  fun getUserPool(): MutableList<UserEntity> = userRepository.findAll(this.userPoolSpecification())

  private fun userPoolSpecification(): Specification<UserEntity> {
    return Specification { root: Root<UserEntity>, query: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder ->
      val predicates = mutableListOf<Predicate>(
        allActiveUsers(criteriaBuilder, root),
        allUsersWithRequiredRole(criteriaBuilder, root),
      )

      // If the offender is an LAO, we only want users with the LAO qualification
      if (isLao) {
        predicates.add(
          allUsersWithLaoQualification(criteriaBuilder, root),
        )
      }

      // If we're allocating an assessment, we don't want users with any of the specialist qualifications
      if (this.allocationType == AllocationType.Assessment && this.requiredQualifications.isEmpty()) {
        val specialistQualifications = listOf(
          UserQualification.ESAP,
          UserQualification.PIPE,
          UserQualification.EMERGENCY,
        )

        specialistQualifications.forEach {
          val userQualifications = root
            .join<UserEntity, MutableList<UserQualificationAssignmentEntity>>(UserEntity::qualifications.name, JoinType.LEFT)

          userQualifications.on(
            criteriaBuilder.equal(
              userQualifications.get<UserQualificationAssignmentEntity>(UserQualificationAssignmentEntity::qualification.name),
              it,
            ),
          )

          predicates.add(userQualifications.isNull)
        }
      } else if (this.requiredQualifications.isNotEmpty()) {
        // If there are required qualifications, then we ask for users that have ALL of the qualifications we want
        predicates.add(
          allUsersWithRequiredQualifications(root, query, criteriaBuilder),
        )
      }

      // Finally, we want to make sure the user does not have the exclusion role for our allocation type
      if (excludeAutoAllocations) {
        predicates.add(
          allUsersWithoutExclusionRole(root, criteriaBuilder),
        )
      }

      query.groupBy(root.get<UUID>("id"))
      criteriaBuilder.and(*predicates.toTypedArray())
    }
  }

  private fun allActiveUsers(criteriaBuilder: CriteriaBuilder, root: Root<UserEntity>) = criteriaBuilder.isTrue(root.get("isActive"))

  private fun allUsersWithRequiredRole(criteriaBuilder: CriteriaBuilder, root: Root<UserEntity>): Predicate {
    val requiredRole = when (this.allocationType) {
      AllocationType.Assessment -> UserRole.CAS1_ASSESSOR
      AllocationType.PlacementRequest -> UserRole.CAS1_MATCHER
      AllocationType.PlacementApplication -> UserRole.CAS1_MATCHER
    }

    val requiredRoleJoin = root
      .join<UserEntity, MutableList<UserEntity>>(UserEntity::roles.name, JoinType.LEFT)

    requiredRoleJoin.on(
      criteriaBuilder.equal(
        requiredRoleJoin.get<UserRole>(UserRoleAssignmentEntity::role.name),
        requiredRole,
      ),
    )

    return requiredRoleJoin.isNotNull
  }

  private fun allUsersWithLaoQualification(criteriaBuilder: CriteriaBuilder, root: Root<UserEntity>): Predicate {
    val userQualifications = root
      .join<UserEntity, MutableList<UserQualificationAssignmentEntity>>(UserEntity::qualifications.name, JoinType.LEFT)

    userQualifications.on(
      criteriaBuilder.equal(
        userQualifications.get<UserQualificationAssignmentEntity>(UserQualificationAssignmentEntity::qualification.name),
        UserQualification.LAO,
      ),
    )

    return userQualifications.isNotNull
  }

  private fun allUsersWithRequiredQualifications(root: Root<UserEntity>, query: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder): Predicate {
    val subQuery = query.subquery(Long::class.java)
    val userQualificationQuery = subQuery.from(UserQualificationAssignmentEntity::class.java)

    subQuery
      .select(criteriaBuilder.count(userQualificationQuery))
      .where(
        criteriaBuilder.and(
          criteriaBuilder.equal(
            userQualificationQuery
              .join<UserQualificationAssignmentEntity, UserEntity>("user", JoinType.LEFT)
              .get<Set<UUID>>("id"),
            root.get<Set<UUID>>("id"),
          ),
          userQualificationQuery
            .get<UserQualificationAssignmentEntity>(UserQualificationAssignmentEntity::qualification.name)
            .`in`(this.requiredQualifications),
        ),
      )

    return criteriaBuilder.equal(subQuery.selection, this.requiredQualifications.size.toLong())
  }

  private fun allUsersWithoutExclusionRole(root: Root<UserEntity>, criteriaBuilder: CriteriaBuilder): Predicate {
    val exclusionRole = when (this.allocationType) {
      AllocationType.Assessment -> UserRole.CAS1_EXCLUDED_FROM_ASSESS_ALLOCATION
      AllocationType.PlacementRequest -> UserRole.CAS1_EXCLUDED_FROM_MATCH_ALLOCATION
      AllocationType.PlacementApplication -> UserRole.CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION
    }

    val exclusionRoleJoin = root
      .join<UserEntity, MutableList<UserEntity>>(UserEntity::roles.name, JoinType.LEFT)

    exclusionRoleJoin.on(
      criteriaBuilder.equal(
        exclusionRoleJoin.get<UserRole>(UserRoleAssignmentEntity::role.name),
        exclusionRole,
      ),
    )

    return exclusionRoleJoin.isNull
  }
}
