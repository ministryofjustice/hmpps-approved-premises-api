package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`

class MigrateCas1UserApAreaTest : MigrationJobTestBase() {

  @Test
  fun `Populate ap area and team codes`() {
    val apArea = apAreaEntityFactory.produceAndPersist()

    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withDefaults()
      withApArea(apArea)
    }

    `Given a User`(
      probationRegion = probationRegion,
      staffUserDetailsConfigBlock = {
        this.withTeams(
          listOf(StaffUserTeamMembershipFactory().withCode("abc").produce()),
        )
      },
    ) { userEntity, _ ->

      userEntity.apArea = null
      userRepository.save(userEntity)
      assertThat(userEntity.apArea).isNull()
      assertThat(userEntity.teamCodes).isNull()

      migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillUserApArea)

      val updatedUser = userRepository.findByIdOrNull(userEntity.id)!!
      assertThat(updatedUser.apArea!!.id).isEqualTo(apArea.id)
      assertThat(updatedUser.teamCodes).isEqualTo(listOf("abc"))
    }
  }

  @Test
  fun `If staff details not found for user (404), fall back to probation region ap area and set team codes to an empty list`() {
    val apArea = apAreaEntityFactory.produceAndPersist()

    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withDefaults()
      withApArea(apArea)
    }

    `Given a User`(
      probationRegion = probationRegion,
      staffUserDetailsConfigBlock = {
        this.withTeams(
          listOf(StaffUserTeamMembershipFactory().withCode("abc").produce()),
        )
      },
      mockStaffUserDetailsCall = false,
    ) { userEntity, _ ->

      userEntity.apArea = null
      userRepository.save(userEntity)
      assertThat(userEntity.apArea).isNull()
      assertThat(userEntity.teamCodes).isNull()

      migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillUserApArea)

      val updatedUser = userRepository.findByIdOrNull(userEntity.id)!!
      assertThat(updatedUser.apArea!!.id).isEqualTo(apArea.id)
      assertThat(updatedUser.teamCodes).isEmpty()
    }
  }
}
