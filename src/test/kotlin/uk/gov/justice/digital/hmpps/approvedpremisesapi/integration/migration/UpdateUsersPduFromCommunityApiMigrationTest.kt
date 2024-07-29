package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockNotFoundStaffUserDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulStaffUserDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.KeyValue
import java.time.LocalDate

class UpdateUsersPduFromCommunityApiMigrationTest : MigrationJobTestBase() {
  @Test
  fun `All users pdu are updated from Community API with a 50ms artificial delay`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

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

    CommunityAPI_mockSuccessfulStaffUserDetailsCall(
      StaffUserDetailsFactory()
        .withUsername(userOneCas3Assessor.deliusUsername)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withBorough(
                KeyValue(
                  code = "PDUCODE2",
                  description = "PDUDESCRIPTION2",
                ),
              )
              .withStartDate(LocalDate.parse("2022-06-02"))
              .produce(),
            StaffUserTeamMembershipFactory()
              .withBorough(
                KeyValue(
                  code = "PDUCODENotExistInCas",
                  description = "PDUDESCRIPTIONNotExistInCas",
                ),
              )
              .withStartDate(LocalDate.parse("2024-02-05"))
              .produce(),
            StaffUserTeamMembershipFactory()
              .withBorough(
                KeyValue(
                  code = "PDUCODE1",
                  description = "PDUDESCRIPTION1",
                ),
              )
              .withStartDate(LocalDate.parse("2024-02-05"))
              .produce(),
          ),
        )
        .produce(),
    )

    CommunityAPI_mockSuccessfulStaffUserDetailsCall(
      StaffUserDetailsFactory()
        .withUsername(userTwoCas3Referrer.deliusUsername)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withBorough(
                KeyValue(
                  code = "PDUCODE2",
                  description = "PDUDESCRIPTION2",
                ),
              )
              .withStartDate(LocalDate.parse("2020-05-19"))
              .produce(),
            StaffUserTeamMembershipFactory()
              .withBorough(
                KeyValue(
                  code = "PDUCODENotExistInCas",
                  description = "PDUDESCRIPTIONNotExistInCas",
                ),
              )
              .withStartDate(LocalDate.parse("2024-02-05"))
              .produce(),
            StaffUserTeamMembershipFactory()
              .withBorough(
                KeyValue(
                  code = "PDUCODE3",
                  description = "PDUDESCRIPTION3",
                ),
              )
              .withStartDate(LocalDate.parse("2022-09-12"))
              .produce(),
          ),
        )
        .produce(),
    )

    CommunityAPI_mockSuccessfulStaffUserDetailsCall(
      StaffUserDetailsFactory()
        .withUsername(userThreeCas1Assessor.deliusUsername)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withBorough(
                KeyValue(
                  code = "PDUCODE1",
                  description = "PDUDESCRIPTION1",
                ),
              )
              .produce(),
          ),
        )
        .produce(),
    )

    CommunityAPI_mockSuccessfulStaffUserDetailsCall(
      StaffUserDetailsFactory()
        .withUsername(userFourCas3Referrer.deliusUsername)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withBorough(
                KeyValue(
                  code = "PDUCODE1",
                  description = "PDUDESCRIPTION1",
                ),
              )
              .withStartDate(LocalDate.parse("2020-02-05"))
              .withEndDate(LocalDate.parse("2022-03-05"))
              .produce(),
            StaffUserTeamMembershipFactory()
              .withBorough(
                KeyValue(
                  code = "PDUCODE2",
                  description = "PDUDESCRIPTION2",
                ),
              )
              .withStartDate(LocalDate.parse("2023-08-07"))
              .produce(),
          ),
        )
        .produce(),
    )

    val startTime = System.currentTimeMillis()
    migrationJobService.runMigrationJob(MigrationJobType.usersPduFromCommunityApi, 1)
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
  fun `Failure to update individual user does not stop processing`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

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

    CommunityAPI_mockNotFoundStaffUserDetailsCall(userOne.deliusUsername)

    CommunityAPI_mockSuccessfulStaffUserDetailsCall(
      StaffUserDetailsFactory()
        .withUsername(userTwo.deliusUsername)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory().withBorough(
              KeyValue(
                code = "PDUCODE3",
                description = "PDUDESCRIPTION3",
              ),
            )
              .produce(),
          ),
        )
        .produce(),
    )

    migrationJobService.runMigrationJob(MigrationJobType.usersPduFromCommunityApi)

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
          it.throwable.message == "Unable to complete GET request to /secure/staff/username/USER1: 404 NOT_FOUND"
      }
  }

  @Test
  fun `When probation delivery unit not exist in CAS does not stop processing other users`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

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

    CommunityAPI_mockSuccessfulStaffUserDetailsCall(
      StaffUserDetailsFactory()
        .withUsername(userOne.deliusUsername)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory().withBorough(
              KeyValue(
                code = "PDUCODE1",
                description = "PDUDESCRIPTION1",
              ),
            )
              .produce(),
          ),
        )
        .produce(),
    )

    CommunityAPI_mockSuccessfulStaffUserDetailsCall(
      StaffUserDetailsFactory()
        .withUsername(userTwo.deliusUsername)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .withDescription("TEAM 1")
              .withBorough(
                KeyValue(
                  code = "PDUCODE5",
                  description = "PDUDESCRIPTION5",
                ),
              )
              .produce(),
          ),
        )
        .produce(),
    )

    migrationJobService.runMigrationJob(MigrationJobType.usersPduFromCommunityApi)

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
