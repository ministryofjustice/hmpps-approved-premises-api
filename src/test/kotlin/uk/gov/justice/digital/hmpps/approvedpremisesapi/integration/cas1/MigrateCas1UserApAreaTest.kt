package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TeamFactoryDeliusContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase

class MigrateCas1UserApAreaTest : MigrationJobTestBase() {

  @Test
  fun `Populate ap area and team codes`() {
    val apArea = givenAnApArea()

    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withDefaults()
      withApArea(apArea)
    }

    givenAUser(
      probationRegion = probationRegion,
      staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "abc"))),
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
    val apArea = givenAnApArea()

    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withDefaults()
      withApArea(apArea)
    }

    givenAUser(
      probationRegion = probationRegion,
      staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "abc"))),
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
