package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockNotFoundStaffUserDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulStaffUserDetailsCall

class UpdateAllUsersFromCommunityApiMigrationTest : MigrationJobTestBase() {
  @Test
  fun `All users are updated from Community API with a 500ms artificial delay`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

    val userOne = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER1")
      withDeliusStaffCode("OLDCODE1")
      withProbationRegion(probationRegion)
    }

    val userTwo = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER2")
      withDeliusStaffCode("OLDCODE2")
      withProbationRegion(probationRegion)
    }

    CommunityAPI_mockSuccessfulStaffUserDetailsCall(
      StaffUserDetailsFactory()
        .withUsername(userOne.deliusUsername)
        .withStaffCode("STAFFCODE1")
        .produce(),
    )

    CommunityAPI_mockSuccessfulStaffUserDetailsCall(
      StaffUserDetailsFactory()
        .withUsername(userTwo.deliusUsername)
        .withStaffCode("STAFFCODE2")
        .produce(),
    )

    val startTime = System.currentTimeMillis()
    migrationJobService.runMigrationJob(MigrationJobType.allUsersFromCommunityApi, 1)
    val endTime = System.currentTimeMillis()

    assertThat(endTime - startTime).isGreaterThan(500 * 2)

    val userOneAfterUpdate = userRepository.findByIdOrNull(userOne.id)!!
    val userTwoAfterUpdate = userRepository.findByIdOrNull(userTwo.id)!!

    assertThat(userOneAfterUpdate.deliusStaffCode).isEqualTo("STAFFCODE1")
    assertThat(userTwoAfterUpdate.deliusStaffCode).isEqualTo("STAFFCODE2")
  }

  @Test
  fun `Failure to update individual user does not stop processing`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

    val userOne = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER1")
      withDeliusStaffCode("OLDCODE1")
      withProbationRegion(probationRegion)
    }

    val userTwo = userEntityFactory.produceAndPersist {
      withDeliusUsername("USER2")
      withDeliusStaffCode("OLDCODE2")
      withProbationRegion(probationRegion)
    }

    CommunityAPI_mockNotFoundStaffUserDetailsCall(userOne.deliusUsername)

    CommunityAPI_mockSuccessfulStaffUserDetailsCall(
      StaffUserDetailsFactory()
        .withUsername(userTwo.deliusUsername)
        .withStaffCode("STAFFCODE2")
        .produce(),
    )

    migrationJobService.runMigrationJob(MigrationJobType.allUsersFromCommunityApi)

    val userOneAfterUpdate = userRepository.findByIdOrNull(userOne.id)!!
    val userTwoAfterUpdate = userRepository.findByIdOrNull(userTwo.id)!!

    assertThat(userOneAfterUpdate.deliusStaffCode).isEqualTo("OLDCODE1")
    assertThat(userTwoAfterUpdate.deliusStaffCode).isEqualTo("STAFFCODE2")
  }
}
