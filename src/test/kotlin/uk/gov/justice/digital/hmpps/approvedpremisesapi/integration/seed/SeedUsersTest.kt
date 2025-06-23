package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddStaffDetailResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedUsersTest : SeedTestBase() {

  @Nested
  inner class UsersSeed {

    @Test
    fun `Seeding a user who doesn't exist in delius errors`() {
      seed(
        SeedFileType.user,
        usersSeedRowToCsv(
          listOf(
            UserRoleAssignmentsSeedCsvRowFactory()
              .withDeliusUsername("INVALID-USER")
              .produce(),
          ),
        ),
      )

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message.contains("Error on row 1:") &&
          it.throwable != null &&
          it.throwable.cause != null &&
          it.throwable.message!!.contains("Could not get user INVALID-USER") &&
          it.throwable.cause!!.message!!.contains("Could not find staff record for user INVALID-USER")
      }
    }

    @Test
    fun `Seeding a new user succeeds`() {
      val probationRegion = givenAProbationRegion()

      val probationRegionDeliusMapping = probationAreaProbationRegionMappingFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      apDeliusContextAddStaffDetailResponse(
        StaffDetailFactory.staffDetail(
          deliusUsername = "UNKNOWN-USER",
          probationArea = ProbationArea(
            code = probationRegionDeliusMapping.probationAreaDeliusCode,
            description = randomStringMultiCaseWithNumbers(10),
          ),
        ),

      )

      seed(
        SeedFileType.user,
        usersSeedRowToCsv(
          listOf(
            UserRoleAssignmentsSeedCsvRowFactory()
              .withDeliusUsername("unknown-user")
              .withRoles(listOf(UserRole.CAS1_ASSESSOR.name, UserRole.CAS1_CRU_MEMBER.name))
              .withQualifications(listOf(UserQualification.PIPE.name))
              .withRemoveExistingRowsAndQualifications(true)
              .produce(),
          ),
        ),
      )

      val persistedUser = userRepository.findByDeliusUsername("UNKNOWN-USER")

      assertThat(persistedUser).isNotNull
      assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
        UserRole.CAS1_ASSESSOR,
        UserRole.CAS1_CRU_MEMBER,
      )
      assertThat(persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)).containsExactlyInAnyOrder(
        UserQualification.PIPE,
      )
    }

    @Test
    fun `Assigning roles to an existing user with no roles succeeds`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "KNOWN-USER"),
        roles = emptyList(),
      )

      seed(
        SeedFileType.user,
        usersSeedRowToCsv(
          listOf(
            UserRoleAssignmentsSeedCsvRowFactory()
              .withDeliusUsername("KNOWN-USER")
              .withRoles(listOf("CAS1_ASSESSOR", "CAS1_CRU_MEMBER"))
              .withQualifications(listOf("PIPE"))
              .withRemoveExistingRowsAndQualifications(true)
              .produce(),
          ),
        ),
      )

      val persistedUser = userRepository.findByDeliusUsername("KNOWN-USER")

      assertThat(persistedUser).isNotNull
      assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
        UserRole.CAS1_ASSESSOR,
        UserRole.CAS1_CRU_MEMBER,
      )
      assertThat(persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)).containsExactlyInAnyOrder(
        UserQualification.PIPE,
      )
    }

    @Test
    fun `Assigning roles to an existing user with roles, don't remove existing roles`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "KNOWN-USER"),
        roles = listOf(UserRole.CAS1_FUTURE_MANAGER),
        qualifications = listOf(UserQualification.EMERGENCY),
      )

      seed(
        SeedFileType.user,
        usersSeedRowToCsv(
          listOf(
            UserRoleAssignmentsSeedCsvRowFactory()
              .withDeliusUsername("KNOWN-USER")
              .withRoles(listOf("CAS1_ASSESSOR", "CAS1_CRU_MEMBER"))
              .withQualifications(listOf("PIPE"))
              .withRemoveExistingRowsAndQualifications(false)
              .produce(),
          ),
        ),
      )

      val persistedUser = userRepository.findByDeliusUsername("KNOWN-USER")

      assertThat(persistedUser).isNotNull
      assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
        UserRole.CAS1_FUTURE_MANAGER,
        UserRole.CAS1_ASSESSOR,
        UserRole.CAS1_CRU_MEMBER,
      )
      assertThat(persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)).containsExactlyInAnyOrder(
        UserQualification.EMERGENCY,
        UserQualification.PIPE,
      )
    }

    @Test
    fun `Assigning roles to an existing user with roles, remove existing roles`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "KNOWN-USER"),
        roles = listOf(UserRole.CAS1_FUTURE_MANAGER),
        qualifications = listOf(UserQualification.EMERGENCY),
      )

      seed(
        SeedFileType.user,
        usersSeedRowToCsv(
          listOf(
            UserRoleAssignmentsSeedCsvRowFactory()
              .withDeliusUsername("KNOWN-USER")
              .withRoles(listOf("CAS1_ASSESSOR", "CAS1_CRU_MEMBER"))
              .withQualifications(listOf("PIPE"))
              .withRemoveExistingRowsAndQualifications(true)
              .produce(),
          ),
        ),
      )

      val persistedUser = userRepository.findByDeliusUsername("KNOWN-USER")

      assertThat(persistedUser).isNotNull
      assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
        UserRole.CAS1_ASSESSOR,
        UserRole.CAS1_CRU_MEMBER,
      )
      assertThat(persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)).containsExactlyInAnyOrder(
        UserQualification.PIPE,
      )
    }

    @Test
    fun `Assigning a non-existent role logs an error`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "known-user"),
        roles = emptyList(),
      )

      seed(
        SeedFileType.user,
        usersSeedRowToCsv(
          listOf(
            UserRoleAssignmentsSeedCsvRowFactory()
              .withDeliusUsername("known-user")
              .withRoles(listOf("WORKFLOW_MANAGEF"))
              .withQualifications(listOf("PIPE"))
              .withRemoveExistingRowsAndQualifications(true)
              .produce(),
          ),
        ),
      )

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Seed Job" &&
          it.throwable != null &&
          it.throwable.message != null &&
          it.throwable.message!!.contains("Unable to deserialize CSV at row: 1: Unrecognised User Role(s): [WORKFLOW_MANAGEF]")
      }
    }

    @Test
    fun `Assigning a non-existent qualification logs an error`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "known-user"),
        roles = emptyList(),
      )

      seed(
        SeedFileType.user,
        usersSeedRowToCsv(
          listOf(
            UserRoleAssignmentsSeedCsvRowFactory()
              .withDeliusUsername("known-user")
              .withQualifications(listOf("PIPEE"))
              .withRemoveExistingRowsAndQualifications(true)
              .produce(),
          ),
        ),
      )

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Seed Job" &&
          it.throwable != null &&
          it.throwable.message != null &&
          it.throwable.message!!.contains("Unable to deserialize CSV at row: 1: Unrecognised User Qualifications(s): [PIPEE]")
      }
    }

    @Test
    fun `Seeding the same user multiple times works`() {
      val probationRegion = givenAProbationRegion()

      val probationRegionDeliusMapping = probationAreaProbationRegionMappingFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }
      StaffDetailFactory.staffDetail(
        probationArea = ProbationArea(
          code = probationRegionDeliusMapping.probationAreaDeliusCode,
          description = randomStringMultiCaseWithNumbers(10),
        ),
      )

      val seedInfos = listOf(
        SeedInfo(
          staffUserDetails = StaffDetailFactory.staffDetail(
            probationArea = ProbationArea(
              code = probationRegionDeliusMapping.probationAreaDeliusCode,
              description = randomStringMultiCaseWithNumbers(10),
            ),
          ),
          roles = listOf(
            UserRole.CAS1_FUTURE_MANAGER,
            UserRole.CAS1_CRU_MEMBER,
            UserRole.CAS1_ASSESSOR,
            UserRole.CAS3_ASSESSOR,
            UserRole.CAS3_REFERRER,
          ),
          qualifications = listOf(UserQualification.EMERGENCY, UserQualification.LAO),
        ),
        SeedInfo(
          staffUserDetails = StaffDetailFactory.staffDetail(
            probationArea = ProbationArea(
              code = probationRegionDeliusMapping.probationAreaDeliusCode,
              description = randomStringMultiCaseWithNumbers(10),
            ),
          ),
          roles = listOf(UserRole.CAS1_FUTURE_MANAGER),
          qualifications = listOf(),
        ),
        SeedInfo(
          staffUserDetails = StaffDetailFactory.staffDetail(
            probationArea = ProbationArea(
              code = probationRegionDeliusMapping.probationAreaDeliusCode,
              description = randomStringMultiCaseWithNumbers(10),
            ),
          ),
          roles = listOf(),
          qualifications = listOf(UserQualification.LAO),
        ),
      )

      seedInfos.forEach {
        apDeliusContextAddStaffDetailResponse(
          it.staffUserDetails,
        )
      }

      val csv = usersSeedRowToCsv(
        seedInfos.map {
          UserRoleAssignmentsSeedCsvRowFactory()
            .withDeliusUsername(it.staffUserDetails.username!!)
            .withRoles(it.roles.map { it.name })
            .withQualifications(it.qualifications.map { it.name })
            .withRemoveExistingRowsAndQualifications(true)
            .produce()
        },
      )
      var iteration = 1
      repeat(20) {
        seed(SeedFileType.user, csv)

        seedInfos.forEach {
          val persistedUser = userRepository.findByDeliusUsername(it.staffUserDetails.username!!.uppercase())!!

          it.iterationValidations += IterationValidation(
            rolesCorrect = persistedUser.roles.map(UserRoleAssignmentEntity::role).containsAll(it.roles),
            qualificationsCorrect = persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)
              .containsAll(it.qualifications),
          )
        }

        iteration += 1
      }

      seedInfos.forEach {
        println(
          "${it.staffUserDetails.username}\n" + it.iterationValidations.mapIndexed { index, validation ->
            "   " +
              "Run $index: roles correct = ${validation.rolesCorrect}, qualifications correct = ${validation.qualificationsCorrect}"
          }
            .joinToString("\n"),
        )
      }

      seedInfos.forEach {
        it.iterationValidations.forEach {
          assertThat(it.rolesCorrect).isTrue
          assertThat(it.qualificationsCorrect).isTrue
        }
      }
    }

    @Test
    fun `Seeding the same user multiple times works for CAS1 user seed job`() {
      val probationRegion = givenAProbationRegion()

      val probationRegionDeliusMapping = probationAreaProbationRegionMappingFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      val seedInfos = listOf(
        SeedInfo(
          staffUserDetails = StaffDetailFactory.staffDetail(
            probationArea = ProbationArea(
              code = probationRegionDeliusMapping.probationAreaDeliusCode,
              description = randomStringMultiCaseWithNumbers(10),
            ),
          ),
          roles = listOf(UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_CRU_MEMBER, UserRole.CAS1_ASSESSOR),
          qualifications = listOf(UserQualification.EMERGENCY, UserQualification.LAO),
        ),
        SeedInfo(
          staffUserDetails = StaffDetailFactory.staffDetail(
            probationArea = ProbationArea(
              code = probationRegionDeliusMapping.probationAreaDeliusCode,
              description = randomStringMultiCaseWithNumbers(10),
            ),
          ),
          roles = listOf(UserRole.CAS1_FUTURE_MANAGER),
          qualifications = listOf(),
        ),
        SeedInfo(
          staffUserDetails = StaffDetailFactory.staffDetail(
            probationArea = ProbationArea(
              code = probationRegionDeliusMapping.probationAreaDeliusCode,
              description = randomStringMultiCaseWithNumbers(10),
            ),
          ),
          roles = listOf(),
          qualifications = listOf(UserQualification.LAO),
        ),
      )

      seedInfos.forEach {
        apDeliusContextAddStaffDetailResponse(
          it.staffUserDetails,
        )
      }

      val csv = usersSeedRowToCsv(
        seedInfos.map {
          UserRoleAssignmentsSeedCsvRowFactory()
            .withDeliusUsername(it.staffUserDetails.username!!)
            .withRoles(it.roles.map { it.name })
            .withQualifications(it.qualifications.map { it.name })
            .withRemoveExistingRowsAndQualifications(true)
            .produce()
        },
      )

      var iteration = 1
      repeat(20) {
        seed(SeedFileType.approvedPremisesUsers, csv)

        seedInfos.forEach {
          val persistedUser = userRepository.findByDeliusUsername(it.staffUserDetails.username!!.uppercase())!!

          it.iterationValidations += IterationValidation(
            rolesCorrect = persistedUser.roles.map(UserRoleAssignmentEntity::role).containsAll(it.roles),
            qualificationsCorrect = persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)
              .containsAll(it.qualifications),
          )
        }

        iteration += 1
      }

      seedInfos.forEach {
        println(
          "${it.staffUserDetails.username}\n" + it.iterationValidations.mapIndexed { index, validation ->
            "   Run $index: roles correct = ${validation.rolesCorrect}, qalifications correct = ${validation.qualificationsCorrect}"
          }
            .joinToString("\n"),
        )
      }

      seedInfos.forEach {
        it.iterationValidations.forEach {
          assertThat(it.rolesCorrect).isTrue
          assertThat(it.qualificationsCorrect).isTrue
        }
      }
    }

    @Test
    fun `Assigning a role to an existing user with no roles succeeds`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "KNOWN-USER"),
        roles = emptyList(),
      )

      seed(
        SeedFileType.user,
        usersSeedRowToCsv(
          listOf(
            UserRoleAssignmentsSeedCsvRowFactory()
              .withDeliusUsername("KNOWN-USER")
              .withRoles(listOf("CAS3_REPORTER"))
              .withQualifications(listOf("PIPE"))
              .withRemoveExistingRowsAndQualifications(true)
              .produce(),
          ),
        ),
      )

      val persistedUser = userRepository.findByDeliusUsername("KNOWN-USER")

      assertThat(persistedUser).isNotNull
      assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
        UserRole.CAS3_REPORTER,
      )
      assertThat(persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)).containsExactlyInAnyOrder(
        UserQualification.PIPE,
      )
    }
  }

  @Nested
  inner class Cas1UsersSeed {

    @Test
    fun `CAS 1 user seed job only overwrites roles for that service`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "MULTI-SERVICE-USER"),
        roles = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS3_ASSESSOR),
      )

      seed(
        SeedFileType.approvedPremisesUsers,
        usersSeedRowToCsv(
          listOf(
            UserRoleAssignmentsSeedCsvRowFactory()
              .withDeliusUsername("MULTI-SERVICE-USER")
              .withRoles(listOf("CAS1_CRU_MEMBER"))
              .withRemoveExistingRowsAndQualifications(true)
              .produce(),
          ),
        ),
      )

      val persistedUser = userRepository.findByDeliusUsername("MULTI-SERVICE-USER")

      assertThat(persistedUser).isNotNull
      assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
        UserRole.CAS1_CRU_MEMBER,
        UserRole.CAS3_ASSESSOR,
      )
    }

    @Test
    fun `CAS1 user seed leaves CRU Management Area Override if not specified`() {
      val area1 = givenACas1CruManagementArea(name = "area1")

      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "MULTI-SERVICE-USER"),
        roles = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS3_ASSESSOR),
        cruManagementArea = area1,
        cruManagementAreaOverride = area1,
      )

      seed(
        SeedFileType.approvedPremisesUsers,
        usersSeedRowToCsv(
          listOf(
            UserRoleAssignmentsSeedCsvRowFactory()
              .withDeliusUsername("MULTI-SERVICE-USER")
              .withRoles(listOf("CAS1_CRU_MEMBER"))
              .withRemoveExistingRowsAndQualifications(true)
              .produce(),
          ),
        ),
      )

      val persistedUser = userRepository.findByDeliusUsername("MULTI-SERVICE-USER")!!
      assertThat(persistedUser.cruManagementArea).isEqualTo(area1)
      assertThat(persistedUser.cruManagementAreaOverride).isEqualTo(area1)
    }

    @Test
    fun `CAS1 user seed updates CRU Management Area Override if specified`() {
      val area1 = givenACas1CruManagementArea(name = "area1")
      val area2 = givenACas1CruManagementArea(name = "area2")

      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "MULTI-SERVICE-USER"),
        roles = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS3_ASSESSOR),
        cruManagementArea = area1,
        cruManagementAreaOverride = area1,
      )

      seed(
        SeedFileType.approvedPremisesUsers,
        usersSeedRowToCsv(
          listOf(
            UserRoleAssignmentsSeedCsvRowFactory()
              .withDeliusUsername("MULTI-SERVICE-USER")
              .withRoles(listOf("CAS1_CRU_MEMBER"))
              .withRemoveExistingRowsAndQualifications(true)
              .withCruManagementAreaOverride("area2")
              .produce(),
          ),
        ),
      )

      val persistedUser = userRepository.findByDeliusUsername("MULTI-SERVICE-USER")!!

      assertThat(persistedUser.cruManagementArea).isEqualTo(area2)
      assertThat(persistedUser.cruManagementAreaOverride).isEqualTo(area2)
    }
  }

  @Nested
  inner class Cas3UserSeed {

    @Test
    fun `CAS 3 user seed job only overwrites roles for that service`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "MULTI-SERVICE-USER"),
        roles = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS3_REPORTER, UserRole.CAS3_ASSESSOR),
      )

      seed(
        SeedFileType.temporaryAccommodationUsers,
        usersSeedRowToCsv(
          listOf(
            UserRoleAssignmentsSeedCsvRowFactory()
              .withDeliusUsername("MULTI-SERVICE-USER")
              .withRoles(listOf("CAS3_REFERRER"))
              .withRemoveExistingRowsAndQualifications(true)
              .produce(),
          ),
        ),
      )

      val persistedUser = userRepository.findByDeliusUsername("MULTI-SERVICE-USER")

      assertThat(persistedUser).isNotNull
      assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
        UserRole.CAS1_ASSESSOR,
        UserRole.CAS3_REFERRER,
      )
    }
  }
}

