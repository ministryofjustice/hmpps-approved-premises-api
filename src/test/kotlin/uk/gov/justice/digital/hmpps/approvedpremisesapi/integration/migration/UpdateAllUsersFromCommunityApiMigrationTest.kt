package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_addStaffDetailResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_mockNotFoundStaffDetailCall

class UpdateAllUsersFromCommunityApiMigrationTest : MigrationJobTestBase() {
  @Test
  fun `All users are updated from Community API with a 50ms artificial delay`() {
    val probationRegion = `Given a Probation Region`()

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
      StaffDetailFactory.staffDetail(deliusUsername = userOne.deliusUsername, code = "STAFFCODE1")

    ApDeliusContext_addStaffDetailResponse(staffuserDetail1)

    val staffUserDetail2 = StaffDetailFactory.staffDetail(deliusUsername = userTwo.deliusUsername, code = "STAFFCODE2")

    ApDeliusContext_addStaffDetailResponse(staffUserDetail2)

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
    val probationRegion = `Given a Probation Region`()

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

    val staffUserDetail = StaffDetailFactory.staffDetail(deliusUsername = userTwo.deliusUsername, code = "STAFFCODE2")

    ApDeliusContext_addStaffDetailResponse(staffUserDetail)

    migrationJobService.runMigrationJob(MigrationJobType.allUsersFromCommunityApi)

    val userOneAfterUpdate = userRepository.findByIdOrNull(userOne.id)!!
    val userTwoAfterUpdate = userRepository.findByIdOrNull(userTwo.id)!!

    assertThat(userOneAfterUpdate.deliusStaffCode).isEqualTo("OLDCODE1")
    assertThat(userTwoAfterUpdate.deliusStaffCode).isEqualTo("STAFFCODE2")
  }
}
