package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

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

class Cas3UpdateUsersPduFromCommunityApiMigrationTest : MigrationJobTestBase() {
  @Test
  fun `All CAS3 users pdu are updated from Community API with a 500ms artificial delay`() {
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

    val userThree = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER3")
      withProbationRegion(probationRegion)
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(userThree)
      withRole(UserRole.CAS1_ASSESSOR)
    }

    val userFour = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER4")
      withProbationRegion(probationRegion)
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(userFour)
      withRole(UserRole.CAS3_REFERRER)
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

    CommunityAPI_mockSuccessfulStaffUserDetailsCall(
      StaffUserDetailsFactory()
        .withUsername(userThree.deliusUsername)
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
        .withUsername(userFour.deliusUsername)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory().withBorough(
              KeyValue(
                code = "PDUCODE2",
                description = "PDUDESCRIPTION2",
              ),
            )
              .produce(),
          ),
        )
        .produce(),
    )

    val startTime = System.currentTimeMillis()
    migrationJobService.runMigrationJob(MigrationJobType.cas3UsersPduFromCommunityApi, 1)
    val endTime = System.currentTimeMillis()

    Assertions.assertThat(endTime - startTime).isGreaterThan(500 * 2)

    val userOneAfterUpdate = userRepository.findByIdOrNull(userOne.id)!!
    val userTwoAfterUpdate = userRepository.findByIdOrNull(userTwo.id)!!
    val userThreeAfterUpdate = userRepository.findByIdOrNull(userThree.id)!!
    val userFourAfterUpdate = userRepository.findByIdOrNull(userFour.id)!!

    Assertions.assertThat(userOneAfterUpdate.probationDeliveryUnit?.deliusCode).isEqualTo("PDUCODE1")
    Assertions.assertThat(userTwoAfterUpdate.probationDeliveryUnit?.deliusCode).isEqualTo("PDUCODE3")
    Assertions.assertThat(userThreeAfterUpdate.probationDeliveryUnit?.deliusCode).isNull()
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

    migrationJobService.runMigrationJob(MigrationJobType.cas3UsersPduFromCommunityApi)

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
            StaffUserTeamMembershipFactory().withBorough(
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

    migrationJobService.runMigrationJob(MigrationJobType.cas3UsersPduFromCommunityApi)

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
          it.throwable.message == "Unable to find community API borough code PDUCODE5 in CAS"
      }
  }
}