private class UserRoleAssignmentsSeedCsvRowFactory : Factory<UsersSeedRow> {
  private var deliusUsername: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var roles: Yielded<List<String>> = { listOf(UserRole.CAS1_ASSESSOR.name) }
  private var qualifications: Yielded<List<String>> = { listOf(UserQualification.PIPE.name) }
  private var removeExistingRowsAndQualifications: Yielded<Boolean> = { false }
  private var cruManagementAreaOverride: Yielded<String?> = { null }

  fun withDeliusUsername(deliusUsername: String) = apply {
    this.deliusUsername = { deliusUsername }
  }

  fun withRoles(roles: List<String>) = apply {
    this.roles = { roles }
  }

  fun withQualifications(qualifications: List<String>) = apply {
    this.qualifications = { qualifications }
  }

  fun withRemoveExistingRowsAndQualifications(removeExistingRolesAndQualifications: Boolean) = apply {
    this.removeExistingRowsAndQualifications = { removeExistingRolesAndQualifications }
  }

  fun withCruManagementAreaOverride(cruManagementAreaOverride: String?) = apply {
    this.cruManagementAreaOverride = { cruManagementAreaOverride }
  }

  override fun produce() = UsersSeedRow(
    deliusUsername = this.deliusUsername(),
    roles = this.roles(),
    qualifications = this.qualifications(),
    removeExistingRolesAndQualifications = this.removeExistingRowsAndQualifications(),
    cruManagementAreaOverride = this.cruManagementAreaOverride(),
  )
}

