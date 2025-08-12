package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddStaffDetailResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockNotFoundStaffDetailByStaffCodeCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockNotFoundStaffDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulStaffDetailByCodeCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ApStaffUserSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import java.time.OffsetDateTime

class UsersBasicSeedJobTest : SeedTestBase() {
  @Test
  fun `Seeding a non existent user logs an error, using delius username `() {
    apDeliusContextMockNotFoundStaffDetailCall("INVALID-USER")

    seed(
      SeedFileType.usersBasic,
      apStaffUserSeedCsvRowsToCsv(
        listOf(
          ApStaffUserSeedCsvRow(deliusUsername = "INVALID-USER", staffCode = null),
        ),
      ),
    )

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message.contains("Error on row 1:") &&
        it.throwable != null &&
        it.throwable.cause != null &&
        it.throwable.cause!!.message!!.contains("Could not find staff record for user INVALID-USER")
    }
  }

  @Test
  fun `Seeding a real but currently unknown user succeeds, using delius username `() {
    val probationRegion = givenAProbationRegion()

    val probationRegionDeliusMapping = probationAreaProbationRegionMappingFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    apDeliusContextAddStaffDetailResponse(
      StaffDetailFactory.staffDetail(
        deliusUsername = "UNKNOWN-USER",
        probationArea = ProbationArea(
          code = probationRegionDeliusMapping.probationAreaDeliusCode,
          description = "description",
        ),
      ),
    )

    seed(
      SeedFileType.usersBasic,
      apStaffUserSeedCsvRowsToCsv(
        listOf(
          ApStaffUserSeedCsvRow(deliusUsername = "unknown-user", staffCode = null),
        ),
      ),
    )

    val persistedUser = userRepository.findByDeliusUsername("UNKNOWN-USER")

    assertThat(persistedUser).isNotNull

    assertThat(logEntries).anyMatch {
      it.level == "info" &&
        it.message.contains("User record for 'UNKNOWN-USER' created")
    }
  }

  @Test fun `Seeding an existing user leaves roles and qualifications untouched, using delius username`() {
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername("PRE-EXISTING-USER")
      withUpdatedAt(OffsetDateTime.now().minusDays(3))
      withYieldedProbationRegion { givenAProbationRegion() }
    }

    val roleEntities = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_CRU_MEMBER).map { role ->
      userRoleAssignmentEntityFactory.produceAndPersist {
        withUser(user)
        withRole(role)
      }
    }
    user.roles.addAll(roleEntities)

    val qualificationEntities = listOf(UserQualification.PIPE, UserQualification.EMERGENCY).map { qualification ->
      userQualificationAssignmentEntityFactory.produceAndPersist {
        withUser(user)
        withQualification(qualification)
      }
    }
    user.qualifications.addAll(qualificationEntities)

    seed(
      SeedFileType.usersBasic,
      apStaffUserSeedCsvRowsToCsv(
        listOf(
          ApStaffUserSeedCsvRow(deliusUsername = "PRE-EXISTING-USER", staffCode = null),
        ),
      ),
    )

    val persistedUser = userRepository.findByDeliusUsername("PRE-EXISTING-USER")

    assertThat(persistedUser).isNotNull

    assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
      UserRole.CAS1_ASSESSOR,
      UserRole.CAS1_CRU_MEMBER,
    )
    assertThat(persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)).containsExactlyInAnyOrder(
      UserQualification.PIPE,
      UserQualification.EMERGENCY,
    )

    assertThat(logEntries).anyMatch {
      it.level == "info" &&
        it.message.contains("User record for 'PRE-EXISTING-USER' already exists. Last updated")
    }
  }

  @Test
  fun `Seeding a non existent staff user logs an error, using staff code`() {
    apDeliusContextMockNotFoundStaffDetailByStaffCodeCall("INVALID-STAFF-CODE")

    seed(
      SeedFileType.usersBasic,
      apStaffUserSeedCsvRowsToCsv(
        listOf(
          ApStaffUserSeedCsvRow(deliusUsername = null, staffCode = "INVALID-STAFF-CODE"),
        ),
      ),
    )

    assertError(
      row = 1,
      message = "Could not resolve username for staff code INVALID-STAFF-CODE",
    )
  }

  @Test
  fun `Seeding a staff member with no username logs an error, using staff code`() {
    apDeliusContextMockSuccessfulStaffDetailByCodeCall(
      StaffDetailFactory.staffDetail(
        code = "STAFF-CODE-WITHOUT-USERNAME",
        deliusUsername = null,
      ),
    )

    seed(
      SeedFileType.usersBasic,
      apStaffUserSeedCsvRowsToCsv(
        listOf(
          ApStaffUserSeedCsvRow(deliusUsername = null, staffCode = "STAFF-CODE-WITHOUT-USERNAME"),
        ),
      ),
    )

    assertError(
      row = 1,
      message = "Could not resolve username for staff code STAFF-CODE-WITHOUT-USERNAME",
    )
  }

  @Test
  fun `Seeding a real but currently unknown user succeeds, using staff code`() {
    val probationRegion = givenAProbationRegion()

    val probationRegionDeliusMapping = probationAreaProbationRegionMappingFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    apDeliusContextMockSuccessfulStaffDetailByCodeCall(
      StaffDetailFactory.staffDetail(
        code = "NEW-USER-STAFF-CODE",
        deliusUsername = "NEW-USER-DELIUS_USERNAME",
        probationArea = ProbationArea(
          code = probationRegionDeliusMapping.probationAreaDeliusCode,
          description = "description",
        ),
      ),
    )

    apDeliusContextAddStaffDetailResponse(
      StaffDetailFactory.staffDetail(
        code = "NEW-USER-STAFF-CODE",
        deliusUsername = "NEW-USER-DELIUS_USERNAME",
        probationArea = ProbationArea(
          code = probationRegionDeliusMapping.probationAreaDeliusCode,
          description = "description",
        ),
      ),
    )

    seed(
      SeedFileType.usersBasic,
      apStaffUserSeedCsvRowsToCsv(
        listOf(
          ApStaffUserSeedCsvRow(deliusUsername = null, staffCode = "NEW-USER-STAFF-CODE"),
        ),
      ),
    )

    val persistedUser = userRepository.findByDeliusUsername("NEW-USER-DELIUS_USERNAME")

    assertThat(persistedUser).isNotNull

    assertThat(logEntries).anyMatch {
      it.level == "info" &&
        it.message.contains("User record for 'NEW-USER-DELIUS_USERNAME' created")
    }
  }

  private fun apStaffUserSeedCsvRowsToCsv(rows: List<ApStaffUserSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "deliusUsername",
        "staffCode",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.deliusUsername ?: "")
        .withQuotedField(it.staffCode ?: "")
        .newRow()
    }

    return builder.build()
  }
}
