package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import java.util.UUID

@SuppressWarnings("LongParameterList")
fun hasQualificationsAndRoles(
  qualifications: List<UserQualification>?,
  roles: List<UserRole>?,
  region: UUID?,
  apArea: UUID?,
  cruManagementArea: UUID?,
  showOnlyActive: Boolean = false,
): Specification<UserEntity> {
  return Specification { root: Root<UserEntity>, query: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder ->
    val predicates = mutableListOf<Predicate>()

    if (qualifications?.isNotEmpty() == true) {
      val userQualifications = root
        .join<UserEntity, MutableList<UserQualificationAssignmentEntity>>(UserEntity::qualifications.name)
        .get<UserQualificationAssignmentEntity>(UserQualificationAssignmentEntity::qualification.name)

      predicates.add(
        criteriaBuilder.and(
          userQualifications.`in`(qualifications),
        ),
      )
    }

    if (roles?.isNotEmpty() == true) {
      val userRoles = root
        .join<UserEntity, MutableList<UserEntity>>(UserEntity::roles.name)
        .get<UserRole>(UserRoleAssignmentEntity::role.name)

      predicates.add(
        criteriaBuilder.and(
          userRoles.`in`(roles),
        ),
      )
    }

    if (showOnlyActive) {
      predicates.add(
        criteriaBuilder.and(
          criteriaBuilder.isTrue(root.get("isActive")),
        ),
      )
    }

    if (region != null) {
      val probationRegionID = root.get<ProbationRegionEntity>("probationRegion").get<UUID>("id")

      predicates.add(
        criteriaBuilder.and(
          criteriaBuilder.equal(probationRegionID, region),
        ),
      )
    }

    if (apArea != null) {
      val apAreaID = root.get<ApArea>("apArea").get<UUID>("id")

      predicates.add(
        criteriaBuilder.and(
          criteriaBuilder.equal(apAreaID, apArea),
        ),
      )
    }

    if (cruManagementArea != null) {
      val cruManagementAreaId = root.get<Cas1CruManagementArea>("cruManagementArea").get<UUID>("id")

      predicates.add(
        criteriaBuilder.and(
          criteriaBuilder.equal(cruManagementAreaId, cruManagementArea),
        ),
      )
    }

    query.distinct(true)
    criteriaBuilder.and(*predicates.toTypedArray())
  }
}