private data class UsersSeedRow(
  val deliusUsername: String,
  val roles: List<String>,
  val qualifications: List<String>,
  val removeExistingRolesAndQualifications: Boolean,
  val cruManagementAreaOverride: String?,
)

private data class SeedInfo(
  val staffUserDetails: StaffDetail,
  val roles: List<UserRole>,
  val qualifications: List<UserQualification>,
  val iterationValidations: MutableList<IterationValidation> = mutableListOf(),
)

private data class IterationValidation(
  val rolesCorrect: Boolean,
  val qualificationsCorrect: Boolean,
)

private fun usersSeedRowToCsv(rows: List<UsersSeedRow>): String {
  val builder = CsvBuilder()
    .withUnquotedFields(
      "delius_username",
      "roles",
      "qualifications",
      "remove_existing_roles_and_qualifications",
      "cru_management_area_override",
    )
    .newRow()

  rows.forEach {
    builder
      .withQuotedField(it.deliusUsername)
      .withQuotedField(it.roles.joinToString(","))
      .withQuotedField(it.qualifications.joinToString(","))
      .withQuotedField(
        if (it.removeExistingRolesAndQualifications) {
          "YES"
        } else {
          "NO"
        },
      )
      .withQuotedField(it.cruManagementAreaOverride ?: "")
      .newRow()
  }

  return builder.build()
}
