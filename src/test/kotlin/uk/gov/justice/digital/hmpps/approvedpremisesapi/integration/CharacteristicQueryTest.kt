package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1CharacteristicRepository

class CharacteristicQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realCharacteristicRepository: Cas1CharacteristicRepository

  @Test
  fun `findAllForRoomId returns all the characteristics for a roomId`() {
    val premises = givenAnApprovedPremises()

    val char1 = cas1CharacteristicEntityFactory.produceAndPersist()
    val char2 = cas1CharacteristicEntityFactory.produceAndPersist()
    val char3 = cas1CharacteristicEntityFactory.produceAndPersist()

    val roomCharacteristics = mutableListOf(char1, char2, char3)

    // otherCharacteristics
    mutableListOf(
      cas1CharacteristicEntityFactory.produceAndPersist(),
      cas1CharacteristicEntityFactory.produceAndPersist(),
    )

    val room = cas1RoomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCharacteristics(roomCharacteristics)
    }

    assertThat(realCharacteristicRepository.findAllForRoomId(room.id)).containsExactlyInAnyOrder(
      char1,
      char2,
      char3,
    )
  }
}
