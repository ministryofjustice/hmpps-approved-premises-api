package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.specification

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root

fun hasQualificationsAndRoles(qualifications: List<UserQualification>?, roles: List<UserRole>?): Specification<UserEntity> {
  return Specification { root: Root<UserEntity>, _: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder ->
    val predicates = mutableListOf<Predicate>()

    if (qualifications?.isNotEmpty() == true) {
      val userQualifications = root
        .join<UserEntity, MutableList<UserQualificationAssignmentEntity>>(UserEntity::qualifications.name)
        .get<UserQualificationAssignmentEntity>(UserQualificationAssignmentEntity::qualification.name)

      predicates.add(
        criteriaBuilder.and(
          userQualifications.`in`(qualifications)
        )
      )
    }

    if (roles?.isNotEmpty() == true) {
      val userRoles = root
        .join<UserEntity, MutableList<UserEntity>>(UserEntity::roles.name)
        .get<UserRole>(UserRoleAssignmentEntity::role.name)

      predicates.add(
        criteriaBuilder.and(
          userRoles.`in`(roles)
        )
      )
    }

    criteriaBuilder.and(*predicates.toTypedArray())
  }
}
