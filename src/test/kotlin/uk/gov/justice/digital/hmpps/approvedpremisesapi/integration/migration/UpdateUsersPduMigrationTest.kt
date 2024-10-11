package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TeamFactoryDeliusContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_addStaffDetailResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_mockNotFoundStaffDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Borough
import java.time.LocalDate

class UpdateUsersPduMigrationTest : MigrationJobTestBase() {
  @Test
  fun `All users pdu are updated from AP-delius integration with a 50ms artificial delay`() {
    val probationRegion = `Given a Probation Region`()

    probationDeliveryUnitFactory.produceAndPersist {
      withDeliusCode("PDUCODE1")
      withProbationRegion(probationRegion)
    }

    val probationDeliveryUnitTwo = probationDeliveryUnitFactory.produceAndPersist {
      withDeliusCode("PDUCODE2")
      withProbationRegion(probationRegion)
    }

    probationDeliveryUnitFactory.produceAndPersist {
      withDeliusCode("PDUCODE3")
      withProbationRegion(probationRegion)
    }

    val userOneCas3Assessor = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER1")
      withProbationRegion(probationRegion)
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(userOneCas3Assessor)
      withRole(UserRole.CAS3_ASSESSOR)
    }

    val userTwoCas3Referrer = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER2")
      withProbationRegion(probationRegion)
      withProbationDeliveryUnit { probationDeliveryUnitTwo }
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(userTwoCas3Referrer)
      withRole(UserRole.CAS3_REFERRER)
    }

    val userThreeCas1Assessor = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER3")
      withProbationRegion(probationRegion)
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(userThreeCas1Assessor)
      withRole(UserRole.CAS1_ASSESSOR)
    }

    val userFourCas3Referrer = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER4")
      withProbationRegion(probationRegion)
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(userFourCas3Referrer)
      withRole(UserRole.CAS3_REFERRER)
    }

    ApDeliusContext_addStaffDetailResponse(
      StaffDetailFactory.staffDetail(
        deliusUsername = userOneCas3Assessor.deliusUsername,
        teams = listOf(
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "PDUCODE2", description = "PDUDESCRIPTION2"),
            startDate = LocalDate.parse("2022-06-02"),
          ),
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "PDUCODENotExistInCas", description = "PDUDESCRIPTIONNotExistInCas"),
            startDate = LocalDate.parse("2024-02-05"),
          ),
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "PDUCODE1", description = "PDUDESCRIPTION1"),
            startDate = LocalDate.parse("2024-02-05"),
          ),
        ),
      ),
    )

    ApDeliusContext_addStaffDetailResponse(
      StaffDetailFactory.staffDetail(
        deliusUsername = userTwoCas3Referrer.deliusUsername,
        teams = listOf(
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "PDUCODE2", description = "PDUDESCRIPTION2"),
            startDate = LocalDate.parse("2020-05-19"),
          ),
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "PDUCODENotExistInCas", description = "PDUDESCRIPTIONNotExistInCas"),
            startDate = LocalDate.parse("2024-02-05"),
          ),
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "PDUCODE3", description = "PDUDESCRIPTION3"),
            startDate = LocalDate.parse("2022-09-12"),
          ),
        ),
      ),
    )

    ApDeliusContext_addStaffDetailResponse(
      StaffDetailFactory.staffDetail(
        deliusUsername = userThreeCas1Assessor.deliusUsername,
        teams = listOf(
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "PDUCODE1", description = "PDUDESCRIPTION1"),
          ),
        ),
      ),
    )

    ApDeliusContext_addStaffDetailResponse(
      StaffDetailFactory.staffDetail(
        deliusUsername = userFourCas3Referrer.deliusUsername,
        teams = listOf(
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "PDUCODE1", description = "PDUDESCRIPTION1"),
            startDate = LocalDate.parse("2020-02-05"),
            endDate = LocalDate.parse("2022-03-05"),
          ),
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "PDUCODE2", description = "PDUDESCRIPTION2"),
            startDate = LocalDate.parse("2023-08-07"),
          ),
        ),
      ),
    )

    val startTime = System.currentTimeMillis()
    migrationJobService.runMigrationJob(MigrationJobType.usersPduByApi, 1)
    val endTime = System.currentTimeMillis()

    Assertions.assertThat(endTime - startTime).isGreaterThan(50 * 2)

    val userOneAfterUpdate = userRepository.findByIdOrNull(userOneCas3Assessor.id)!!
    val userTwoAfterUpdate = userRepository.findByIdOrNull(userTwoCas3Referrer.id)!!
    val userThreeAfterUpdate = userRepository.findByIdOrNull(userThreeCas1Assessor.id)!!
    val userFourAfterUpdate = userRepository.findByIdOrNull(userFourCas3Referrer.id)!!

    Assertions.assertThat(userOneAfterUpdate.probationDeliveryUnit?.deliusCode).isEqualTo("PDUCODE1")
    Assertions.assertThat(userTwoAfterUpdate.probationDeliveryUnit?.deliusCode).isEqualTo("PDUCODE3")
    Assertions.assertThat(userThreeAfterUpdate.probationDeliveryUnit?.deliusCode).isEqualTo("PDUCODE1")
    Assertions.assertThat(userFourAfterUpdate.probationDeliveryUnit?.deliusCode).isEqualTo("PDUCODE2")
  }

  @Test
  fun `Failure to update individual user does not stop processing - PDU migration`() {
    val probationRegion = `Given a Probation Region`()

    val probationDeliveryUnitTwo = probationDeliveryUnitFactory.produceAndPersist {
      withDeliusCode("PDUCODE2")
      withProbationRegion(probationRegion)
    }

    probationDeliveryUnitFactory.produceAndPersist {
      withDeliusCode("PDUCODE3")
      withProbationRegion(probationRegion)
    }

    val userOne = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER1")
      withProbationRegion(probationRegion)
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(userOne)
      withRole(UserRole.CAS3_ASSESSOR)
    }

    val userTwo = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER2")
      withProbationRegion(probationRegion)
      withProbationDeliveryUnit { probationDeliveryUnitTwo }
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(userTwo)
      withRole(UserRole.CAS3_REFERRER)
    }

    ApDeliusContext_mockNotFoundStaffDetailCall(userOne.deliusUsername)

    ApDeliusContext_addStaffDetailResponse(
      StaffDetailFactory.staffDetail(
        deliusUsername = userTwo.deliusUsername,
        teams = listOf(
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "PDUCODE3", description = "PDUDESCRIPTION3"),
          ),
        ),
      ),
    )

    migrationJobService.runMigrationJob(MigrationJobType.usersPduByApi)

    val userOneAfterUpdate = userRepository.findByIdOrNull(userOne.id)!!
    val userTwoAfterUpdate = userRepository.findByIdOrNull(userTwo.id)!!

    Assertions.assertThat(userOneAfterUpdate.probationDeliveryUnit?.deliusCode).isNull()
    Assertions.assertThat(userTwoAfterUpdate.probationDeliveryUnit?.deliusCode).isEqualTo("PDUCODE3")

    Assertions.assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to update user PDU. User id ${userOne.id}" &&
          it.throwable != null &&
          it.throwable.message == "Unable to complete GET request to /staff/USER1: 404 NOT_FOUND"
      }
  }

  @Test
  fun `When probation delivery unit not exist in CAS does not stop processing other users`() {
    val probationRegion = `Given a Probation Region`()

    probationDeliveryUnitFactory.produceAndPersist {
      withDeliusCode("PDUCODE1")
      withProbationRegion(probationRegion)
    }

    val userOne = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER1")
      withProbationRegion(probationRegion)
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(userOne)
      withRole(UserRole.CAS3_REFERRER)
    }

    val userTwo = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER2")
      withProbationRegion(probationRegion)
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(userTwo)
      withRole(UserRole.CAS3_REPORTER)
    }
    ApDeliusContext_addStaffDetailResponse(
      StaffDetailFactory.staffDetail(
        deliusUsername = userOne.deliusUsername,
        teams = listOf(
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "PDUCODE1", description = "PDUDESCRIPTION1"),
          ),
        ),
      ),
    )

    ApDeliusContext_addStaffDetailResponse(
      StaffDetailFactory.staffDetail(
        deliusUsername = userTwo.deliusUsername,
        teams = listOf(
          TeamFactoryDeliusContext.team(
            code = "TEAM1",
            name = "TEAM 1",
            borough = Borough(code = "PDUCODE5", description = "PDUDESCRIPTION5"),
            startDate = LocalDate.parse("2022-06-02"),
          ),
        ),
      ),
    )

    migrationJobService.runMigrationJob(MigrationJobType.usersPduByApi)

    val userOneAfterUpdate = userRepository.findByIdOrNull(userOne.id)!!
    val userTwoAfterUpdate = userRepository.findByIdOrNull(userTwo.id)!!

    Assertions.assertThat(userOneAfterUpdate.probationDeliveryUnit?.deliusCode).isEqualTo("PDUCODE1")
    Assertions.assertThat(userTwoAfterUpdate.probationDeliveryUnit?.deliusCode).isNull()

    Assertions.assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to update user PDU. User id ${userTwo.id}" &&
          it.throwable != null &&
          it.throwable.message == "PDU could not be determined for user USER2. Considered 1 teams TEAM 1 (TEAM1) with borough PDUDESCRIPTION5 (PDUCODE5)"
      }
  }
}
