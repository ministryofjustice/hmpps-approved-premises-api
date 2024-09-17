package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.toStaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_addStaffDetailResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_mockNotFoundStaffDetailCall

class UpdateAllUsersFromCommunityApiMigrationTest : MigrationJobTestBase() {
  @Test
  fun `All users are updated from Community API with a 50ms artificial delay`() {
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

    val staffuserDetail1 =
      StaffUserDetailsFactory()
        .withUsername(userOne.deliusUsername)
        .withStaffCode("STAFFCODE1")
        .produce()
    ApDeliusContext_addStaffDetailResponse(staffuserDetail1.toStaffDetail())

    val staffUserDetail2 = StaffUserDetailsFactory()
      .withUsername(userTwo.deliusUsername)
      .withStaffCode("STAFFCODE2")
      .produce()
    ApDeliusContext_addStaffDetailResponse(staffUserDetail2.toStaffDetail())

    val startTime = System.currentTimeMillis()
    migrationJobService.runMigrationJob(MigrationJobType.allUsersFromCommunityApi, 1)
    val endTime = System.currentTimeMillis()

    assertThat(endTime - startTime).isGreaterThan(50 * 2)

    val userOneAfterUpdate = userRepository.findByIdOrNull(userOne.id)!!
    val userTwoAfterUpdate = userRepository.findByIdOrNull(userTwo.id)!!

    assertThat(userOneAfterUpdate.deliusStaffCode).isEqualTo("STAFFCODE1")
    assertThat(userTwoAfterUpdate.deliusStaffCode).isEqualTo("STAFFCODE2")
  }

  @Test
  fun `Failure to update individual user does not stop processing - Update all users`() {
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

    ApDeliusContext_mockNotFoundStaffDetailCall(userOne.deliusUsername)

    val staffUserDetail = StaffUserDetailsFactory()
      .withUsername(userTwo.deliusUsername)
      .withStaffCode("STAFFCODE2")
      .produce()

    ApDeliusContext_addStaffDetailResponse(staffUserDetail.toStaffDetail())

    migrationJobService.runMigrationJob(MigrationJobType.allUsersFromCommunityApi)

    val userOneAfterUpdate = userRepository.findByIdOrNull(userOne.id)!!
    val userTwoAfterUpdate = userRepository.findByIdOrNull(userTwo.id)!!

    assertThat(userOneAfterUpdate.deliusStaffCode).isEqualTo("OLDCODE1")
    assertThat(userTwoAfterUpdate.deliusStaffCode).isEqualTo("STAFFCODE2")
  }
}
