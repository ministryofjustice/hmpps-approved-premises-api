package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_APPEALS_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_FUTURE_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_USER_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER

class MigrateCas1ManagerToFutureManagerTest : MigrationJobTestBase() {

  @Test
  fun `Users with manager role and updated to future manager`() {
    val userOneCas3Assessor = createUserWithRoles(
      username = "USER1",
      roles = listOf(CAS3_ASSESSOR),
    )

    val userTwoCas1FutureManager = createUserWithRoles(
      username = "USER2",
      roles = listOf(CAS1_FUTURE_MANAGER),
    )

    val userThreeCas1Manager = createUserWithRoles(
      username = "USER3",
      roles = listOf(CAS1_MANAGER),
    )

    val userFourCas1MultipleRolesButFutureManager = createUserWithRoles(
      username = "USER4",
      roles = listOf(CAS1_ASSESSOR, CAS1_MANAGER, CAS3_REPORTER),
    )

    val userFiveCas1LotsOfRolesNotManager = createUserWithRoles(
      username = "USER5",
      roles = listOf(CAS1_ASSESSOR, CAS1_USER_MANAGER, CAS3_REPORTER, CAS1_APPEALS_MANAGER),
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1ManagerToFutureManager, 1)

    val userOneCas3AssessorUpdated = userRepository.findByIdOrNull(userOneCas3Assessor.id)!!
    val userTwoCas1FutureManagerUpdated = userRepository.findByIdOrNull(userTwoCas1FutureManager.id)!!
    val userThreeCas1ManagerUpdated = userRepository.findByIdOrNull(userThreeCas1Manager.id)!!
    val userFourCas1MultipleRolesButFutureManagerUpdated = userRepository.findByIdOrNull(userFourCas1MultipleRolesButFutureManager.id)!!
    val userFiveCas1LotsOfRolesNotManagerUpdated = userRepository.findByIdOrNull(userFiveCas1LotsOfRolesNotManager.id)!!

    Assertions.assertThat(userOneCas3AssessorUpdated.getUserRoles()).containsOnly(CAS3_ASSESSOR)
    Assertions.assertThat(userTwoCas1FutureManagerUpdated.getUserRoles()).containsOnly(CAS1_FUTURE_MANAGER)
    Assertions.assertThat(userThreeCas1ManagerUpdated.getUserRoles()).containsOnly(CAS1_FUTURE_MANAGER)
    Assertions.assertThat(userFourCas1MultipleRolesButFutureManagerUpdated.getUserRoles()).containsOnly(CAS1_ASSESSOR, CAS1_FUTURE_MANAGER, CAS3_REPORTER)
    Assertions.assertThat(userFiveCas1LotsOfRolesNotManagerUpdated.getUserRoles()).containsOnly(
      CAS1_ASSESSOR,
      CAS1_USER_MANAGER,
      CAS3_REPORTER,
      CAS1_APPEALS_MANAGER,
    )
  }

  fun createUserWithRoles(username: String, roles: List<UserRole>): UserEntity {
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername(username)
      withProbationRegion(
        probationRegionEntityFactory.produceAndPersist {
          withApArea(apAreaEntityFactory.produceAndPersist())
        },
      )
    }
    roles.forEach {
      userRoleAssignmentEntityFactory.produceAndPersist {
        withUser(user)
        withRole(it)
      }
    }
    return user
  }

  fun UserEntity.getUserRoles() = this.roles.map { it.role }
}
