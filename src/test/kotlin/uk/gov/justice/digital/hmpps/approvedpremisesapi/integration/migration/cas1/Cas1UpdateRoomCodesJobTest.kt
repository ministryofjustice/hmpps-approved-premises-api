package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremisesRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService

class Cas1UpdateRoomCodesJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Test
  fun `Update room codes`() {
    val premises = givenAnApprovedPremises(
      qCode = "Q001",
    )

    val room1WithCorrectCode = givenAnApprovedPremisesRoom(
      premises = premises,
      code = "Q001-1",
      name = "1",
    )

    val room2WithIncorrectCode = givenAnApprovedPremisesRoom(
      premises = premises,
      code = "Q002-2",
      name = "2",
    )

    val room3WithIncorrectCode = givenAnApprovedPremisesRoom(
      premises = premises,
      code = "3",
      name = "3",
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1RoomCodes)

    assertThat(roomRepository.findByIdOrNull(room1WithCorrectCode.id)!!.code).isEqualTo("Q001-1")
    assertThat(roomRepository.findByIdOrNull(room2WithIncorrectCode.id)!!.code).isEqualTo("Q001-2")
    assertThat(roomRepository.findByIdOrNull(room3WithIncorrectCode.id)!!.code).isEqualTo("Q001-3")
  }
}
